package ru.breezeproject.core.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import ru.breezeproject.api.config.ModuleConfig;

public class YamlModuleConfig implements ModuleConfig {
  private YamlConfiguration root;
  private ConfigurationSection section;
  private final Path configFile;

  public YamlModuleConfig(YamlConfiguration configuration, Path configFile) {
    this.root = configuration;
    this.section = configuration;
    this.configFile = configFile;
  }

  private YamlModuleConfig(YamlConfiguration root, ConfigurationSection section, Path configFile) {
    this.root = root;
    this.section = section;
    this.configFile = configFile;
  }

  @Override
  public String getString(String path) {
    return section.getString(path);
  }

  @Override
  public String getString(String path, String def) {
    return section.getString(path, def);
  }

  @Override
  public int getInt(String path) {
    return section.getInt(path);
  }

  @Override
  public int getInt(String path, int def) {
    return section.getInt(path, def);
  }

  @Override
  public double getDouble(String path) {
    return section.getDouble(path);
  }

  @Override
  public double getDouble(String path, double def) {
    return section.getDouble(path, def);
  }

  @Override
  public boolean getBoolean(String path) {
    return section.getBoolean(path);
  }

  @Override
  public boolean getBoolean(String path, boolean def) {
    return section.getBoolean(path, def);
  }

  @Override
  public List<?> getList(String path) {
    List<?> list = section.getList(path);
    return list == null ? Collections.emptyList() : list;
  }

  @Override
  public List<String> getStringList(String path) {
    return section.getStringList(path);
  }

  @Override
  public Set<String> getKeys() {
    return section.getKeys(false);
  }

  @Override
  public ModuleConfig getSection(String path) {
    ConfigurationSection nested = section.getConfigurationSection(path);
    return nested == null ? null : new YamlModuleConfig(root, nested, configFile);
  }

  @Override
  public void save() throws IOException {
    root.save(configFile.toFile());
  }

  @Override
  public void reload() {
    this.root = YamlConfiguration.loadConfiguration(configFile.toFile());
    this.section = root;
  }
}