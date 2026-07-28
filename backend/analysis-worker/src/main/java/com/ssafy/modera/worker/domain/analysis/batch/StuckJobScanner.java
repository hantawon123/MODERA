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
 * AI 콜백이 끝내 도착하지 않아 PROCESSING에 갇힌 job을 주기적으로 실패 확정한다.
 *
 * 이 배치가 없으면 그 job은 영원히 PROCESSING으로 남는다. 분석 파이프라인의 다른 실패 경로는
 * 전부 job을 FAILED로 남기고 ANALYSIS_FAILED를 발행하지만(요청 실패, AI가 보낸 FAILED,
 * 콜백 저장 실패), "콜백이 아예 오지 않는" 경우만은 아무도 알아채지 못한다. 사용자 화면에서는
 * 이미지가 영원히 분석 중으로 보인다.
 *
 * 실패를 확정하는 경로는 AnalysisCallbackService와 같다 — job을 FAILED로 기록하고
 * ANALYSIS_FAILED를 발행해 api-server가 read model을 갱신하게 한다.
 */
@Slf4j
@Component
public class StuckJobScanner {

    private static final String PROCESSING = "PROCESSING";
    private static final String ANALYSIS_TIMEOUT = "ANALYSIS_TIMEOUT";
    private static final String TIMEOUT_MESSAGE = "AI 분석 콜백이 도착하지 않아 타임아웃 처리했다";

    private final AnalysisJobRepository analysisJobRepository;
    private final EventPublisher eventPublisher;
    private final Duration stuckTimeout;

    public StuckJobScanner(AnalysisJobRepository analysisJobRepository,
                           EventPublisher eventPublisher,
                           @Value("${analysis.stuck-timeout}") Duration stuckTimeout) {
        this.analysisJobRepository = analysisJobRepository;
        this.eventPublisher = eventPublisher;
        this.stuckTimeout = stuckTimeout;
    }

    /**
     * fixedDelay를 쓰는 이유: 이전 실행이 끝난 뒤부터 간격을 센다. fixedRate였다면 한 사이클이
     * 주기보다 오래 걸릴 때 실행이 겹쳐 같은 후보를 동시에 처리하게 된다.
     */
    @Scheduled(fixedDelayString = "${analysis.stuck-scan-interval}")
    public void scanStuckJobs() {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(stuckTimeout);
        List<AnalysisJob> candidates = analysisJobRepository.findTop100ByStatusAndStartedAtBefore(PROCESSING, cutoff);
        if (candidates.isEmpty()) {
            return;
        }

        log.warn("타임아웃 후보 job {}건 발견: cutoff={}", candidates.size(), cutoff);
        for (AnalysisJob job : candidates) {
            // 한 건이 깨져도 나머지는 계속 처리한다. 여기서 예외가 밖으로 나가면 이번 사이클의
            // 남은 후보가 통째로 밀리고, 스케줄러 스레드에서 터진 예외라 원인도 잘 드러나지 않는다.
            try {
                failTimedOutJob(job);
            } catch (Exception e) {
                log.error("타임아웃 처리 중 오류 — 이 job은 건너뛴다: jobId={} imageId={}",
                        job.getJobId(), job.getImageId(), e);
            }
        }
    }

    private void failTimedOutJob(AnalysisJob job) {
        int updated = analysisJobRepository.markTimedOut(
                job.getJobId(), ANALYSIS_TIMEOUT, TIMEOUT_MESSAGE, OffsetDateTime.now());
        if (updated == 0) {
            // 조회 직후 콜백이 도착했거나 다른 인스턴스가 먼저 확정한 경우. 정상 상황이다.
            log.info("이미 확정된 job이라 타임아웃 처리를 건너뛴다: jobId={}", job.getJobId());
            return;
        }

        log.warn("job 타임아웃 확정: jobId={} imageId={} startedAt={}",
                job.getJobId(), job.getImageId(), job.getStartedAt());

        // AnalysisCallbackService.hasUserId와 같은 이유다. userId가 없으면 api-server가
        // read model에 잘못된 소유자로 기록하게 되므로, 틀린 값을 채우는 대신 발행을 생략한다.
        if (job.getUserId() == null) {
            log.error("job에 userId가 없어 ANALYSIS_FAILED 발행을 생략한다(003 마이그레이션 이전 행으로 추정): "
                    + "jobId={} imageId={}", job.getJobId(), job.getImageId());
            return;
        }

        eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.ANALYSIS_FAILED, 1,
                new AnalysisFailedPayload(job.getImageId(), job.getUserId(),
                        ANALYSIS_TIMEOUT, TIMEOUT_MESSAGE, true));
        log.warn("ANALYSIS_FAILED 발행(타임아웃): jobId={}", job.getJobId());
    }
}
