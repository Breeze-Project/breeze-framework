package ru.breezeproject.core.context;

import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.module.BreezeModuleContext;
import ru.breezeproject.api.service.ServiceRegistry;

import java.io.File;

public class BreezeModuleContextImpl implements BreezeModuleContext {

    private final ServiceRegistry serviceRegistry;
    private final EventBus eventBus;
    private final File dataFolder;

    public BreezeModuleContextImpl(ServiceRegistry serviceRegistry, EventBus eventBus, File dataFolder) {
        this.serviceRegistry = serviceRegistry;
        this.eventBus = eventBus;
        this.dataFolder = dataFolder;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    @Override
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }
}
