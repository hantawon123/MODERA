--liquibase formatted sql

--changeset modera-api:190-create-document-table
--comment Create immutable Markdown documents in document_schema.
CREATE TABLE document_schema.document (
    document_id SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    content     TEXT         NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    del_yn      CHAR(1)      NOT NULL DEFAULT 'N',

    CONSTRAINT ck_document_del_yn CHECK (del_yn IN ('Y', 'N'))
);

--rollback DROP TABLE document_schema.document;
