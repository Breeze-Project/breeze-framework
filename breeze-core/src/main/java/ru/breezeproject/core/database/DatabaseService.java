package ru.breezeproject.core.database;

import java.util.Optional;

import javax.sql.DataSource;

public interface DatabaseService {
  void connect(DatabaseConfig config);

  void migrate();

  void rollbackTo(int targetVersion);

  void shutdown();

  boolean isConnected();

  Optional<DataSource> getDataSource();

  Optional<DatabaseVendor> getVendor();
}
