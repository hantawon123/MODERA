--liquibase formatted sql

--changeset modera-api:030-create-tag-names-search-text-function splitStatements:false
--comment Build a normalized search string from tag names stored in the query read model JSON snapshot.
CREATE FUNCTION query_schema.tag_names_search_text(tags_value JSONB)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
STRICT
PARALLEL SAFE
AS $$
    SELECT COALESCE(
        string_agg(tag ->> 'name', ' ' ORDER BY tag_order),
        ''
    )
    FROM jsonb_array_elements(tags_value)
        WITH ORDINALITY AS source_tag(tag, tag_order);
$$;
--rollback DROP FUNCTION query_schema.tag_names_search_text(JSONB);

--changeset modera-api:030-add-user-image-keyword-search-indexes
--comment Add pg_bigm indexes for case-insensitive partial matching on image titles and tag names.
CREATE INDEX ix_user_image_view_title_bigm
    ON query_schema.user_image_view
    USING GIN (LOWER(title) gin_bigm_ops)
    WHERE del_yn = 'N'
      AND title IS NOT NULL;

CREATE INDEX ix_user_image_view_tag_names_bigm
    ON query_schema.user_image_view
    USING GIN (
        LOWER(query_schema.tag_names_search_text(tags)) gin_bigm_ops
    )
    WHERE del_yn = 'N';

--rollback DROP INDEX query_schema.ix_user_image_view_tag_names_bigm;
--rollback DROP INDEX query_schema.ix_user_image_view_title_bigm;
