package ru.breezeproject.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseManager {
  private static final long CONNECTION_TIMEOUT_MS = 10_000;
  private static final long MAX_LIFETIME_MS = 1_800_000;
  private static final long LEAK_DETECTION_MS = 15_000;

  private final Logger logger;
  private HikariDataSource dataSource;
  private DatabaseVendor vendor;

  public DatabaseManager(Logger logger) {
    this.logger = logger;
  }

  public void connect(DatabaseConfig config) {
    this.vendor = DatabaseVendor.fromConfigValue(config.type());

    HikariConfig hikariConfig = new HikariConfig();

    hikariConfig.setDriverClassName(vendor.driverClassName());
    hikariConfig.setJdbcUrl(vendor.buildJdbcUrl(config.host(), config.port(), config.name()));
    hikariConfig.setUsername(config.user());
    hikariConfig.setPassword(config.password());
    hikariConfig.setMaximumPoolSize(config.poolSize());
    hikariConfig.setPoolName("BreezeCore-Pool");

    hikariConfig.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
    hikariConfig.setMaxLifetime(MAX_LIFETIME_MS);
    hikariConfig.setLeakDetectionThreshold(LEAK_DETECTION_MS);

    this.dataSource = new HikariDataSource(hikariConfig);
    logger.info("Connected to " + vendor + " database '" + config.name() + "' at " + config.host() + ":" + config.port()
        + " (pool size " + config.poolSize() + ")");
  }

  public Connection getConnection() throws SQLException {
    if (dataSource == null) {
      throw new IllegalStateException("DatabaseManager.connect() was not called before getConnection()");
    }
    return dataSource.getConnection();
  }

  public HikariDataSource getDataSource() {
    if (dataSource == null) {
      throw new IllegalStateException("DatabaseManager.connect() was not called before getDataSource()");
    }
    return dataSource;
  }

  public DatabaseVendor getVendor() {
    if (vendor == null) {
      throw new IllegalStateException("DatabaseManager.connect() was not called before getVendor()");
    }
    return vendor;
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
