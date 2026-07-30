package com.ssafy.modera.worker.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.contract.EventEnvelope;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import com.ssafy.modera.contract.payload.ImageUploadedPayload;
import com.ssafy.modera.worker.domain.analysis.client.AnalysisClient;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisJobRepository;
import io.lettuce.core.RedisCommandTimeoutException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
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
import java.util.Set;
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

    /**
     * 이 상태의 job이 이미 있으면 다시 요청하지 않는다. FAILED는 재시도 여지를 남긴다.
     */
    private static final Set<String> ACTIVE_JOB_STATUSES = Set.of("PENDING", "PROCESSING", "COMPLETED");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AnalysisJobRepository analysisJobRepository;
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
                        // block은 spring.data.redis.timeout(Lettuce 명령 타임아웃)보다 항상 짧게
                        // 유지해야 한다 — 그렇지 않으면 유휴 상태에서 새 메시지를 기다리는 동안
                        // 매번 RedisCommandTimeoutException이 발생한다.
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(5)),
                        StreamOffset.create(Streams.IMAGE_ANALYSIS, ReadOffset.lastConsumed())
                );
                if (records != null) {
                    for (MapRecord<String, String, String> record : records) {
                        processRecord(record);
                    }
                }
            } catch (QueryTimeoutException | RedisCommandTimeoutException e) {
                // block(5초) 동안 새 메시지가 없어서 생기는 정상적인 유휴 타임아웃이다.
                // ERROR로 남기지 않고 조용히 다음 루프(재블로킹)로 넘어간다.
                log.debug("image-analysis 유휴 타임아웃(새 메시지 없음), 재시도");
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
    public void processRecord(MapRecord<String, String, String> record) {
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

        // 같은 이미지에 이미 진행 중이거나 완료된 job이 있으면 다시 요청하지 않는다.
        // webhook 재전송이나 PEL 재처리로 같은 IMAGE_UPLOADED가 두 번 도착할 수 있는데,
        // 그때마다 job이 새로 생기면 AI 호출도 중복된다(analysis_result는 UNIQUE로 막히지만
        // AI 요청 비용은 그대로 나간다).
        if (analysisJobRepository.existsByImageIdAndStatusIn(imageId, ACTIVE_JOB_STATUSES)) {
            log.info("이미 진행 중이거나 완료된 분석이 있어 요청을 건너뛴다: imageId={}", imageId);
            return;
        }

        // 온디바이스 OCR은 여기서만 손에 들어온다. AI 콜백은 refined 텍스트만 돌려주므로
        // raw/lang/confidence를 결과에 남기려면 job에 실어 콜백 시점까지 들고 가야 한다.
        ImageUploadedPayload.ClientOcr clientOcr = payload.clientOcr();

        AnalysisJob job = AnalysisJob.builder()
                .imageId(imageId)
                .userId(payload.userId())
                .stage("FULL")
                .status("PENDING")
                .attempt(1)
                .triggerType("INITIAL")
                .queuedAt(OffsetDateTime.now())
                // 재시도(AnalysisRetryScanner)가 AI에 다시 요청할 때 쓸 재료.
                // worker는 modera_api를 못 보므로 이 시점에 실어두는 게 유일한 기회다.
                .s3Key(payload.s3Key())
                .clientOcrRawText(clientOcr == null ? null : clientOcr.rawText())
                .clientOcrLang(clientOcr == null ? null : clientOcr.lang())
                .clientOcrConfidence(toFloat(clientOcr == null ? null : clientOcr.confidence()))
                .build();
        try {
            analysisJobRepository.save(job);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateActiveJob(imageId)) {
                // uq_analysis_job_active가 잡은 경합이다. 위 조회 가드를 통과한 뒤 INSERT 사이에
                // 다른 스레드(또는 인스턴스)가 같은 이미지의 job을 먼저 만들었다.
                // 이건 실패가 아니라 "다른 쪽이 이미 처리 중"이므로 정상 스킵으로 취급한다.
                // 예외를 그대로 올리면 일시 오류로 분류돼 XACK되지 않고 PEL에 남아, 재처리를
                // 반복하다 한도 초과로 폐기된다. ANALYSIS_FAILED를 발행해도 안 된다 —
                // 실제로는 분석이 정상 진행 중인데 사용자에게 실패로 보인다.
                log.info("동시에 생성된 job이 있어 요청을 건너뛴다: imageId={}", imageId);
                return;
            }
            throw e;
        }

        job.markProcessing(OffsetDateTime.now());
        analysisJobRepository.save(job);

        try {
            // 요청 보내고 끝
            // 결과는 AI가 콜백으로 보내며 AnalysisCallbackService가 처리
            analysisClient.requestAnalysis(job, payload.s3Key(), payload.clientOcr());
        } catch (Exception e) {
            log.error("분석 실패: imageId={}", imageId, e);
            job.markFailed("ANALYSIS_REQUEST_ERROR", String.valueOf(e.getMessage()), true, OffsetDateTime.now());
            analysisJobRepository.save(job);

            AnalysisFailedPayload failedPayload = new AnalysisFailedPayload(
                    imageId, payload.userId(), "ANALYSIS_REQUEST_ERROR", String.valueOf(e.getMessage()), true);
            eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.ANALYSIS_FAILED, 1, failedPayload);
        }
    }

    /**
     * 제약 위반이 "활성 job 중복" 때문인지 확인한다.
     *
     * 인덱스 이름이나 예외 메시지를 문자열로 비교하는 대신 상태를 다시 조회한다 —
     * 정말 다른 쪽이 job을 만들었다면 지금 조회하면 보인다. NOT NULL 위반 같은 다른
     * 제약 위반은 여기서 false가 되어 원래대로 예외가 올라간다.
     *
     * 인덱스는 PENDING·PROCESSING만 덮지만 조회는 COMPLETED까지 본다. 경쟁에서 이긴 쪽이
     * 그 사이 분석을 끝냈을 수 있기 때문이다.
     */
    private boolean isDuplicateActiveJob(Integer imageId) {
        return analysisJobRepository.existsByImageIdAndStatusIn(imageId, ACTIVE_JOB_STATUSES);
    }

    /** ClientOcr.confidence는 Double, analysis_job.client_ocr_confidence는 REAL이다. */
    private Float toFloat(Double value) {
        return value == null ? null : value.floatValue();
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
