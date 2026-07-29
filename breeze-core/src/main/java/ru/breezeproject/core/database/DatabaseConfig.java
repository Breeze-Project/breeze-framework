package ru.breezeproject.core.database;

import org.bukkit.configuration.file.FileConfiguration;

public record DatabaseConfig(
    String type,
    String host,
    int port,
    String name,
    String user,
    String password,
    int poolSize) {

  public static DatabaseConfig fromConfig(final FileConfiguration config) {
    final String type = config.getString("database.type", "mysql");
    final DatabaseVendor vendor;
    try {
      vendor = DatabaseVendor.fromConfigValue(type);
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid database.type '" + type + "' in config.yml", e);
    }

    final String host = vendor.isRemote()
        ? config.getString("database.host", "localhost")
        : null;

    final int port = config.contains("database.port")
        ? config.getInt("database.port")
        : vendor.defaultPort();

    final String name = vendor.isRemote()
        ? config.getString("database.name", "breezecore")
        : config.getString("database.file", "breezecore.db");

    final String user = vendor.isRemote()
        ? config.getString("database.user", "root")
        : null;

    final String password = vendor.isRemote()
        ? config.getString("database.password", "")
        : null;

    final int poolSize = vendor.isRemote()
        ? config.getInt("database.pool-size", 10)
        : 1;

    return new DatabaseConfig(type, host, port, name, user, password, poolSize);
  }
}
