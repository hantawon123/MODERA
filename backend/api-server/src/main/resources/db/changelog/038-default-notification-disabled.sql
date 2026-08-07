--liquibase formatted sql

--changeset modera-api:038-default-notification-disabled
--comment New user settings default to notifications disabled and server analysis enabled.
ALTER TABLE user_schema.user_setting
    ALTER COLUMN notification_enabled SET DEFAULT FALSE,
    ALTER COLUMN server_analysis_enabled SET DEFAULT TRUE;

--rollback ALTER TABLE user_schema.user_setting ALTER COLUMN notification_enabled SET DEFAULT TRUE;
--rollback ALTER TABLE user_schema.user_setting ALTER COLUMN server_analysis_enabled SET DEFAULT TRUE;
