--liquibase formatted sql

--changeset analysis-worker:700-add-s3-key-to-job
-- 재분석(자동 재시도)에 필요한 원본 객체 키를 job에 실어 나른다.
--
-- worker는 modera_api에 접속하지 않으므로(스키마 경계) 재시도 시점에 s3Key를
-- 다시 조회할 곳이 없다. IMAGE_UPLOADED payload가 유일한 출처라, client_ocr_*와
-- 같은 방식으로 job 생성 시점에 컬럼으로 실어둔다(이벤트 값의 논리 참조).
--
-- NULL 허용: 이 changeset 이전에 만들어진 행은 값이 없다. 재시도 배치는
-- s3_key IS NULL인 행을 후보에서 제외한다(재시도에 필요한 재료가 없으므로).
ALTER TABLE analysis_job
    ADD COLUMN s3_key VARCHAR(255);

--rollback ALTER TABLE analysis_job DROP COLUMN s3_key;

--changeset analysis-worker:710-add-retry-candidate-index
-- 자동 재시도 스윕용 부분 인덱스. FAILED + retryable인 행만 덮으므로 크기가 작고,
-- 정상 운영에서는 거의 항상 0건에 가깝다. completed_at은 실패 확정 시각이라
-- 백오프(실패 후 일정 시간 경과) 조건까지 인덱스로 거를 수 있다.
CREATE INDEX idx_analysis_job_retry
    ON analysis_job (completed_at)
    WHERE status = 'FAILED' AND retryable = TRUE AND s3_key IS NOT NULL;

--rollback DROP INDEX idx_analysis_job_retry;
