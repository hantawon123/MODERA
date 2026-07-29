--liquibase formatted sql

--changeset modera-api:024-add-taxonomy-soft-delete
--comment Add soft-delete flags to independent taxonomy objects.
ALTER TABLE taxonomy_schema.category
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_taxonomy_category_del_yn
        CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE taxonomy_schema.tag
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_taxonomy_tag_del_yn
        CHECK (del_yn IN ('Y', 'N'));

--rollback ALTER TABLE taxonomy_schema.tag DROP CONSTRAINT ck_taxonomy_tag_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE taxonomy_schema.category DROP CONSTRAINT ck_taxonomy_category_del_yn, DROP COLUMN del_yn;

--changeset modera-api:024-add-library-soft-delete
--comment Add soft-delete flags to logical cross-domain relationships without physical foreign keys.
ALTER TABLE library_schema.user_document
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_user_document_del_yn
        CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE library_schema.image_category
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_image_category_del_yn
        CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE library_schema.image_tag
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_image_tag_del_yn
        CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE library_schema.image_document
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_image_document_del_yn
        CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE library_schema.user_favorite_image
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_user_favorite_image_del_yn
        CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE library_schema.image_schedule
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_image_schedule_del_yn
        CHECK (del_yn IN ('Y', 'N'));

--rollback ALTER TABLE library_schema.image_schedule DROP CONSTRAINT ck_image_schedule_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE library_schema.user_favorite_image DROP CONSTRAINT ck_user_favorite_image_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE library_schema.image_document DROP CONSTRAINT ck_image_document_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE library_schema.image_tag DROP CONSTRAINT ck_image_tag_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE library_schema.image_category DROP CONSTRAINT ck_image_category_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE library_schema.user_document DROP CONSTRAINT ck_user_document_del_yn, DROP COLUMN del_yn;

--changeset modera-api:024-add-query-soft-delete
--comment Add soft-delete flags to independent read models.
ALTER TABLE query_schema.user_category_view
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_user_category_view_del_yn
        CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE query_schema.document_image_view
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_document_image_view_del_yn
        CHECK (del_yn IN ('Y', 'N'));

--rollback ALTER TABLE query_schema.document_image_view DROP CONSTRAINT ck_document_image_view_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE query_schema.user_category_view DROP CONSTRAINT ck_user_category_view_del_yn, DROP COLUMN del_yn;
