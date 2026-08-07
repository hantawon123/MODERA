--liquibase formatted sql

--changeset modera-api:018-add-document-updated-at
ALTER TABLE document_schema.document
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
--rollback ALTER TABLE document_schema.document DROP COLUMN updated_at;

--changeset modera-api:018-enforce-document-owner
ALTER TABLE library_schema.user_document
    ADD CONSTRAINT uq_user_document_document_id UNIQUE (document_id);
--rollback ALTER TABLE library_schema.user_document DROP CONSTRAINT uq_user_document_document_id;

--changeset modera-api:018-track-image-document-added-at
ALTER TABLE library_schema.image_document
    DROP CONSTRAINT uq_image_document_document_id_sort_order,
    DROP COLUMN sort_order,
    ADD COLUMN added_at TIMESTAMPTZ NOT NULL DEFAULT now();
--rollback ALTER TABLE library_schema.image_document ADD COLUMN sort_order INTEGER;
--rollback WITH ranked AS (SELECT image_document_id, row_number() OVER (PARTITION BY document_id ORDER BY added_at, image_document_id) - 1 AS sort_order FROM library_schema.image_document) UPDATE library_schema.image_document target SET sort_order = ranked.sort_order FROM ranked WHERE target.image_document_id = ranked.image_document_id;
--rollback ALTER TABLE library_schema.image_document ALTER COLUMN sort_order SET NOT NULL;
--rollback ALTER TABLE library_schema.image_document ADD CONSTRAINT uq_image_document_document_id_sort_order UNIQUE (document_id, sort_order);
--rollback ALTER TABLE library_schema.image_document DROP COLUMN added_at;

--changeset modera-api:018-require-document-image-summary
ALTER TABLE query_schema.document_image_view
    ALTER COLUMN summary SET NOT NULL;
--rollback ALTER TABLE query_schema.document_image_view ALTER COLUMN summary DROP NOT NULL;

--changeset modera-api:018-drop-taxonomy-vectors
ALTER TABLE taxonomy_schema.category
    DROP COLUMN vector;

ALTER TABLE taxonomy_schema.tag
    DROP COLUMN vector;
--rollback ALTER TABLE taxonomy_schema.category ADD COLUMN vector vector(768);
--rollback ALTER TABLE taxonomy_schema.tag ADD COLUMN vector vector(768);
