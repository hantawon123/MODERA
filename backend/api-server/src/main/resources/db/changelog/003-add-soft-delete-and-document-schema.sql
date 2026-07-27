--liquibase formatted sql

--changeset modera-api:110-create-document-schema
--comment Create the document service schema without adding tables.
CREATE SCHEMA IF NOT EXISTS document_schema;
--rollback DROP SCHEMA IF EXISTS document_schema;

--changeset modera-api:111-add-soft-delete-columns
--comment Add a Y/N soft-delete flag to every API business table.
ALTER TABLE user_schema.users
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_users_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE user_schema.refresh_token
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_refresh_token_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE user_schema.user_setting
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_user_setting_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE image_schema.image_asset
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_image_asset_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE library_schema.category
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_category_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE library_schema.tag
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_tag_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE library_schema.user_image
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_user_image_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE library_schema.image_tag
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_image_tag_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE query_schema.user_image_view
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_user_image_view_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE query_schema.image_search_document
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_image_search_document_del_yn CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE query_schema.search_history
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_search_history_del_yn CHECK (del_yn IN ('Y', 'N'));

--rollback ALTER TABLE query_schema.search_history DROP CONSTRAINT ck_search_history_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE query_schema.image_search_document DROP CONSTRAINT ck_image_search_document_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE query_schema.user_image_view DROP CONSTRAINT ck_user_image_view_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE library_schema.image_tag DROP CONSTRAINT ck_image_tag_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE library_schema.user_image DROP CONSTRAINT ck_user_image_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE library_schema.tag DROP CONSTRAINT ck_tag_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE library_schema.category DROP CONSTRAINT ck_category_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE image_schema.image_asset DROP CONSTRAINT ck_image_asset_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE user_schema.user_setting DROP CONSTRAINT ck_user_setting_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE user_schema.refresh_token DROP CONSTRAINT ck_refresh_token_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE user_schema.users DROP CONSTRAINT ck_users_del_yn, DROP COLUMN del_yn;
