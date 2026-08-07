--liquibase formatted sql

--changeset modera-api:016-create-user-document-view
CREATE TABLE query_schema.user_document_view (
    user_id       INTEGER NOT NULL,
    document_id   INTEGER NOT NULL,
    name          VARCHAR(255) NOT NULL,
    content       TEXT NOT NULL,
    image_count   INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_yn        CHAR(1) NOT NULL DEFAULT 'N',

    PRIMARY KEY (user_id, document_id),

    CONSTRAINT ck_user_document_view_del_yn
        CHECK (del_yn IN ('Y', 'N')),

    CONSTRAINT ck_user_document_view_image_count
        CHECK (image_count >= 0)
);
--rollback DROP TABLE query_schema.user_document_view;
