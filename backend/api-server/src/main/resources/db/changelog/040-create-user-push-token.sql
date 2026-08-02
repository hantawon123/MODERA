--liquibase formatted sql

--changeset modera-api:040-create-user-push-token
--comment Store one active FCM token per user device for data synchronization messages.
CREATE TABLE user_schema.user_push_token (
    push_token_id BIGSERIAL PRIMARY KEY,
    user_id       INTEGER NOT NULL REFERENCES user_schema.users(user_id) ON DELETE CASCADE,
    device_id     VARCHAR(64) NOT NULL,
    fcm_token     TEXT NOT NULL,
    del_yn        CHAR(1) NOT NULL DEFAULT 'N',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_push_token_device UNIQUE (user_id, device_id),
    CONSTRAINT ck_user_push_token_del_yn CHECK (del_yn IN ('Y', 'N'))
);

CREATE UNIQUE INDEX uk_user_push_token_active_fcm
    ON user_schema.user_push_token (fcm_token)
    WHERE del_yn = 'N';

CREATE INDEX ix_user_push_token_active_user
    ON user_schema.user_push_token (user_id)
    WHERE del_yn = 'N';

--rollback DROP TABLE user_schema.user_push_token;
