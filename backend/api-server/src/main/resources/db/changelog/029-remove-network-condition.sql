--liquibase formatted sql

--changeset modera-api:029-remove-network-condition
--comment Remove the unused network condition setting because analysis is always allowed on any network.
ALTER TABLE user_schema.user_setting
    DROP COLUMN network_condition;

--rollback ALTER TABLE user_schema.user_setting ADD COLUMN network_condition VARCHAR(10) NOT NULL DEFAULT 'WIFI';
