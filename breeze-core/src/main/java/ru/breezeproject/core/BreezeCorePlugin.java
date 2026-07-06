package ru.breezeproject.core;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.breezeproject.core.commands.ModulesCommand;
import ru.breezeproject.core.database.DatabaseManager;
import ru.breezeproject.core.database.DatabaseMigrator;
import ru.breezeproject.core.loader.ModuleLoader;
import ru.breezeproject.core.service.SimpleServiceRegistry;

public final class BreezeCorePlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ModuleLoader moduleLoader;
    private SimpleServiceRegistry serviceRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(getLogger());
        try {
            databaseManager.connect(getConfig());
            new DatabaseMigrator(getLogger()).migrate(databaseManager.getDataSource(), databaseManager.getVendor());
        } catch (Exception e) {
            getLogger().severe("Could not connect to the database, disabling BreezeCore: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.serviceRegistry = new SimpleServiceRegistry();
        this.moduleLoader = new ModuleLoader(getDataFolder(), serviceRegistry, getLogger(), this);
        this.moduleLoader.loadAll();

        PluginCommand cmd = getCommand("breezemodules");
        if (cmd != null) {
            cmd.setExecutor(new ModulesCommand(moduleLoader));
        }

        getLogger().info("BreezeCore enabled, " + moduleLoader.getLoadedModules().size() + " module(s) loaded.");
    }

    @Override
    public void onDisable() {
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
