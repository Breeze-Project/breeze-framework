package ru.breezeproject.core.loader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.configuration.file.YamlConfiguration;

import ru.breezeproject.api.config.ModuleConfig;
import ru.breezeproject.core.config.YamlModuleConfig;

public class ModuleConfigLoader {
  public ModuleConfig loadOrCreate(final JarFile jar, final Path moduleDataFolder) throws Exception {
    Files.createDirectories(moduleDataFolder);

    final Path configFile = moduleDataFolder.resolve("config.yml");
    if (!Files.exists(configFile)) {
      final JarEntry defaultConfigEntry = jar.getJarEntry("config.yml");
      if (defaultConfigEntry != null) {
        try (InputStream in = jar.getInputStream(defaultConfigEntry)) {
          Files.copy(in, configFile);
        }
      } else {
        Files.createFile(configFile);
      }
    }

    return new YamlModuleConfig(YamlConfiguration.loadConfiguration(configFile.toFile()), configFile);
  }
}
