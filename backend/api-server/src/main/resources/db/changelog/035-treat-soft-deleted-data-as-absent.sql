--liquibase formatted sql

--changeset modera-api:035-active-only-user-image-uniqueness
--comment Allow a new active relation after a previous user-image relation was soft deleted.
ALTER TABLE library_schema.user_image
    DROP CONSTRAINT uq_user_image_user_id_image_id;

CREATE UNIQUE INDEX uq_user_image_active_user_id_image_id
    ON library_schema.user_image (user_id, image_id)
    WHERE del_yn = 'N';

--rollback DROP INDEX library_schema.uq_user_image_active_user_id_image_id;
--rollback ALTER TABLE library_schema.user_image ADD CONSTRAINT uq_user_image_user_id_image_id UNIQUE (user_id, image_id);

--changeset modera-api:035-active-only-image-registration-request-uniqueness
--comment Treat soft-deleted idempotency history as absent without restoring it.
ALTER TABLE image_schema.image_registration_request
    DROP CONSTRAINT uq_image_registration_request_user_client;

CREATE UNIQUE INDEX uq_image_registration_request_active_user_client
    ON image_schema.image_registration_request (user_id, client_request_id)
    WHERE del_yn = 'N';

--rollback DROP INDEX image_schema.uq_image_registration_request_active_user_client;
--rollback ALTER TABLE image_schema.image_registration_request ADD CONSTRAINT uq_image_registration_request_user_client UNIQUE (user_id, client_request_id);
