--liquibase formatted sql

--changeset modera-api:032-remove-redundant-created-at
--comment Remove redundant creation timestamps where updated_at is the only lifecycle timestamp needed.
ALTER TABLE user_schema.users
    DROP COLUMN created_at;

ALTER TABLE document_schema.document
    DROP COLUMN created_at;

ALTER TABLE schedule_schema.schedule
    DROP COLUMN created_at;

ALTER TABLE library_schema.user_schedule
    DROP COLUMN created_at;

ALTER TABLE query_schema.user_document_view
    DROP COLUMN created_at;

ALTER TABLE query_schema.user_schedule_view
    DROP COLUMN created_at;

--rollback ALTER TABLE user_schema.users ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
--rollback ALTER TABLE document_schema.document ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
--rollback ALTER TABLE schedule_schema.schedule ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
--rollback ALTER TABLE library_schema.user_schedule ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
--rollback ALTER TABLE query_schema.user_document_view ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
--rollback ALTER TABLE query_schema.user_schedule_view ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
