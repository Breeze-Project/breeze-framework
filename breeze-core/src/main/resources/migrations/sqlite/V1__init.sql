CREATE TABLE IF NOT EXISTS breeze_players (
    uuid        TEXT     NOT NULL PRIMARY KEY,
    username    TEXT     NOT NULL,
    first_join  INTEGER  NOT NULL,
    last_join   INTEGER  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_username ON breeze_players (username);

CREATE TABLE IF NOT EXISTS breeze_module_registry (
    module_name TEXT    NOT NULL PRIMARY KEY,
    version     TEXT    NOT NULL,
    last_loaded INTEGER NOT NULL
);
