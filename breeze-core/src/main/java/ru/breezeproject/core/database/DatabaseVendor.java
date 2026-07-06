package ru.breezeproject.core.database;

import java.util.Arrays;

public enum DatabaseVendor {
  MYSQL("mysql", 3306, "com.mysql.cj.jdbc.Driver") {
    @Override
    String buildJdbcUrl(final String host, final int port, final String database) {
      return "jdbc:mysql://" + host + ":" + port + "/" + database
          + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
    }
  },
  POSTGRESQL("postgresql", 5432, "org.postgresql.Driver") {
    @Override
    String buildJdbcUrl(final String host, final int port, final String database) {
      return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }
  };

  public static DatabaseVendor fromConfigValue(final String value) {
    if (value == null || value.isBlank()) {
      return MYSQL;
    }
    return Arrays.stream(values())
        .filter(v -> v.configKey.equalsIgnoreCase(value.trim()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown database.type '" + value + "'. Supported values: mysql, postgresql"));
  }

  private final String configKey;
  private final int defaultPort;

  private final String driverClassName;

  DatabaseVendor(final String configKey, final int defaultPort, final String driverClassName) {
    this.configKey = configKey;
    this.defaultPort = defaultPort;
    this.driverClassName = driverClassName;
  }

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

  abstract String buildJdbcUrl(String host, int port, String database);
}
