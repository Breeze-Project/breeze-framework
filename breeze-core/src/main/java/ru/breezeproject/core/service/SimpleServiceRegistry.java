package ru.breezeproject.core.service;

import ru.breezeproject.api.service.ServiceRegistry;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SimpleServiceRegistry implements ServiceRegistry {

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    @Override
    public <T> void register(Class<T> serviceType, T implementation) {
        services.put(serviceType, implementation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> serviceType) {
        return Optional.ofNullable((T) services.get(serviceType));
    }

    @Override
    public void unregister(Class<?> serviceType) {
        services.remove(serviceType);
    }
}
