package ru.breezeproject.core.loader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.configuration.file.YamlConfiguration;

import ru.breezeproject.api.module.ModuleDescription;

public class ModuleDescriptorReader {
  public ModuleDescription read(JarFile jar) throws IOException {
    JarEntry entry = jar.getJarEntry("module.yml");
    if (entry == null) {
      return null;
    }

    YamlConfiguration yaml = YamlConfiguration
        .loadConfiguration(new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8));

    return new ModuleDescription(
        yaml.getString("name"),
        yaml.getString("version", "?"),
        yaml.getString("main"),
        yaml.getString("api-version"),
        yaml.getStringList("depends"));
  }
}
