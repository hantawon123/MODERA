package com.ssafy.modera.api.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.contract.EventEnvelope;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.AnalysisCompletedPayload;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * analysis-result 스트림을 api-consumers 그룹으로 소비한다(XREADGROUP 블로킹 루프).
 * eventId 기준으로 중복 처리를 막는다 — 같은 eventId는 항상 같은 imageId+modelVersion을
 * 담고 있으므로 eventId 단위 dedup이 곧 imageId+modelVersion 단위 dedup과 같다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisResultConsumer {

    private static final String PROCESSED_EVENTS_KEY = "modera:processed-events:analysis-result";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AnalysisResultEventHandler eventHandler;

    private volatile boolean running = true;
    private Thread consumerThread;

    @PostConstruct
    public void start() {
        ensureConsumerGroup();
        consumerThread = Thread.ofVirtual().name("analysis-result-consumer").start(this::loop);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    private void ensureConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(Streams.ANALYSIS_RESULT, ReadOffset.from("0"), Streams.GROUP_API_CONSUMERS);
        } catch (Exception e) {
            if (isBusyGroup(e)) {
                log.debug("Consumer Group이 이미 있다: stream={}, group={}", Streams.ANALYSIS_RESULT, Streams.GROUP_API_CONSUMERS);
            } else {
                throw e;
            }
        }
    }

    private boolean isBusyGroup(Exception e) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains("BUSYGROUP")) {
                return true;
            }
        }
        return false;
    }

    private void loop() {
        String consumerName = "api-consumer-" + UUID.randomUUID();
        while (running) {
            try {
                List<MapRecord<String, String, String>> records = redisTemplate.<String, String>opsForStream().read(
                        Consumer.from(Streams.GROUP_API_CONSUMERS, consumerName),
                        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(5)),
                        StreamOffset.create(Streams.ANALYSIS_RESULT, ReadOffset.lastConsumed())
                );
                if (records != null) {
                    for (MapRecord<String, String, String> record : records) {
                        processRecord(record);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    log.error("analysis-result 스트림 처리 중 오류", e);
                    sleepQuietly();
                }
            }
        }
    }

    private void processRecord(MapRecord<String, String, String> record) {
        try {
            EventEnvelope envelope = EventEnvelope.fromFieldMap(record.getValue());
            if (!markProcessedIfNew(envelope.eventId())) {
                log.info("이미 처리한 이벤트라 건너뛴다: eventId={}", envelope.eventId());
            } else if (EventTypes.ANALYSIS_COMPLETED.equals(envelope.eventType())) {
                AnalysisCompletedPayload payload = envelope.readPayload(AnalysisCompletedPayload.class, objectMapper);
                eventHandler.handleCompleted(payload);
                log.info("ANALYSIS_COMPLETED 처리 완료: imageId={}", payload.imageId());
            } else if (EventTypes.ANALYSIS_FAILED.equals(envelope.eventType())) {
                AnalysisFailedPayload payload = envelope.readPayload(AnalysisFailedPayload.class, objectMapper);
                eventHandler.handleFailed(payload);
                log.info("ANALYSIS_FAILED 처리 완료: imageId={}", payload.imageId());
            } else {
                log.warn("알 수 없는 eventType이라 무시한다: {}", envelope.eventType());
            }
        } catch (Exception e) {
            log.error("analysis-result 이벤트 처리 실패: recordId={}", record.getId(), e);
        } finally {
            redisTemplate.opsForStream().acknowledge(Streams.ANALYSIS_RESULT, Streams.GROUP_API_CONSUMERS, record.getId().getValue());
        }
    }

    private boolean markProcessedIfNew(String eventId) {
        Long added = redisTemplate.opsForSet().add(PROCESSED_EVENTS_KEY, eventId);
        return added != null && added > 0;
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(Duration.ofSeconds(1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
