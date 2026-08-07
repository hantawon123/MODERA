--liquibase formatted sql

--changeset modera-api:028-create-tag-snapshot-validator splitStatements:false
--comment Validate the object structure used by tag snapshots in query read models.
CREATE FUNCTION query_schema.is_valid_tag_snapshot(tags_value JSONB)
RETURNS BOOLEAN
LANGUAGE SQL
IMMUTABLE
STRICT
AS $$
    SELECT jsonb_typeof(tags_value) = 'array'
       AND NOT EXISTS (
            SELECT 1
            FROM jsonb_array_elements(tags_value) AS tag
            WHERE jsonb_typeof(tag) <> 'object'
               OR NOT (tag ? 'tagId')
               OR NOT (tag ? 'name')
               OR jsonb_typeof(tag -> 'name') <> 'string'
               OR jsonb_typeof(tag -> 'tagId') NOT IN ('number', 'null')
       );
$$;
--rollback DROP FUNCTION query_schema.is_valid_tag_snapshot(JSONB);

--changeset modera-api:028-replace-tag-names-with-tag-objects
--comment Store tag identifiers and names together as JSON objects in query read models.
ALTER TABLE query_schema.user_image_view
    ADD COLUMN tags JSONB NOT NULL DEFAULT '[]'::JSONB;

UPDATE query_schema.user_image_view AS image_view
SET tags = COALESCE(
    (
        SELECT jsonb_agg(
            jsonb_build_object(
                'tagId', NULL,
                'name', tag_name
            )
            ORDER BY tag_order
        )
        FROM unnest(
            COALESCE(image_view.tag_names, '{}'::TEXT[])
        ) WITH ORDINALITY AS source_tag(tag_name, tag_order)
    ),
    '[]'::JSONB
);

ALTER TABLE query_schema.user_image_view
    ADD CONSTRAINT ck_user_image_view_tags
        CHECK (query_schema.is_valid_tag_snapshot(tags)),
    DROP COLUMN tag_names;

ALTER TABLE query_schema.document_image_view
    ADD COLUMN tags JSONB NOT NULL DEFAULT '[]'::JSONB;

UPDATE query_schema.document_image_view AS document_image
SET tags = COALESCE(
    (
        SELECT jsonb_agg(
            jsonb_build_object(
                'tagId', NULL,
                'name', tag_name
            )
            ORDER BY tag_order
        )
        FROM unnest(
            COALESCE(document_image.tag_names, '{}'::TEXT[])
        ) WITH ORDINALITY AS source_tag(tag_name, tag_order)
    ),
    '[]'::JSONB
);

ALTER TABLE query_schema.document_image_view
    ADD CONSTRAINT ck_document_image_view_tags
        CHECK (query_schema.is_valid_tag_snapshot(tags)),
    DROP COLUMN tag_names;

--rollback ALTER TABLE query_schema.user_image_view ADD COLUMN tag_names TEXT[];
--rollback UPDATE query_schema.user_image_view AS image_view SET tag_names = (SELECT array_agg(tag ->> 'name' ORDER BY tag_order) FROM jsonb_array_elements(image_view.tags) WITH ORDINALITY AS source_tag(tag, tag_order));
--rollback ALTER TABLE query_schema.document_image_view ADD COLUMN tag_names TEXT[] NOT NULL DEFAULT '{}';
--rollback UPDATE query_schema.document_image_view AS document_image SET tag_names = COALESCE((SELECT array_agg(tag ->> 'name' ORDER BY tag_order) FROM jsonb_array_elements(document_image.tags) WITH ORDINALITY AS source_tag(tag, tag_order)), '{}'::TEXT[]);
--rollback ALTER TABLE query_schema.user_image_view DROP CONSTRAINT ck_user_image_view_tags, DROP COLUMN tags;
--rollback ALTER TABLE query_schema.document_image_view DROP CONSTRAINT ck_document_image_view_tags, DROP COLUMN tags;
