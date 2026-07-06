package ru.breezeproject.core.event;

import org.junit.jupiter.api.Test;
import ru.breezeproject.api.event.BreezeEvent;
import ru.breezeproject.api.event.EventBus;
import ru.breezeproject.api.event.EventPriority;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleEventBusTest {

    static class PingEvent extends BreezeEvent {
        PingEvent() {
            super(true);
        }
    }

    static class OtherEvent extends BreezeEvent {
    }

    private SimpleEventBus newBus() {
        return new SimpleEventBus(Logger.getLogger("test"));
    }

    @Test
    void subscriberReceivesPublishedEvent() {
        SimpleEventBus bus = newBus();
        List<PingEvent> received = new ArrayList<>();

        bus.subscribe(PingEvent.class, received::add);
        bus.publish(new PingEvent());

        assertEquals(1, received.size());
    }

    @Test
    void subscribersOnlyReceiveTheirOwnEventType() {
        SimpleEventBus bus = newBus();
        List<Object> received = new ArrayList<>();

        bus.subscribe(PingEvent.class, received::add);
        bus.publish(new OtherEvent());

        assertTrue(received.isEmpty());
    }

    @Test
    void handlersRunInPriorityOrder() {
        SimpleEventBus bus = newBus();
        List<String> order = new ArrayList<>();

        bus.subscribe(PingEvent.class, EventPriority.MONITOR, e -> order.add("monitor"));
        bus.subscribe(PingEvent.class, EventPriority.LOWEST, e -> order.add("lowest"));
        bus.subscribe(PingEvent.class, EventPriority.HIGH, e -> order.add("high"));
        bus.subscribe(PingEvent.class, EventPriority.NORMAL, e -> order.add("normal"));

        bus.publish(new PingEvent());

        assertEquals(List.of("lowest", "normal", "high", "monitor"), order);
    }

    @Test
    void aThrowingHandlerDoesNotStopOtherHandlers() {
        SimpleEventBus bus = newBus();
        List<String> received = new ArrayList<>();

        bus.subscribe(PingEvent.class, EventPriority.LOW, e -> {
            throw new RuntimeException("boom");
        });
        bus.subscribe(PingEvent.class, EventPriority.HIGH, e -> received.add("second handler ran"));

        bus.publish(new PingEvent());

        assertEquals(List.of("second handler ran"), received);
    }

    @Test
    void unsubscribeStopsFurtherDelivery() {
        SimpleEventBus bus = newBus();
        List<PingEvent> received = new ArrayList<>();

        EventBus.Subscription sub = bus.subscribe(PingEvent.class, received::add);
        bus.unsubscribe(sub);
        bus.publish(new PingEvent());

        assertTrue(received.isEmpty());
    }

    @Test
    void cancellingAnEventIsVisibleToLaterHandlersAndPublisher() {
        SimpleEventBus bus = newBus();

        bus.subscribe(PingEvent.class, EventPriority.LOWEST, e -> e.setCancelled(true));

        PingEvent event = new PingEvent();
        bus.publish(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void scopedViewTracksSubscriptionsInSink() {
        SimpleEventBus bus = newBus();
        List<EventBus.Subscription> sink = new ArrayList<>();
        EventBus scoped = bus.scopedView(sink);

        scoped.subscribe(PingEvent.class, e -> {
        });
        scoped.subscribe(OtherEvent.class, e -> {
        });

        assertEquals(2, sink.size());
    }

    @Test
    void unsubscribeAllRemovesEveryTrackedSubscription() {
        SimpleEventBus bus = newBus();
        List<EventBus.Subscription> sink = new ArrayList<>();
        EventBus scoped = bus.scopedView(sink);
        List<PingEvent> received = new ArrayList<>();

        scoped.subscribe(PingEvent.class, received::add);

        bus.unsubscribeAll(sink);

        bus.publish(new PingEvent());

        assertTrue(received.isEmpty());
    }
}
