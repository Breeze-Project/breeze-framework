# ADR-003: API — Refactor или Rewrite?

**Дата:** 2026-07-30
**Статус:** Draft
**Контекст:** Разделение `breeze-framework` на два отдельных репозитория (`breeze-api` и `breeze-core`). Перед миграцией решаем: переписать API с нуля или отрефакторить текущий.

---

## 1. Текущее состояние

**breeze-api** — 17 файлов, ~500 строк нетто, одна зависимость (Canvas API). Чистые интерфейсы и абстракции, ноль реализаций.

### Полная карта API

```
ru.breezeproject.api
├── BreezeApiVersion.java          — проверка совместимости (semver)
│
├── analytics/
│   ├── AnalyticsEvents.java       — 40+ констант (player_join, block_break, ...)
│   ├── AnalyticsService.java      — track/identify (Player и UUID overloads)
│   └── PostHogClient.java         — capture/identify/flush/shutdown
│
├── command/
│   ├── BreezeCommandSender.java   — обёртка CommandSender (getName, sendMessage, hasPermission)
│   ├── ModuleCommandExecutor.java — onCommand(sender, label, args)
│   └── ModuleTabCompleter.java    — onTabComplete(sender, args)
│
├── config/
│   └── ModuleConfig.java          — getString/getInt/getBoolean/getSection/save/reload
│
├── event/
│   ├── BreezeEvent.java           — cancellable base
│   ├── BreezeListener.java        — пустой маркер ⚠️
│   ├── EventBus.java              — subscribe(publish/unsubscribe, приоритеты
│   ├── EventPriority.java         — LOWEST→MONITOR
│   └── chat/
│       └── PlayerChatMessageEvent.java  — единственное встроенное событие
│
├── module/
│   ├── BreezeModule.java          — abstract class с onEnable/onDisable
│   ├── BreezeModuleContext.java   — ServiceRegistry, EventBus, Scheduler, команды
│   └── ModuleDescription.java     — record (name, version, main, api-version, depends)
│
├── schedule/
│   ├── BreezeScheduler.java       — 5 доменов, 11 методов
│   └── BreezeTask.java            — cancel/isCancelled
│
├── service/
│   └── ServiceRegistry.java       — register/get/unregister
│
└── staff/
    └── StaffVanishService.java    — isVanished(Player)
```

### Ключевые достоинства

| Решение | Почему хорошо |
|---------|---------------|
| **Module → ModuleContext → сервисы** | Правильный lifecycle: модуль не лезет напрямую в Bukkit |
| **BreezeScheduler покрывает 5 доменов Folia** | Не надо думать "а какой scheduler?", API сразу правильный |
| **EventBus с subscription-based** | Естественный cleanup при disable модуля |
| **ServiceRegistry — 3 метода** | Минимально возможный DI, ничего лишнего |
| **BreezeApiVersion.isCompatible()** | Правильно locked по major, можно расширять minor + patch |
| **ModuleDescription — record** | Иммутабельный, никакого boilerplate |
| **ModuleCommandExecutor / BreezeCommandSender** | Чистая абстракция Bukkit команд, модули не знают про CommandSender |

---

## 2. Что можно/нужно поправить

### 2.1 🔴 `BreezeListener` — пустая маркерная заглушка

```java
public interface BreezeListener {}
```

В `BreezeModuleContextImpl.registerListener()`:
```java
if (!(listener instanceof Listener bukkitListener)) {
    throw new IllegalArgumentException("Listener must implement org.bukkit.event.Listener");
}
```

Маркер бесполезен — без каста к Bukkit `Listener` всё равно падает. Модуль всё равно импортирует Bukkit event'ы.

**Что делать:**
- **Вариант A (проще):** Убрать `BreezeListener` из API. `registerListener(Listener)` — модуль передаёт Bukkit Listener напрямую.
- **Вариант B (чище):** Убрать `registerListener` в принципе. Модуль подписывается на Bukkit события через `Bukkit.getPluginManager().registerEvents()`, а на Breeze-события — через `EventBus`. Breeze не должен дублировать Bukkit event API.

**Рекомендация:** Вариант A — меньше ломаем, убираем только маркер.

### 2.2 🔴 `getOwnerPluginHandle()` — утечка Bukkit

```java
Object getOwnerPluginHandle();  // в BreezeModuleContext
```

Модуль может сделать `(JavaPlugin) context.getOwnerPluginHandle()` и полезть в Bukkit API напрямую, нарушая изоляцию. Этот метод существует только потому что `DynamicCommandRegistrar` и `Bukkit.getPluginManager()` нужны внутри implementation'а.

**Что делать:** Убрать из API. Если модулю нужен `JavaPlugin` для Bukkit API — он не модуль. Core использует `ownerPlugin` внутри себя, модулям он не нужен.

### 2.3 🟡 `AnalyticsService` — дубляж методов

