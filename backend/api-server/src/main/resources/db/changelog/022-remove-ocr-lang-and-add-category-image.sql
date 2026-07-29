--liquibase formatted sql

--changeset modera-api:022-remove-ocr-lang
--comment Remove the OCR language value from the API database.
ALTER TABLE image_schema.ocr
    DROP COLUMN lang;

--rollback ALTER TABLE image_schema.ocr ADD COLUMN lang VARCHAR(10) NOT NULL DEFAULT 'und';
--rollback ALTER TABLE image_schema.ocr ALTER COLUMN lang DROP DEFAULT;

--changeset modera-api:022-add-category-image-s3-key
--comment Store the MinIO/S3 object key for each category image and expose it through the category read model.
ALTER TABLE taxonomy_schema.category
    ADD COLUMN image_s3_key VARCHAR(255);

ALTER TABLE taxonomy_schema.category
    ADD CONSTRAINT uq_taxonomy_category_image_s3_key
        UNIQUE (image_s3_key);

ALTER TABLE query_schema.user_category_view
    ADD COLUMN image_s3_key VARCHAR(255);

UPDATE query_schema.user_category_view AS category_view
SET image_s3_key = category.image_s3_key
FROM taxonomy_schema.category AS category
WHERE category.category_id = category_view.category_id;

--rollback ALTER TABLE query_schema.user_category_view DROP COLUMN image_s3_key;
--rollback ALTER TABLE taxonomy_schema.category DROP CONSTRAINT uq_taxonomy_category_image_s3_key;
--rollback ALTER TABLE taxonomy_schema.category DROP COLUMN image_s3_key;
