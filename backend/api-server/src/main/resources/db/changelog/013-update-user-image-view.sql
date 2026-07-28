--liquibase formatted sql

--changeset modera-api:210-update-user-image-view
--comment Align the user image read model with the current API response and analysis result.
ALTER TABLE query_schema.user_image_view
    DROP COLUMN nickname,
    DROP COLUMN updated_at,
    ADD COLUMN key_information TEXT[],
    ADD COLUMN structured_data JSONB,
    ADD COLUMN is_documented_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    ADD COLUMN is_calendared_yn VARCHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_user_image_view_is_documented_yn
        CHECK (is_documented_yn IN ('Y', 'N')),
    ADD CONSTRAINT ck_user_image_view_is_calendared_yn
        CHECK (is_calendared_yn IN ('Y', 'N'));

--rollback ALTER TABLE query_schema.user_image_view DROP CONSTRAINT ck_user_image_view_is_calendared_yn, DROP CONSTRAINT ck_user_image_view_is_documented_yn, DROP COLUMN is_calendared_yn, DROP COLUMN is_documented_yn, DROP COLUMN structured_data, DROP COLUMN key_information, ADD COLUMN nickname VARCHAR(30), ADD COLUMN updated_at TIMESTAMPTZ;
