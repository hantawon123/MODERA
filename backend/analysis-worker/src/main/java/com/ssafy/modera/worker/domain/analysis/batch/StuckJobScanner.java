package com.ssafy.modera.worker.domain.analysis.batch;

import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisJobRepository;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 종료 상태에 도달하지 못한 채 방치된 job을 주기적으로 실패 확정한다.
 *
 * 두 종류를 훑는다.
 * <ul>
 *   <li>{@code PROCESSING} — AI에 분석을 요청했지만 콜백이 끝내 도착하지 않은 job.
 *       {@code started_at} 기준으로 찾는다.</li>
 *   <li>{@code PENDING} — job 행만 만들어진 채 분석이 시작되지 못한 job. 저장 직후 worker가
 *       죽으면 {@code started_at}이 NULL이라 위 조회에 걸리지 않으므로 {@code queued_at}
 *       기준으로 찾는다.</li>
 * </ul>
 *
 * 이 배치가 없으면 두 경우 모두 아무도 알아채지 못한다. 분석 파이프라인의 다른 실패 경로는
 * 전부 job을 FAILED로 남기고 ANALYSIS_FAILED를 발행하지만(요청 실패, AI가 보낸 FAILED,
 * 콜백 저장 실패), 이 둘만은 조용히 남아 사용자 화면에서 이미지가 영원히 분석 중으로 보인다.
 *
 * 실패를 확정하는 경로는 AnalysisCallbackService와 같다 — job을 FAILED로 기록하고
 * ANALYSIS_FAILED를 발행해 api-server가 read model을 갱신하게 한다.
 */
@Slf4j
@Component
public class StuckJobScanner {

    private static final String PENDING = "PENDING";
    private static final String PROCESSING = "PROCESSING";

    private static final String ANALYSIS_TIMEOUT = "ANALYSIS_TIMEOUT";
    private static final String TIMEOUT_MESSAGE = "AI 분석 콜백이 도착하지 않아 타임아웃 처리했다";

    /** 콜백 미도착(ANALYSIS_TIMEOUT)과 구분한다 — 분석이 시작조차 되지 않은 경우다. */
    private static final String ANALYSIS_NOT_STARTED = "ANALYSIS_NOT_STARTED";
    private static final String NOT_STARTED_MESSAGE = "job 생성 후 분석이 시작되지 못한 채 방치되어 타임아웃 처리했다";

    private final AnalysisJobRepository analysisJobRepository;
    private final EventPublisher eventPublisher;
    private final Duration stuckTimeout;
    private final Duration pendingTimeout;

    public StuckJobScanner(AnalysisJobRepository analysisJobRepository,
                           EventPublisher eventPublisher,
                           @Value("${analysis.stuck-timeout}") Duration stuckTimeout,
                           @Value("${analysis.pending-timeout}") Duration pendingTimeout) {
        this.analysisJobRepository = analysisJobRepository;
        this.eventPublisher = eventPublisher;
        this.stuckTimeout = stuckTimeout;
        this.pendingTimeout = pendingTimeout;
    }

    /**
     * fixedDelay를 쓰는 이유: 이전 실행이 끝난 뒤부터 간격을 센다. fixedRate였다면 한 사이클이
     * 주기보다 오래 걸릴 때 실행이 겹쳐 같은 후보를 동시에 처리하게 된다.
     *
     * 두 스윕이 같은 주기를 공유한다. 임계 시간만 다르고 확정 경로는 동일해서 굳이 별도
     * 스케줄로 나눌 이유가 없다.
     */
    @Scheduled(fixedDelayString = "${analysis.stuck-scan-interval}")
    public void scanStuckJobs() {
        OffsetDateTime now = OffsetDateTime.now();

        sweep(analysisJobRepository.findTop100ByStatusAndStartedAtBefore(PROCESSING, now.minus(stuckTimeout)),
                PROCESSING, ANALYSIS_TIMEOUT, TIMEOUT_MESSAGE);

        sweep(analysisJobRepository.findTop100ByStatusAndQueuedAtBefore(PENDING, now.minus(pendingTimeout)),
                PENDING, ANALYSIS_NOT_STARTED, NOT_STARTED_MESSAGE);
    }

    private void sweep(List<AnalysisJob> candidates, String expectedStatus, String errorCode, String errorMessage) {
        if (candidates.isEmpty()) {
            return;
        }

        log.warn("{} 상태로 방치된 job {}건 발견", expectedStatus, candidates.size());
        for (AnalysisJob job : candidates) {
            // 한 건이 깨져도 나머지는 계속 처리한다. 여기서 예외가 밖으로 나가면 이번 사이클의
            // 남은 후보가 통째로 밀리고, 스케줄러 스레드에서 터진 예외라 원인도 잘 드러나지 않는다.
            try {
                failStuckJob(job, expectedStatus, errorCode, errorMessage);
            } catch (Exception e) {
                log.error("타임아웃 처리 중 오류 — 이 job은 건너뛴다: jobId={} imageId={}",
                        job.getJobId(), job.getImageId(), e);
            }
        }
    }

    private void failStuckJob(AnalysisJob job, String expectedStatus, String errorCode, String errorMessage) {
        int updated = analysisJobRepository.markTimedOut(
                job.getJobId(), expectedStatus, errorCode, errorMessage, OffsetDateTime.now());
        if (updated == 0) {
            // 조회 직후 상태가 바뀌었거나 다른 인스턴스가 먼저 확정한 경우. 정상 상황이다.
            log.info("이미 확정된 job이라 타임아웃 처리를 건너뛴다: jobId={}", job.getJobId());
            return;
        }

        log.warn("job 타임아웃 확정: jobId={} imageId={} errorCode={} queuedAt={} startedAt={}",
                job.getJobId(), job.getImageId(), errorCode, job.getQueuedAt(), job.getStartedAt());

        // AnalysisCallbackService.hasUserId와 같은 이유다. userId가 없으면 api-server가
        // read model에 잘못된 소유자로 기록하게 되므로, 틀린 값을 채우는 대신 발행을 생략한다.
        if (job.getUserId() == null) {
            log.error("job에 userId가 없어 ANALYSIS_FAILED 발행을 생략한다(003 마이그레이션 이전 행으로 추정): "
                    + "jobId={} imageId={}", job.getJobId(), job.getImageId());
            return;
        }

        eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.ANALYSIS_FAILED, 1,
                new AnalysisFailedPayload(job.getImageId(), job.getUserId(),
                        errorCode, errorMessage, true, job.getTriggerType()));
        log.warn("ANALYSIS_FAILED 발행({}): jobId={}", errorCode, job.getJobId());
    }
}
