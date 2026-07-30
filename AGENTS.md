# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Breeze Framework — a Minecraft plugin framework for Paper/Folia servers (Canvas 1.21.11). Provides a modular plugin system: third-party "Breeze modules" load from JARs at runtime via the core plugin, with their own lifecycle, config, events, commands, scheduler, and service injection.

## Build & Test

```bash
# Build everything
./gradlew build

# Build/publish API only (for module developers)
./gradlew :breeze-api:build
./gradlew :breeze-api:publishToMavenLocal

# Build core (fatjar with shading)
./gradlew :breeze-core:build

# Run tests
./gradlew :breeze-api:test
./gradlew :breeze-core:test

# Run Flyway migrations against a specific vendor
./gradlew :breeze-core:flywayMigrate -PdbVendor=mysql -Pflyway.url=jdbc:mysql://localhost:3306/breezecore -Pflyway.user=root -Pflyway.password=pass
```

**CI**: GitHub Actions builds both subprojects, tests API, runs integrations against real MySQL 8.4 and PostgreSQL 16 (Flyway migrations + idempotency check), and creates GitHub releases on `v*` tags.

**JitPack**: Published with `:breeze-api:publishToMavenLocal -x test` on JDK 21.

## Project Structure

```
breeze-framework/
├── breeze-api/           # Public API — the only artifact module devs depend on
│   └── src/main/java/ru/breezeproject/api/
│       ├── module/       # BreezeModule base class, BreezeModuleContext, ModuleDescription
│       ├── event/        # EventBus, BreezeEvent, BreezeListener, EventPriority
│       ├── schedule/     # BreezeScheduler, BreezeTask (abstracts Folia's 5 scheduling domains)
│       ├── service/      # ServiceRegistry (lightweight DI)
│       ├── config/       # ModuleConfig (typed config access for modules)
│       ├── command/      # ModuleCommandExecutor, ModuleTabCompleter, BreezeCommandSender
│       ├── analytics/    # AnalyticsService, PostHogClient, AnalyticsEvents
│       └── staff/        # StaffVanishService
│
├── breeze-core/          # Implementation — the runnable Bukkit plugin
│   └── src/main/java/ru/breezeproject/core/
│       ├── bootstrap/    # BreezeCoreBootstrap — wires all services, starts module loader
│       ├── loader/       # ModuleLoader (implements ModuleManager), ModuleDescriptorReader,
│       │                 # ModuleConfigLoader, DisabledModulesStore
│       ├── event/        # SimpleEventBus — priority-ordered, concurrent event dispatch
│       ├── schedule/     # FoliaBreezeScheduler + FoliaBreezeTask
│       ├── service/      # SimpleServiceRegistry
│       ├── command/      # DynamicCommandRegistrar (reflection-based CommandMap),
│       │                 # CoreCommandRegistrar, BukkitCommandSenderAdapter
│       ├── commands/     # ModulesCommand (/breezemodules)
│       ├── config/       # YamlModuleConfig
│       ├── context/      # BreezeModuleContextImpl
│       ├── database/     # DatabaseService, DatabaseManager (HikariCP),
│       │                 # DatabaseMigrator (Flyway), RollbackRunner, DatabaseConfig, DatabaseVendor
│       └── analytics/    # CoreAnalyticsService, CorePostHogClient (posthog-server SDK)
│       └── resources/
│           ├── plugin.yml      # Bukkit plugin descriptor
│           ├── config.yml      # Default config (database, posthog, modules)
│           └── migrations/     # Flyway SQL migrations per vendor
│               ├── sqlite/
│               ├── mysql/
│               └── postgresql/
```

## Architecture

### Boot Sequence
1. `BreezeCorePlugin.onEnable()` → `BreezeCoreBootstrap.start()`
2. Bootstrap creates all core services (`ServiceRegistry`, `EventBus`, `Scheduler`, `PostHogClient`, `AnalyticsService`, `DatabaseService`)
3. Database initialized asynchronously (if enabled in config)
4. `ModuleLoader.loadAll()` scans the modules directory for `.jar` files with `module.yml`
5. `CoreCommandRegistrar` registers `/breezemodules`
6. Auto-scan timer starts (periodically checks for new module JARs)

### Module Loading
Each module JAR must contain `module.yml` with `name`, `main`, `version`, and `api-version` fields. The module is loaded in a `URLClassLoader`, instantiated via `BreezeModule`, and provided a `BreezeModuleContextImpl` that wraps the shared `EventBus` (with per-module subscription tracking), `ServiceRegistry`, and scheduler. On disable, the context's `cleanup()` unsubscribes all listeners, unregisters commands, and closes the classloader.

### Database Layer (Optional)
- Database is opt-in: `database.enabled: false` by default
- Supports MySQL, PostgreSQL, and SQLite via Flyway migrations
- Vendor-specific migrations under `resources/migrations/{vendor}/`
- Connection pooling via HikariCP; SQLite driver gets a pool of 1

### Scheduler
`FoliaBreezeScheduler` wraps all five Folia scheduling domains:
- **Global** — server-wide sync tasks
- **Async** — off-thread tasks
- **Entity** — tasks tied to an entity's tick region
- **Location** — tasks tied to a chunk's tick region

### Version Compatibility
`BreezeApiVersion.isCompatible()` enforces semver: major must match, minor must be <= current, patch must be <= current.

## Agents

Project-specific agents in [`.agents/`](.agents/) provide specialized context for common tasks — supported by Claude Code, Codex, and other AI coding tools:

| Agent | Purpose |
|-------|---------|
| [`java-reviewer`](.agents/java-reviewer.md) | Review Java code for Bukkit/Paper plugin patterns, thread safety, classloader hygiene |
| [`build-error-resolver`](.agents/build-error-resolver.md) | Fix Gradle build errors, Canvas API issues, Flyway migrations |
| [`database-reviewer`](.agents/database-reviewer.md) | Review Flyway migrations across all 3 vendors (SQLite/MySQL/PostgreSQL) |

Use with: `claude code --agent <name>`, `codex --agent <name>`, or via the Agent tool in conversations.

## Architecture Docs

- [`docs/architecture/adr-001-microkernel-architecture.md`](docs/architecture/adr-001-microkernel-architecture.md) — Microkernel blueprint: adapter layer, business modules, Service Registry, ClassLoader isolation, lifecycle pipeline.
- [`docs/architecture/adr-002-cross-module-integration.md`](docs/architecture/adr-002-cross-module-integration.md) — Cross-module integration patterns: Permissions, Events, and Extension Registry for decoupled module communication.
- [`docs/architecture/spec-001-core-api-adapters.md`](docs/architecture/spec-001-core-api-adapters.md) — Contract in Core, Implementation in Adapter. ServiceRegistry, User/UserService, 3-phase lifecycle.

## Key Conventions

- **Style**: 2-space indentation, no tabs, no trailing whitespace. All Java files use the formatting seen throughout the codebase.
- **Naming**: `package ru.breezeproject.api.*` for API, `ru.breezeproject.core.*` for implementation.
- **Service pattern**: Interfaces in `breeze-api`, implementations in `breeze-core`, registered in `ServiceRegistry`.
- **Module lifecycle**: Constructor → `init()` → `onEnable()` → runtime → `onDisable()`.
- **Database migrations**: Each vendor has a separate `migrations/{vendor}/` directory. Always add a migration for ALL three vendors when changing schema.
- **Tests**: JUnit 5 (Jupiter). No test framework wrappers. Each subproject tests independently.
