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
  public static DatabaseConfig fromConfig(FileConfiguration config) {
    return new DatabaseConfig(config.getString("database.type", "mysql"),
        config.getString("database.host", "localhost"),
        config.getInt("database.port", 3306),
        config.getString("database.name", "breezecore"),
        config.getString("database.user", "root"),
        config.getString("database.password", ""),
        config.getInt("database.pool-size", 10));
  }
}
