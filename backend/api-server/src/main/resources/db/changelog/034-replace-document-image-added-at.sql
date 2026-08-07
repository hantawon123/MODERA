--liquibase formatted sql

--changeset modera-api:034-replace-document-image-added-at
--comment Use updated_at consistently for document-image relationship and read-model timestamps.
ALTER TABLE library_schema.image_document
    RENAME COLUMN added_at TO updated_at;

DROP INDEX query_schema.ix_document_image_view_document;

ALTER TABLE query_schema.document_image_view
    DROP COLUMN added_at;

CREATE INDEX ix_document_image_view_document
    ON query_schema.document_image_view (
        document_id,
        updated_at,
        image_document_id
    );

--rollback DROP INDEX query_schema.ix_document_image_view_document;
--rollback ALTER TABLE query_schema.document_image_view ADD COLUMN added_at TIMESTAMPTZ;
--rollback UPDATE query_schema.document_image_view SET added_at = updated_at;
--rollback ALTER TABLE query_schema.document_image_view ALTER COLUMN added_at SET NOT NULL;
--rollback ALTER TABLE query_schema.document_image_view ALTER COLUMN added_at SET DEFAULT now();
--rollback CREATE INDEX ix_document_image_view_document ON query_schema.document_image_view (document_id, added_at, image_document_id);
--rollback ALTER TABLE library_schema.image_document RENAME COLUMN updated_at TO added_at;
