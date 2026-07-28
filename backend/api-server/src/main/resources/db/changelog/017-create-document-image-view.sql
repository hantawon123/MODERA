--liquibase formatted sql

--changeset modera-api:017-create-document-image-view
CREATE TABLE query_schema.document_image_view (
    image_document_id INTEGER PRIMARY KEY,
    user_id           INTEGER NOT NULL,
    document_id       INTEGER NOT NULL,
    image_id          INTEGER NOT NULL,
    title             VARCHAR(100) NOT NULL,
    summary           TEXT,
    thumbnail_key     VARCHAR(255) NOT NULL,
    tag_names         TEXT[] NOT NULL DEFAULT '{}',
    added_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_document_image_view_document
    ON query_schema.document_image_view (
        document_id,
        added_at,
        image_document_id
    );
--rollback DROP TABLE query_schema.document_image_view;
