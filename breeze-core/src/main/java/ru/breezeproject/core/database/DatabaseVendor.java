package ru.breezeproject.core.database;

public enum DatabaseVendor {

    MYSQL("mysql", 3306, "com.mysql.cj.jdbc.Driver") {
        @Override
        String buildJdbcUrl(String host, int port, String database) {
            return "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
        }
    },
    POSTGRESQL("postgresql", 5432, "org.postgresql.Driver") {
        @Override
        String buildJdbcUrl(String host, int port, String database) {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database;
        }
    };

    private final String configKey;
    private final int defaultPort;
    private final String driverClassName;

    DatabaseVendor(String configKey, int defaultPort, String driverClassName) {
        this.configKey = configKey;
        this.defaultPort = defaultPort;
        this.driverClassName = driverClassName;
    }

    abstract String buildJdbcUrl(String host, int port, String database);

    public int defaultPort() {
        return defaultPort;
    }

    public String driverClassName() {
        return driverClassName;
    }

    public String migrationsLocation() {
        return "classpath:migrations/" + configKey + "/";
    }

    public String rollbackResourceRoot() {
        return "rollback/" + configKey;
    }

    public static DatabaseVendor fromConfigValue(String value, java.util.logging.Logger logger) {
        if (value == null || value.isBlank()) {
            return MYSQL;
        }
        for (DatabaseVendor vendor : values()) {
            if (vendor.configKey.equalsIgnoreCase(value.trim())) {
                return vendor;
            }
        }
        logger.warning("Unknown database.type '" + value + "', falling back to mysql. "
                + "Supported values: mysql, postgresql.");
        return MYSQL;
    }
}
