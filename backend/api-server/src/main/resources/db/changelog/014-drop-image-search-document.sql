--liquibase formatted sql

--changeset modera-api:014-drop-image-search-document
DROP TABLE query_schema.image_search_document;
--rollback CREATE TABLE query_schema.image_search_document (image_id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL, title VARCHAR(100), summary TEXT, ocr_text TEXT, category_name VARCHAR(30), tag_names TEXT[], structured_fields JSONB, embedding vector(768), indexed_at TIMESTAMPTZ, del_yn CHAR(1) NOT NULL DEFAULT 'N', CONSTRAINT ck_image_search_document_del_yn CHECK (del_yn IN ('Y', 'N')));
--rollback CREATE INDEX ix_search_document_user ON query_schema.image_search_document (user_id);
--rollback CREATE INDEX ix_search_document_ocr_bigm ON query_schema.image_search_document USING gin (ocr_text gin_bigm_ops);
