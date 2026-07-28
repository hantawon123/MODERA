--liquibase formatted sql

--changeset analysis-worker:200-add-user-id-to-job
-- 콜백에 userId가 없어 결과 이벤트 발행 시 필요하다.
-- job 생성 시점에 저장한다.
ALTER TABLE analysis_job ADD COLUMN user_id INTEGER;
--rollback ALTER TABLE analsysis_job DROP COLUMN user_id;