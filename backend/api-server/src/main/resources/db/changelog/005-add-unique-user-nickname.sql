--liquibase formatted sql

--changeset modera-api:130-add-unique-user-nickname
--comment Guarantee nickname uniqueness at the database level.
ALTER TABLE user_schema.users
    ADD CONSTRAINT uq_users_nickname UNIQUE (nickname);

--rollback ALTER TABLE user_schema.users DROP CONSTRAINT uq_users_nickname;
