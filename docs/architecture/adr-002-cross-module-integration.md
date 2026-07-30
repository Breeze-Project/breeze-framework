# ADR-002: Cross-Module Integration Patterns

**Дата:** 2026-07-30
**Статус:** Draft
**Контекст:** Как связать два бизнес-модуля (Clans + Donate, Chat + Clans и т.д.), не создавая циклических зависимостей и сохраняя модульность.

---

## Проблема

Если модуль `Clans` начнёт напрямую импортировать классы из `Donate`, а `Donate` из `Clans` — теряется вся модульность. При отключении доната кланы перестанут компилироваться или упадут в runtime.

**Решение:** Три паттерна интеграции — от простого к сложному.

---

## Вариант 1. Через систему прав (Permissions)

В 90% случаев кросс-модульное взаимодействие для донат-фич не требует прямого кода между модулями. Оно решается через **Permission Nodes**, которые модуль `Clans` запрашивает у `PermissionService`.

### Как это работает

1. Модуль `Clans` **не знает про модуль `Donate`**. Он просто проверяет лимиты по пермишенам.
2. Модуль `Donate` (или плагин авто-доната) при покупке просто выдаёт игроку/клану соответствующий пермишен.

### Пример

```java
public int getMaxTagLength(UUID playerId) {
    PermissionService perms = ServiceRegistry.getOrThrow(PermissionService.class);

    if (perms.hasPermission(playerId, "clans.tag.length.6")) return 6;
    if (perms.hasPermission(playerId, "clans.tag.length.5")) return 5;

    return 4; // дефолтный лимит для всех
}
```

### Оценка

| Критерий | Оценка |
|----------|--------|
| **Сложность** | Минимальная |
| **Связанность** | Нулевая (модули ничего не знают друг о друге) |
| **Graceful degradation** | При отключении Donate — всё работает с дефолтными лимитами |
| **Применимость** | Только для статических лимитов/прав |

---

## Вариант 2. Через события (Event-Driven)

Если логика сложнее (например: «за каждый купленный статус доната размер тега увеличивается на 1, плюс учитывается уровень клана»), используется **Шина Событий (Event Bus)**.

### 1. Событие в модуле `Clans`

```java
public class ClanTagLengthCalculateEvent extends Event {
    private final UUID playerId;
    private int maxAllowedLength = 4; // дефолт

    public ClanTagLengthCalculateEvent(UUID playerId) {
        this.playerId = playerId;
    }

    public void setMaxAllowedLength(int newLimit) {
        if (newLimit > this.maxAllowedLength) {
            this.maxAllowedLength = newLimit;
        }
    }

    public int getMaxAllowedLength() { return maxAllowedLength; }
}
```

### 2. Слушатель в модуле `Donate`

```java
public class DonateClanListener implements Listener {

    @EventHandler
    public void onTagCalculate(ClanTagLengthCalculateEvent event) {
        if (hasVIPPlus(event.getPlayerId())) {
            event.setMaxAllowedLength(5);
        }
    }
}
```

### 3. Вызов в модуле `Clans`

```java
ClanTagLengthCalculateEvent event = new ClanTagLengthCalculateEvent(player.getUniqueId());
Bukkit.getPluginManager().callEvent(event);

int finalLimit = event.getMaxAllowedLength(); // 5, если Donate обработал событие
```

### Оценка

| Критерий | Оценка |
|----------|--------|
| **Сложность** | Средняя (нужно объявить событие) |
| **Связанность** | Soft depend: Donate знает о событии из Clans, Clans не знает о Donate |
| **Graceful degradation** | При отключении Donate — событие просто никто не слушает, лимит = 4 |
| **Применимость** | Динамические расчёты, реакция нескольких модулей на одно событие |

---

## Вариант 3. Extension Registry (Кастомные Сервисы в Ядре)

Если модуль `Donate` хочет дать модулю `Clans` полноценное API для расширения функций (например, провайдер бонусов), модуль `Clans` создаёт **интерфейс расширения** в своём API, а регистрация проходит через общий `ServiceRegistry`.

### 1. Интерфейс расширения (в модуле `Clans` или `core-api`)

```java
public interface ClanBonusProvider {
    int getExtraTagLength(UUID playerId);
}
```

### 2. Модуль `Donate` реализует и регистрирует

```java
public class DonateBonusAdapter implements ClanBonusProvider {
    @Override
    public int getExtraTagLength(UUID playerId) {
        return isDonator(playerId) ? 1 : 0;
    }
}

// При включении модуля Donate:
ServiceRegistry.register(ClanBonusProvider.class, new DonateBonusAdapter());
```

### 3. Модуль `Clans` безопасно использует

```java
int baseLimit = 4;

int extraLimit = ServiceRegistry.get(ClanBonusProvider.class)
    .map(provider -> provider.getExtraTagLength(playerId))
    .orElse(0); // модуля доната нет — доп. лимит = 0

int totalMaxTagLength = baseLimit + extraLimit;
```

### Оценка

| Критерий | Оценка |
|----------|--------|
| **Сложность** | Выше среднего (нужен интерфейс + регистрация) |
| **Связанность** | Loose: Clans объявляет интерфейс, Donate его реализует |
| **Graceful degradation** | Optional/Fallback на случай отсутствия реализации |
| **Применимость** | Жёсткая, но безопасная интеграция с полной типизацией |

---

## Сводный вердикт

1. **Вариант 1 (Permissions)** — если всё сводится к выдаче прав за деньги. Стандарт индустрии Minecraft-серверов.
2. **Вариант 2 (Events)** — если изменение условий происходит динамически и на событие могут реагировать **несколько разных модулей** (Донат + Квесты + Сезоны).
3. **Вариант 3 (Extension Registry)** — если нужно построить типизированную, но безопасную интеграцию с возможностью полного отключения модуля-партнёра.
