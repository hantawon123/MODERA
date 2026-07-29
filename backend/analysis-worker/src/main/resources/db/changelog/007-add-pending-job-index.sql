--liquibase formatted sql

--changeset analysis-worker:600-add-pending-job-index
-- 분석이 시작되지 못한 채 PENDING으로 남은 job을 찾는 스윕용 부분 인덱스.
--
-- 005의 stuck 인덱스는 started_at 기준인데, PENDING 행은 started_at이 아직 NULL이라
-- 그 인덱스로는 찾을 수 없다. 정상 흐름에서 PENDING은 다음 저장에서 곧바로 PROCESSING이
-- 되므로 이 인덱스가 덮는 행은 거의 항상 0건에 가깝다.
CREATE INDEX idx_analysis_job_pending ON analysis_job (queued_at)
    WHERE status = 'PENDING';
--rollback DROP INDEX idx_analysis_job_pending;
