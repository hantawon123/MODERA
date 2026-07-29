--liquibase formatted sql

--changeset modera-api:031-remove-image-created-at
--comment Use uploaded_at as the image timeline and remove the redundant image creation timestamps.
ALTER TABLE image_schema.image_asset
    DROP COLUMN created_at;

ALTER TABLE query_schema.user_image_view
    DROP COLUMN created_at;

--rollback ALTER TABLE image_schema.image_asset ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
--rollback ALTER TABLE query_schema.user_image_view ADD COLUMN created_at TIMESTAMPTZ;
