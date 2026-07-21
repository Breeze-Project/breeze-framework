package ru.breezeproject.api.analytics;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public interface AnalyticsService {

    void track(UUID playerId, String event, Map<String, Object> properties);

    void track(Player player, String event, Map<String, Object> properties);

    void identify(UUID playerId, Map<String, Object> traits);

    void identify(Player player, Map<String, Object> traits);

    boolean isEnabled();
}
