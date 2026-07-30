# TODO — Architecture Compliance

Несоответствия между документами (docs/architecture/) и реальным кодом.

## 🔴 Критические (нарушают идеологию microkernel)

### 1. Vendor-реализации в ядре

**Проблема:** Согласно ADR-001, ядро не должно содержать реализации вендорных API и драйверов БД. Всё что есть сейчас:

- `PostHog` — `CorePostHogClient`, `CoreAnalyticsService` лежат прямо в `breeze-core`
- `HikariCP` + `Flyway` — `DatabaseManager`, `DatabaseMigrator`, `RollbackRunner`, `DatabaseServiceImpl` — всё в `breeze-core`

**Что нужно:** Вынести vendor-реализации в adapter-модули:
- `adapters/posthog-adapter` — PostHog клиент и AnalyticsService
- `adapters/database-adapter` — HikariCP + Flyway вместе (один сквозной concern: пул соединений + миграции, делить бессмысленно)

В `breeze-core` оставить только интерфейсы (уже есть в `breeze-api`) и фабрику/делегат.

### 2. Нет слоя адаптеров

**Проблема:** ADR описывает слой `adapters/` между ядром и внешними плагинами/LuckPerms. В коде:
- Нет директории `adapters/` ни в проекте, ни в схеме загрузки модулей
- `ModuleLoader` сканирует одну плоскую `modules/` директорию — нет разделения на adapters и business modules

**Что нужно:** Либо ввести `modules/adapters/` и `modules/features/` с разными фазами загрузки, либо использовать `module.yml` с classification-полем.

> **Важно:** Адаптеры и бизнес-модули — это отдельные репозитории, не часть breeze-framework. Ядро предоставляет API (`breeze-api`), а адаптеры/модули импортируют его через JitPack/Maven и живут в своих репозиториях. В breeze-framework должна быть только схема загрузки (интерфейсы + ModuleLoader).

### 3. Нет контрактов, описанных в ADR-001

**Проблема:** ADR описывает core-интерфейсы, которых нет в `breeze-api`:

| Контракт | В ADR | В коде |
|----------|-------|--------|
| `PermissionService` | Полноценный интерфейс | ❌ Отсутствует (есть только `StaffVanishService` — другое) |
| `DatabaseProvider` | Чистый `Connection` из пула | ❌ Отсутствует (вместо него `DatabaseService` — уже обёртка на HikariCP) |
| `GrpcTransportProvider` | gRPC канал | ❌ Отсутствует |

**Что нужно:** Добавить интерфейсы в `breeze-api`, реализовать в адаптерах.

---

## 🟡 Средние (не нарушают идеологию, но расходятся с документацией)

### 4. ServiceRegistry — разный API

**Проблема:** В ADR-001 `ServiceRegistry` — статический класс с методами `register()` / `get()` / `getOrThrow()` / `unregister()`. В коде — интерфейс с инстанс-методами через `SimpleServiceRegistry`.

Документация описывает статику:
```java
ServiceRegistry.getOrThrow(PermissionService.class);
```
Код требует инстанс:
```java
serviceRegistry.getOrThrow(PermissionService.class); // нет такого метода
getContext().getServiceRegistry().get(Foo.class) // реальность
```

**Что нужно:** Либо привести документацию к реальности, либо добавить статический фасад/прокси к `SimpleServiceRegistry`.

### 5. Lifecycle pipeline не соответствует ADR

**Проблема:** ADR описывает 4 фазы загрузки:

```
Phase 1: Core Init
Phase 2: Adapters Load
Phase 3: Fallback Check (регистрация заглушек)
Phase 4: Business Modules Load
```

Реальность: `BreezeCoreBootstrap.start()` делает всё линейно — создал сервисы → загрузил БД → загрузил модули. Нет:
- Отдельной фазы адаптеров
- Fallback-заглушек для отсутствующих сервисов
- Проверки критических сервисов перед загрузкой бизнес-модулей

### 6. DB-миграции в ядре

**Проблема:** `breeze-core` содержит Flyway миграции для 3 вендоров и код для их запуска (`DatabaseMigrator`). По ADR-001, ядро не должно содержать SQL Driver implementations. Миграции — ответственность модулей или адаптеров БД.

---

## 🟢 Мелкие

### 7. Нет `depends` проверки при загрузке модулей

`ModuleDescription` содержит поле `depends`, но `ModuleLoader.loadModule()` его не проверяет.

### 8. Нет BreezeApiVersion в конфиге модуля

`ModuleLoader` проверяет `api-version` модуля при загрузке, но при несовместимости просто логирует ошибку — нет механизма graceful degradation или fallback-режима.

---

## Порядок действий (предлагаемый)

1. **Выделить `adapters/` в структуру проекта** — отдельный Gradle subproject или отдельная директория в `modules/`
2. **Вынести PostHog в `adapters/posthog-adapter`** — оставить в `breeze-api` только `AnalyticsService` + `PostHogClient` интерфейсы
3. **Вынести Database в `adapters/database-adapter`** — оставить в `breeze-api` только `DatabaseProvider` (чистый Connection)
4. **Добавить `PermissionService` в `breeze-api`** — реализация будет в `adapters/luckperms-adapter`
5. **Добавить `GrpcTransportProvider` в `breeze-api`** — для будущей микросервисной связи
6. **Разделить lifecycle на фазы** — Adapters Load → Fallback Check → Business Modules Load
7. **Привести документацию** — либо ADR к реальности, либо код к ADR
