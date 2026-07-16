package ru.breezeproject.core.bootstrap;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.schedule.BreezeScheduler;
import ru.breezeproject.api.schedule.BreezeTask;
import ru.breezeproject.api.service.ServiceRegistry;
import ru.breezeproject.core.command.CoreCommandRegistrar;
import ru.breezeproject.core.command.DynamicCommandRegistrar;
import ru.breezeproject.core.database.DatabaseConfig;
import ru.breezeproject.core.database.DatabaseService;
import ru.breezeproject.core.database.DatabaseServiceImpl;
import ru.breezeproject.core.event.SimpleEventBus;
import ru.breezeproject.core.loader.ModuleLoader;
import ru.breezeproject.core.loader.ModuleManager;
import ru.breezeproject.core.schedule.FoliaBreezeScheduler;
import ru.breezeproject.core.service.SimpleServiceRegistry;

public class BreezeCoreBootstrap {
  private final JavaPlugin plugin;
  private final Logger logger;

  private final ServiceRegistry serviceRegistry;
  private final EventBus eventBus;
  private final BreezeScheduler scheduler;
  private final DatabaseService databaseService;
  private final ModuleManager moduleManager;
  private final CoreCommandRegistrar commandRegistrar;

  private BreezeTask moduleScanTask;

  public BreezeCoreBootstrap(final JavaPlugin plugin) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();

    this.serviceRegistry = new SimpleServiceRegistry();
    this.eventBus = new SimpleEventBus(logger);
    this.scheduler = new FoliaBreezeScheduler(plugin);
    this.databaseService = new DatabaseServiceImpl(logger);

    serviceRegistry.register(BreezeScheduler.class, scheduler);

    final DynamicCommandRegistrar dynamicRegistrar = createCommandRegistrar(logger);
    final Path modulesDirectory = resolveModulesDirectory(plugin);

    this.moduleManager = new ModuleLoader(
        modulesDirectory,
        plugin.getDataFolder().toPath(),
        serviceRegistry,
        eventBus,
        logger,
        plugin,
        scheduler,
        dynamicRegistrar);
    this.commandRegistrar = new CoreCommandRegistrar(plugin, moduleManager);
  }

  public void start() {
    final FileConfiguration config = plugin.getConfig();

    if (config.getBoolean("database.enabled", false)) {
      scheduler.runAsync(() -> {
        try {
          databaseService.connect(DatabaseConfig.fromConfig(config));
          databaseService.migrate();
          scheduler.runGlobal(this::onPostDatabaseInit);
        } catch (final Exception e) {
          logger.severe("Could not connect to the database, disabling BreezeCore: " + e.getMessage());
          plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
      });
    } else {
      logger.info("Database is disabled in config.yml. Skipping database connection and migrations.");
      onPostDatabaseInit();
    }
  }

  public void stop() {
    if (moduleScanTask != null) {
      moduleScanTask.cancel();
      moduleScanTask = null;
    }
    moduleManager.unloadAll();
    databaseService.shutdown();
    serviceRegistry.unregister(BreezeScheduler.class);
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

  public BreezeScheduler getScheduler() {
    return scheduler;
  }

  private void onPostDatabaseInit() {
    moduleManager.loadAll();
    commandRegistrar.registerAll();
    startModuleScanTask();
    logger.info("BreezeCore enabled, " + moduleManager.getLoadedModules().size() + " module(s) loaded.");
  }

  private void startModuleScanTask() {
    final FileConfiguration config = plugin.getConfig();
    if (!config.getBoolean("modules.auto_scan.enabled", true)) {
      return;
    }

    final long intervalSeconds = Math.max(5L, config.getLong("modules.auto_scan.interval_seconds", 30));
    final long settleMs = Math.max(500L, config.getLong("modules.copy_settle_ms", 2000));
    final long intervalTicks = intervalSeconds * 20L;

    moduleScanTask = scheduler.runGlobalTimer(() -> {
      final int loaded = moduleManager.scanForNewModules(settleMs);
      if (loaded > 0) {
        logger.info("Auto-scan loaded " + loaded + " new module(s).");
      }
    }, intervalTicks, intervalTicks);
  }

  private Path resolveModulesDirectory(final JavaPlugin plugin) {
    final String configuredDirectory = plugin.getConfig().getString("modules.directory", "modules");
    return plugin.getDataFolder().toPath().resolve(configuredDirectory);
  }

  private DynamicCommandRegistrar createCommandRegistrar(final Logger logger) {
    try {
      return new DynamicCommandRegistrar(logger);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException("Could not access Bukkit's CommandMap", e);
    }
  }
}
