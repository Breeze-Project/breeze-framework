package ru.breezeproject.core.loader.fixtures;

import ru.breezeproject.api.module.BreezeModule;

import java.util.concurrent.atomic.AtomicInteger;

public class RecordingTestModule extends BreezeModule {

    public static final AtomicInteger ENABLE_COUNT = new AtomicInteger();
    public static final AtomicInteger DISABLE_COUNT = new AtomicInteger();

    public RecordingTestModule() {
    }

    public static void resetCounters() {
        ENABLE_COUNT.set(0);
        DISABLE_COUNT.set(0);
    }

    @Override
    public void onEnable() {
        ENABLE_COUNT.incrementAndGet();
    }

    @Override
    public void onDisable() {
        DISABLE_COUNT.incrementAndGet();
    }
}
