package ru.breezeproject.core.event;

import ru.breezeproject.api.event.BreezeEvent;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.event.EventPriority;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleEventBus implements EventBus {

    private final Map<Class<? extends BreezeEvent>, List<HandlerEntry<?>>> handlers = new ConcurrentHashMap<>();
    private final Logger logger;

    public SimpleEventBus(Logger logger) {
        this.logger = logger;
    }

    @Override
    public <T extends BreezeEvent> Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
        return subscribe(eventType, EventPriority.NORMAL, handler);
    }

    @Override
    public <T extends BreezeEvent> Subscription subscribe(Class<T> eventType, EventPriority priority, Consumer<T> handler) {
        HandlerEntry<T> entry = new HandlerEntry<>(eventType, priority, handler);
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(entry);
        return entry;
    }

    @Override
    public void unsubscribe(Subscription subscription) {
        if (!(subscription instanceof HandlerEntry<?> entry)) {
            return;
        }
        List<HandlerEntry<?>> list = handlers.get(entry.eventType());
        if (list != null) {
            list.remove(entry);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BreezeEvent> void publish(T event) {
        List<HandlerEntry<?>> list = handlers.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }

        list.stream()
                .sorted((a, b) -> a.priority().ordinal() - b.priority().ordinal())
                .forEach(entry -> {
                    try {
                        ((Consumer<T>) entry.handler()).accept(event);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error dispatching " + event.getClass().getSimpleName()
                                + " to a subscriber", e);
                    }
                });
    }

    public void unsubscribeAll(List<Subscription> subscriptions) {
        subscriptions.forEach(this::unsubscribe);
    }

    public EventBus scopedView(List<Subscription> sink) {
        return new EventBus() {
            @Override
            public <T extends BreezeEvent> Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
                Subscription sub = SimpleEventBus.this.subscribe(eventType, handler);
                sink.add(sub);
                return sub;
            }

            @Override
            public <T extends BreezeEvent> Subscription subscribe(Class<T> eventType, EventPriority priority, Consumer<T> handler) {
                Subscription sub = SimpleEventBus.this.subscribe(eventType, priority, handler);
                sink.add(sub);
                return sub;
            }

            @Override
            public void unsubscribe(Subscription subscription) {
                SimpleEventBus.this.unsubscribe(subscription);
                sink.remove(subscription);
            }

            @Override
            public <T extends BreezeEvent> void publish(T event) {
                SimpleEventBus.this.publish(event);
            }
        };
    }

    private record HandlerEntry<T extends BreezeEvent>(
            Class<T> eventType,
            EventPriority priority,
            Consumer<T> handler
    ) implements Subscription {
    }
}
