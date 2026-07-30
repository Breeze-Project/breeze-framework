# Breeze Core

**Имплементация Breeze Framework — запускаемый Bukkit-плагин.**

Загружает, изолирует и управляет Breeze-модулями на CanvasMC/Paper 1.21.11 с поддержкой Folia.

## Установка

1. Скачать `breeze-core-*.jar` с [GitHub Releases](https://github.com/BreezeProject/breeze-framework/releases)
2. Положить в `plugins/`
3. Настроить `plugins/BreezeCore/config.yml`
4. Перезапустить сервер (или `/reload`)

## Конфигурация

`plugins/BreezeCore/config.yml`:

```yaml
database:
  enabled: false             # включить БД
  type: sqlite               # sqlite | mysql | postgresql
  host: localhost
  port: 3306
  user: root
  password: "CHANGE_ME"
  pool-size: 10
  file: breezecore.db        # для sqlite — путь к файлу

posthog:
  enabled: false
  api_key: ""
  host: "https://us.i.posthog.com"
  debug: false

modules:
  directory: modules         # папка с JAR-модулями
  auto_scan:
    enabled: true            # авто-загрузка новых модулей
    interval_seconds: 30
  copy_settle_ms: 2000       # сколько ждать после копирования JAR
```

## База данных

Опционально. Включается `database.enabled: true`.

- **SQLite** — встроенная, без настройки, пул = 1 соединение
- **MySQL 8.4** — удалённая, настраиваемый пул HikariCP
- **PostgreSQL 16** — удалённая, настраиваемый пул HikariCP

Flyway миграции лежат в `resources/migrations/{vendor}/`. При добавлении схемы — миграция пишется для всех трёх вендоров.

## Аналитика (PostHog)

Встроенный клиент PostHog. Включается `posthog.enabled: true` + указание `api_key`.

Модули отправляют события через `AnalyticsService`:

```java
ServiceRegistry.getOrThrow(AnalyticsService.class)
    .track(player, "module_event", Map.of("key", "value"));
```

## Структура

```
breeze-core.jar
├── ru/breezeproject/core/
│   ├── bootstrap/       # BreezeCoreBootstrap — инициализация всего
│   ├── loader/          # ModuleLoader — сканирование, загрузка, ClassLoader'ы
│   ├── event/           # SimpleEventBus — приоритетная шина
│   ├── schedule/        # FoliaBreezeScheduler — 5 доменов планировщика
│   ├── command/         # DynamicCommandRegistrar — Bukkit CommandMap через рефлексию
│   ├── commands/        # ModulesCommand — /breezemodules
│   ├── context/         # BreezeModuleContextImpl — окружение модуля
│   ├── config/          # YamlModuleConfig
│   ├── database/        # DatabaseService, HikariCP, Flyway, DatabaseVendor
│   └── analytics/       # CoreAnalyticsService, CorePostHogClient
├── plugin.yml
├── config.yml
└── migrations/
    ├── sqlite/
    ├── mysql/
    └── postgresql/
```

## Boot sequence

```
BreezeCorePlugin.onEnable()
  └─► BreezeCoreBootstrap.start()
       ├─► ServiceRegistry, EventBus, Scheduler инициализация
       ├─► Database (async, если включена)
       │    └─► DatabaseServiceImpl.connect() + migrate()
       ├─► ModuleLoader.loadAll()
       │    ├─► сканирование modules/*.jar
       │    ├─► ClassLoader + ModuleContext + init()
       │    └─► module.onEnable()
       ├─► CoreCommandRegistrar (/breezemodules)
       └─► Auto-scan timer (каждые N сек)
```

## ClassLoader изоляция

Каждый модуль загружается в отдельный `URLClassLoader`. Родитель — ClassLoader ядра.

- **Модуль видит:** классы `breeze-api`, Java, CanvasMC/Paper API
- **Модуль НЕ видит:** другие модули, реализации адаптеров, внутренности `breeze-core`

При выключении модуля: `onDisable()` → cleanup подписок/команд → закрытие ClassLoader.

## Сборка из исходников

```bash
# Полный fatjar (breeze-api + зависимости)
./gradlew :breeze-core:build

# Fatjar лежит в:
# breeze-core/build/libs/breeze-core-*.jar
```

## CI миграции

В CI Flyway миграции прогоняются против реальных MySQL 8.4 и PostgreSQL 16 с проверкой идемпотентности:
- Первый запуск — применение миграций
- Второй запуск — проверка, что повторный запуск не падает
- Проверка наличия таблиц: `breeze_players`, `breeze_module_registry`, `flyway_schema_history`

Локально:

```bash
./gradlew :breeze-core:flywayMigrate -PdbVendor=mysql -Pflyway.url=jdbc:mysql://localhost:3306/breezecore -Pflyway.user=root -Pflyway.password=pass
```
