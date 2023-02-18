CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--;

create table users (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    email      TEXT NOT NULL,
    UNIQUE (email)
);

--;

create table feeds (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    user_id             UUID NOT NULL,
    url_source          TEXT NOT NULL,
    url_website         TEXT,
    url_favicon         TEXT,
    url_image           TEXT,
    domain              TEXT,
    language            TEXT,
    title               TEXT,
    subtitle            TEXT,
    http_status         INTEGER,
    http_etag           TEXT,
    http_last_modified  TEXT,
    sync_interval       INTEGER NOT NULL DEFAULT 3600,
    sync_date_next      TIMESTAMP WITHOUT TIME ZONE,
    sync_count          INTEGER NOT NULL DEFAULT 0,
    sync_msg_count      INTEGER NOT NULL DEFAULT 0,
    entry_count_total   INTEGER NOT NULL DEFAULT 0,
    entry_count_unread  INTEGER NOT NULL DEFAULT 0,
    UNIQUE (user_id, url_source)
);

--;

create table entries (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    feed_id    UUID,
    guid       TEXT NOT NULL,
    link       TEXT,
    author     TEXT,
    title      TEXT,
    summary    TEXT,
    date_published_at  TIMESTAMP WITHOUT TIME ZONE,
    date_updated_at    TIMESTAMP WITHOUT TIME ZONE,
    UNIQUE (feed_id, guid)
);

--;

create table enclosures (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    entry_id   UUID,
    url        TEXT,
    length     INTEGER,
    type       TEXT
)
