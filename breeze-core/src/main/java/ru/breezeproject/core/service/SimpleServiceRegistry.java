package ru.breezeproject.core.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ru.breezeproject.api.service.ServiceRegistry;

public class SimpleServiceRegistry implements ServiceRegistry {
  private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

  @Override
  public <T> void register(final Class<T> serviceType, final T implementation) {
    services.put(serviceType, implementation);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> get(final Class<T> serviceType) {
    return Optional.ofNullable((T) services.get(serviceType));
  }

  @Override
  public void unregister(final Class<?> serviceType) {
    services.remove(serviceType);
  }
}
