--liquibase formatted sql

--changeset analysis-worker:400-add-stuck-job-index
-- stuck job 감지 배치(PROCESSING 상태로 오래 머문 job 조회)용 부분 인덱스.
-- PROCESSING 행은 항상 소수라 부분 인덱스로 두면 크기가 거의 0으로 유지된다.
CREATE INDEX idx_analysis_job_stuck ON analysis_job (started_at)
    WHERE status = 'PROCESSING';
--rollback DROP INDEX idx_analysis_job_stuck;