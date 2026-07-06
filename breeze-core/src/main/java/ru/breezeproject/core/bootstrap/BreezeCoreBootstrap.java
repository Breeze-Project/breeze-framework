package ru.breezeproject.core.bootstrap;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.service.ServiceRegistry;
import ru.breezeproject.core.command.CoreCommandRegistrar;
import ru.breezeproject.core.command.DynamicCommandRegistrar;
import ru.breezeproject.core.database.DatabaseConfig;
import ru.breezeproject.core.database.DatabaseService;
import ru.breezeproject.core.database.DatabaseServiceImpl;
import ru.breezeproject.core.event.SimpleEventBus;
import ru.breezeproject.core.loader.ModuleLoader;
import ru.breezeproject.core.loader.ModuleManager;
import ru.breezeproject.core.service.SimpleServiceRegistry;

public class BreezeCoreBootstrap {
  private final JavaPlugin plugin;
  private final Logger logger;

  private final ServiceRegistry serviceRegistry;
  private final EventBus eventBus;
  private final DatabaseService databaseService;
  private final ModuleManager moduleManager;
  private final CoreCommandRegistrar commandRegistrar;

  private BukkitTask initTask;

  public BreezeCoreBootstrap(final JavaPlugin plugin) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();

    this.serviceRegistry = new SimpleServiceRegistry();
    this.eventBus = new SimpleEventBus(logger);
    this.databaseService = new DatabaseServiceImpl(logger);

    final DynamicCommandRegistrar dynamicRegistrar = createCommandRegistrar(logger);

    this.moduleManager = new ModuleLoader(
        plugin.getDataFolder().toPath(),
        serviceRegistry,
        eventBus,
        logger,
        plugin,
        dynamicRegistrar);
    this.commandRegistrar = new CoreCommandRegistrar(plugin, moduleManager);
  }

  public void start() {
    final FileConfiguration config = plugin.getConfig();

    if (config.getBoolean("database.enabled", false)) {
      initTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        try {
          databaseService.connect(DatabaseConfig.fromConfig(config));
          databaseService.migrate();
          Bukkit.getScheduler().runTask(plugin, this::onPostDatabaseInit);
        } catch (final Exception e) {
          logger.severe("Could not connect to the database, disabling BreezeCore: " + e.getMessage());
          Bukkit.getPluginManager().disablePlugin(plugin);
        }
      });
    } else {
      logger.info("Database is disabled in config.yml. Skipping database connection and migrations.");
      onPostDatabaseInit();
    }
  }

  public void stop() {
    if (initTask != null) {
      initTask.cancel();
    }
    if (moduleManager != null) {
      moduleManager.unloadAll();
    }
    if (databaseService != null) {
      databaseService.shutdown();
    }
  }

  public ModuleManager getModuleManager() {
    return moduleManager;
  }

  public DatabaseService getDatabaseService() {
    return databaseService;
  }

  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public EventBus getEventBus() {
    return eventBus;
  }

  private void onPostDatabaseInit() {
    moduleManager.loadAll();
    commandRegistrar.registerAll();
    logger.info("BreezeCore enabled, " + moduleManager.getLoadedModules().size() + " module(s) loaded.");
  }

  private DynamicCommandRegistrar createCommandRegistrar(final Logger logger) {
    try {
      return new DynamicCommandRegistrar(logger);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException("Could not access Bukkit's CommandMap", e);
    }
  }
}
