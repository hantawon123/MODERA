--liquibase formatted sql

--changeset modera-api:170-split-thumbnail-and-ocr
--comment Move thumbnail metadata out of image_asset and add one-to-one OCR storage.
CREATE TABLE image_schema.thumbnail (
    thumbnail_id SERIAL PRIMARY KEY,
    image_id     INTEGER      NOT NULL,
    s3_key       VARCHAR(255) NOT NULL,

    CONSTRAINT fk_thumbnail_image
        FOREIGN KEY (image_id)
        REFERENCES image_schema.image_asset(image_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_thumbnail_image_id UNIQUE (image_id),
    CONSTRAINT uq_thumbnail_s3_key UNIQUE (s3_key)
);

INSERT INTO image_schema.thumbnail (image_id, s3_key)
SELECT image_id, thumbnail_key
FROM image_schema.image_asset
WHERE thumbnail_key IS NOT NULL;

CREATE TABLE image_schema.ocr (
    ocr_id   SERIAL PRIMARY KEY,
    content  TEXT    NOT NULL,
    lang     VARCHAR(10) NOT NULL,
    image_id INTEGER NOT NULL,

    CONSTRAINT fk_ocr_image
        FOREIGN KEY (image_id)
        REFERENCES image_schema.image_asset(image_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_ocr_image_id UNIQUE (image_id)
);

ALTER TABLE image_schema.image_asset
    DROP COLUMN thumbnail_key,
    DROP COLUMN updated_at;

--rollback ALTER TABLE image_schema.image_asset ADD COLUMN thumbnail_key VARCHAR(255), ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
--rollback UPDATE image_schema.image_asset image_asset SET thumbnail_key = thumbnail.s3_key FROM image_schema.thumbnail thumbnail WHERE image_asset.image_id = thumbnail.image_id;
--rollback DROP TABLE image_schema.ocr;
--rollback DROP TABLE image_schema.thumbnail;
