package ru.breezeproject.core.loader;

import java.util.Map;
import java.util.Set;

import ru.breezeproject.api.module.BreezeModule;

public interface ModuleManager {
  void loadAll();

  void unloadAll();

  boolean reload(String name);

  int loadNew();

  boolean load(String name);

  boolean disable(String name);

  boolean enable(String name);

  int scanForNewModules(long settleMs);

  Map<String, BreezeModule> getLoadedModules();

  Set<String> getDisabledModules();
}
