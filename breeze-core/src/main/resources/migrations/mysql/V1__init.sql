CREATE TABLE IF NOT EXISTS breeze_players (
    uuid        CHAR(36)     NOT NULL PRIMARY KEY,
    username    VARCHAR(16)  NOT NULL,
    first_join  BIGINT       NOT NULL,
    last_join   BIGINT       NOT NULL,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS breeze_module_registry (
    module_name VARCHAR(64) NOT NULL PRIMARY KEY,
    version     VARCHAR(32) NOT NULL,
    last_loaded BIGINT      NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
