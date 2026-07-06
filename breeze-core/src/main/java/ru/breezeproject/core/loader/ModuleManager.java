package ru.breezeproject.core.loader;

import java.util.Map;

import ru.breezeproject.api.module.BreezeModule;

public interface ModuleManager {
  void loadAll();

  void unloadAll();

  boolean reload(String name);

  Map<String, BreezeModule> getLoadedModules();
}
