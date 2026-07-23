package com.ssafy.modera.api.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.contract.EventEnvelope;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.AnalysisCompletedPayload;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import io.lettuce.core.RedisCommandTimeoutException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.QueryTimeoutException;
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
    private static final Duration PROCESSED_EVENTS_TTL = Duration.ofDays(7);
    private static final String MDC_KEY_EVENT_ID = "eventId";

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
                        // block은 spring.data.redis.timeout(Lettuce 명령 타임아웃)보다 항상 짧게
                        // 유지해야 한다 — 그렇지 않으면 유휴 상태에서 새 메시지를 기다리는 동안
                        // 매번 RedisCommandTimeoutException이 발생한다.
                        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(5)),
                        StreamOffset.create(Streams.ANALYSIS_RESULT, ReadOffset.lastConsumed())
                );
                if (records != null) {
                    for (MapRecord<String, String, String> record : records) {
                        processRecord(record);
                    }
                }
            } catch (QueryTimeoutException | RedisCommandTimeoutException e) {
                // block(5초) 동안 새 메시지가 없어서 생기는 정상적인 유휴 타임아웃이다.
                // ERROR로 남기지 않고 조용히 다음 루프(재블로킹)로 넘어간다.
                log.debug("analysis-result 유휴 타임아웃(새 메시지 없음), 재시도");
            } catch (Exception e) {
                if (running) {
                    log.error("analysis-result 스트림 처리 중 오류", e);
                    sleepQuietly();
                }
            }
        }
    }

    /**
     * 이벤트 컨슈머 예외 정책:
     *  - envelope/payload 파싱 실패(영구 오류 — 재시도해도 항상 똑같이 실패)는
     *    eventId를 로그에 남기고 XACK해서 스킵한다.
     *  - 그 외(핸들러 처리 중 DB 등 일시 오류로 간주)는 eventId를 로그에 남기되
     *    XACK하지 않는다 — PEL(Pending Entries List)에 남아 재전달을 기다린다.
     *    PEL에 오래 남은 메시지를 자동으로 재할당/재처리하는 배치(XAUTOCLAIM)는
     *    아직 없다. TODO: XAUTOCLAIM 기반 재처리 배치 추가.
     */
    private void processRecord(MapRecord<String, String, String> record) {
        EventEnvelope envelope;
        try {
            envelope = EventEnvelope.fromFieldMap(record.getValue());
        } catch (Exception e) {
            log.error("analysis-result envelope 파싱 실패(영구 오류로 판단, 스킵): recordId={}", record.getId(), e);
            acknowledge(record);
            return;
        }

        MDC.put(MDC_KEY_EVENT_ID, envelope.eventId());
        try {
            if (!markProcessedIfNew(envelope.eventId())) {
                log.info("이미 처리한 이벤트라 건너뛴다: eventId={}", envelope.eventId());
            } else {
                dispatch(envelope);
            }
            acknowledge(record);
        } catch (PayloadParseException e) {
            log.error("analysis-result payload 파싱 실패(영구 오류로 판단, 스킵): eventId={}, eventType={}",
                    envelope.eventId(), envelope.eventType(), e.getCause());
            acknowledge(record);
        } catch (Exception e) {
            log.error("analysis-result 이벤트 처리 중 일시 오류로 판단, XACK 보류(재전달 대기): eventId={}, eventType={}",
                    envelope.eventId(), envelope.eventType(), e);
        } finally {
            MDC.remove(MDC_KEY_EVENT_ID);
        }
    }

    private void dispatch(EventEnvelope envelope) {
        if (EventTypes.ANALYSIS_COMPLETED.equals(envelope.eventType())) {
            AnalysisCompletedPayload payload = readPayload(envelope, AnalysisCompletedPayload.class);
            eventHandler.handleCompleted(payload);
            log.info("ANALYSIS_COMPLETED 처리 완료: eventId={}, imageId={}", envelope.eventId(), payload.imageId());
        } else if (EventTypes.ANALYSIS_FAILED.equals(envelope.eventType())) {
            AnalysisFailedPayload payload = readPayload(envelope, AnalysisFailedPayload.class);
            eventHandler.handleFailed(payload);
            log.info("ANALYSIS_FAILED 처리 완료: eventId={}, imageId={}", envelope.eventId(), payload.imageId());
        } else {
            log.warn("알 수 없는 eventType이라 무시한다: eventId={}, eventType={}", envelope.eventId(), envelope.eventType());
        }
    }

    private <T> T readPayload(EventEnvelope envelope, Class<T> type) {
        try {
            return envelope.readPayload(type, objectMapper);
        } catch (Exception e) {
            throw new PayloadParseException(e);
        }
    }

    private void acknowledge(MapRecord<String, String, String> record) {
        redisTemplate.opsForStream().acknowledge(Streams.ANALYSIS_RESULT, Streams.GROUP_API_CONSUMERS, record.getId().getValue());
    }

    /** payload JSON 자체가 깨진 경우를 "일시 오류"와 구분하기 위한 내부 마커 예외. */
    private static final class PayloadParseException extends RuntimeException {
        PayloadParseException(Throwable cause) {
            super(cause);
        }
    }

    private boolean markProcessedIfNew(String eventId) {
        Long added = redisTemplate.opsForSet().add(PROCESSED_EVENTS_KEY, eventId);
        // SET은 멤버 단위 TTL이 없어 키 전체에 건다. 쓸 때마다 갱신되는 슬라이딩 TTL이라
        // 계속 트래픽이 있으면 오래된 eventId도 같이 남지만, 최소한 무한정 쌓이지는 않는다.
        redisTemplate.expire(PROCESSED_EVENTS_KEY, PROCESSED_EVENTS_TTL);
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
