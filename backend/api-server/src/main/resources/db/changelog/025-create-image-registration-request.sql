--liquibase formatted sql

--changeset modera-api:025-create-image-registration-request
--comment Persist image registration idempotency keys and their processing results.
CREATE TABLE image_schema.image_registration_request (
    image_registration_request_id SERIAL PRIMARY KEY,
    user_id                       INTEGER NOT NULL,
    client_request_id             UUID NOT NULL,
    image_id                      INTEGER,
    status                        VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    failure_reason                VARCHAR(100),
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at                  TIMESTAMPTZ,

    CONSTRAINT uq_image_registration_request_user_client
        UNIQUE (user_id, client_request_id),
    CONSTRAINT fk_image_registration_request_image
        FOREIGN KEY (image_id)
        REFERENCES image_schema.image_asset(image_id)
        ON DELETE SET NULL,
    CONSTRAINT ck_image_registration_request_status
        CHECK (
            status IN (
                'PROCESSING',
                'REGISTERED',
                'DUPLICATED',
                'FAILED'
            )
        )
);

CREATE INDEX ix_image_registration_request_image_id
    ON image_schema.image_registration_request (image_id);

CREATE INDEX ix_image_registration_request_processing
    ON image_schema.image_registration_request (updated_at)
    WHERE status = 'PROCESSING';

--rollback DROP TABLE image_schema.image_registration_request;
