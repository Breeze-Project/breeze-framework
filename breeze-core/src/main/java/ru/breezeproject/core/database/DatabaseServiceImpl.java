package ru.breezeproject.core.database;

import java.util.Optional;
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
    connectionManager.getDataSource().ifPresent(dataSource ->
        connectionManager.getVendor().ifPresent(vendor ->
            migrator.migrate(dataSource, vendor)));
  }

  @Override
  public void rollbackTo(final int targetVersion) {
    connectionManager.getDataSource().ifPresent(dataSource ->
        connectionManager.getVendor().ifPresent(vendor ->
            rollbackRunner.rollbackTo(dataSource, vendor, targetVersion)));
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
  public Optional<DataSource> getDataSource() {
    return connectionManager.getDataSource().map(ds -> (DataSource) ds);
  }

  @Override
  public Optional<DatabaseVendor> getVendor() {
    return connectionManager.getVendor();
  }
}
