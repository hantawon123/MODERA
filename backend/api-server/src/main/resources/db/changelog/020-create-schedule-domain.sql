--liquibase formatted sql

--changeset modera-api:020-create-schedule-object
CREATE SCHEMA schedule_schema;

CREATE TABLE schedule_schema.schedule (
    schedule_id  SERIAL PRIMARY KEY,
    title        VARCHAR(100) NOT NULL,
    start_at     TIMESTAMPTZ DEFAULT NULL,
    end_at       TIMESTAMPTZ DEFAULT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_yn       CHAR(1) NOT NULL DEFAULT 'N',

    CONSTRAINT ck_schedule_period
        CHECK (
            start_at IS NULL
            OR end_at IS NULL
            OR end_at >= start_at
        ),

    CONSTRAINT ck_schedule_del_yn
        CHECK (del_yn IN ('Y', 'N'))
);
--rollback DROP SCHEMA schedule_schema CASCADE;

--changeset modera-api:020-create-user-schedule-relation
CREATE TABLE library_schema.user_schedule (
    user_schedule_id  SERIAL PRIMARY KEY,
    user_id           INTEGER NOT NULL,
    schedule_id       INTEGER NOT NULL,
    is_calendared_yn  CHAR(1) NOT NULL DEFAULT 'N',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_yn            CHAR(1) NOT NULL DEFAULT 'N',

    CONSTRAINT uq_user_schedule_user_id_schedule_id
        UNIQUE (user_id, schedule_id),

    CONSTRAINT uq_user_schedule_schedule_id
        UNIQUE (schedule_id),

    CONSTRAINT ck_user_schedule_is_calendared_yn
        CHECK (is_calendared_yn IN ('Y', 'N')),

    CONSTRAINT ck_user_schedule_del_yn
        CHECK (del_yn IN ('Y', 'N'))
);

CREATE INDEX ix_user_schedule_user_id
    ON library_schema.user_schedule (user_id);
--rollback DROP TABLE library_schema.user_schedule;

--changeset modera-api:020-create-image-schedule-relation
CREATE TABLE library_schema.image_schedule (
    image_schedule_id SERIAL PRIMARY KEY,
    image_id          INTEGER NOT NULL,
    schedule_id       INTEGER NOT NULL,

    CONSTRAINT uq_image_schedule_schedule_id
        UNIQUE (schedule_id)
);

CREATE INDEX ix_image_schedule_image_id
    ON library_schema.image_schedule (image_id);
--rollback DROP TABLE library_schema.image_schedule;

--changeset modera-api:020-create-user-schedule-view
CREATE TABLE query_schema.user_schedule_view (
    user_id           INTEGER NOT NULL,
    schedule_id       INTEGER NOT NULL,
    image_id          INTEGER NOT NULL,
    title             VARCHAR(100) NOT NULL,
    start_at          TIMESTAMPTZ DEFAULT NULL,
    end_at            TIMESTAMPTZ DEFAULT NULL,
    is_calendared_yn  CHAR(1) NOT NULL DEFAULT 'N',
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    del_yn            CHAR(1) NOT NULL DEFAULT 'N',

    PRIMARY KEY (user_id, schedule_id),

    CONSTRAINT ck_user_schedule_view_period
        CHECK (
            start_at IS NULL
            OR end_at IS NULL
            OR end_at >= start_at
        ),

    CONSTRAINT ck_user_schedule_view_is_calendared_yn
        CHECK (is_calendared_yn IN ('Y', 'N')),

    CONSTRAINT ck_user_schedule_view_del_yn
        CHECK (del_yn IN ('Y', 'N'))
);

CREATE INDEX ix_user_schedule_view_calendar
    ON query_schema.user_schedule_view (
        user_id,
        is_calendared_yn,
        del_yn,
        start_at
    );
--rollback DROP TABLE query_schema.user_schedule_view;
