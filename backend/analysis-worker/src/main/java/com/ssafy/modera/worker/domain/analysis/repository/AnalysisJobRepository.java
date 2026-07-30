package com.ssafy.modera.worker.domain.analysis.repository;

import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Integer> {

    /**
     * 타임아웃 후보 조회. 상한을 두는 이유는 AI 서버가 오래 죽어 있었다면 후보가 한꺼번에
     * 쌓이기 때문이다. 한 번에 다 끌어오면 배치 한 사이클이 얼마나 걸릴지 예측할 수 없어진다.
     * 상한을 넘긴 나머지는 다음 주기에 잡힌다.
     */
    List<AnalysisJob> findTop100ByStatusAndStartedAtBefore(String status, OffsetDateTime cutoff);

    /**
     * 분석이 시작되지 못한 채 방치된 job 조회.
     *
     * PENDING 행은 `started_at`이 아직 NULL이라 위 조회에 걸리지 않는다. job을 PENDING으로
     * 저장한 직후 worker가 죽으면 그 상태로 남으므로 `queued_at`을 기준으로 찾는다.
     */
    List<AnalysisJob> findTop100ByStatusAndQueuedAtBefore(String status, OffsetDateTime cutoff);

    /**
     * 기대한 상태 그대로인 job만 골라 FAILED로 확정하고, 실제로 바꾼 행 수를 돌려준다.
     *
     * 조회 시점과 UPDATE 시점 사이에 AI 콜백이 도착해 job이 COMPLETED가 되었을 수 있다.
     * 엔티티를 읽어 markFailed()로 덮으면 정상 완료를 실패로 오염시키므로, WHERE에 status
     * 조건을 함께 걸어 "아직 그 상태인 경우에만" 바꾼다. `expectedStatus`는 호출하는 스윕에
     * 따라 `PROCESSING`(콜백 미도착) 또는 `PENDING`(분석 미시작)이 된다.
     *
     * 반환값이 1이면 이 호출이 상태를 확정한 것이므로 ANALYSIS_FAILED를 발행해도 된다.
     * 0이면 콜백이 먼저 이겼거나 다른 worker 인스턴스가 먼저 처리한 것이라 발행하지 않는다.
     * 다중 인스턴스에서 같은 job에 이벤트가 중복 발행되는 것도 이 조건으로 함께 막힌다.
     *
     * retryable=true로 고정한다. 콜백이 오지 않은 원인(AI 서버 다운·네트워크)은 시간이 지나면
     * 해소될 수 있는 종류라, 나중에 재분석 대상으로 고를 수 있어야 한다.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE AnalysisJob j
               SET j.status = 'FAILED',
                   j.errorCode = :errorCode,
                   j.errorMessage = :errorMessage,
                   j.retryable = true,
                   j.completedAt = :now
             WHERE j.jobId = :jobId
               AND j.status = :expectedStatus
            """)
    int markTimedOut(@Param("jobId") Integer jobId,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("errorCode") String errorCode,
                     @Param("errorMessage") String errorMessage,
                     @Param("now") OffsetDateTime now);

    /**
     * 같은 이미지에 이미 진행 중이거나 완료된 job이 있는지 확인한다.
     * webhook 재전송, PEL 재처리로 IMAGE_UPLADED가 중복 도착했을 때
     * AI 호출까지 중복되는 걸 막는 용도이다.
     */
    boolean existsByImageIdAndStatusIn(Integer imageId, Collection<String> statuses);

    /**
     * 자동 재시도 후보: "그 이미지의 가장 최근 job"이 FAILED + retryable인 것.
     *
     * NOT EXISTS(더 최신 job)로 최신 행만 고르는 것이 핵심이다 — 재시도 job을 만들면
     * 그 행이 최신이 되므로, 원래 FAILED 행은 별도 마킹 없이도 다음 스윕부터 자연히
     * 빠진다. 재시도가 또 실패하면 새 FAILED 행(attempt+1)이 최신이 되어 다시 후보가
     * 되고, attempt 상한에서 멈춘다.
     *
     * 나머지 조건들:
     *  - s3_key IS NULL 제외: 008 이전 행은 AI에 다시 보낼 재료가 없다.
     *  - completed_at(실패 확정 시각) < :failedBefore — 백오프. 실패 직후 바로 다시
     *    요청하면 같은 원인(AI 다운 등)으로 그대로 실패해 attempt만 태운다.
     *  - LIMIT 100: StuckJobScanner의 Top100과 같은 이유(한 사이클 시간 예측).
     */
    @Query(value = """
            SELECT j.* FROM analysis_job j
            WHERE j.status = 'FAILED'
              AND j.retryable = TRUE
              AND j.s3_key IS NOT NULL
              AND j.attempt < :maxAttempt
              AND j.completed_at < :failedBefore
              AND NOT EXISTS (
                  SELECT 1 FROM analysis_job newer
                  WHERE newer.image_id = j.image_id
                    AND newer.job_id > j.job_id
              )
            ORDER BY j.job_id
            LIMIT 100
            """, nativeQuery = true)
    List<AnalysisJob> findRetryCandidates(@Param("maxAttempt") int maxAttempt,
                                          @Param("failedBefore") OffsetDateTime failedBefore);
}
