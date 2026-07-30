# ADR-001: Microkernel Architecture — System Blueprint

**Дата:** 2026-07-30
**Статус:** Draft
**Архитектурный стиль:** Microkernel (Plugin-based) with Dedicated Adapters & Business Modules
**Целевая платформа:** CanvasMC / Paper (Java 21+)
**Проект:** Breeze Framework

---

## 1. Концепция и Идеология

Система проектируется по принципу **«Тонкое Ядро» (Lightweight Microkernel)**.

Ядро **не содержит** бизнес-логики, вендорных API (LuckPerms, CMI) и конкретных драйверов БД. Его задача — выступать в роли легкого контроллера жизненного цикла, контрактора (хранилища интерфейсов) и реестра сервисов (Service Locator / Registry).

### Ключевые принципы

1. **Zero Runtime Overhead** — Вся маршрутизация сервисов и связывание происходят на этапе загрузки (`onEnable`). В `tick`-петле сервера модули обращаются к интерфейсам напрямую, без лишних оберток ядра ($\mathcal{O}(1)$ к производительности).
2. **Vendor Decoupling (Слепота бизнес-модулей)** — Модули бизнес-логики (Clans, Donate и др.) не знают про существование внешних плагинов (LuckPerms). Они общаются только с интерфейсами Ядра.
3. **Domain Data Ownership** — Бизнес-модули самостоятельно владеют своими SQL-миграциями и `.proto`-схемами gRPC. Ядро лишь предоставляет ресурсы для их выполнения (пулы соединений и сетевые транспорты).

---

## 2. Архитектурная Схема

```
                    ┌───────────────────────────────────────────┐
                    │         ВНЕШНИЕ ЗАВИСИМОСТИ              │
                    │    (LuckPerms / Flecton / CMI / ...)      │
                    └─────────────────────┬─────────────────────┘
                                          │ (Vendor Specific API)
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          СЛОЙ АДАПТЕРОВ (Adapters)                              │
│   ┌─────────────────────────┐  ┌────────────────────────┐  ┌─────────────────┐  │
│   │    luckperms-adapter    │  │    flecton-adapter     │  │  hikari-adapter │  │
│   └────────────┬────────────┘  └───────────┬────────────┘  └────────┬────────┘  │
└────────────────┼───────────────────────────┼────────────────────────┼────────────┘
                 │ (Регистрация Сервисов)    │                        │
                 ▼                           ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            ЯДРО (Core Engine & API)                             │
│                                                                                 │
│  ┌────────────────────────┐   ┌───────────────────────┐   ┌──────────────────┐  │
│  │   ServiceRegistry      │   │   ModuleClassLoader   │   │   Core Contracts │  │
│  │ (ConcurrentHashMap)    │   │ (Isolated ClassLoad)  │   │   (Interfaces)   │  │
│  └────────────────────────┘   └───────────────────────┘   └──────────────────┘  │
└────────────────▲───────────────────────────▲────────────────────────▲────────────┘
                 │                           │                        │
                 │ (Запрос Сервисов)         │                        │
┌────────────────┼───────────────────────────┼────────────────────────┼────────────┘
│                │            СЛОЙ БИЗНЕС-ЛОГИКИ (Modules)             │
│   ┌────────────┴────────────┐  ┌───────────┴────────────┐  ┌─────────┴───────┐  │
│   │      clans-module       │  │     donate-module      │  │   chat-module   │  │
│   │ ┌─────────────────────┐ │  │ ┌────────────────────┐ │  │                 │  │
│   │ │ SQL Queries / Proto │ │  │ │ SQL Queries / Proto│ │  │                 │  │
│   │ └─────────────────────┘ │  │ └────────────────────┘ │  │                 │  │
│   └─────────────────────────┘  └────────────────────────┘  └─────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Спецификация слоев и зон ответственности

| Слой | Содержимое | Запрещенные зависимости |
|------|------------|------------------------|
| **core-api / core-engine** | • Интерфейсы сервисов (`PermissionService`, `DatabaseProvider`)<br>• `ServiceRegistry`<br>• `ModuleManager` & ClassLoaders<br>• Lifecycle Management (`onEnable`/`onDisable`) | ❌ LuckPerms API<br>❌ NMS / Bukkit Heavy Logic<br>❌ SQL Driver implementations / HikariCP |
| **adapters/\*** | • Реализации контрактов Ядра<br>• Инициализация пулов БД (HikariCP)<br>• Инициализация общих gRPC транспортов | ❌ Код бизнес-логики (кланы, донат)<br>❌ Прямые вызовы между соседними адаптерами |
| **modules/\*** | • Бизнес-логика (Clans, Donate)<br>• Собственный SQL-код (таблицы, SELECT/INSERT)<br>• Собственные `.proto` файлы и gRPC stubs | ❌ Прямой импорт LuckPerms/Vendor классов<br>❌ Управление lifecycle сторонних адаптеров |

---

## 4. Базовые компоненты Ядра

### 4.1. Service Registry (Реестр Сервисов)

Thread-safe хранилище ссылок на сервисы с поддержкой `Optional` во избежание `NullPointerException`.

```java
package ru.breezeproject.core.service;

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
            new IllegalStateException("Required service not registered: " + serviceClass.getName()));
    }

    public static void unregister(Class<?> serviceClass) {
        SERVICES.remove(serviceClass);
    }
}
```

### 4.2. Базовые Контракты Ядра (Core Contracts)

#### PermissionService

```java
package ru.breezeproject.api.service.permission;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PermissionService {
    boolean hasPermission(UUID playerId, String permission);
    CompletableFuture<Boolean> hasPermissionAsync(UUID playerId, String permission);
}
```

#### DatabaseProvider

Ядро **не строит** абстракции над SQL. Оно лишь выдает чистые `Connection` из изолированного адаптером пула.

```java
package ru.breezeproject.api.service.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseProvider {
    Connection getConnection() throws SQLException;
    boolean isHealthy();
}
```

#### GrpcTransportProvider

Предоставляет общий управляемый канал для микросервисной связи.

```java
package ru.breezeproject.api.service.grpc;

