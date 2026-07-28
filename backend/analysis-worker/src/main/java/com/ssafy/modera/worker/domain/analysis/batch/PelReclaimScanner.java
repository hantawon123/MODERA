package com.ssafy.modera.worker.domain.analysis.batch;

import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.worker.domain.event.ImageAnalysisConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class PelReclaimScanner {

    /**
     * PEL에서 뺏어올 때 쓰는 전용 컨슈머 이름.
     * 로그·XPENDING에서 재처리분을 구분할 수 있다.
     */
    private static final String RECLAIMER = "pel-reclaimer";

    private final StringRedisTemplate redisTemplate;
    private final ImageAnalysisConsumer imageAnalysisConsumer;
    private final Duration minIdle;
    private final long maxDelivery;

    public PelReclaimScanner(StringRedisTemplate redisTemplate,
                             ImageAnalysisConsumer imageAnalysisConsumer,
                             @Value("${analysis.pel-min-idle}") Duration minIdle,
                             @Value("${analysis.pel-max-delivery}") long maxDelivery) {
        this.redisTemplate = redisTemplate;
        this.imageAnalysisConsumer = imageAnalysisConsumer;
        this.minIdle = minIdle;
        this.maxDelivery = maxDelivery;
    }

    @Scheduled(fixedDelayString = "${analysis.pel-scan-interval}")
    public void reclaimPendingMessages() {
        PendingMessages pending = redisTemplate.opsForStream().pending(
                Streams.IMAGE_ANALYSIS,
                Streams.GROUP_ANALYSIS_WORKERS,
                Range.unbounded(),
                100,
                minIdle
        );

        if (pending.isEmpty()) {
            log.debug("PEL 재처리 대상 없음");
            return;
        }
        log.info("PEL 재처리 후보 {}건", pending.size());
        for (PendingMessage message : pending) {
            String recordId = message.getIdAsString();
            // 한 건이 깨져도 나머지는 계속 처리한다(StuckJobScanner와 같은 이유).
            try {
                if (message.getTotalDeliveryCount() > maxDelivery) {
                    // 재처리를 반복해도 계속 실패한 메시지. 그대로 두면 매 주기마다 다시
                    // claim되어 영원히 돈다. XACK으로 PEL에서 빼고 로그만 남긴다.
                    // 스트림 엔트리 자체는 남으므로 원문은 XRANGE로 추적할 수 있다.
                    // (payload에 사용자 OCR 원문이 들어 있어 로그에는 남기지 않는다)
                    log.error("재전달 한도 초과로 재처리를 포기한다: id={} delivery={} — "
                                    + "원문 확인: XRANGE {} {} {}",
                            recordId, message.getTotalDeliveryCount(),
                            Streams.IMAGE_ANALYSIS, recordId, recordId);
                    acknowledge(recordId);
                    continue;
                }

                log.warn("PEL 재처리 대상: id={} consumer={} idle={} delivery={}",
                        recordId, message.getConsumerName(),
                        message.getElapsedTimeSinceLastDelivery(),
                        message.getTotalDeliveryCount());

                List<MapRecord<String, String, String>> claimed = redisTemplate.<String, String>opsForStream()
                        .claim(Streams.IMAGE_ANALYSIS,
                                Streams.GROUP_ANALYSIS_WORKERS,
                                RECLAIMER,
                                RedisStreamCommands.XClaimOptions.minIdle(minIdle).ids(recordId));

                if (claimed == null || claimed.isEmpty()) {
                    // XPENDING 조회와 claim 사이에 원래 컨슈머가 XACK했거나 다른 인스턴스가
                    // 먼저 가져간 경우. 경쟁에서 밀린 것뿐이라 정상 상황이다.
                    log.debug("claim하지 못했다(이미 다른 쪽이 처리한 것으로 보인다): id={}", recordId);
                    continue;
                }

                // 컨슈머와 같은 경로로 처리한다 — 파싱 실패 스킵·XACK·일시 오류 시 PEL 유지
                // 정책이 재처리에도 그대로 적용된다. 중복 job 가드도 handleImageUploaded 안에 있다.
                imageAnalysisConsumer.processRecord(claimed.get(0));

            } catch (Exception e) {
                log.error("PEL 재처리 중 오류 — 이 메시지는 건너뛴다: id={}", recordId, e);
            }
        }

    }
    private void acknowledge(String recordId) {
        redisTemplate.opsForStream().acknowledge(
                Streams.IMAGE_ANALYSIS, Streams.GROUP_ANALYSIS_WORKERS, recordId);
    }
}
