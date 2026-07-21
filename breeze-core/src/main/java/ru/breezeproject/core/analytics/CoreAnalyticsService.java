package ru.breezeproject.core.analytics;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import ru.breezeproject.api.analytics.AnalyticsService;
import ru.breezeproject.api.analytics.PostHogClient;

public final class CoreAnalyticsService implements AnalyticsService {
  private final PostHogClient postHogClient;

  public CoreAnalyticsService(final PostHogClient postHogClient) {
    this.postHogClient = postHogClient;
  }

  @Override
  public void track(final UUID playerId, final String event, final Map<String, Object> properties) {
    if (playerId == null || !postHogClient.isEnabled()) {
      return;
    }
    postHogClient.capture(playerId.toString(), event, copy(properties));
  }

  @Override
  public void track(final Player player, final String event, final Map<String, Object> properties) {
    if (player == null || !postHogClient.isEnabled()) {
      return;
    }
    final Map<String, Object> payload = copy(properties);
    payload.putIfAbsent("player_name", player.getName());
    payload.putIfAbsent("player_uuid", player.getUniqueId().toString());
    track(player.getUniqueId(), event, payload);
  }

  @Override
  public void identify(final UUID playerId, final Map<String, Object> traits) {
    if (playerId == null || !postHogClient.isEnabled()) {
      return;
    }
    postHogClient.identify(playerId.toString(), copy(traits));
  }

  @Override
  public void identify(final Player player, final Map<String, Object> traits) {
    if (player == null || !postHogClient.isEnabled()) {
      return;
    }
    final Map<String, Object> payload = copy(traits);
    payload.putIfAbsent("minecraft_name", player.getName());
    payload.putIfAbsent("minecraft_uuid", player.getUniqueId().toString());
    identify(player.getUniqueId(), payload);
  }

  @Override
  public boolean isEnabled() {
    return postHogClient.isEnabled();
  }

  private Map<String, Object> copy(final Map<String, Object> source) {
    return source == null ? new HashMap<>() : new HashMap<>(source);
  }
}
