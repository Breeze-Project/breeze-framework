package ru.breezeproject.core.database;

import java.util.Arrays;

public enum DatabaseVendor {
  MYSQL("mysql", 3306, "com.mysql.cj.jdbc.Driver", true) {
    @Override
    String buildJdbcUrl(final String host, final int port, final String database) {
      return "jdbc:mysql://" + host + ":" + port + "/" + database
          + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
    }
  },
  POSTGRESQL("postgresql", 5432, "org.postgresql.Driver", true) {
    @Override
    String buildJdbcUrl(final String host, final int port, final String database) {
      return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }
  },
  SQLITE("sqlite", -1, "org.sqlite.JDBC", false) {
    @Override
    String buildJdbcUrl(final String host, final int port, final String database) {
      return "jdbc:sqlite:" + database;
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
            "Unknown database.type '" + value + "'. Supported values: mysql, postgresql, sqlite"));
  }

  private final String configKey;
  private final int defaultPort;
  private final String driverClassName;
  private final boolean remote;

  DatabaseVendor(final String configKey, final int defaultPort, final String driverClassName, final boolean remote) {
    this.configKey = configKey;
    this.defaultPort = defaultPort;
    this.driverClassName = driverClassName;
    this.remote = remote;
  }

  public int defaultPort() {
    return defaultPort;
  }

  public String driverClassName() {
    return driverClassName;
  }

  public boolean isRemote() {
    return remote;
  }

  public String migrationsLocation() {
    return "classpath:migrations/" + configKey + "/";
  }

  public String rollbackResourceRoot() {
    return "rollback/" + configKey;
  }

  abstract String buildJdbcUrl(String host, int port, String database);
}
