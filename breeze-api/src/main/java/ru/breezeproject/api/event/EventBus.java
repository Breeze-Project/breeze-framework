package ru.breezeproject.api.event;

import java.util.function.Consumer;

public interface EventBus {

    <T extends BreezeEvent> Subscription subscribe(Class<T> eventType, Consumer<T> handler);

    <T extends BreezeEvent> Subscription subscribe(Class<T> eventType, EventPriority priority, Consumer<T> handler);

    void unsubscribe(Subscription subscription);

    <T extends BreezeEvent> void publish(T event);

    interface Subscription {
        Class<? extends BreezeEvent> eventType();
    }
}
