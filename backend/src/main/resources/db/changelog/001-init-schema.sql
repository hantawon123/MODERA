--liquibase formatted sql
-- modera 스키마 v2 (API 명세 2026-07-21 + 팀 확정사항 반영)
-- 확정: 직렬 파이프라인 / 카테고리 1:N / FCM은 확장 여지만 / 영구 보관 / 마스킹 제외 / 수정 기능 없음
-- 배치: src/main/resources/db/changelog/001-init-schema.sql

--changeset modera:000-extensions dbms:postgresql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_bigm;
--rollback SELECT 1;

--changeset modera:010-users
CREATE TABLE users (
    user_id        BIGSERIAL PRIMARY KEY,
    provider       VARCHAR(10)  NOT NULL DEFAULT 'LOCAL',  -- LOCAL|GOOGLE|KAKAO (Java enum 관리, CHECK 없음)
    provider_id    VARCHAR(255),                           -- 소셜 제공자 고유 ID (LOCAL=NULL)
    login_id       VARCHAR(20)  UNIQUE,                    -- LOCAL 전용 (소셜=NULL, 검증은 Spring)
    password_hash  VARCHAR(72),                            -- bcrypt, LOCAL 전용
    email          VARCHAR(255) NOT NULL UNIQUE,
    nickname       VARCHAR(30)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_id)                         -- "구글의 이 사람"은 하나
);
--rollback DROP TABLE users;

--changeset modera:011-user-setting
CREATE TABLE user_setting (
    user_id                  BIGINT PRIMARY KEY REFERENCES users ON DELETE CASCADE,
    server_analysis_enabled  BOOLEAN NOT NULL DEFAULT true,
    network_condition        VARCHAR(10) NOT NULL DEFAULT 'WIFI_ONLY'
                             CHECK (network_condition IN ('ANY','WIFI_ONLY')),
    analysis_completion_noti BOOLEAN NOT NULL DEFAULT true,
    analysis_failure_noti    BOOLEAN NOT NULL DEFAULT true,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);
--rollback DROP TABLE user_setting;

--changeset modera:012-refresh-token
CREATE TABLE refresh_token (
    token_id    BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users ON DELETE CASCADE,
    device_id   VARCHAR(64) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, device_id)
);
--rollback DROP TABLE refresh_token;

--changeset modera:020-category
CREATE TABLE category (
    category_id  BIGSERIAL PRIMARY KEY,
    user_id      BIGINT REFERENCES users ON DELETE CASCADE,   -- NULL = 시스템('기타')
    name         VARCHAR(30) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()           -- 이미지 배정 시 갱신(7-2 최신순)
);
CREATE UNIQUE INDEX uq_category_system ON category (name) WHERE user_id IS NULL;
CREATE UNIQUE INDEX uq_category_user   ON category (user_id, name) WHERE user_id IS NOT NULL;
INSERT INTO category (user_id, name) VALUES (NULL, '기타');
--rollback DROP TABLE category;

--changeset modera:030-image
CREATE TABLE image (
    image_id            BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users ON DELETE CASCADE,
    category_id         BIGINT REFERENCES category ON DELETE RESTRICT,  -- 대분류 하나, 분석 전 NULL
    category_confidence REAL,
    file_name           VARCHAR(255) NOT NULL,
    content_hash        CHAR(64) NOT NULL,
    file_size           INTEGER NOT NULL,
    source_url          TEXT,                                  -- 4-1 url
    s3_key              VARCHAR(255),
    thumbnail_key       VARCHAR(255),
    status              VARCHAR(12) NOT NULL DEFAULT 'QUEUED'
                        CHECK (status IN ('QUEUED','PROCESSING','COMPLETED','FAILED','CANCELED')),
    uploaded_at         TIMESTAMPTZ,
    favorite            BOOLEAN NOT NULL DEFAULT false,
    title               VARCHAR(100),
    summary             TEXT,
    ocr_raw_text        TEXT,
    ocr_refined_text    TEXT,
    ocr_lang            VARCHAR(10),
    ocr_confidence      REAL,
    informative         BOOLEAN,
    structured_type     VARCHAR(20),
    structured_fields   JSONB,
    key_information     JSONB,
    analysis_confidence REAL,
    embedding           vector(768),
    model_version       VARCHAR(40),
    analyzed_at         TIMESTAMPTZ,
    last_viewed_at      TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, content_hash)
);
--rollback DROP TABLE image;

