package ru.breezeproject.core;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import ru.breezeproject.core.commands.ModulesCommand;
import ru.breezeproject.core.database.DatabaseConfig;
import ru.breezeproject.core.database.DatabaseManager;
import ru.breezeproject.core.database.DatabaseMigrator;
import ru.breezeproject.core.loader.ModuleLoader;
import ru.breezeproject.core.service.SimpleServiceRegistry;

public final class BreezeCorePlugin extends JavaPlugin {
  private DatabaseManager databaseManager;
  private ModuleLoader moduleLoader;
  private SimpleServiceRegistry serviceRegistry;
  private BukkitTask initTask;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    if (getConfig().getBoolean("database.enabled", false)) {
      initTask = getServer().getScheduler().runTaskAsynchronously(this, () -> {

        this.databaseManager = new DatabaseManager(getLogger());
        try {
          databaseManager.connect(DatabaseConfig.fromConfig(getConfig()));
          new DatabaseMigrator(getLogger()).migrate(databaseManager.getDataSource(), databaseManager.getVendor());
          getServer().getScheduler().runTask(this, this::loadModules);
        } catch (Exception e) {
          getLogger().severe("Could not connect to the database, disabling BreezeCore: " + e.getMessage());
          getServer().getPluginManager().disablePlugin(this);
        }
      });
    } else {
      getLogger().info("Database is disabled in config.yml. Skipping database connection and migrations.");
      loadModules();
    }
  }

  private void loadModules() {
    this.serviceRegistry = new SimpleServiceRegistry();
    this.moduleLoader = new ModuleLoader(getDataFolder().toPath(), serviceRegistry, getLogger(), this);
    this.moduleLoader.loadAll();

    PluginCommand cmd = getCommand("breezemodules");
    if (cmd == null) {
      throw new IllegalStateException("Command 'breezemodules' not defined in plugin.yml");
    }
    cmd.setExecutor(new ModulesCommand(moduleLoader));

    getLogger().info("BreezeCore enabled, " + moduleLoader.getLoadedModules().size() + " module(s) loaded.");
  }

  @Override
  public void onDisable() {
    if (initTask != null) {
      initTask.cancel();
    }
    if (moduleLoader != null) {
      moduleLoader.unloadAll();
    }
    if (databaseManager != null) {
      databaseManager.shutdown();
    }
  }

  public ModuleLoader getModuleLoader() {
    return moduleLoader;
  }

  public DatabaseManager getDatabaseManager() {
    return databaseManager;
  }
}
