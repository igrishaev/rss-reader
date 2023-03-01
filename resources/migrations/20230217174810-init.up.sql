CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--;

CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at   TIMESTAMP WITHOUT TIME ZONE,
    email        TEXT NOT NULL,
    UNIQUE (email)
);

--;

CREATE TABLE feeds (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at          TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at          TIMESTAMP WITHOUT TIME ZONE,
    url_source          TEXT NOT NULL,
    url_website         TEXT,
    url_favicon         TEXT,
    url_icon            TEXT,
    url_image           TEXT,
    rss_feed_type       TEXT,
    rss_encoding        TEXT,
    rss_description     TEXT,
    rss_domain          TEXT,
    rss_language        TEXT,
    rss_title           TEXT,
    rss_author          TEXT,
    rss_editor          TEXT,
    rss_subtitle        TEXT,
    rss_published_at    TIMESTAMP WITHOUT TIME ZONE,
    http_status         INTEGER,
    http_etag           TEXT,
    http_last_modified  TEXT,
    sync_interval       INTEGER NOT NULL DEFAULT 3600,
    sync_date_prev      TIMESTAMP WITHOUT TIME ZONE,
    sync_date_next      TIMESTAMP WITHOUT TIME ZONE,
    sync_count          INTEGER NOT NULL DEFAULT 0,
    entries_count       INTEGER NOT NULL DEFAULT 0,
    err_attempts        INTEGER NOT NULL DEFAULT 0,
    err_class           TEXT,
    err_message         TEXT,
    is_stopped          BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (url_source)
);

--;

CREATE TABLE subscriptions (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    user_id           UUID NOT NULL,
    feed_id           UUID NOT NULL,
    unread_count      INTEGER NOT NULL DEFAULT 0,
    sync_count        INTEGER NOT NULL DEFAULT 0,
    sync_interval     INTEGER NOT NULL DEFAULT 3600,
    sync_date_prev    TIMESTAMP WITHOUT TIME ZONE,
    sync_date_next    TIMESTAMP WITHOUT TIME ZONE,
    opt_title         TEXT,
    opt_order         TEXT,
    opt_layout        TEXT,
    opt_unread_only   BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (feed_id, user_id)
);

--;

CREATE TABLE entries (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at         TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at         TIMESTAMP WITHOUT TIME ZONE,
    feed_id            UUID NOT NULL,
    guid               TEXT NOT NULL,
    link               TEXT,
    author             TEXT,
    title              TEXT,
    summary            TEXT,
    teaser             TEXT,
    date_published_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    date_updated_at    TIMESTAMP WITHOUT TIME ZONE,
    UNIQUE (feed_id, guid)
);

--;

CREATE TABLE messages (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    entry_id          UUID NOT NULL,
    subscription_id   UUID NOT NULL,
    date_published_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,
    is_marked         BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (entry_id, subscription_id)
);

--;

CREATE INDEX idx_messages_cursor ON messages USING BTREE
((extract(epoch from date_published_at)::text || '|' || id));

--;

CREATE TABLE enclosures (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    parent_id  UUID NOT NULL,
    url        TEXT,
    length     INTEGER,
    type       TEXT
);

--;

CREATE TABLE categories (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    parent_id     UUID NOT NULL,
    parent_type   TEXT NOT NULL,
    category      TEXT NOT NULL,
    UNIQUE (parent_id, category)
);

--;

CREATE TABLE auth_codes (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    email         TEXT NOT NULL
);
