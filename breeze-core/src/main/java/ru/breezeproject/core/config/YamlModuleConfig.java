package ru.breezeproject.core.config;

import java.io.IOException;
import java.nio.file.Path;

import org.bukkit.configuration.file.YamlConfiguration;

import ru.breezeproject.api.config.ModuleConfig;

public class YamlModuleConfig implements ModuleConfig {
  private YamlConfiguration configuration;
  private final Path configFile;

  public YamlModuleConfig(final YamlConfiguration configuration, final Path configFile) {
    this.configuration = configuration;
    this.configFile = configFile;
  }

  @Override
  public String getString(final String path) {
    return configuration.getString(path);
  }

  @Override
  public String getString(final String path, final String def) {
    return configuration.getString(path, def);
  }

  @Override
  public int getInt(final String path) {
    return configuration.getInt(path);
  }

  @Override
  public int getInt(final String path, final int def) {
    return configuration.getInt(path, def);
  }

  @Override
  public boolean getBoolean(final String path) {
    return configuration.getBoolean(path);
  }

  @Override
  public boolean getBoolean(final String path, final boolean def) {
    return configuration.getBoolean(path, def);
  }

  @Override
  public void save() throws IOException {
    configuration.save(configFile.toFile());
  }

  @Override
  public void reload() {
    this.configuration = YamlConfiguration.loadConfiguration(configFile.toFile());
  }
}