import io.grpc.Channel;

public interface GrpcTransportProvider {
    Channel getGlobalChannel();
}
```

---

## 5. Имплементация Адаптеров

### 5.1. LuckPerms Adapter

Адаптер оборачивает Vendor API в стандартный контракт Ядра.

```java
package ru.breezeproject.adapters.luckperms;

import ru.breezeproject.core.service.ServiceRegistry;
import ru.breezeproject.api.service.permission.PermissionService;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsAdapter implements PermissionService {

    private final LuckPerms luckPerms;

    public LuckPermsAdapter() {
        this.luckPerms = LuckPermsProvider.get();
    }

    public void init() {
        ServiceRegistry.register(PermissionService.class, this);
    }

    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        User user = luckPerms.getUserManager().getUser(playerId);
        if (user == null) return false;
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionAsync(UUID playerId, String permission) {
        return luckPerms.getUserManager().loadUser(playerId)
            .thenApply(user -> user.getCachedData().getPermissionData().checkPermission(permission).asBoolean());
    }
}
```

---

## 6. Имплементация Бизнес-Модуля

Модуль `clans` хранит собственные SQL-запросы, свои `.proto` для общения с веб-сервисом кланов, а права проверяет через контракты Ядра.

```java
package ru.breezeproject.modules.clans;

import ru.breezeproject.core.service.ServiceRegistry;
import ru.breezeproject.api.service.database.DatabaseProvider;
import ru.breezeproject.api.service.permission.PermissionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class ClanManager {

    private final PermissionService perms;
    private final DatabaseProvider dbProvider;

    public ClanManager() {
        // Извлечение сервисов при инициализации (Zero Overhead в дальнейшем runtime)
        this.perms = ServiceRegistry.getOrThrow(PermissionService.class);
        this.dbProvider = ServiceRegistry.getOrThrow(DatabaseProvider.class);
    }

    public void createClan(UUID leaderId, String clanName) {
        // 1. Проверка прав через контракты Ядра
        if (!perms.hasPermission(leaderId, "clans.create")) {
            throw new SecurityException("No permission to create clan");
        }

        // 2. Выполнение прямого SQL-запроса модуля
        String sql = "INSERT INTO clans (id, name, leader_id) VALUES (?, ?, ?)";
        try (Connection conn = dbProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, clanName);
            ps.setString(3, leaderId.toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 3. Отправка gRPC сообщения во внешний микросервис (используя сгенерированный внутри модуля stub)
        // ClanServiceGrpc.newBlockingStub(grpcChannel)...
    }
}
```

---

## 7. Жизненный цикл и порядок загрузки (Lifecycle Pipeline)

Во избежание гонок состояний система строго соблюдает фазы инициализации:

```
[ Phase 1: CORE INIT ]
  ├── Инициализация ServiceRegistry
  └── Сканирование директории /modules/

[ Phase 2: ADAPTERS LOAD ]
  ├── Загрузка /modules/adapters/*.jar
  ├── Инициализация соединений к БД (HikariCP)
  └── Регистрация реализации сервисов в ServiceRegistry

[ Phase 3: FALLBACK CHECK ]
  ├── Проверка присутствия критических сервисов (PermissionService, DatabaseProvider)
  └── Если сервис отсутствует → Регистрация Fallback-заглушки (например, OpPermissionFallback)

[ Phase 4: BUSINESS MODULES LOAD ]
  ├── Загрузка /modules/features/*.jar
  ├── Извлечение ссылок из ServiceRegistry
  ├── Выполнение локальных SQL-миграций модулей
  └── Регистрация эвент-листенеров CanvasMC/Paper
```

---

## 8. Изоляция ClassLoader'ов

Каждый модуль загружается через кастомный `ModuleClassLoader`, родителем которого является `ClassLoader` Ядра:

- **Модули видят:** Все классы `core-api.jar` и стандартные библиотеки Java/CanvasMC.
- **Модули НЕ видят:** Внутренности других бизнес-модулей и реализации адаптеров (только их интерфейсы, переданные в Ядро).
- **Результат:** Исключаются конфликты версий сторонних библиотек (например, если два разных модуля используют разные версии одной и той же библиотеки, они изолированы в своих `ClassLoader`'ах).
