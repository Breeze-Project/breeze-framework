package ru.breezeproject.core.loader;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import ru.breezeproject.api.BreezeApiVersion;
import ru.breezeproject.api.config.ModuleConfig;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.module.BreezeModule;
import ru.breezeproject.core.context.BreezeModuleContextImpl;
import ru.breezeproject.core.command.DynamicCommandRegistrar;
import ru.breezeproject.core.config.YamlModuleConfig;
import ru.breezeproject.api.service.ServiceRegistry;
import ru.breezeproject.core.event.SimpleEventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class ModuleLoader {
  private final Path directory;
  private final Path dataRoot;
  private final ServiceRegistry serviceRegistry;
  private final SimpleEventBus eventBus;
  private final Logger logger;
  private final DynamicCommandRegistrar commandRegistrar;
  private final Plugin ownerPlugin;

  private final Map<String, BreezeModule> loadedModules = new LinkedHashMap<>();
  private final Map<String, URLClassLoader> classLoaders = new LinkedHashMap<>();
  private final Map<String, List<EventBus.Subscription>> moduleSubscriptions = new LinkedHashMap<>();
  private final Map<String, Path> sourceFiles = new LinkedHashMap<>();
  private final Map<String, BreezeModuleContextImpl> moduleContexts = new LinkedHashMap<>();

  public ModuleLoader(Path pluginDataFolder, ServiceRegistry serviceRegistry, Logger logger, Plugin ownerPlugin) {
    this.directory = pluginDataFolder.resolve("modules");
    this.dataRoot = pluginDataFolder.resolve("modules/data");
    this.serviceRegistry = serviceRegistry;
    this.eventBus = new SimpleEventBus(logger);
    this.logger = logger;
    this.ownerPlugin = ownerPlugin;
    try {
      if (!Files.exists(directory)) {
        Files.createDirectories(directory);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Could not create modules directory", e);
    }

    DynamicCommandRegistrar registrar;
    try {
      registrar = new DynamicCommandRegistrar(logger);
    } catch (Exception e) {
      throw new IllegalStateException("Could not access Bukkit's CommandMap", e);
    }

    this.commandRegistrar = registrar;
  }

  public void loadAll() {
    try {
      Files.list(directory)
          .filter(p -> p.toString().endsWith(".jar"))
          .forEach(file -> {
            try {
              loadModule(file);
            } catch (Exception e) {
              logger.severe("Failed to load module from " + file.getFileName() + ": " + e.getMessage());
            }
          });
    } catch (Exception e) {
      logger.severe("Failed to list module directory: " + e.getMessage());
    }
  }

  public boolean reload(String name) {
    Path file = sourceFiles.get(name);
    if (file == null) {
      return false;
    }

    unloadModule(name);

    if (!Files.exists(file)) {
      logger.severe("Cannot reload '" + name + "': jar file " + file.getFileName() + " no longer exists on disk.");
      return false;
    }

    try {
      loadModule(file);
    } catch (Exception e) {
      logger.severe("Failed to reload module '" + name + "': " + e.getMessage());
      return false;
    }
    return true;
  }

  private void loadModule(Path file) throws Exception {
    String mainClassPath;
    String name;
    String declaredApiVersion;

    try (JarFile jar = new JarFile(file.toFile())) {
      JarEntry descriptorEntry = jar.getJarEntry("module.yml");
      if (descriptorEntry == null) {
        logger.warning("Skipping " + file.getFileName() + ": missing module.yml");
        return;
      }

      YamlConfiguration descriptor = YamlConfiguration.loadConfiguration(
          new InputStreamReader(jar.getInputStream(descriptorEntry), StandardCharsets.UTF_8));

      mainClassPath = descriptor.getString("main");
      name = descriptor.getString("name");
      declaredApiVersion = descriptor.getString("api-version");

      if (mainClassPath == null || name == null) {
        logger.warning("Skipping " + file.getFileName() + ": module.yml must declare 'main' and 'name'");
        return;
      }

      if (loadedModules.containsKey(name)) {
        logger.warning("Duplicate module name '" + name + "', skipping " + file.getFileName());
        return;
      }

      if (!BreezeApiVersion.isCompatible(declaredApiVersion)) {
        logger.severe("Skipping module '" + name + "': declares api-version '" + declaredApiVersion
            + "' which is incompatible with the running breeze-api "
            + BreezeApiVersion.CURRENT + ". Rebuild the module against a supported API version.");
        return;
      }

      Path moduleDataFolder = dataRoot.resolve(name);
      ModuleConfig runtimeConfig = loadOrCreateModuleConfig(jar, moduleDataFolder);

      URLClassLoader moduleLoader = new URLClassLoader(
          new URL[] { file.toUri().toURL() },
          this.getClass().getClassLoader());

      Class<?> clazz = Class.forName(mainClassPath, true, moduleLoader);
      BreezeModule module = (BreezeModule) clazz.getDeclaredConstructor().newInstance();

      List<EventBus.Subscription> subscriptions = new ArrayList<>();
      BreezeModuleContextImpl context = new BreezeModuleContextImpl(
          serviceRegistry,
          eventBus.scopedView(subscriptions),
          moduleDataFolder,
          commandRegistrar,
          ownerPlugin,
          name);

      module.init(name, runtimeConfig, Logger.getLogger(name), context);
      module.onEnable();

      loadedModules.put(name, module);
      classLoaders.put(name, moduleLoader);
      moduleSubscriptions.put(name, subscriptions);
      sourceFiles.put(name, file);
      moduleContexts.put(name, context);

      logger.info("Loaded module '" + name + "' (" + descriptor.getString("version", "?") + ")");
    }
  }

  private ModuleConfig loadOrCreateModuleConfig(JarFile jar, Path moduleDataFolder) throws Exception {
    if (!Files.exists(moduleDataFolder)) {
      Files.createDirectories(moduleDataFolder);
    }

    Path configFile = moduleDataFolder.resolve("config.yml");
    if (!Files.exists(configFile)) {
      JarEntry defaultConfigEntry = jar.getJarEntry("config.yml");
      if (defaultConfigEntry != null) {
        try (InputStream in = jar.getInputStream(defaultConfigEntry)) {
          Files.copy(in, configFile);
        }
      } else {
        Files.createFile(configFile);
      }
    }

    return new YamlModuleConfig(YamlConfiguration.loadConfiguration(configFile.toFile()), configFile);
  }

  private void unloadModule(String name) {
    BreezeModule module = loadedModules.remove(name);
    if (module != null) {
      try {
        module.onDisable();
      } catch (Exception e) {
        logger.severe("Error disabling module '" + name + "': " + e.getMessage());
      }
    }

    List<EventBus.Subscription> subscriptions = moduleSubscriptions.remove(name);
    if (subscriptions != null) {
      eventBus.unsubscribeAll(subscriptions);
    }

    BreezeModuleContextImpl context = moduleContexts.remove(name);
    if (context != null) {
      if (commandRegistrar != null) {
        context.unregisterAllCommands();
      }
      context.unregisterAllListeners();
    }

    URLClassLoader loader = classLoaders.remove(name);
    if (loader != null) {
      try {
        loader.close();
      } catch (Exception e) {
        logger.warning("Could not close classloader for module '" + name + "': " + e.getMessage());
      }
    }
  }

  public void unloadAll() {
    new ArrayList<>(loadedModules.keySet()).forEach(this::unloadModule);
    sourceFiles.clear();
  }

  public Map<String, BreezeModule> getLoadedModules() {
    return loadedModules;
  }

  public SimpleEventBus getEventBus() {
    return eventBus;
  }
}
