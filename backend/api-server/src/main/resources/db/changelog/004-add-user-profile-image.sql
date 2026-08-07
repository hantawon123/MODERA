--liquibase formatted sql

--changeset modera-api:120-add-user-profile-image
--comment Add the predefined profile image selector (0 through 5) to users.
ALTER TABLE user_schema.users
    ADD COLUMN profile_image_id INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_users_profile_image_id
        CHECK (profile_image_id BETWEEN 0 AND 5);

--rollback ALTER TABLE user_schema.users DROP CONSTRAINT ck_users_profile_image_id, DROP COLUMN profile_image_id;
