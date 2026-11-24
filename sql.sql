-- DeepFocus local SQLite schema, version 3.
-- Android database name: deep_focus.db
-- The first section is the fresh-install schema.

CREATE TABLE deployed_fish (
    name TEXT PRIMARY KEY NOT NULL,
    description TEXT NOT NULL,
    temperament TEXT NOT NULL,
    rarity TEXT NOT NULL,
    water TEXT NOT NULL,
    bond INTEGER NOT NULL,
    image_res INTEGER NOT NULL,
    image_key TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL
);

CREATE TABLE todos (
    id INTEGER PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    completed INTEGER NOT NULL
);

CREATE TABLE focus_settings (
    id INTEGER PRIMARY KEY NOT NULL,
    minutes INTEGER NOT NULL
);

CREATE TABLE wallet (
    id INTEGER PRIMARY KEY NOT NULL,
    coins INTEGER NOT NULL DEFAULT 1280
);

CREATE TABLE app_state (
    id INTEGER PRIMARY KEY NOT NULL,
    selected_scene_name TEXT NOT NULL DEFAULT '月光海沟',
    today_focus_minutes INTEGER NOT NULL DEFAULT 0,
    today_date TEXT NOT NULL DEFAULT '',
    user_name TEXT NOT NULL DEFAULT '',
    theme_mode TEXT NOT NULL DEFAULT 'SYSTEM'
);

CREATE TABLE focus_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    scene_name TEXT NOT NULL,
    planned_minutes INTEGER NOT NULL,
    actual_seconds INTEGER NOT NULL,
    reward_coins INTEGER NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('COMPLETED', 'ABORTED')),
    started_at INTEGER NOT NULL,
    ended_at INTEGER NOT NULL
);

CREATE TABLE active_session (
    session_id INTEGER PRIMARY KEY NOT NULL,
    scene_name TEXT NOT NULL,
    planned_minutes INTEGER NOT NULL,
    remaining_seconds INTEGER NOT NULL,
    state TEXT NOT NULL,
    started_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE notification_whitelist (
    package_name TEXT PRIMARY KEY NOT NULL,
    app_name TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1
);

-- Version 2 migration from the original version 1 schema.
-- Run these two statements against an existing version 1 database instead of
-- running them after the fresh-install section.
-- ALTER TABLE deployed_fish ADD COLUMN image_key TEXT NOT NULL DEFAULT '';
-- UPDATE deployed_fish SET image_key = name WHERE image_key = '';

-- Version 3 migration from version 2 schema.
-- ALTER TABLE app_state ADD COLUMN user_name TEXT NOT NULL DEFAULT '';
-- ALTER TABLE app_state ADD COLUMN theme_mode TEXT NOT NULL DEFAULT 'SYSTEM';

-- Supporting rows created by SQLiteOpenHelper.onCreate/onUpgrade.
INSERT OR IGNORE INTO wallet (id, coins) VALUES (1, 1280);
INSERT OR IGNORE INTO app_state (
    id, selected_scene_name, today_focus_minutes, today_date, user_name, theme_mode
) VALUES (
    1, '月光海沟', 0, date('now'), '', 'SYSTEM'
);
