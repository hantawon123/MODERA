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
     * PROCESSING인 job만 골라 FAILED로 확정하고, 실제로 바꾼 행 수를 돌려준다.
     *
     * 조회 시점과 UPDATE 시점 사이에 AI 콜백이 도착해 job이 COMPLETED가 되었을 수 있다.
     * 엔티티를 읽어 markFailed()로 덮으면 정상 완료를 실패로 오염시키므로, WHERE에 status
     * 조건을 함께 걸어 "아직 PROCESSING인 경우에만" 바꾼다.
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
               AND j.status = 'PROCESSING'
            """)
    int markTimedOut(@Param("jobId") Integer jobId,
                     @Param("errorCode") String errorCode,
                     @Param("errorMessage") String errorMessage,
                     @Param("now") OffsetDateTime now);

    /**
     * 같은 이미지에 이미 진행 중이거나 완료된 job이 있는지 확인한다.
     * webhook 재전송, PEL 재처리로 IMAGE_UPLADED가 중복 도착했을 때
     * AI 호출까지 중복되는 걸 막는 용도이다.
     */
    boolean existsByImageIdAndStatusIn(Integer imageId, Collection<String> statuses);
}
