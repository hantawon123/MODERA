--liquibase formatted sql

--changeset modera-api:041-create-user-data-change-outbox
--comment Persist user-visible read-model changes before sending FCM data messages.
CREATE SCHEMA IF NOT EXISTS event_schema;

CREATE TABLE event_schema.user_data_change_outbox (
    outbox_id             UUID PRIMARY KEY,
    user_id               INTEGER NOT NULL REFERENCES user_schema.users(user_id) ON DELETE CASCADE,
    resource              VARCHAR(50) NOT NULL,
    resource_id           VARCHAR(100),
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count           INTEGER NOT NULL DEFAULT 0,
    available_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    processing_started_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at               TIMESTAMPTZ,
    CONSTRAINT ck_user_data_change_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
    CONSTRAINT ck_user_data_change_outbox_retry_count
        CHECK (retry_count >= 0)
);

CREATE INDEX ix_user_data_change_outbox_dispatch
    ON event_schema.user_data_change_outbox (available_at, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');

--rollback DROP TABLE event_schema.user_data_change_outbox;
