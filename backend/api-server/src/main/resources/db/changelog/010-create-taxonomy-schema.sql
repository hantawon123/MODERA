--liquibase formatted sql

--changeset modera-api:180-create-taxonomy-schema
--comment Create the immutable category and tag vocabulary in taxonomy_schema.
CREATE SCHEMA taxonomy_schema;

CREATE TABLE taxonomy_schema.category (
    category_id SERIAL PRIMARY KEY,
    name        VARCHAR(30) NOT NULL,
    vector      vector(768) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_taxonomy_category_name UNIQUE (name)
);

CREATE TABLE taxonomy_schema.tag (
    tag_id     SERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    vector     vector(768) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_taxonomy_tag_name UNIQUE (name)
);

--rollback DROP TABLE taxonomy_schema.tag;
--rollback DROP TABLE taxonomy_schema.category;
--rollback DROP SCHEMA taxonomy_schema;
