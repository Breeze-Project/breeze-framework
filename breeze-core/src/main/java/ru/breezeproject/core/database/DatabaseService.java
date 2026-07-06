package ru.breezeproject.core.database;

import javax.sql.DataSource;

public interface DatabaseService {
  void connect(DatabaseConfig config);

  void migrate();

  void rollbackTo(int targetVersion);

  void shutdown();

  boolean isConnected();

  DataSource getDataSource();

  DatabaseVendor getVendor();
}
