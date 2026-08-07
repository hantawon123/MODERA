--liquibase formatted sql

--changeset modera-api:033-add-soft-delete-to-request-history
--comment Soft-delete user-owned idempotency request history during stored-data reset.
ALTER TABLE image_schema.image_registration_request
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_image_registration_request_del_yn
        CHECK (del_yn IN ('Y', 'N'));

ALTER TABLE document_schema.document_generation_request
    ADD COLUMN del_yn CHAR(1) NOT NULL DEFAULT 'N',
    ADD CONSTRAINT ck_document_generation_request_del_yn
        CHECK (del_yn IN ('Y', 'N'));

--rollback ALTER TABLE document_schema.document_generation_request DROP CONSTRAINT ck_document_generation_request_del_yn, DROP COLUMN del_yn;
--rollback ALTER TABLE image_schema.image_registration_request DROP CONSTRAINT ck_image_registration_request_del_yn, DROP COLUMN del_yn;
