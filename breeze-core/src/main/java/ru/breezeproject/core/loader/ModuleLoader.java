package ru.breezeproject.core.loader;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import ru.breezeproject.api.BreezeApiVersion;
import ru.breezeproject.api.config.ModuleConfig;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.module.BreezeModule;
import ru.breezeproject.api.module.ModuleDescription;
import ru.breezeproject.api.service.ServiceRegistry;
import ru.breezeproject.core.command.DynamicCommandRegistrar;
import ru.breezeproject.core.context.BreezeModuleContextImpl;

public class ModuleLoader implements ModuleManager {
  private final Path directory;
  private final Path dataRoot;
  private final ServiceRegistry serviceRegistry;
  private final EventBus eventBus;
  private final Logger logger;
  private final DynamicCommandRegistrar commandRegistrar;
  private final JavaPlugin ownerPlugin;
  private final ModuleDescriptorReader descriptorReader;
  private final ModuleConfigLoader configLoader;

  private final Map<String, BreezeModule> loadedModules = new LinkedHashMap<>();
  private final Map<String, URLClassLoader> classLoaders = new LinkedHashMap<>();
  private final Map<String, Path> sourceFiles = new LinkedHashMap<>();
  private final Map<String, BreezeModuleContextImpl> moduleContexts = new LinkedHashMap<>();

  public ModuleLoader(final Path pluginDataFolder,
      final ServiceRegistry serviceRegistry,
      final EventBus eventBus,
      final Logger logger,
      final JavaPlugin ownerPlugin,
      final DynamicCommandRegistrar commandRegistrar) {
    this.directory = pluginDataFolder.resolve("modules");
    this.dataRoot = pluginDataFolder.resolve("modules/data");
    this.serviceRegistry = serviceRegistry;
    this.eventBus = eventBus;
    this.logger = logger;
    this.ownerPlugin = ownerPlugin;
    this.commandRegistrar = commandRegistrar;
    this.descriptorReader = new ModuleDescriptorReader();
    this.configLoader = new ModuleConfigLoader();

    try {
      Files.createDirectories(directory);
    } catch (final Exception e) {
      throw new IllegalStateException("Could not create modules directory", e);
    }
  }

  @Override
  public void loadAll() {
    try {
      Files.list(directory)
          .filter(p -> p.toString().endsWith(".jar"))
          .forEach(file -> {
            try {
              loadModule(file);
            } catch (final Exception e) {
              logger.severe("Failed to load module from " + file.getFileName() + ": " + e.getMessage());
            }
          });
    } catch (final Exception e) {
      logger.severe("Failed to list module directory: " + e.getMessage());
    }
  }

  @Override
  public boolean reload(final String name) {
    final Path file = sourceFiles.get(name);
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
    } catch (final Exception e) {
      logger.severe("Failed to reload module '" + name + "': " + e.getMessage());
      return false;
    }
    return true;
  }

  @Override
  public void unloadAll() {
    new ArrayList<>(loadedModules.keySet()).forEach(this::unloadModule);
    sourceFiles.clear();
  }

  @Override
  public Map<String, BreezeModule> getLoadedModules() {
    return Map.copyOf(loadedModules);
  }

  private void loadModule(final Path file) throws Exception {
    try (JarFile jar = new JarFile(file.toFile())) {
      final Optional<ModuleDescription> descriptorOpt = descriptorReader.read(jar);

      if (descriptorOpt.isEmpty()) {
        logger.warning("Skipping " + file.getFileName() + ": missing module.yml");
        return;
      }

      final ModuleDescription descriptor = descriptorOpt.get();
      if (descriptor.main() == null || descriptor.name() == null) {
        logger.warning("Skipping " + file.getFileName() + ": module.yml must declare 'main' and 'name'");
        return;
      }
      if (loadedModules.containsKey(descriptor.name())) {
        logger.warning("Duplicate module name '" + descriptor.name() + "', skipping " + file.getFileName());
        return;
      }
      if (!BreezeApiVersion.isCompatible(descriptor.apiVersion())) {
        logger.severe("Skipping module '" + descriptor.name() + "': declares api-version '"
            + descriptor.apiVersion() + "' which is incompatible with the running breeze-api "
            + BreezeApiVersion.CURRENT + ".");
        return;
      }

      final Path moduleDataFolder = dataRoot.resolve(descriptor.name());
      final ModuleConfig runtimeConfig = configLoader.loadOrCreate(jar, moduleDataFolder);

      final URLClassLoader moduleLoader = new URLClassLoader(
          new URL[] { file.toUri().toURL() },
          this.getClass().getClassLoader());

      final BreezeModule module = instantiateModule(moduleLoader, descriptor.main());

      final BreezeModuleContextImpl context = new BreezeModuleContextImpl(
          serviceRegistry,
          eventBus,
          moduleDataFolder,
          commandRegistrar,
          ownerPlugin,
          descriptor.name());

      module.init(descriptor.name(), runtimeConfig, Logger.getLogger(descriptor.name()), context);
      module.onEnable();

      loadedModules.put(descriptor.name(), module);
      classLoaders.put(descriptor.name(), moduleLoader);
      sourceFiles.put(descriptor.name(), file);
      moduleContexts.put(descriptor.name(), context);

      logger.info("Loaded module '" + descriptor.name() + "' (" + descriptor.version() + ")");
    }
  }

  private BreezeModule instantiateModule(final URLClassLoader loader, final String mainClass) throws Exception {
    final Class<?> clazz = Class.forName(mainClass, true, loader);
    return (BreezeModule) clazz.getDeclaredConstructor().newInstance();
  }

  private void unloadModule(final String name) {
    final BreezeModule module = loadedModules.remove(name);
    if (module != null) {
      try {
        module.onDisable();
      } catch (final Exception e) {
        logger.severe("Error disabling module '" + name + "': " + e.getMessage());
      }
    }

    final BreezeModuleContextImpl context = moduleContexts.remove(name);
    if (context != null) {
      context.cleanup();
    }

    final URLClassLoader loader = classLoaders.remove(name);
    if (loader != null) {
      try {
        loader.close();
      } catch (final Exception e) {
        logger.warning("Could not close classloader for module '" + name + "': " + e.getMessage());
      }
    }
  }
}
