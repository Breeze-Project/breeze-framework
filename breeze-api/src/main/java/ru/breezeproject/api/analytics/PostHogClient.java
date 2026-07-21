package ru.breezeproject.api.analytics;

import java.util.Map;

public interface PostHogClient {

    void capture(String distinctId, String event, Map<String, Object> properties);

    void identify(String distinctId, Map<String, Object> traits);

    void flush();

    boolean isEnabled();

    void shutdown();
}
