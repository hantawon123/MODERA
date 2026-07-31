--liquibase formatted sql

--changeset modera-api:039-add-soft-delete-to-user-category-history
--comment Allow stored-data reset to retain category history while treating it as absent.
ALTER TABLE library_schema.user_image_category_history
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_user_image_category_history_del_yn
        CHECK (del_yn IN ('Y', 'N'));

CREATE INDEX ix_user_image_category_history_active_latest
    ON library_schema.user_image_category_history (
        user_image_id,
        created_at DESC,
        user_image_category_history_id DESC
    )
    WHERE del_yn = 'N';

--rollback DROP INDEX library_schema.ix_user_image_category_history_active_latest;
--rollback ALTER TABLE library_schema.user_image_category_history DROP CONSTRAINT ck_user_image_category_history_del_yn, DROP COLUMN del_yn;
