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

--changeset analysis-worker:020-job-id-image-id-to-integer
-- job_id: bigint → integer. analysis_result.job_id가 analysis_job.job_id를 참조하는
-- FK라서, 제약을 내렸다가 타입을 바꾸고 다시 건다.
ALTER TABLE analysis_result DROP CONSTRAINT analysis_result_job_id_fkey;

ALTER TABLE analysis_job ALTER COLUMN job_id TYPE integer;
ALTER TABLE analysis_result ALTER COLUMN job_id TYPE integer;

ALTER TABLE analysis_result ADD CONSTRAINT analysis_result_job_id_fkey
    FOREIGN KEY (job_id) REFERENCES analysis_job (job_id);

-- image_id: uuid → integer. uuid→integer는 캐스트 경로가 없어서(값을 의미 있게
-- 변환할 방법이 없음) 컬럼을 드롭하고 새로 만든다. 기존 행의 image_id 값은 유실됨
-- (로컬 개발 데이터 기준 — 운영 데이터가 있다면 이 방식 그대로 쓰면 안 됨).
ALTER TABLE analysis_result DROP CONSTRAINT analysis_result_image_id_model_version_key;

ALTER TABLE analysis_job DROP COLUMN image_id;
ALTER TABLE analysis_job ADD COLUMN image_id INTEGER NOT NULL DEFAULT 0;
ALTER TABLE analysis_job ALTER COLUMN image_id DROP DEFAULT;

ALTER TABLE analysis_result DROP COLUMN image_id;
ALTER TABLE analysis_result ADD COLUMN image_id INTEGER NOT NULL DEFAULT 0;
ALTER TABLE analysis_result ALTER COLUMN image_id DROP DEFAULT;

ALTER TABLE analysis_result ADD CONSTRAINT analysis_result_image_id_model_version_key
    UNIQUE (image_id, model_version);
--rollback ALTER TABLE analysis_result DROP CONSTRAINT analysis_result_image_id_model_version_key; ALTER TABLE analysis_result DROP COLUMN image_id; ALTER TABLE analysis_result ADD COLUMN image_id UUID NOT NULL; ALTER TABLE analysis_job DROP COLUMN image_id; ALTER TABLE analysis_job ADD COLUMN image_id UUID NOT NULL; ALTER TABLE analysis_result ADD CONSTRAINT analysis_result_image_id_model_version_key UNIQUE (image_id, model_version); ALTER TABLE analysis_result DROP CONSTRAINT analysis_result_job_id_fkey; ALTER TABLE analysis_result ALTER COLUMN job_id TYPE bigint; ALTER TABLE analysis_job ALTER COLUMN job_id TYPE bigint; ALTER TABLE analysis_result ADD CONSTRAINT analysis_result_job_id_fkey FOREIGN KEY (job_id) REFERENCES analysis_job (job_id);
