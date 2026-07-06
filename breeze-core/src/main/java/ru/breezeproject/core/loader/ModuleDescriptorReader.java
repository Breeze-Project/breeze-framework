package ru.breezeproject.core.loader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.configuration.file.YamlConfiguration;

import ru.breezeproject.api.module.ModuleDescription;

public class ModuleDescriptorReader {
  public Optional<ModuleDescription> read(final JarFile jar) throws IOException {
    final JarEntry entry = jar.getJarEntry("module.yml");
    if (entry == null) {
      return null;
    }

    final YamlConfiguration yaml = YamlConfiguration
        .loadConfiguration(new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8));

    return Optional.of(new ModuleDescription(
        yaml.getString("name"),
        yaml.getString("version", "?"),
        yaml.getString("main"),
        yaml.getString("api-version"),
        yaml.getStringList("depends")));
  }
}
