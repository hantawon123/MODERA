--liquibase formatted sql

--changeset modera-api:023-add-user-image-soft-delete
--comment Track user-specific image deletion without changing the shared image object.
ALTER TABLE library_schema.user_image
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE library_schema.user_image
    ADD CONSTRAINT ck_user_image_del_yn
        CHECK (del_yn IN ('Y', 'N'));

--rollback ALTER TABLE library_schema.user_image DROP CONSTRAINT ck_user_image_del_yn;
--rollback ALTER TABLE library_schema.user_image DROP COLUMN del_yn;

--changeset modera-api:023-add-user-document-deleted-image-count
--comment Track the number of deleted images represented in each user document read model.
ALTER TABLE query_schema.user_document_view
    ADD COLUMN del_image_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE query_schema.user_document_view
    ADD CONSTRAINT ck_user_document_view_del_image_count
        CHECK (del_image_count >= 0);

--rollback ALTER TABLE query_schema.user_document_view DROP CONSTRAINT ck_user_document_view_del_image_count;
--rollback ALTER TABLE query_schema.user_document_view DROP COLUMN del_image_count;
