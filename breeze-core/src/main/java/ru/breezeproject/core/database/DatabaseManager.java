package ru.breezeproject.core.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {
  private static final long CONNECTION_TIMEOUT_MS = 10_000;
  private static final long MAX_LIFETIME_MS = 1_800_000;
  private static final long LEAK_DETECTION_MS = 15_000;

  private final Logger logger;
  private HikariDataSource dataSource;
  private DatabaseVendor vendor;

  public DatabaseManager(final Logger logger) {
    this.logger = logger;
  }

  public void connect(final DatabaseConfig config) {
    this.vendor = DatabaseVendor.fromConfigValue(config.type());

    final HikariConfig hikariConfig = new HikariConfig();

    hikariConfig.setDriverClassName(vendor.driverClassName());
    hikariConfig.setJdbcUrl(vendor.buildJdbcUrl(config.host(), config.port(), config.name()));

    if (vendor.isRemote()) {
      hikariConfig.setUsername(config.user());
      hikariConfig.setPassword(config.password());
      hikariConfig.setMaximumPoolSize(config.poolSize());
      hikariConfig.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
      hikariConfig.setMaxLifetime(MAX_LIFETIME_MS);
      hikariConfig.setLeakDetectionThreshold(LEAK_DETECTION_MS);
    } else {
      hikariConfig.setMaximumPoolSize(1);
      hikariConfig.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
      hikariConfig.setMaxLifetime(0);
    }

    hikariConfig.setPoolName("BreezeCore-Pool");

    this.dataSource = new HikariDataSource(hikariConfig);

    if (vendor.isRemote()) {
      logger.info("Connected to " + vendor + " database '" + config.name() + "' at " + config.host() + ":" + config.port()
          + " (pool size " + config.poolSize() + ")");
    } else {
      logger.info("Connected to SQLite database at " + config.name());
    }
  }

  public Optional<Connection> getConnection() {
    if (dataSource == null || dataSource.isClosed()) {
      return Optional.empty();
    }
    try {
      return Optional.of(dataSource.getConnection());
    } catch (final SQLException e) {
      logger.warning("Failed to obtain database connection: " + e.getMessage());
      return Optional.empty();
    }
  }

  public Optional<HikariDataSource> getDataSource() {
    if (dataSource == null || dataSource.isClosed()) {
      return Optional.empty();
    }
    return Optional.of(dataSource);
  }

  public Optional<DatabaseVendor> getVendor() {
    return Optional.ofNullable(vendor);
  }

  public void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      logger.info("Database connection pool closed.");
    }
  }

  public boolean isConnected() {
    return dataSource != null && !dataSource.isClosed();
  }
}
