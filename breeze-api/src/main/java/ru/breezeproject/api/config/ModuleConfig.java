package ru.breezeproject.api.config;

import java.io.IOException;

public interface ModuleConfig {
  String getString(String path);

  String getString(String path, String def);

  int getInt(String path);

  int getInt(String path, int def);

  boolean getBoolean(String path);

  boolean getBoolean(String path, boolean def);

  void save() throws IOException;

  void reload();
}
