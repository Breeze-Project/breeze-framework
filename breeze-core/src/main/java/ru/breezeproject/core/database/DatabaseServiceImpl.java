package ru.breezeproject.core.database;

import java.util.logging.Logger;

import javax.sql.DataSource;

public class DatabaseServiceImpl implements DatabaseService {
  private final DatabaseManager connectionManager;
  private final DatabaseMigrator migrator;
  private final RollbackRunner rollbackRunner;

  public DatabaseServiceImpl(final Logger logger) {
    this.connectionManager = new DatabaseManager(logger);
    this.migrator = new DatabaseMigrator(logger);
    this.rollbackRunner = new RollbackRunner(logger);
  }

  @Override
  public void connect(final DatabaseConfig config) {
    connectionManager.connect(config);
  }

  @Override
  public void migrate() {
    migrator.migrate(connectionManager.getDataSource(), connectionManager.getVendor());
  }

  @Override
  public void rollbackTo(final int targetVersion) {
    rollbackRunner.rollbackTo(connectionManager.getDataSource(), connectionManager.getVendor(), targetVersion);
  }

  @Override
  public void shutdown() {
    connectionManager.shutdown();
  }

  @Override
  public boolean isConnected() {
    return connectionManager.isConnected();
  }

  @Override
  public DataSource getDataSource() {
    return connectionManager.getDataSource();
  }

  @Override
  public DatabaseVendor getVendor() {
    return connectionManager.getVendor();
  }
}
