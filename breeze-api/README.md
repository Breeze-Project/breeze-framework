# Breeze API

**Публичный API для разработки Breeze-модулей.**

Единственная зависимость, которая нужна модулю. Не содержит реализаций — только интерфейсы и базовые классы.

```xml
<dependency>
    <groupId>ru.breezeproject</groupId>
    <artifactId>breeze-api</artifactId>
    <version>1.7.0</version>
    <scope>provided</scope>
</dependency>
```

## Что входит

### `BreezeModule` — базовый класс модуля

```java
public class MyModule extends BreezeModule {
    @Override
    public void onEnable() { }
    @Override
    public void onDisable() { }
}
```

Жизненный цикл: `constructor → init() → onEnable() ↔ onDisable()`.

### `BreezeModuleContext` — окружение модуля

Через `getContext()` доступны:

| Метод | Назначение |
|-------|------------|
| `getServiceRegistry()` | Доступ к сервисам ядра (БД, analytics, scheduler) |
| `getEventBus()` | Подписка на события Breeze (приоритетная шина) |
| `registerCommand()` | Регистрация команд без `plugin.yml` |
| `registerListener()` | Регистрация Bukkit-слушателей |
| `getScheduler()` | Folia-совместимый планировщик |
| `getDataFolder()` | Директория для данных модуля |

### `ServiceRegistry` — лёгкий DI

```java
// Получить сервис
ServiceRegistry.getOrThrow(AnalyticsService.class)
    .track(player, "event", properties);

// С опциональным fallback
ServiceRegistry.get(DatabaseService.class)
    .ifPresent(db -> db.migrate());
```

Доступные сервисы: `BreezeScheduler`, `AnalyticsService`, `PostHogClient`, `DatabaseService`.

### `EventBus` — шина событий

```java
getContext().getEventBus().subscribe(PlayerChatMessageEvent.class,
    EventPriority.HIGH,
    event -> event.setCancelled(true));
```

Подписки автоматически отписываются при выключении модуля.

### `BreezeScheduler` — Folia-шедулер

```java
getContext().getScheduler().runGlobalLater(() ->
    player.sendMessage("Через 5 сек"), 100L); // 100 тиков
```

5 доменов: `runGlobal`, `runAsync`, `runAtEntity`, `runAtLocation` + их `Later`/`Timer` варианты.

### Другие контракты

- **`ModuleConfig`** — типизированный доступ к YAML-конфигу модуля
- **`BreezeCommandSender`** — абстракция отправителя команды
- **`ModuleCommandExecutor`** / **`ModuleTabCompleter`** — командные интерфейсы
- **`AnalyticsService`** / **`PostHogClient`** — аналитика
- **`AnalyticsEvents`** — константы имён событий
- **`StaffVanishService`** — API ваниша для staff-модулей
- **`BreezeApiVersion`** — проверка совместимости версий

## Сборка

```bash
./gradlew :breeze-api:build
./gradlew :breeze-api:publishToMavenLocal  # публикация в локальный Maven
```

## Создание модуля

Структура JAR:

```
mymodule.jar
├── module.yml            # дескриптор модуля
├── com/example/MyModule.class
├── config.yml            # (опционально) конфиг по умолчанию
└── migrations/           # (опционально) SQL-миграции Flyway
    └── sqlite/
    └── mysql/
    └── postgresql/
```

Пример `module.yml`:

```yaml
name: mymodule
version: 1.0.0
main: com.example.MyModule
api-version: 1.7.0
depends: []
```
