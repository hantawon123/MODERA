--liquibase formatted sql
-- modera_analysis DB 스키마 (public). analysis-worker가 단독 소유하며 api-server는 이 DB에 접속하지 않는다.
-- image_id는 image_db(다른 서비스)가 발급한 값의 논리 참조 — FK 없음.
-- pgvector(vector) 확장은 DB init 스크립트(local-infra/analysis-db)에서 이미 생성되어 있다.
-- 배치: analysis-worker/src/main/resources/db/changelog/001-init-schema.sql

--changeset analysis-worker:010-analysis-job
CREATE TABLE analysis_job (
    job_id         BIGSERIAL PRIMARY KEY,
    image_id       UUID NOT NULL,
    stage          VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    attempt        INTEGER NOT NULL DEFAULT 0,
    trigger_type   VARCHAR(20),
    error_code     VARCHAR(40),
    error_message  TEXT,
    retryable      BOOLEAN,
    model_version  VARCHAR(40),
    queued_at      TIMESTAMPTZ,
    started_at     TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ
);
--rollback DROP TABLE analysis_job;

--changeset analysis-worker:011-analysis-result
-- job_id FK는 같은 DB(analysis_job) 내부 참조라 허용. image_id는 다른 서비스 소유값의 논리 참조.
-- UNIQUE(image_id, model_version)로 ANALYSIS_COMPLETED 이벤트 재처리 시 중복 저장을 막는다.
CREATE TABLE analysis_result (
    result_id            BIGSERIAL PRIMARY KEY,
    job_id               BIGINT NOT NULL REFERENCES analysis_job,
    image_id             UUID NOT NULL,
    ocr_raw_text         TEXT,
    ocr_refined_text     TEXT,
    ocr_lang             VARCHAR(10),
    ocr_confidence       REAL,
    summary              TEXT,
    informative          BOOLEAN,
    structured_type      VARCHAR(20),
    structured_fields    JSONB,
    key_information      JSONB,
    analysis_confidence  REAL,
    embedding            vector(768),
    model_version        VARCHAR(40),
    analyzed_at          TIMESTAMPTZ,
    UNIQUE (image_id, model_version)
);
--rollback DROP TABLE analysis_result;

--changeset analysis-worker:020-job-id-to-integer
--comment Convert analysis_job/analysis_result job_id to integer (image_id is handled separately by 002-convert-image-id-to-integer.sql).
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT count(*) FROM analysis_job
--precondition-sql-check expectedResult:0 SELECT count(*) FROM analysis_result
ALTER TABLE analysis_result DROP CONSTRAINT analysis_result_job_id_fkey;

ALTER TABLE analysis_job ALTER COLUMN job_id TYPE integer;
ALTER TABLE analysis_result ALTER COLUMN job_id TYPE integer;

ALTER TABLE analysis_result ADD CONSTRAINT analysis_result_job_id_fkey
    FOREIGN KEY (job_id) REFERENCES analysis_job (job_id);
--rollback ALTER TABLE analysis_result DROP CONSTRAINT analysis_result_job_id_fkey; ALTER TABLE analysis_result ALTER COLUMN job_id TYPE bigint; ALTER TABLE analysis_job ALTER COLUMN job_id TYPE bigint; ALTER TABLE analysis_result ADD CONSTRAINT analysis_result_job_id_fkey FOREIGN KEY (job_id) REFERENCES analysis_job (job_id);
