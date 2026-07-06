package ru.breezeproject.api.module;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

public abstract class BreezeModule {

    private String name;
    private boolean enabled;
    private FileConfiguration config;
    private Logger logger;
    private BreezeModuleContext context;

    public final void init(String name, FileConfiguration config, Logger logger, BreezeModuleContext context) {
        this.name = name;
        this.config = config;
        this.logger = logger;
        this.context = context;
    }

    public abstract void onEnable();

    public abstract void onDisable();

    final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public final String getName() {
        return name;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final FileConfiguration getConfig() {
        return config;
    }

    public final Logger getLogger() {
        return logger;
    }

    public final BreezeModuleContext getContext() {
        return context;
    }
}