```java
void track(UUID playerId, String event, Map<String, Object> properties);
void track(Player player, String event, Map<String, Object> properties);  // player.getUniqueId()
void identify(UUID playerId, Map<String, Object> traits);
void identify(Player player, Map<String, Object> traits);                 // player.getUniqueId()
```

Player-overload'ы ничего не добавляют — только `player.getUniqueId()`. Bukkit-зависимость в API-контракте.

**Что делать:** Убрать методы с `Player`. Оставить только `UUID`. Если модулю нужно передать координаты/мир/доп. данные — он кладёт их в `properties`.

### 2.4 🟡 `BreezeEvent.cancelled` не thread-safe

```java
private boolean cancelled;  // нет volatile
public void setCancelled(final boolean cancelled) { ... }
```

Два listener'а из разных потоков (async scheduler + global) могут одновременно вызвать `setCancelled` — race condition.

**Что делать:** `private volatile boolean cancelled;`

### 2.5 🟢 Нет `PermissionService` в API

ADR-002 описывает кросс-модульные пермишены как основной паттерн, но контракта нет — только `StaffVanishService` (vanished/not vanished).

**Что делать:** Добавить в API опционально — не блокер для разделения репо. Можно сделать позже.

---

## 3. Решение: Refactor, не Rewrite

### Почему не rewrite

| Аргумент | Обоснование |
|----------|-------------|
| **API — 17 файлов / ~500 строк** | Переписать 17 файлов можно за день. Но что это даст? Те же интерфейсы с теми же методами. |
| **Абстракции уже правильные** | Module → Context → EventBus/Scheduler/ServiceRegistry — верная архитектура. Переписывая, ты воспроизведёшь то же самое с косметическими отличиями. |
| **Риск регрессии** | Модули (Clans, Donate, Chat) пишутся под текущий API. Любое несовпадение — перегрузки, порядок параметров, exception spec — сломает их. Нулевая польза, реальный вред. |
| **Разделение репо — механическая работа** | API уже отдельный Gradle-модуль. Нужно: создать git-репо для API, перенести код, настроить publish на JitPack, в core заменить `project(":breeze-api")` на dependency. Код не меняется. |
| **API не будет расти экспоненциально** | Minecraft-фреймворк имеет конечный набор контрактов. Нет сценария, где API внезапно станет 50 000 строк. |

### Что даёт refactor вместо rewrite

1. **Совместимость не ломается** — модули продолжают работать
2. **Разделение репо занимает часы, а не дни** — ту же механику можно сделать на текущем коде
3. **Рефактор точечный** — 5 изменений, а не полная замена
4. **Меньше багов** — правишь известные проблемы, не вносишь новые

---

## 4. План refactor'а

### Phase 1: Подготовка (30 мин)

1. Создать репозиторий `breeze-api`
2. Скопировать текущий `breeze-api/` (весь код как есть)
3. Настроить `settings.gradle.kts`, `jitpack.yml`, publish
4. Убедиться, что сборка проходит независимо
5. В `breeze-core` заменить `implementation(project(":breeze-api"))` на `implementation("ru.breezeproject:breeze-api:1.7.0")` (через JitPack или локальный maven)

### Phase 2: Refactor API (2-3 часа)

| № | Что | Файл | Изменение |
|---|-----|------|-----------|
| 1 | Удалить `BreezeListener` | `BreezeListener.java` | Удалить файл. `registerListener` принимает `org.bukkit.event.Listener` напрямую |
| 2 | Убрать `getOwnerPluginHandle()` | `BreezeModuleContext.java` | Удалить метод. Если нужно в core — перенести в impl |
| 3 | Убрать Player-overload'ы | `AnalyticsService.java` | Удалить `track(Player, ...)` и `identify(Player, ...)` |
| 4 | Сделать cancelled volatile | `BreezeEvent.java` | `private volatile boolean cancelled;` |

### Phase 3: Пересобрать core (1 час)

1. Обновить dependency на новый `breeze-api` (JitPack)
2. Адаптировать `BreezeModuleContextImpl` под изменения API
3. Собрать, протестировать

### Phase 4: Добавить PermissionService (опционально, ещё 1 час)

```java
public interface PermissionService {
    boolean hasPermission(UUID playerId, String permission);
    CompletableFuture<Boolean> hasPermissionAsync(UUID playerId, String permission);
}
```

Реализация — в adapter (LuckPerms). Core регистрирует fallback (OpPermissionFallback).

---

## 5. Итог

| | Rewrite | Refactor |
|---|---|---|
| **Время** | 2-3 дня + отладка | 3-4 часа |
| **Риск регрессии** | Высокий | Низкий (5 точечных изменений) |
| **Профит** | Минимальный | Те же улучшения + сохранение совместимости |
| **Совместимость модулей** | Ломается | Сохраняется |

**Вердикт: Refactor.** API уже хорошо спроектирован. Исправляем 4 конкретные проблемы и занимаемся настоящей работой — разделением репо и building'ом.
