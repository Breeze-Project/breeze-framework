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
  private YamlConfiguration configuration;
  private final Path configFile;
  private final ConfigurationSection section;

  public YamlModuleConfig(final YamlConfiguration configuration, final Path configFile) {
    this(configuration, configFile, configuration);
  }

  private YamlModuleConfig(final YamlConfiguration configuration, final Path configFile,
      final ConfigurationSection section) {
    this.configuration = configuration;
    this.configFile = configFile;
    this.section = section;
  }

  @Override
  public String getString(final String path) {
    return section.getString(path);
  }

  @Override
  public String getString(final String path, final String def) {
    return section.getString(path, def);
  }

  @Override
  public int getInt(final String path) {
    return section.getInt(path);
  }

  @Override
  public int getInt(final String path, final int def) {
    return section.getInt(path, def);
  }

  @Override
  public double getDouble(final String path) {
    return section.getDouble(path);
  }

  @Override
  public double getDouble(final String path, final double def) {
    return section.getDouble(path, def);
  }

  @Override
  public boolean getBoolean(final String path) {
    return section.getBoolean(path);
  }

  @Override
  public boolean getBoolean(final String path, final boolean def) {
    return section.getBoolean(path, def);
  }

  @Override
  public ModuleConfig getSection(final String path) {
    final ConfigurationSection child = section.getConfigurationSection(path);
    if (child == null) {
      return null;
    }
    return new YamlModuleConfig(configuration, configFile, child);
  }

  @Override
  public List<?> getList(final String path) {
    return section.getList(path, Collections.emptyList());
  }

  @Override
  public Set<String> getKeys() {
    return section.getKeys(false);
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
