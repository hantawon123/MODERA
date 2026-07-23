package com.ssafy.modera.worker.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.contract.EventEnvelope;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.AnalysisCompletedPayload;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import com.ssafy.modera.contract.payload.ImageUploadedPayload;
import com.ssafy.modera.worker.domain.analysis.client.AnalysisClient;
import com.ssafy.modera.worker.domain.analysis.client.AnalysisOutcome;
import com.ssafy.modera.worker.domain.analysis.client.AnalysisRequest;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisJobRepository;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisResultRepository;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisResultRow;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * image-analysis 스트림을 analysis-workers 그룹으로 소비한다(XREADGROUP 블로킹 루프).
 * web 서버가 없는 프로세스라 이 컨슈머 스레드가 non-daemon platform thread로 살아있는 동안
 * JVM이 종료되지 않는다(가상 스레드는 항상 daemon이라 이 용도로는 쓰지 않는다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageAnalysisConsumer {

    private static final String MDC_KEY_EVENT_ID = "eventId";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisClient analysisClient;
    private final EventPublisher eventPublisher;

    private volatile boolean running = true;
    private Thread consumerThread;

    @PostConstruct
    public void start() {
        ensureConsumerGroup();
        consumerThread = new Thread(this::loop, "image-analysis-consumer");
        consumerThread.start();
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
            redisTemplate.opsForStream().createGroup(Streams.IMAGE_ANALYSIS, ReadOffset.from("0"), Streams.GROUP_ANALYSIS_WORKERS);
        } catch (Exception e) {
            if (isBusyGroup(e)) {
                log.debug("Consumer Group이 이미 있다: stream={}, group={}", Streams.IMAGE_ANALYSIS, Streams.GROUP_ANALYSIS_WORKERS);
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
        String consumerName = "worker-" + UUID.randomUUID();
        while (running) {
            try {
                List<MapRecord<String, String, String>> records = redisTemplate.<String, String>opsForStream().read(
                        Consumer.from(Streams.GROUP_ANALYSIS_WORKERS, consumerName),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(5)),
                        StreamOffset.create(Streams.IMAGE_ANALYSIS, ReadOffset.lastConsumed())
                );
                if (records != null) {
                    for (MapRecord<String, String, String> record : records) {
                        processRecord(record);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    log.error("image-analysis 스트림 처리 중 오류", e);
                    sleepQuietly();
                }
            }
        }
    }

    /**
     * 이벤트 컨슈머 예외 정책(api-server의 AnalysisResultConsumer와 동일 기준):
     *  - envelope/payload 파싱 실패(영구 오류)는 eventId를 로그에 남기고 XACK해서 스킵한다.
     *  - "AI 분석 자체의 실패"(AnalysisClient 호출 실패 등)는 이 클래스의 기존 정책대로
     *    처리한다 — job을 FAILED로 기록하고 ANALYSIS_FAILED를 발행한 뒤 정상 처리로
     *    취급해 XACK한다({@link #handleImageUploaded} 내부 catch 참고).
     *  - 그 외(예: job 생성 자체가 DB 오류로 실패)는 일시 오류로 간주해 XACK하지 않고
     *    PEL에 남긴다. TODO: XAUTOCLAIM 기반 재처리 배치 추가.
     */
    private void processRecord(MapRecord<String, String, String> record) {
        EventEnvelope envelope;
        try {
            envelope = EventEnvelope.fromFieldMap(record.getValue());
        } catch (Exception e) {
            log.error("image-analysis envelope 파싱 실패(영구 오류로 판단, 스킵): recordId={}", record.getId(), e);
            acknowledge(record);
            return;
        }

        MDC.put(MDC_KEY_EVENT_ID, envelope.eventId());
        try {
            if (EventTypes.IMAGE_UPLOADED.equals(envelope.eventType())) {
                ImageUploadedPayload payload = readPayload(envelope, ImageUploadedPayload.class);
                handleImageUploaded(envelope, payload);
            } else {
                log.warn("알 수 없는 eventType이라 무시한다: eventId={}, eventType={}", envelope.eventId(), envelope.eventType());
            }
            acknowledge(record);
        } catch (PayloadParseException e) {
            log.error("image-analysis payload 파싱 실패(영구 오류로 판단, 스킵): eventId={}, eventType={}",
                    envelope.eventId(), envelope.eventType(), e.getCause());
            acknowledge(record);
        } catch (Exception e) {
            log.error("image-analysis 이벤트 처리 중 일시 오류로 판단, XACK 보류(재전달 대기): eventId={}, eventType={}",
                    envelope.eventId(), envelope.eventType(), e);
        } finally {
            MDC.remove(MDC_KEY_EVENT_ID);
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
        redisTemplate.opsForStream().acknowledge(Streams.IMAGE_ANALYSIS, Streams.GROUP_ANALYSIS_WORKERS, record.getId().getValue());
    }

    private void handleImageUploaded(EventEnvelope envelope, ImageUploadedPayload payload) {
        Integer imageId = payload.imageId();

        AnalysisJob job = AnalysisJob.builder()
                .imageId(imageId)
                .stage("ANALYSIS")
                .status("PENDING")
                .attempt(1)
                .triggerType("INITIAL")
                .queuedAt(OffsetDateTime.now())
                .build();
        analysisJobRepository.save(job);

        job.markProcessing(OffsetDateTime.now());
        analysisJobRepository.save(job);

        try {
            AnalysisOutcome outcome = analysisClient.analyze(new AnalysisRequest(payload.imageId(), payload.s3Key()));

            boolean inserted = analysisResultRepository.insert(new AnalysisResultRow(
                    job.getJobId(), imageId, outcome.ocrRawText(), outcome.ocrRefinedText(), outcome.ocrLang(),
                    outcome.ocrConfidence(), outcome.summary(), outcome.informative(), outcome.structuredType(),
                    outcome.structuredFieldsJson(), outcome.keyInformationJson(), outcome.analysisConfidence(),
                    outcome.embedding(), outcome.modelVersion(), OffsetDateTime.now().toInstant()
            ));
            if (!inserted) {
                log.info("이미 저장된 (imageId, modelVersion) 조합이라 결과 저장을 건너뛴다: imageId={}, modelVersion={}",
                        imageId, outcome.modelVersion());
            }

            job.markCompleted(outcome.modelVersion(), OffsetDateTime.now());
            analysisJobRepository.save(job);

            // ocrText는 클라이언트에 노출할 최종본이라 refined 쪽을 보낸다(raw는 worker 내부에만 남는다).
            AnalysisCompletedPayload completedPayload = new AnalysisCompletedPayload(
                    payload.imageId(), payload.userId(), outcome.summary(), outcome.ocrRefinedText(),
                    null, null, outcome.structuredFieldsJson(), "COMPLETED", outcome.modelVersion()
            );
            eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.ANALYSIS_COMPLETED, 1, completedPayload);
            log.info("ANALYSIS_COMPLETED 발행: imageId={}", imageId);
        } catch (Exception e) {
            log.error("분석 실패: imageId={}", imageId, e);
            job.markFailed("ANALYSIS_ERROR", String.valueOf(e.getMessage()), true, OffsetDateTime.now());
            analysisJobRepository.save(job);

            AnalysisFailedPayload failedPayload = new AnalysisFailedPayload(
                    payload.imageId(), payload.userId(), "ANALYSIS_ERROR", String.valueOf(e.getMessage()), true);
            eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.ANALYSIS_FAILED, 1, failedPayload);
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(Duration.ofSeconds(1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** payload JSON 자체가 깨진 경우를 "일시 오류"와 구분하기 위한 내부 마커 예외. */
    private static final class PayloadParseException extends RuntimeException {
        PayloadParseException(Throwable cause) {
            super(cause);
        }
    }
}