--changeset modera:031-image-indexes
CREATE INDEX ix_image_user_created  ON image (user_id, created_at DESC);
CREATE INDEX ix_image_user_cat      ON image (user_id, category_id, created_at DESC);
CREATE INDEX ix_image_user_uploaded ON image (user_id, uploaded_at DESC);
CREATE INDEX ix_image_user_viewed   ON image (user_id, last_viewed_at DESC NULLS LAST);
CREATE INDEX ix_image_user_fav      ON image (user_id, created_at DESC) WHERE favorite;
CREATE INDEX ix_image_user_status   ON image (user_id, status);
CREATE INDEX ix_image_ocr_bigm      ON image USING gin (ocr_refined_text gin_bigm_ops);
CREATE INDEX ix_image_embedding     ON image USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ix_image_price         ON image (((structured_fields->>'price')::numeric))
    WHERE jsonb_exists(structured_fields, 'price');
--rollback DROP INDEX ix_image_user_created, ix_image_user_cat, ix_image_user_uploaded, ix_image_user_viewed, ix_image_user_fav, ix_image_user_status, ix_image_ocr_bigm, ix_image_embedding, ix_image_price;

--changeset modera:040-tag
CREATE TABLE tag (
    tag_id       BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users ON DELETE CASCADE,
    name         VARCHAR(50) NOT NULL,
    embedding    vector(768),                              -- 8-3 태그 유사어 검색
    usage_count  INTEGER NOT NULL DEFAULT 0,               -- 앱 유지 + 재조정 배치 보정
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);
CREATE INDEX ix_tag_embedding  ON tag USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ix_tag_user_usage ON tag (user_id, usage_count DESC);
--rollback DROP TABLE tag;

--changeset modera:041-image-tag
CREATE TABLE image_tag (
    image_id    BIGINT NOT NULL REFERENCES image ON DELETE CASCADE,
    tag_id      BIGINT NOT NULL REFERENCES tag ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (image_id, tag_id)
);
CREATE INDEX ix_imgtag_tag ON image_tag (tag_id, image_id);
--rollback DROP TABLE image_tag;

--changeset modera:050-analysis-job
CREATE TABLE analysis_job (
    job_id        BIGSERIAL PRIMARY KEY,                   -- 10-1·10-4 멱등 키(jobId+stage)
    image_id      BIGINT NOT NULL REFERENCES image ON DELETE CASCADE,
    stage         VARCHAR(15) NOT NULL
                  CHECK (stage IN ('OCR','LLM','IMAGE_ANALYSIS','AGENT','INDEXING')),
    status        VARCHAR(12) NOT NULL DEFAULT 'QUEUED'
                  CHECK (status IN ('QUEUED','PROCESSING','COMPLETED','FAILED','EMPTY','CANCELED')),
    attempt       INTEGER NOT NULL DEFAULT 1,
    trigger_type  VARCHAR(12) NOT NULL DEFAULT 'INITIAL'
                  CHECK (trigger_type IN ('INITIAL','RETRY','REANALYSIS')),
    error_code    VARCHAR(40),
    error_message TEXT,
    retryable     BOOLEAN,
    model_version VARCHAR(40),
    queued_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at    TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ
);
CREATE INDEX ix_job_image  ON analysis_job (image_id, queued_at DESC);
CREATE INDEX ix_job_active ON analysis_job (status, queued_at)
    WHERE status IN ('QUEUED','PROCESSING');
--rollback DROP TABLE analysis_job;

--changeset modera:060-search-history
CREATE TABLE search_history (
    history_id   BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users ON DELETE CASCADE,
    query        VARCHAR(200) NOT NULL,
    search_type  VARCHAR(10) NOT NULL CHECK (search_type IN ('KEYWORD','NATURAL')),
    searched_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_history_user ON search_history (user_id, searched_at DESC);
--rollback DROP TABLE search_history;

-- =====================================================================
-- 확장 포인트 (모두 changeset 하나 추가로 대응)
--  * FCM 도입     → user_device(user_id, device_id, fcm_token) 테이블 추가
--  * 수정 기능 부활 → image_tag.source, image.field_sources 컬럼 추가
--  * 촬영시각 정렬  → image.taken_at 컬럼 추가
--  * 소프트 삭제   → image.deleted_at 추가 + 부분 인덱스 WHERE 절 갱신
--  * 대용량 전환   → image를 user_id 해시 파티셔닝 (쿼리 무변경)
-- =====================================================================
