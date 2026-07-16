package ru.breezeproject.core.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bukkit.plugin.java.JavaPlugin;

import ru.breezeproject.api.BreezeApiVersion;
import ru.breezeproject.api.config.ModuleConfig;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.module.BreezeModule;
import ru.breezeproject.api.module.ModuleDescription;
import ru.breezeproject.api.schedule.BreezeScheduler;
import ru.breezeproject.api.service.ServiceRegistry;
import ru.breezeproject.core.command.DynamicCommandRegistrar;
import ru.breezeproject.core.context.BreezeModuleContextImpl;

public class ModuleLoader implements ModuleManager {
  private static final class FileWatchState {
    private long lastSize = -1L;
    private long stableSince;
  }

  private final Path directory;
  private final Path dataRoot;
  private final ServiceRegistry serviceRegistry;
  private final EventBus eventBus;
  private final Logger logger;
  private final DynamicCommandRegistrar commandRegistrar;
  private final JavaPlugin ownerPlugin;
  private final BreezeScheduler scheduler;
  private final ModuleDescriptorReader descriptorReader;
  private final ModuleConfigLoader configLoader;
  private final DisabledModulesStore disabledModules;

  private final Map<String, BreezeModule> loadedModules = new LinkedHashMap<>();
  private final Map<String, URLClassLoader> classLoaders = new LinkedHashMap<>();
  private final Map<String, Path> sourceFiles = new LinkedHashMap<>();
  private final Map<String, BreezeModuleContextImpl> moduleContexts = new LinkedHashMap<>();
  private final Map<Path, FileWatchState> watchStates = new ConcurrentHashMap<>();

  public ModuleLoader(final Path modulesDirectory,
      final Path pluginDataFolder,
      final ServiceRegistry serviceRegistry,
      final EventBus eventBus,
      final Logger logger,
      final JavaPlugin ownerPlugin,
      final BreezeScheduler scheduler,
      final DynamicCommandRegistrar commandRegistrar) {
    this.directory = modulesDirectory;
    this.dataRoot = pluginDataFolder.resolve("modules/data");
    this.serviceRegistry = serviceRegistry;
    this.eventBus = eventBus;
    this.logger = logger;
    this.ownerPlugin = ownerPlugin;
    this.scheduler = scheduler;
    this.commandRegistrar = commandRegistrar;
    this.descriptorReader = new ModuleDescriptorReader();
    this.configLoader = new ModuleConfigLoader();
    this.disabledModules = new DisabledModulesStore(pluginDataFolder, logger);

    try {
      Files.createDirectories(directory);
      Files.createDirectories(dataRoot);
    } catch (final Exception e) {
      throw new IllegalStateException("Could not create modules directory", e);
    }
  }

  @Override
  public void loadAll() {
    scanForNewModules(0L);
  }

  @Override
  public int loadNew() {
    return scanForNewModules(0L);
  }

  @Override
  public int scanForNewModules(final long settleMs) {
    int loadedCount = 0;
    try (Stream<Path> files = Files.list(directory)) {
      final List<Path> jars = files.filter(path -> path.toString().endsWith(".jar")).sorted().toList();
      for (final Path file : jars) {
        if (isLoadedFrom(file)) {
          continue;
        }
        final Optional<String> moduleName = peekModuleName(file);
        if (moduleName.isPresent() && disabledModules.isDisabled(moduleName.get())) {
          sourceFiles.putIfAbsent(moduleName.get(), file);
          continue;
        }
        if (settleMs > 0L && !isFileSettled(file, settleMs)) {
          continue;
        }
        try {
          final int sizeBefore = loadedModules.size();
          loadModule(file);
          if (loadedModules.size() > sizeBefore) {
            loadedCount++;
          }
        } catch (final Exception e) {
          logger.severe("Failed to load module from " + file.getFileName() + ": " + e.getMessage());
        }
      }
    } catch (final Exception e) {
      logger.severe("Failed to list module directory: " + e.getMessage());
    }
    return loadedCount;
  }

