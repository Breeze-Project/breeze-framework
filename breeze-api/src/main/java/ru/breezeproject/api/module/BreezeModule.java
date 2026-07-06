package ru.breezeproject.api.module;

import java.io.IOException;
import java.util.logging.Logger;

import ru.breezeproject.api.config.ModuleConfig;

public abstract class BreezeModule {
  private String name;
  private boolean enabled;
  private ModuleConfig config;
  private Logger logger;
  private BreezeModuleContext context;

  public final void init(final String name, final ModuleConfig config, final Logger logger,
      final BreezeModuleContext context) {
    this.name = name;
    this.config = config;
    this.logger = logger;
    this.context = context;
  }

  public abstract void onEnable();

  public abstract void onDisable();

  public final String getName() {
    return name;
  }

  public final boolean isEnabled() {
    return enabled;
  }

  public final ModuleConfig getConfig() {
    return config;
  }

  public final Logger getLogger() {
    return logger;
  }

  public final BreezeModuleContext getContext() {
    return context;
  }

  public final void saveConfig() {
    try {
      config.save();
    } catch (final IOException e) {
      throw new RuntimeException("Could not save config for module " + name, e);
    }
  }

  public final void reloadConfig() {
    config.reload();
  }
}
