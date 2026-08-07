--liquibase formatted sql

--changeset modera-api:021-extend-user-image-view-for-category
--comment Add category identity and actual storage upload time to the user image read model.
ALTER TABLE query_schema.user_image_view
    ADD COLUMN category_id INTEGER,
    ADD COLUMN uploaded_at TIMESTAMPTZ;

UPDATE query_schema.user_image_view AS view
SET category_id = relation.category_id
FROM library_schema.image_category AS relation
WHERE relation.image_id = view.image_id;

UPDATE query_schema.user_image_view AS view
SET uploaded_at = asset.uploaded_at
FROM image_schema.image_asset AS asset
WHERE asset.image_id = view.image_id;

CREATE INDEX ix_user_image_view_category_uploaded
    ON query_schema.user_image_view (
        user_id,
        category_id,
        uploaded_at DESC,
        image_id
    )
    WHERE del_yn = 'N';

--rollback DROP INDEX query_schema.ix_user_image_view_category_uploaded;
--rollback ALTER TABLE query_schema.user_image_view DROP COLUMN uploaded_at, DROP COLUMN category_id;

--changeset modera-api:021-create-user-category-view
--comment Create the per-user category read model used by the category list API.
CREATE TABLE query_schema.user_category_view (
    user_id             INTEGER NOT NULL,
    category_id         INTEGER NOT NULL,
    category_name       VARCHAR(30) NOT NULL,
    image_count         INTEGER NOT NULL DEFAULT 0,
    latest_uploaded_at  TIMESTAMPTZ,

    PRIMARY KEY (user_id, category_id),

    CONSTRAINT ck_user_category_view_image_count
        CHECK (image_count >= 0)
);

INSERT INTO query_schema.user_category_view (
    user_id,
    category_id,
    category_name,
    image_count,
    latest_uploaded_at
)
SELECT
    user_id,
    category_id,
    MAX(category_name),
    COUNT(*)::INTEGER,
    MAX(uploaded_at)
FROM query_schema.user_image_view
WHERE del_yn = 'N'
  AND category_id IS NOT NULL
GROUP BY user_id, category_id;

CREATE INDEX ix_user_category_view_name
    ON query_schema.user_category_view (
        user_id,
        category_name,
        category_id
    );

CREATE INDEX ix_user_category_view_latest_uploaded
    ON query_schema.user_category_view (
        user_id,
        latest_uploaded_at DESC,
        category_id
    );

CREATE INDEX ix_user_category_view_image_count
    ON query_schema.user_category_view (
        user_id,
        image_count DESC,
        category_id
    );

--rollback DROP TABLE query_schema.user_category_view;
