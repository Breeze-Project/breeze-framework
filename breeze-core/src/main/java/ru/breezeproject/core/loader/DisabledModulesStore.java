package ru.breezeproject.core.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;

final class DisabledModulesStore {
  private final Path file;
  private final Logger logger;
  private final Set<String> disabled = new LinkedHashSet<>();
  private final Object lock = new Object();

  DisabledModulesStore(final Path pluginDataFolder, final Logger logger) {
    this.file = pluginDataFolder.resolve("disabled-modules.yml");
    this.logger = logger;
    load();
  }

  void load() {
    synchronized (lock) {
      disabled.clear();
      if (!Files.exists(file)) {
        return;
      }
      final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
      for (final String name : yaml.getStringList("disabled")) {
        if (name != null && !name.isBlank()) {
          disabled.add(name);
        }
      }
    }
  }

  void disable(final String name) {
    synchronized (lock) {
      disabled.add(name);
      save();
    }
  }

  void enable(final String name) {
    synchronized (lock) {
      disabled.remove(name);
      save();
    }
  }

  boolean isDisabled(final String name) {
    synchronized (lock) {
      return disabled.contains(name);
    }
  }

  Set<String> getDisabled() {
    synchronized (lock) {
      return Set.copyOf(disabled);
    }
  }

  private void save() {
    try {
      final Path parent = file.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      final YamlConfiguration yaml = new YamlConfiguration();
      yaml.set("disabled", new ArrayList<>(disabled));
      yaml.save(file.toFile());
    } catch (final IOException e) {
      logger.warning("Could not save disabled-modules.yml: " + e.getMessage());
    }
  }
}
