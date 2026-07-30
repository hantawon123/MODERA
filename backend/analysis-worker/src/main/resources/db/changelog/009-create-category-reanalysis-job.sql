--liquibase formatted sql

--changeset analysis-worker:009-category-reanalysis-job
--comment Keep category-only reanalysis lifecycle separate from full analysis jobs.
CREATE TABLE category_reanalysis_job (
    category_request_id   UUID PRIMARY KEY,
    user_id               INTEGER NOT NULL,
    image_id              INTEGER NOT NULL,
    excluded_category_ids INTEGER[] NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_category_reanalysis_job_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

--rollback DROP TABLE category_reanalysis_job;
