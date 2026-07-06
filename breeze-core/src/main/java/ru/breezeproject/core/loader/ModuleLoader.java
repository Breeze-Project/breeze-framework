package ru.breezeproject.core.loader;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.breezeproject.api.BreezeApiVersion;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.module.BreezeModule;
import ru.breezeproject.core.context.BreezeModuleContextImpl;
import ru.breezeproject.core.command.DynamicCommandRegistrar;
import ru.breezeproject.api.service.ServiceRegistry;
import ru.breezeproject.core.event.SimpleEventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class ModuleLoader {

    private final File directory;
    private final File dataRoot;
    private final ServiceRegistry serviceRegistry;
    private final SimpleEventBus eventBus;
    private final Logger logger;
    private final DynamicCommandRegistrar commandRegistrar;

    private final Map<String, BreezeModule> loadedModules = new LinkedHashMap<>();
    private final Map<String, URLClassLoader> classLoaders = new LinkedHashMap<>();
    private final Map<String, List<EventBus.Subscription>> moduleSubscriptions = new LinkedHashMap<>();
    private final Map<String, File> sourceFiles = new LinkedHashMap<>();
    private final Map<String, BreezeModuleContextImpl> moduleContexts = new LinkedHashMap<>();

    public ModuleLoader(File pluginDataFolder, ServiceRegistry serviceRegistry, Logger logger) {
        this.directory = new File(pluginDataFolder, "modules");
        this.dataRoot = new File(pluginDataFolder, "modules/data");
        this.serviceRegistry = serviceRegistry;
        this.eventBus = new SimpleEventBus(logger);
        this.logger = logger;
        if (!directory.exists()) {
            directory.mkdirs();
        }

        DynamicCommandRegistrar registrar;
        try {
            registrar = new DynamicCommandRegistrar(logger);
        } catch (Exception e) {
            logger.severe("Could not access Bukkit's CommandMap; modules won't be able to register commands: "
                    + e.getMessage());
            registrar = null;
        }
        this.commandRegistrar = registrar;
    }

    public void loadAll() {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                loadModule(file);
            } catch (Exception e) {
                logger.severe("Failed to load module from " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public boolean reload(String name) {
        File file = sourceFiles.get(name);
        if (file == null) {
            return false;
        }

        unloadModule(name);

        if (!file.exists()) {
            logger.severe("Cannot reload '" + name + "': jar file " + file.getName() + " no longer exists on disk.");
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

    private void loadModule(File file) throws Exception {
        String mainClassPath;
        String name;
        String declaredApiVersion;

        try (JarFile jar = new JarFile(file)) {
            JarEntry descriptorEntry = jar.getJarEntry("module.yml");
            if (descriptorEntry == null) {
                logger.warning("Skipping " + file.getName() + ": missing module.yml");
                return;
            }

            YamlConfiguration descriptor = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(jar.getInputStream(descriptorEntry), StandardCharsets.UTF_8)
            );

            mainClassPath = descriptor.getString("main");
            name = descriptor.getString("name");
            declaredApiVersion = descriptor.getString("api-version");

            if (mainClassPath == null || name == null) {
                logger.warning("Skipping " + file.getName() + ": module.yml must declare 'main' and 'name'");
                return;
            }

            if (loadedModules.containsKey(name)) {
                logger.warning("Duplicate module name '" + name + "', skipping " + file.getName());
                return;
            }

            if (!BreezeApiVersion.isCompatible(declaredApiVersion)) {
                logger.severe("Skipping module '" + name + "': declares api-version '" + declaredApiVersion
                        + "' which is incompatible with the running breeze-api "
                        + BreezeApiVersion.CURRENT + ". Rebuild the module against a supported API version.");
                return;
            }

            File moduleDataFolder = new File(dataRoot, name);
            YamlConfiguration runtimeConfig = loadOrCreateModuleConfig(jar, moduleDataFolder);

            URLClassLoader moduleLoader = new URLClassLoader(
                    new URL[]{file.toURI().toURL()},
                    this.getClass().getClassLoader()
            );

            Class<?> clazz = Class.forName(mainClassPath, true, moduleLoader);
            BreezeModule module = (BreezeModule) clazz.getDeclaredConstructor().newInstance();

            List<EventBus.Subscription> subscriptions = new ArrayList<>();
            BreezeModuleContextImpl context = new BreezeModuleContextImpl(
                    serviceRegistry,
                    eventBus.scopedView(subscriptions),
                    moduleDataFolder,
                    commandRegistrar,
                    name
            );

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

    private YamlConfiguration loadOrCreateModuleConfig(JarFile jar, File moduleDataFolder) throws Exception {
        if (!moduleDataFolder.exists()) {
            moduleDataFolder.mkdirs();
        }

        File configFile = new File(moduleDataFolder, "config.yml");
        if (!configFile.exists()) {
            JarEntry defaultConfigEntry = jar.getJarEntry("config.yml");
            if (defaultConfigEntry != null) {
                try (InputStream in = jar.getInputStream(defaultConfigEntry);
                     FileOutputStream out = new FileOutputStream(configFile)) {
                    in.transferTo(out);
                }
            } else {
                configFile.createNewFile();
            }
        }

        return YamlConfiguration.loadConfiguration(configFile);
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
        if (context != null && commandRegistrar != null) {
            context.unregisterAllCommands();
        }

        URLClassLoader loader = classLoaders.remove(name);
        if (loader != null) {
            try {
                loader.close();
            } catch (Exception ignored) {
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
