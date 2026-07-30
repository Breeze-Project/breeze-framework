# Breeze Framework

**Микроядерный модульный фреймворк для CanvasMC/Paper 1.21.11.**

Позволяет загружать, выгружать и перезагружать «Breeze-модули» (отдельные JAR-файлы) прямо на работающем сервере — без перезапуска. Каждый модуль живёт в изолированном ClassLoader'е со своим lifecycle, конфигом, событиями, командами и шедулером.

```xml
<repository>
    <id>jitpack</id>
    <url>https://jitpack.io</url>
</repository>
<dependency>
    <groupId>ru.breezeproject</groupId>
    <artifactId>breeze-api</artifactId>
    <version>1.7.0</version>
    <scope>provided</scope>
</dependency>
```

## Быстрый старт

```java
public class MyModule extends BreezeModule {

    @Override
    public void onEnable() {
        getLogger().info("Модуль " + getName() + " включён!");

        // Команда
        getContext().registerCommand("mycmd", List.of(), "My command", "/mycmd",
            (sender, label, args) -> {
                sender.sendMessage("Привет!");
                return true;
            }, null);

        // Событие
        getContext().getEventBus().subscribe(PlayerJoinEvent.class,
            event -> getLogger().info("Зашёл " + event.getPlayer().getName()));

        // Сервис
        getContext().getServiceRegistry().getOrThrow(AnalyticsService.class)
            .track(player, "module_event", Map.of("module", getName()));
    }

    @Override
    public void onDisable() {
        getLogger().info("Модуль " + getName() + " выключен.");
    }
}
```

## Архитектура

Breeze построен по **микроядерной архитектуре**:

```
  [JAR-модули]        adapters/          core-engine
       │                  │                  │
  clans.jar ──► ClassLoader Isolation ──► ServiceRegistry
  donate.jar ──► изолированный URLCL     EventBus
  chat.jar  ──► не видят друг друга       Scheduler
       │                                  DB (HikariCP + Flyway)
       │                     ▲            Analytics (PostHog)
       └─────────────────────┘
        (получают сервисы из ядра)
```

**Три слоя:**
1. **Ядро** (`breeze-core`) — ServiceRegistry, EventBus, Scheduler, ModuleLoader. Нулевая бизнес-логика.
2. **Адаптеры** (`adapters/`) — оборачивают внешние плагины (LuckPerms и др.) в контракты ядра.
3. **Модули** (`modules/`) — бизнес-логика, ничего не знают о внешних плагинах напрямую.

Подробно: [docs/architecture/adr-001-microkernel-architecture.md](docs/architecture/adr-001-microkernel-architecture.md).

## Возможности

- **Изолированная загрузка модулей** — каждый модуль в своём ClassLoader, без конфликтов библиотек
- **Service Registry** — легковесный DI: интерфейсы в `breeze-api`, реализации подключаются адаптерами
- **Event Bus** — приоритетная шина событий, per-module tracking подписок для clean disable
- **Folia-совместимый Scheduler** — все 5 доменов: global, async, entity, location
- **Опциональная БД** — SQLite / MySQL / PostgreSQL через Flyway миграции + HikariCP
- **PostHog Analytics** — встроенная аналитика, опционально
- **Dynamic Commands** — регистрация команд модулей через Bukkit CommandMap (без plugin.yml)
- **Auto-scan** — автоматическая загрузка новых JAR-модулей из папки по таймеру

## Модуль: module.yml

Каждый модуль — JAR-файл с `module.yml` в корне:

```yaml
name: mymodule
version: 1.0.0
main: com.example.MyModule
api-version: 1.7.0
depends: []
```

## Команды

```
/breezemodules                   — список загруженных и отключённых модулей
/breezemodules load [name]       — загрузить новый модуль (все или по имени)
/breezemodules reload <name>     — перезагрузить модуль с диска
/breezemodules enable <name>     — включить отключённый модуль
/breezemodules disable <name>    — отключить загруженный модуль
```

## Сборка

```bash
# Всё сразу
./gradlew build

# Только API (для разработки модулей)
./gradlew :breeze-api:build

# Core (fatjar)
./gradlew :breeze-core:build

# Тесты
./gradlew :breeze-api:test
./gradlew :breeze-core:test
```

## Интеграция модулей (без циклических зависимостей)

Три способа связать модули, не создав спагетти:

| Способ | Когда использовать |
|--------|-------------------|
| **Permissions** | Статические лимиты и права — модуль Donate выдаёт пермишен, модуль Clans его читает |
| **Events** | Динамические расчёты — Clans бросает событие, Donate на него отвечает |
| **Extension Registry** | Типизированное API — Clans объявляет интерфейс, Donate регистрирует реализацию в ServiceRegistry |

Подробно: [docs/architecture/adr-002-cross-module-integration.md](docs/architecture/adr-002-cross-module-integration.md).

## CI / CD

GitHub Actions:
- **build** — сборка и тесты API + Core на JDK 21
- **migrate-against-real-mysql** — Flyway миграции против реального MySQL 8.4
- **migrate-against-real-postgresql** — Flyway миграции против реального PostgreSQL 16
- **release** — GitHub Release с артефактом `breeze-core-*.jar` при пуше тега `v*`

## Требования

- Java 21+
- CanvasMC 1.21.11 или Paper 1.21.11 (с Folia)
- Gradle (через ./gradlew)

---

**Лицензия:** MIT
