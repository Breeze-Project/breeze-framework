package ru.breezeproject.core.config;

import java.io.IOException;
import java.nio.file.Path;

import org.bukkit.configuration.file.YamlConfiguration;

import ru.breezeproject.api.config.ModuleConfig;

public class YamlModuleConfig implements ModuleConfig {
  private YamlConfiguration configuration;
  private final Path configFile;

  public YamlModuleConfig(YamlConfiguration configuration, Path configFile) {
    this.configuration = configuration;
    this.configFile = configFile;
  }

  @Override
  public String getString(String path) {
    return configuration.getString(path);
  }

  @Override
  public String getString(String path, String def) {
    return configuration.getString(path, def);
  }

  @Override
  public int getInt(String path) {
    return configuration.getInt(path);
  }

  @Override 
  public int getInt(String path, int def) {
    return configuration.getInt(path, def);
  }

  @Override
  public boolean getBoolean(String path) {
    return configuration.getBoolean(path);
  }

  @Override
  public boolean getBoolean(String path, boolean def) {
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
