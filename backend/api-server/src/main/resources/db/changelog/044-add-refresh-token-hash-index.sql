--liquibase formatted sql

--changeset modera-api:044-add-refresh-token-hash-index
CREATE UNIQUE INDEX uq_refresh_token_token_hash
    ON user_schema.refresh_token (token_hash);
--rollback DROP INDEX user_schema.uq_refresh_token_token_hash;
