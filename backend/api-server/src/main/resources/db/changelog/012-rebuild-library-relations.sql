--liquibase formatted sql

--changeset modera-api:200-rebuild-library-relations
--comment Keep only cross-domain relationships in library_schema.
DROP TABLE library_schema.image_tag;

ALTER TABLE library_schema.user_image
    DROP CONSTRAINT user_image_category_id_fkey,
    DROP CONSTRAINT user_image_user_id_client_request_id_key,
    DROP CONSTRAINT ck_user_image_del_yn,
    DROP COLUMN client_request_id,
    DROP COLUMN category_id,
    DROP COLUMN title,
    DROP COLUMN favorite,
    DROP COLUMN analysis_status,
    DROP COLUMN last_viewed_at,
    DROP COLUMN created_at,
    DROP COLUMN updated_at,
    DROP COLUMN del_yn;

ALTER TABLE library_schema.user_image
    RENAME CONSTRAINT uq_user_image_user_image
    TO uq_user_image_user_id_image_id;

CREATE INDEX ix_user_image_image_id
    ON library_schema.user_image (image_id);

DROP TABLE library_schema.category;
DROP TABLE library_schema.tag;

CREATE TABLE library_schema.user_document (
    user_document_id SERIAL PRIMARY KEY,
    user_id          INTEGER NOT NULL,
    document_id      INTEGER NOT NULL,

    CONSTRAINT uq_user_document_user_id_document_id
        UNIQUE (user_id, document_id)
);

CREATE INDEX ix_user_document_document_id
    ON library_schema.user_document (document_id);

CREATE TABLE library_schema.image_category (
    image_category_id SERIAL PRIMARY KEY,
    image_id          INTEGER NOT NULL,
    category_id       INTEGER NOT NULL,

    CONSTRAINT uq_image_category_image_id UNIQUE (image_id)
);

CREATE INDEX ix_image_category_category_id
    ON library_schema.image_category (category_id);

CREATE TABLE library_schema.image_tag (
    image_tag_id SERIAL PRIMARY KEY,
    image_id     INTEGER NOT NULL,
    tag_id       INTEGER NOT NULL,

    CONSTRAINT uq_image_tag_image_id_tag_id UNIQUE (image_id, tag_id)
);

CREATE INDEX ix_image_tag_tag_id
    ON library_schema.image_tag (tag_id);

CREATE TABLE library_schema.image_document (
    image_document_id SERIAL PRIMARY KEY,
    image_id          INTEGER NOT NULL,
    document_id       INTEGER NOT NULL,
    sort_order        INTEGER NOT NULL,

    CONSTRAINT uq_image_document_document_id_image_id
        UNIQUE (document_id, image_id),
    CONSTRAINT uq_image_document_document_id_sort_order
        UNIQUE (document_id, sort_order)
);

CREATE INDEX ix_image_document_image_id
    ON library_schema.image_document (image_id);

--rollback DROP TABLE library_schema.image_document;
--rollback DROP TABLE library_schema.image_tag;
--rollback DROP TABLE library_schema.image_category;
--rollback DROP TABLE library_schema.user_document;
--rollback CREATE TABLE library_schema.category (category_id SERIAL PRIMARY KEY, user_id INTEGER NOT NULL, name VARCHAR(30) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), del_yn CHAR(1) NOT NULL DEFAULT 'N', CONSTRAINT category_user_id_name_key UNIQUE (user_id, name), CONSTRAINT ck_category_del_yn CHECK (del_yn IN ('Y', 'N')));
--rollback CREATE TABLE library_schema.tag (tag_id SERIAL PRIMARY KEY, user_id INTEGER NOT NULL, name VARCHAR(50) NOT NULL, usage_count INTEGER NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), del_yn CHAR(1) NOT NULL DEFAULT 'N', CONSTRAINT tag_user_id_name_key UNIQUE (user_id, name), CONSTRAINT ck_tag_del_yn CHECK (del_yn IN ('Y', 'N')));
--rollback ALTER TABLE library_schema.user_image DROP CONSTRAINT uq_user_image_user_id_image_id;
--rollback ALTER TABLE library_schema.user_image ADD CONSTRAINT uq_user_image_user_image UNIQUE (user_id, image_id);
--rollback ALTER TABLE library_schema.user_image ADD COLUMN client_request_id VARCHAR(100), ADD COLUMN category_id INTEGER REFERENCES library_schema.category(category_id), ADD COLUMN title VARCHAR(100), ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE, ADD COLUMN analysis_status VARCHAR(20) NOT NULL DEFAULT 'NONE', ADD COLUMN last_viewed_at TIMESTAMPTZ, ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N';
--rollback UPDATE library_schema.user_image SET client_request_id = 'rollback-' || user_image_id;
--rollback ALTER TABLE library_schema.user_image ALTER COLUMN client_request_id SET NOT NULL, ADD CONSTRAINT user_image_user_id_client_request_id_key UNIQUE (user_id, client_request_id), ADD CONSTRAINT ck_user_image_del_yn CHECK (del_yn IN ('Y', 'N'));
--rollback CREATE TABLE library_schema.image_tag (tag_id INTEGER NOT NULL REFERENCES library_schema.tag(tag_id) ON DELETE CASCADE, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), del_yn CHAR(1) NOT NULL DEFAULT 'N', user_image_id INTEGER NOT NULL REFERENCES library_schema.user_image(user_image_id) ON DELETE CASCADE, CONSTRAINT image_tag_pkey PRIMARY KEY (user_image_id, tag_id), CONSTRAINT ck_image_tag_del_yn CHECK (del_yn IN ('Y', 'N')));
--rollback DROP INDEX library_schema.ix_user_image_image_id;