  @Override
  public boolean load(final String name) {
    if (loadedModules.containsKey(name)) {
      return true;
    }
    if (disabledModules.isDisabled(name)) {
      logger.warning("Cannot load module '" + name + "': it is disabled. Use /breezemodules enable first.");
      return false;
    }

    final Path knownFile = sourceFiles.get(name);
    if (knownFile != null && Files.exists(knownFile)) {
      try {
        loadModule(knownFile);
        return loadedModules.containsKey(name);
      } catch (final Exception e) {
        logger.severe("Failed to load module '" + name + "': " + e.getMessage());
        return false;
      }
    }

    final Optional<Path> discovered = findJarByModuleName(name);
    if (discovered.isEmpty()) {
      return false;
    }
    try {
      loadModule(discovered.get());
      return loadedModules.containsKey(name);
    } catch (final Exception e) {
      logger.severe("Failed to load module '" + name + "': " + e.getMessage());
      return false;
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

    if (disabledModules.isDisabled(name)) {
      logger.warning("Cannot reload '" + name + "': module is disabled.");
      return false;
    }

    try {
      loadModule(file);
    } catch (final Exception e) {
      logger.severe("Failed to reload module '" + name + "': " + e.getMessage());
      return false;
    }
    return loadedModules.containsKey(name);
  }

  @Override
  public boolean disable(final String name) {
    if (loadedModules.containsKey(name)) {
      unloadModule(name);
    }
    disabledModules.disable(name);
    return true;
  }

  @Override
  public boolean enable(final String name) {
    disabledModules.enable(name);
    if (loadedModules.containsKey(name)) {
      return true;
    }
    return load(name);
  }

  @Override
  public void unloadAll() {
    new ArrayList<>(loadedModules.keySet()).forEach(this::unloadModule);
    sourceFiles.clear();
    watchStates.clear();
  }

  @Override
  public Map<String, BreezeModule> getLoadedModules() {
    return Map.copyOf(loadedModules);
  }

  @Override
  public Set<String> getDisabledModules() {
    return disabledModules.getDisabled();
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
        logger.warning("Module '" + descriptor.name() + "' is already loaded, skipping " + file.getFileName());
        sourceFiles.put(descriptor.name(), file);
        return;
      }
      if (disabledModules.isDisabled(descriptor.name())) {
        logger.info("Skipping module '" + descriptor.name() + "': disabled");
        sourceFiles.put(descriptor.name(), file);
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
          scheduler,
          descriptor.name());

      module.init(descriptor.name(), runtimeConfig, Logger.getLogger(descriptor.name()), context);
      module.onEnable();

      loadedModules.put(descriptor.name(), module);
      classLoaders.put(descriptor.name(), moduleLoader);
      sourceFiles.put(descriptor.name(), file);
      moduleContexts.put(descriptor.name(), context);
      watchStates.remove(file);

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

  private boolean isLoadedFrom(final Path file) {
    return sourceFiles.entrySet().stream()
        .anyMatch(entry -> entry.getValue().equals(file) && loadedModules.containsKey(entry.getKey()));
  }

  private Optional<String> peekModuleName(final Path file) {
    try (JarFile jar = new JarFile(file.toFile())) {
      return descriptorReader.read(jar).map(ModuleDescription::name);
    } catch (final IOException e) {
      logger.warning("Could not read module descriptor from " + file.getFileName() + ": " + e.getMessage());
      return Optional.empty();
    }
  }

  private Optional<Path> findJarByModuleName(final String name) {
    try (Stream<Path> files = Files.list(directory)) {
      for (final Path file : files.filter(path -> path.toString().endsWith(".jar")).toList()) {
        final Optional<String> moduleName = peekModuleName(file);
        if (moduleName.isPresent() && moduleName.get().equals(name)) {
          return Optional.of(file);
        }
      }
    } catch (final IOException e) {
      logger.warning("Could not scan modules directory: " + e.getMessage());
    }
    return Optional.empty();
  }

  private boolean isFileSettled(final Path file, final long settleMs) {
    try {
      final long size = Files.size(file);
      final long now = System.currentTimeMillis();
      final FileWatchState state = watchStates.computeIfAbsent(file, ignored -> new FileWatchState());
      if (state.lastSize != size) {
        state.lastSize = size;
        state.stableSince = now;
        return false;
      }
      return now - state.stableSince >= settleMs;
    } catch (final IOException e) {
      return false;
    }
  }
}
