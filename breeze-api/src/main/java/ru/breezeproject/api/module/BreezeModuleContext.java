package ru.breezeproject.api.module;

import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.service.ServiceRegistry;

public interface BreezeModuleContext {

    ServiceRegistry getServiceRegistry();

    EventBus getEventBus();

    java.io.File getDataFolder();
}
