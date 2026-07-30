--liquibase formatted sql

--changeset modera-api:036-user-image-category-history
--comment Store the current per-user category, one pending request, and the latest five category results.
ALTER TABLE library_schema.user_image
    ADD COLUMN current_category_id INTEGER,
    ADD COLUMN pending_category_request_id UUID;

CREATE TABLE library_schema.user_image_category_history (
    user_image_category_history_id SERIAL PRIMARY KEY,
    user_image_id                  INTEGER NOT NULL
        REFERENCES library_schema.user_image(user_image_id),
    category_id                    INTEGER NOT NULL,
    source                         VARCHAR(20) NOT NULL,
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_user_image_category_history_source
        CHECK (source IN ('INITIAL', 'REANALYSIS'))
);

CREATE INDEX ix_user_image_category_history_latest
    ON library_schema.user_image_category_history (
        user_image_id,
        created_at DESC,
        user_image_category_history_id DESC
    );

UPDATE library_schema.user_image user_image
SET current_category_id = image_category.category_id
FROM library_schema.image_category image_category
WHERE image_category.image_id = user_image.image_id
  AND image_category.del_yn = 'N'
  AND user_image.del_yn = 'N';

INSERT INTO library_schema.user_image_category_history (
    user_image_id,
    category_id,
    source
)
SELECT user_image_id, current_category_id, 'INITIAL'
FROM library_schema.user_image
WHERE current_category_id IS NOT NULL
  AND del_yn = 'N';

--rollback DROP TABLE library_schema.user_image_category_history;
--rollback ALTER TABLE library_schema.user_image DROP COLUMN pending_category_request_id, DROP COLUMN current_category_id;
