--liquibase formatted sql

--changeset modera-api:027-unify-user-notification-setting
--comment Replace per-result notification settings with one global notification switch.
ALTER TABLE user_schema.user_setting
    ADD COLUMN notification_enabled BOOLEAN;

UPDATE user_schema.user_setting
SET notification_enabled =
    analysis_completion_noti AND analysis_failure_noti;

ALTER TABLE user_schema.user_setting
    ALTER COLUMN notification_enabled SET DEFAULT TRUE,
    ALTER COLUMN notification_enabled SET NOT NULL,
    DROP COLUMN analysis_completion_noti,
    DROP COLUMN analysis_failure_noti;

--rollback ALTER TABLE user_schema.user_setting ADD COLUMN analysis_completion_noti BOOLEAN;
--rollback ALTER TABLE user_schema.user_setting ADD COLUMN analysis_failure_noti BOOLEAN;
--rollback UPDATE user_schema.user_setting SET analysis_completion_noti = notification_enabled, analysis_failure_noti = notification_enabled;
--rollback ALTER TABLE user_schema.user_setting ALTER COLUMN analysis_completion_noti SET DEFAULT TRUE, ALTER COLUMN analysis_completion_noti SET NOT NULL, ALTER COLUMN analysis_failure_noti SET DEFAULT TRUE, ALTER COLUMN analysis_failure_noti SET NOT NULL;
--rollback ALTER TABLE user_schema.user_setting DROP COLUMN notification_enabled;
