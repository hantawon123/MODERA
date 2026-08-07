package com.ssafy.modera.worker.domain.analysis.batch;

import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import com.ssafy.modera.contract.payload.ImageUploadedPayload;
import com.ssafy.modera.worker.domain.analysis.client.AnalysisClient;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisJobRepository;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * FAILED + retryable=true로 끝난 분석을 주기적으로 자동 재시도한다.
 *
 * 실패를 만드는 경로 4개(요청 실패, AI FAILED 콜백, 콜백 저장 실패, 타임아웃)는 전부
 * retryable을 남기지만, 지금까지 그 값을 읽는 곳이 없어 실패는 영구 종착지였다.
 * 이 배치가 그 값을 소비하는 유일한 곳이다.
 *
 * 재시도는 기존 행을 되돌리지 않고 새 job 행(attempt+1, trigger_type=RETRY)을 만든다 —
 * 실패 이력이 행으로 남아야 attempt 상한을 셀 수 있고, 후보 조회가 "이미지의 최신 job"만
 * 보므로 새 행이 생기는 순간 원래 FAILED 행은 별도 마킹 없이 후보에서 빠진다
 * (AnalysisJobRepository.findRetryCandidates 참고).
 *
 * 상한(retry-max-attempt)은 최초 시도를 포함한 총 시도 횟수다. 상한에 닿은 이미지는
 * 더 건드리지 않는다 — retryable=false와 같은 종착지가 되고, 그 뒤의 구제는 수동
 * 재분석(별도 API, 미구현) 몫이다.
 *
 * 재시도 중 api-server의 read model은 FAILED로 남는다. "재시도 시작"을 알리는 이벤트
 * 타입이 계약에 없기 때문인데, 성공하면 ANALYSIS_COMPLETED가 FAILED를 덮으므로
 * 사용자에게는 "실패였다가 어느 순간 완료"로 보인다.
 */
@Slf4j
@Component
public class AnalysisRetryScanner {

    private static final String TRIGGER_RETRY = "RETRY";
    private static final String ANALYSIS_REQUEST_ERROR = "ANALYSIS_REQUEST_ERROR";

    private final AnalysisJobRepository analysisJobRepository;
    private final AnalysisClient analysisClient;
    private final EventPublisher eventPublisher;
    private final Duration retryBackoff;
    private final int retryMaxAttempt;

    public AnalysisRetryScanner(AnalysisJobRepository analysisJobRepository,
                                AnalysisClient analysisClient,
                                EventPublisher eventPublisher,
                                @Value("${analysis.retry-backoff}") Duration retryBackoff,
                                @Value("${analysis.retry-max-attempt}") int retryMaxAttempt) {
        this.analysisJobRepository = analysisJobRepository;
        this.analysisClient = analysisClient;
        this.eventPublisher = eventPublisher;
        this.retryBackoff = retryBackoff;
        this.retryMaxAttempt = retryMaxAttempt;
    }

    /** fixedDelay인 이유는 StuckJobScanner와 같다 — 사이클이 겹치면 같은 후보를 중복 처리한다. */
    @Scheduled(fixedDelayString = "${analysis.retry-scan-interval}")
    public void scanRetryCandidates() {
        List<AnalysisJob> candidates = analysisJobRepository.findRetryCandidates(
                retryMaxAttempt, OffsetDateTime.now().minus(retryBackoff));
        if (candidates.isEmpty()) {
            return;
        }

        log.info("재시도 대상 FAILED job {}건 발견", candidates.size());
        for (AnalysisJob failed : candidates) {
            // 한 건이 깨져도 나머지는 계속 처리한다(StuckJobScanner.sweep과 같은 이유).
            try {
                retry(failed);
            } catch (Exception e) {
                log.error("재시도 처리 중 오류 — 이 job은 건너뛴다: jobId={} imageId={}",
                        failed.getJobId(), failed.getImageId(), e);
            }
        }
    }

    private void retry(AnalysisJob failed) {
        AnalysisJob retryJob = AnalysisJob.builder()
                .imageId(failed.getImageId())
                .userId(failed.getUserId())
                .stage(failed.getStage())
                .status("PENDING")
                .attempt(failed.getAttempt() + 1)
                .triggerType(failed.getTriggerType() == null
                        ? TRIGGER_RETRY : failed.getTriggerType())
                .queuedAt(OffsetDateTime.now())
                .s3Key(failed.getS3Key())
                .clientOcrRawText(failed.getClientOcrRawText())
                .clientOcrLang(failed.getClientOcrLang())
                .clientOcrConfidence(failed.getClientOcrConfidence())
                .build();
        try {
            analysisJobRepository.save(retryJob);
        } catch (DataIntegrityViolationException e) {
            // uq_analysis_job_active 경합. 다른 인스턴스의 스윕(또는 새 IMAGE_UPLOADED 처리)이
            // 같은 이미지의 활성 job을 먼저 만들었다. 그쪽이 진행 중이므로 이번엔 물러난다.
            log.info("동시에 만들어진 활성 job이 있어 재시도를 건너뛴다: imageId={}", failed.getImageId());
            return;
        }

        retryJob.markProcessing(OffsetDateTime.now());
        analysisJobRepository.save(retryJob);

        try {
            analysisClient.requestAnalysis(retryJob, failed.getS3Key(), toClientOcr(failed));
            log.info("재분석 요청: imageId={} attempt={}/{} 원인이었던 오류={}",
                    failed.getImageId(), retryJob.getAttempt(), retryMaxAttempt, failed.getErrorCode());
        } catch (Exception e) {
            // ImageAnalysisConsumer의 요청 실패 처리와 같은 경로. 여기서 FAILED로 남긴 행은
            // 최신 job이 되므로 백오프 뒤 다음 스윕이 다시 집는다(attempt 상한까지).
            log.error("재분석 요청 실패: imageId={} attempt={}", failed.getImageId(), retryJob.getAttempt(), e);
            retryJob.markFailed(ANALYSIS_REQUEST_ERROR, String.valueOf(e.getMessage()), true, OffsetDateTime.now());
            analysisJobRepository.save(retryJob);

            // userId 없는 행은 발행 생략 — AnalysisCallbackService.hasUserId와 같은 이유.
            // (s3_key 필터 때문에 실제로는 008 이후 행만 오므로 거의 항상 채워져 있다.)
            if (retryJob.getUserId() == null) {
                log.error("job에 userId가 없어 ANALYSIS_FAILED 발행을 생략한다: jobId={} imageId={}",
                        retryJob.getJobId(), retryJob.getImageId());
                return;
            }
            eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.ANALYSIS_FAILED, 1,
                    new AnalysisFailedPayload(retryJob.getImageId(), retryJob.getUserId(),
                            ANALYSIS_REQUEST_ERROR, String.valueOf(e.getMessage()), true,
                            retryJob.getTriggerType()));
        }
    }

    /** job에 실어둔 온디바이스 OCR을 요청 형태로 복원한다. 원래부터 없었다면 null. */
    private ImageUploadedPayload.ClientOcr toClientOcr(AnalysisJob job) {
        if (job.getClientOcrRawText() == null || job.getClientOcrRawText().isBlank()) {
            return null;
        }
        return new ImageUploadedPayload.ClientOcr(
                job.getClientOcrRawText(),
                job.getClientOcrLang(),
                job.getClientOcrConfidence() == null ? null : job.getClientOcrConfidence().doubleValue());
    }
}
