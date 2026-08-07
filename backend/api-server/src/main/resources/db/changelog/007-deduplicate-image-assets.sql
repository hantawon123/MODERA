--liquibase formatted sql

--changeset modera-api:150-add-user-image-id-and-unique-content-hash
--comment Allow one image asset to be linked to multiple users and deduplicate assets by SHA-256 hash.
ALTER TABLE library_schema.image_tag
    DROP CONSTRAINT image_tag_image_id_fkey;

ALTER TABLE library_schema.user_image
    ADD COLUMN user_image_id SERIAL;

ALTER TABLE library_schema.user_image
    DROP CONSTRAINT user_image_pkey,
    ADD CONSTRAINT user_image_pkey PRIMARY KEY (user_image_id),
    ADD CONSTRAINT uq_user_image_user_image UNIQUE (user_id, image_id);

ALTER TABLE library_schema.image_tag
    ADD COLUMN user_image_id INTEGER;

UPDATE library_schema.image_tag image_tag
SET user_image_id = user_image.user_image_id
FROM library_schema.user_image user_image
WHERE image_tag.image_id = user_image.image_id;

ALTER TABLE library_schema.image_tag
    ALTER COLUMN user_image_id SET NOT NULL,
    DROP CONSTRAINT image_tag_pkey,
    DROP COLUMN image_id,
    ADD CONSTRAINT image_tag_pkey PRIMARY KEY (user_image_id, tag_id),
    ADD CONSTRAINT image_tag_user_image_id_fkey
        FOREIGN KEY (user_image_id)
        REFERENCES library_schema.user_image(user_image_id)
        ON DELETE CASCADE;

ALTER TABLE image_schema.image_asset
    ADD CONSTRAINT uq_image_asset_content_hash UNIQUE (content_hash);

--rollback ALTER TABLE image_schema.image_asset DROP CONSTRAINT uq_image_asset_content_hash;
--rollback ALTER TABLE library_schema.image_tag ADD COLUMN image_id INTEGER;
--rollback UPDATE library_schema.image_tag image_tag SET image_id = user_image.image_id FROM library_schema.user_image user_image WHERE image_tag.user_image_id = user_image.user_image_id;
--rollback ALTER TABLE library_schema.image_tag DROP CONSTRAINT image_tag_user_image_id_fkey, DROP CONSTRAINT image_tag_pkey, ALTER COLUMN image_id SET NOT NULL, DROP COLUMN user_image_id, ADD CONSTRAINT image_tag_pkey PRIMARY KEY (image_id, tag_id);
--rollback ALTER TABLE library_schema.user_image DROP CONSTRAINT uq_user_image_user_image, DROP CONSTRAINT user_image_pkey, ADD CONSTRAINT user_image_pkey PRIMARY KEY (image_id), DROP COLUMN user_image_id;
--rollback ALTER TABLE library_schema.image_tag ADD CONSTRAINT image_tag_image_id_fkey FOREIGN KEY (image_id) REFERENCES library_schema.user_image(image_id) ON DELETE CASCADE;
