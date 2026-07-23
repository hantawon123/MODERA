--liquibase formatted sql

--changeset modera-api:100-convert-identifiers-to-integer
--comment Convert all requested identifiers to PostgreSQL INTEGER and remove the unused source_url.
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT count(*) FROM image_schema.image_asset
--precondition-sql-check expectedResult:0 SELECT count(*) FROM library_schema.user_image
--precondition-sql-check expectedResult:0 SELECT count(*) FROM library_schema.image_tag
--precondition-sql-check expectedResult:0 SELECT count(*) FROM query_schema.user_image_view
--precondition-sql-check expectedResult:0 SELECT count(*) FROM query_schema.image_search_document

ALTER TABLE user_schema.refresh_token
    DROP CONSTRAINT refresh_token_user_id_fkey;
ALTER TABLE user_schema.user_setting
    DROP CONSTRAINT user_setting_user_id_fkey;
ALTER TABLE library_schema.user_image
    DROP CONSTRAINT user_image_category_id_fkey;
ALTER TABLE library_schema.image_tag
    DROP CONSTRAINT image_tag_image_id_fkey,
    DROP CONSTRAINT image_tag_tag_id_fkey;

ALTER TABLE user_schema.users
    ALTER COLUMN user_id TYPE INTEGER;
ALTER TABLE user_schema.refresh_token
    ALTER COLUMN token_id TYPE INTEGER,
    ALTER COLUMN user_id TYPE INTEGER;
ALTER TABLE user_schema.user_setting
    ALTER COLUMN user_id TYPE INTEGER;

ALTER TABLE library_schema.category
    ALTER COLUMN category_id TYPE INTEGER,
    ALTER COLUMN user_id TYPE INTEGER;
ALTER TABLE library_schema.tag
    ALTER COLUMN tag_id TYPE INTEGER,
    ALTER COLUMN user_id TYPE INTEGER;
ALTER TABLE library_schema.user_image
    ALTER COLUMN image_id TYPE INTEGER USING NULL::INTEGER,
    ALTER COLUMN user_id TYPE INTEGER,
    ALTER COLUMN category_id TYPE INTEGER;
ALTER TABLE library_schema.image_tag
    ALTER COLUMN image_id TYPE INTEGER USING NULL::INTEGER,
    ALTER COLUMN tag_id TYPE INTEGER;

ALTER TABLE image_schema.image_asset
    ALTER COLUMN image_id TYPE INTEGER USING NULL::INTEGER,
    DROP COLUMN source_url;
CREATE SEQUENCE image_schema.image_asset_image_id_seq AS INTEGER;
ALTER TABLE image_schema.image_asset
    ALTER COLUMN image_id SET DEFAULT nextval('image_schema.image_asset_image_id_seq');
ALTER SEQUENCE image_schema.image_asset_image_id_seq
    OWNED BY image_schema.image_asset.image_id;

ALTER TABLE query_schema.user_image_view
    ALTER COLUMN user_id TYPE INTEGER,
    ALTER COLUMN image_id TYPE INTEGER USING NULL::INTEGER;
ALTER TABLE query_schema.image_search_document
    ALTER COLUMN image_id TYPE INTEGER USING NULL::INTEGER,
    ALTER COLUMN user_id TYPE INTEGER;
ALTER TABLE query_schema.search_history
    ALTER COLUMN history_id TYPE INTEGER,
    ALTER COLUMN user_id TYPE INTEGER;

ALTER SEQUENCE user_schema.users_user_id_seq AS INTEGER;
ALTER SEQUENCE user_schema.refresh_token_token_id_seq AS INTEGER;
ALTER SEQUENCE library_schema.category_category_id_seq AS INTEGER;
ALTER SEQUENCE library_schema.tag_tag_id_seq AS INTEGER;
ALTER SEQUENCE query_schema.search_history_history_id_seq AS INTEGER;

ALTER TABLE user_schema.refresh_token
    ADD CONSTRAINT refresh_token_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES user_schema.users(user_id) ON DELETE CASCADE;
ALTER TABLE user_schema.user_setting
    ADD CONSTRAINT user_setting_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES user_schema.users(user_id) ON DELETE CASCADE;
ALTER TABLE library_schema.user_image
    ADD CONSTRAINT user_image_category_id_fkey
        FOREIGN KEY (category_id) REFERENCES library_schema.category(category_id);
ALTER TABLE library_schema.image_tag
    ADD CONSTRAINT image_tag_image_id_fkey
        FOREIGN KEY (image_id) REFERENCES library_schema.user_image(image_id) ON DELETE CASCADE,
    ADD CONSTRAINT image_tag_tag_id_fkey
        FOREIGN KEY (tag_id) REFERENCES library_schema.tag(tag_id) ON DELETE CASCADE;
