--liquibase formatted sql
-- modera-api DB 스키마 (SOA 재구성) — user_schema / image_schema / library_schema / query_schema
-- 서비스 경계를 넘는 FK는 만들지 않는다. 다른 schema가 소유한 값은 컬럼으로만 저장하는 논리 참조.
-- pgvector(vector), pg_bigm 확장은 DB init 스크립트(local-infra/api-db)에서 이미 생성되어 있다.
-- 배치: api-server/src/main/resources/db/changelog/001-init-schema.sql

--changeset modera-api:000-schemas
CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS image_schema;
CREATE SCHEMA IF NOT EXISTS library_schema;
CREATE SCHEMA IF NOT EXISTS query_schema;
--rollback DROP SCHEMA IF EXISTS user_schema CASCADE; DROP SCHEMA IF EXISTS image_schema CASCADE; DROP SCHEMA IF EXISTS library_schema CASCADE; DROP SCHEMA IF EXISTS query_schema CASCADE;

--changeset modera-api:010-users
CREATE TABLE user_schema.users (
    user_id        BIGSERIAL PRIMARY KEY,
    provider       VARCHAR(10)  NOT NULL DEFAULT 'LOCAL',
    provider_id    VARCHAR(255),
    login_id       VARCHAR(20)  UNIQUE,
    password_hash  VARCHAR(72),
    email          VARCHAR(255) UNIQUE,
    nickname       VARCHAR(30)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
--rollback DROP TABLE user_schema.users;

--changeset modera-api:011-refresh-token
CREATE TABLE user_schema.refresh_token (
    token_id    BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES user_schema.users ON DELETE CASCADE,
    device_id   VARCHAR(64) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, device_id)
);
--rollback DROP TABLE user_schema.refresh_token;

--changeset modera-api:012-user-setting
CREATE TABLE user_schema.user_setting (
    user_id                   BIGINT PRIMARY KEY REFERENCES user_schema.users ON DELETE CASCADE,
    server_analysis_enabled   BOOLEAN NOT NULL DEFAULT true,
    network_condition         VARCHAR(10) NOT NULL DEFAULT 'WIFI',
    analysis_completion_noti  BOOLEAN NOT NULL DEFAULT true,
    analysis_failure_noti     BOOLEAN NOT NULL DEFAULT true,
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);
--rollback DROP TABLE user_schema.user_setting;

--changeset modera-api:020-image-asset
CREATE TABLE image_schema.image_asset (
    image_id       UUID PRIMARY KEY,
    file_name      VARCHAR(255) NOT NULL,
    content_hash   CHAR(64) NOT NULL,
    file_size      INTEGER NOT NULL,
    source_url     TEXT,
    s3_key         VARCHAR(255) NOT NULL UNIQUE,
    thumbnail_key  VARCHAR(255),
    upload_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    uploaded_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
--rollback DROP TABLE image_schema.image_asset;

--changeset modera-api:030-category
CREATE TABLE library_schema.category (
    category_id  BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    name         VARCHAR(30) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);
--rollback DROP TABLE library_schema.category;

--changeset modera-api:031-tag
CREATE TABLE library_schema.tag (
    tag_id       BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    name         VARCHAR(50) NOT NULL,
    usage_count  INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);
--rollback DROP TABLE library_schema.tag;

--changeset modera-api:032-user-image
-- category_id FK는 같은 library_schema 내부 참조라 허용. user_id/image_id는 다른 schema/DB 소유값의 논리 참조.
CREATE TABLE library_schema.user_image (
    image_id           UUID PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    client_request_id  VARCHAR(100) NOT NULL,
    category_id        BIGINT REFERENCES library_schema.category,
    title               VARCHAR(100),
    favorite            BOOLEAN NOT NULL DEFAULT false,
    analysis_status     VARCHAR(20) NOT NULL DEFAULT 'NONE',
    last_viewed_at       TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, client_request_id)
);
--rollback DROP TABLE library_schema.user_image;

--changeset modera-api:033-image-tag
CREATE TABLE library_schema.image_tag (
    image_id    UUID NOT NULL REFERENCES library_schema.user_image ON DELETE CASCADE,
    tag_id      BIGINT NOT NULL REFERENCES library_schema.tag ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (image_id, tag_id)
);
--rollback DROP TABLE library_schema.image_tag;

--changeset modera-api:040-user-image-view
CREATE TABLE query_schema.user_image_view (
    user_id          BIGINT NOT NULL,
    image_id         UUID NOT NULL,
    nickname         VARCHAR(30),
    file_name        VARCHAR(255),
    s3_key           VARCHAR(255),
    thumbnail_key    VARCHAR(255),
    title            VARCHAR(100),
    summary          TEXT,
    category_name    VARCHAR(30),
    tag_names        TEXT[],
    upload_status    VARCHAR(20),
    analysis_status  VARCHAR(20),
    favorite         BOOLEAN,
    created_at       TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ,
    PRIMARY KEY (user_id, image_id)
);
--rollback DROP TABLE query_schema.user_image_view;

--changeset modera-api:041-image-search-document
CREATE TABLE query_schema.image_search_document (
    image_id           UUID PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    title              VARCHAR(100),
    summary            TEXT,
    ocr_text           TEXT,
    category_name      VARCHAR(30),
    tag_names          TEXT[],
    structured_fields  JSONB,
    embedding          vector(768),
    indexed_at         TIMESTAMPTZ
);
CREATE INDEX ix_search_document_user ON query_schema.image_search_document (user_id);
-- pg_bigm 사용 가능 확인됨(3단계에서 빌드 성공) → ocr_text 유사검색용 GIN 인덱스 실적용
CREATE INDEX ix_search_document_ocr_bigm ON query_schema.image_search_document USING gin (ocr_text gin_bigm_ops);
-- 후순위: 데이터 볼륨/쿼리 패턴이 확정된 뒤 ivfflat 또는 hnsw 파라미터를 정해 추가한다.
-- CREATE INDEX ix_search_document_embedding ON query_schema.image_search_document USING hnsw (embedding vector_cosine_ops);
--rollback DROP TABLE query_schema.image_search_document;

--changeset modera-api:042-search-history
CREATE TABLE query_schema.search_history (
    history_id   BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    query        VARCHAR(200) NOT NULL,
    search_type  VARCHAR(10) NOT NULL,
    searched_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_search_history_user ON query_schema.search_history (user_id, searched_at DESC);
--rollback DROP TABLE query_schema.search_history;
