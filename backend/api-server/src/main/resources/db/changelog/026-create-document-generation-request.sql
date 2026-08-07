--liquibase formatted sql

--changeset modera-api:026-create-document-generation-request
--comment Persist document generation idempotency keys and asynchronous processing results.
CREATE TABLE document_schema.document_generation_request (
    document_generation_request_id SERIAL PRIMARY KEY,
    user_id                        INTEGER NOT NULL,
    client_request_id              UUID NOT NULL,
    operation_type                 VARCHAR(20) NOT NULL,
    source_document_id             INTEGER,
    result_document_id             INTEGER,
    status                         VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    failure_reason                 VARCHAR(100),
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at                   TIMESTAMPTZ,

    CONSTRAINT uq_document_generation_request_user_client
        UNIQUE (user_id, client_request_id),
    CONSTRAINT fk_document_generation_request_source
        FOREIGN KEY (source_document_id)
        REFERENCES document_schema.document(document_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_document_generation_request_result
        FOREIGN KEY (result_document_id)
        REFERENCES document_schema.document(document_id)
        ON DELETE SET NULL,
    CONSTRAINT ck_document_generation_request_operation
        CHECK (
            operation_type IN (
                'CREATE',
                'ADD_IMAGES',
                'EXCLUDE_IMAGES'
            )
        ),
    CONSTRAINT ck_document_generation_request_source
        CHECK (
            (operation_type = 'CREATE' AND source_document_id IS NULL)
            OR
            (
                operation_type IN ('ADD_IMAGES', 'EXCLUDE_IMAGES')
                AND source_document_id IS NOT NULL
            )
        ),
    CONSTRAINT ck_document_generation_request_status
        CHECK (
            status IN (
                'QUEUED',
                'PROCESSING',
                'COMPLETED',
                'FAILED'
            )
        )
);

CREATE INDEX ix_document_generation_request_source
    ON document_schema.document_generation_request (source_document_id);

CREATE INDEX ix_document_generation_request_result
    ON document_schema.document_generation_request (result_document_id);

CREATE INDEX ix_document_generation_request_pending
    ON document_schema.document_generation_request (updated_at)
    WHERE status IN ('QUEUED', 'PROCESSING');

--rollback DROP TABLE document_schema.document_generation_request;
