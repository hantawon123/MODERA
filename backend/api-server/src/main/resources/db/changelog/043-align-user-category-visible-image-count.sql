--liquibase formatted sql

--changeset modera-api:043-align-user-category-visible-image-count
--comment Align cached category counts with the exact visibility rules of the image list API.
UPDATE query_schema.user_category_view category_view
SET image_count = (
        SELECT COUNT(*)::INTEGER
        FROM query_schema.user_image_view image_view
        WHERE image_view.user_id = category_view.user_id
          AND image_view.category_id = category_view.category_id
          AND image_view.del_yn = 'N'
          AND image_view.analysis_status IN ('COMPLETED', 'EMPTY')
    ),
    latest_uploaded_at = (
        SELECT MAX(image_view.uploaded_at)
        FROM query_schema.user_image_view image_view
        WHERE image_view.user_id = category_view.user_id
          AND image_view.category_id = category_view.category_id
          AND image_view.del_yn = 'N'
          AND image_view.analysis_status IN ('COMPLETED', 'EMPTY')
    )
WHERE category_view.del_yn = 'N';

--rollback not required: this changes derived read-model values only
