--liquibase formatted sql

--changeset modera-api:140-add-unique-social-provider
--comment A social account must map to only one Modera user.
CREATE UNIQUE INDEX uq_users_provider_provider_id
    ON user_schema.users (provider, provider_id)
    WHERE provider_id IS NOT NULL;

--rollback DROP INDEX user_schema.uq_users_provider_provider_id;
