package ru.breezeproject.api.config;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface ModuleConfig {
  String getString(String path);

  String getString(String path, String def);

  int getInt(String path);

  int getInt(String path, int def);

  double getDouble(String path);

  double getDouble(String path, double def);

  boolean getBoolean(String path);

  boolean getBoolean(String path, boolean def);

  ModuleConfig getSection(String path);

  List<?> getList(String path);

  Set<String> getKeys();

  void save() throws IOException;

  void reload();
}
