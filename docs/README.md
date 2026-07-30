# Breeze Framework — Architecture Documentation

Архитектурные документы и принятые решения (ADR) для Breeze Framework.

## ADR — Architecture Decision Records

| Документ | Описание |
|----------|----------|
| [ADR-001: Microkernel Architecture](architecture/adr-001-microkernel-architecture.md) | Общая архитектура: тонкое ядро, слой адаптеров, бизнес-модули, Service Registry, ClassLoader изоляция, lifecycle pipeline |
| [ADR-002: Cross-Module Integration](architecture/adr-002-cross-module-integration.md) | Паттерны интеграции модулей без циклических зависимостей: Permissions, Events, Extension Registry |

## Spec — Specification

| Документ | Описание |
|----------|----------|
| [Spec-001: Core API & User Adapter](architecture/spec-001-core-api-adapters.md) | Чёткая граница слоёв: Contract in Core, Implementation in Adapter. ServiceRegistry, User/UserService контракты, lifecycle pipeline |

## Навигация

- `architecture/` — ADR и системные схемы
- `README.md` — этот файл, точка входа в документацию
