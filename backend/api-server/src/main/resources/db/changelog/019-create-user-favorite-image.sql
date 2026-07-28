--liquibase formatted sql

--changeset modera-api:019-create-user-favorite-image
CREATE TABLE library_schema.user_favorite_image (
    user_favorite_image_id SERIAL PRIMARY KEY,
    user_id                INTEGER NOT NULL,
    image_id               INTEGER NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_user_favorite_image_user_id_image_id
        UNIQUE (user_id, image_id)
);

CREATE INDEX ix_user_favorite_image_image_id
    ON library_schema.user_favorite_image (image_id);
--rollback DROP TABLE library_schema.user_favorite_image;

--changeset modera-api:019-require-user-image-view-favorite
UPDATE query_schema.user_image_view
SET favorite = FALSE
WHERE favorite IS NULL;

ALTER TABLE query_schema.user_image_view
    ALTER COLUMN favorite SET DEFAULT FALSE,
    ALTER COLUMN favorite SET NOT NULL;
--rollback ALTER TABLE query_schema.user_image_view ALTER COLUMN favorite DROP NOT NULL, ALTER COLUMN favorite DROP DEFAULT;
