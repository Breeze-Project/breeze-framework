package ru.breezeproject.core.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.breezeproject.api.event.BreezeEvent;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.event.EventPriority;

public class SimpleEventBus implements EventBus {
  private record HandlerEntry<T extends BreezeEvent>(
      Class<T> eventType,
      EventPriority priority,
      Consumer<T> handler) implements Subscription {
  }

  private final Map<Class<? extends BreezeEvent>, List<HandlerEntry<?>>> handlers = new ConcurrentHashMap<>();

  private final Logger logger;

  public SimpleEventBus(final Logger logger) {
    this.logger = logger;
  }

  @Override
  public <T extends BreezeEvent> Subscription subscribe(final Class<T> eventType, final Consumer<T> handler) {
    return subscribe(eventType, EventPriority.NORMAL, handler);
  }

  @Override
  public <T extends BreezeEvent> Subscription subscribe(final Class<T> eventType, final EventPriority priority,
      final Consumer<T> handler) {
    final HandlerEntry<T> entry = new HandlerEntry<>(eventType, priority, handler);
    final List<HandlerEntry<?>> list = handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());

    int insertIndex = 0;
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).priority().ordinal() <= priority.ordinal()) {
        insertIndex = i + 1;
      } else {
        break;
      }
    }
    list.add(insertIndex, entry);
    return entry;
  }

  @Override
  public void unsubscribe(final Subscription subscription) {
    if (!(subscription instanceof final HandlerEntry<?> entry)) {
      return;
    }
    final List<HandlerEntry<?>> list = handlers.get(entry.eventType());
    if (list != null) {
      list.remove(entry);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends BreezeEvent> void publish(final T event) {
    final List<HandlerEntry<?>> list = handlers.get(event.getClass());
    if (list == null || list.isEmpty()) {
      return;
    }

    list.forEach(entry -> {
      try {
        ((Consumer<T>) entry.handler()).accept(event);
      } catch (final Exception e) {
        logger.log(Level.SEVERE, "Error dispatching " + event.getClass().getSimpleName() + " to a subscriber", e);
      }
    });
  }
}
