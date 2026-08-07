--liquibase formatted sql

--changeset modera-api:160-remove-users-nickname-profile
--comment Remove nickname and profile image selector from users.
ALTER TABLE user_schema.users
    DROP CONSTRAINT IF EXISTS uq_users_nickname,
    DROP CONSTRAINT IF EXISTS ck_users_profile_image_id,
    DROP COLUMN nickname,
    DROP COLUMN profile_image_id;

--rollback ALTER TABLE user_schema.users ADD COLUMN nickname VARCHAR(30), ADD COLUMN profile_image_id INTEGER NOT NULL DEFAULT 0;
--rollback UPDATE user_schema.users SET nickname = 'user_' || user_id;
--rollback ALTER TABLE user_schema.users ALTER COLUMN nickname SET NOT NULL, ADD CONSTRAINT uq_users_nickname UNIQUE (nickname), ADD CONSTRAINT ck_users_profile_image_id CHECK (profile_image_id BETWEEN 0 AND 5);
