# Spec-001: Microkernel Core API & User Adapter Architecture

**Статус:** Draft
**Цель:** Чёткая граница между Core API, System Adapters и Business Modules.
**Принцип:** Contract in Core, Implementation in Adapter.

---

## 1. Архитектурные слои

### 1.1 `core-api` (Contracts & Service Registry)

**Правило:** Ноль реализаций, ноль тяжёлых зависимостей (без gRPC, Netty, HikariCP, внешних vendor API).

**Роль:** Единственный source of truth для интерфейсов и кросс-модульных контрактов.

**Ключевые компоненты:**
- `ServiceRegistry` — thread-safe контейнер сервисов (интерфейс → реализация)
- `User` / `UserSession` — интерфейс/дата-контракт пользователя (`backendId`, `minecraftUuid`, `username`)
- `UserService` — методы для получения онлайн/офлайн сессий
- `DatabaseProvider` / `GrpcTransportProvider` — интерфейсы для доступа к подключениям

### 1.2 `adapters/user-backend-adapter` (System Adapter)

**Правило:** Имплементирует контракты ядра, используя vendor-specific технологии.

**Роль:** Вся сетевая/gRPC коммуникация с backend-сервисом.

**Обязанности:**
- Загружается до бизнес-модулей
- Содержит `.proto` файлы и сгенерированные gRPC стабы
- На хуке `AsyncPlayerPreLoginEvent` загружает профиль игрока из backend по gRPC
- Регистрирует `UserService` в `ServiceRegistry`

### 1.3 `modules/*` (Business Logic)

**Правило:** Зависит **только** от `core-api`. Без импорта классов адаптеров или vendor-пакетов.

**Роль:** Реализует игровые фичи (Clans, Donate, Chat).

**Обязанности:**
- Взаимодействует с данными через `ServiceRegistry.get(UserService.class)`
- Получает глобальные ID игроков (`user.getBackendId()`) из in-memory объектов ядра
- Хранит свои SQL-миграции и `.proto` схемы локально в модуле

---

## 2. Core API Контракты

### 2.1 ServiceRegistry

```java
package ru.breezeproject.api.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceRegistry {
    private static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    private ServiceRegistry() {}

    public static <T> void register(Class<T> serviceClass, T implementation) {
        SERVICES.put(serviceClass, implementation);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> get(Class<T> serviceClass) {
        return Optional.ofNullable((T) SERVICES.get(serviceClass));
    }

    public static <T> T getOrThrow(Class<T> serviceClass) {
        return get(serviceClass).orElseThrow(() ->
            new IllegalStateException("Service not registered: " + serviceClass.getName()));
    }

    public static void unregister(Class<?> serviceClass) {
        SERVICES.remove(serviceClass);
    }
}
```

### 2.2 User & UserService

```java
package ru.breezeproject.api.user;

import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface User {
    long getBackendId();
    UUID getMinecraftUuid();
    String getUsername();

    <T> Optional<T> getAttribute(String key, Class<T> type);
    void setAttribute(String key, Object value);
}

public interface UserService {
    Optional<User> getUser(UUID minecraftUuid);
    Optional<User> getUser(long backendId);
    CompletableFuture<User> fetchOfflineUser(String username);
}
```

---

## 3. Lifecycle Pipeline

```
Phase 1: CORE LOADING
  ├── Инициализация ServiceRegistry
  └── Загрузка ModuleClassLoader'ов

Phase 2: ADAPTERS LOADING
  ├── Загрузка system adapter'ов (user-backend-adapter и др.)
  ├── Инициализация gRPC ManagedChannel
  └── Регистрация реализаций (UserService) в ServiceRegistry

Phase 3: BUSINESS MODULES LOADING
  ├── Загрузка бизнес-модулей (Clans, Donate, Chat)
  ├── Получение UserService из ServiceRegistry
  └── Старт обработки доменных событий
```

---

## 4. Связанные документы

- [ADR-001: Microkernel Architecture](adr-001-microkernel-architecture.md) — общая архитектурная схема
- [ADR-002: Cross-Module Integration](adr-002-cross-module-integration.md) — паттерны интеграции модулей
- [TODO: Architecture Compliance](../../TODO.md) — план приведения кода к описанной архитектуре
