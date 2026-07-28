--liquibase formatted sql

--changeset modera-api:015-drop-search-history
DROP TABLE query_schema.search_history;
--rollback CREATE TABLE query_schema.search_history (history_id SERIAL PRIMARY KEY, user_id INTEGER NOT NULL, query VARCHAR(200) NOT NULL, search_type VARCHAR(10) NOT NULL, searched_at TIMESTAMPTZ NOT NULL DEFAULT now(), del_yn CHAR(1) NOT NULL DEFAULT 'N', CONSTRAINT ck_search_history_del_yn CHECK (del_yn IN ('Y', 'N')));
--rollback CREATE INDEX ix_search_history_user ON query_schema.search_history (user_id, searched_at DESC);
