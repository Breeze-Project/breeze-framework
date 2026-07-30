# Contributing

## Commit Message Format

```
<type>(<scope>): <description>

<optional body>
```

### Types

| Type     | Когда использовать |
|----------|-------------------|
| `feat`   | Новая фича (пользователю/админу видно) |
| `fix`    | Исправление бага |
| `refactor` | Переписал код, но поведение не изменилось |
| `docs`   | Только документация |
| `test`   | Добавил/поправил тесты |
| `chore`  | Всё остальное: сборка, CI, зависимости, настройки |
| `perf`   | Оптимизация производительности |
| `ci`     | Изменения в CI/CD (GitHub Actions, jitpack) |
| `style`  | Форматирование, отступы, запятые — без логики |

### Scopes (опционально)

| Scope          | Часть проекта |
|----------------|---------------|
| `api`          | `breeze-api` |
| `core`         | `breeze-core` |
| `bootstrap`    | Инициализация и lifecycle |
| `loader`       | ModuleLoader, ClassLoader |
| `event`        | EventBus |
| `schedule`     | FoliaScheduler |
| `db`           | Database (HikariCP, Flyway, миграции) |
| `analytics`    | PostHog |
| `command`      | DynamicCommandRegistrar, команды |
| `docs`         | Документация, ADR, README |
| `ci`           | GitHub Actions, JitPack |
| `build`        | Gradle, зависимости |

### Примеры

```
feat(api): add UserService contract for cross-module user resolution
feat(loader): implement adapter/business module phase separation
fix(db): close HikariCP connection on module disable
refactor(event): replace CopyOnWriteArrayList with lock-free algo
docs(api): add ServiceRegistry javadoc
chore(build): update Canvas API to 1.21.11
perf(schedule): cache ScheduledTask instances
```
