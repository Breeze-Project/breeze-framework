package ru.breezeproject.core;

import org.bukkit.plugin.java.JavaPlugin;

import ru.breezeproject.core.bootstrap.BreezeCoreBootstrap;

public final class BreezeCorePlugin extends JavaPlugin {
  private BreezeCoreBootstrap bootstrap;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    bootstrap = new BreezeCoreBootstrap(this);
    bootstrap.start();
  }

  @Override
  public void onDisable() {
    if (bootstrap != null) {
      bootstrap.stop();
    }
  }
}
