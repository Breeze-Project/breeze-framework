package ru.breezeproject.core.analytics;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

import com.posthog.server.PostHog;
import com.posthog.server.PostHogCaptureOptions;
import com.posthog.server.PostHogConfig;
import com.posthog.server.PostHogInterface;

import ru.breezeproject.api.analytics.PostHogClient;

public final class CorePostHogClient implements PostHogClient {
  private final Logger logger;
  private final boolean enabled;
  private final PostHogInterface posthog;

  public CorePostHogClient(final FileConfiguration config, final Logger logger) {
    this.logger = logger;
    final String apiKey = config.getString("posthog.api_key", "").trim();
    final String host = config.getString("posthog.host", "https://us.i.posthog.com").trim();
    final boolean configuredEnabled = config.getBoolean("posthog.enabled", false);
    final boolean isEnabled = configuredEnabled && !apiKey.isEmpty();
    this.enabled = isEnabled;

    if (!isEnabled) {
      this.posthog = null;
      return;
    }

    final boolean debug = config.getBoolean("posthog.debug", false);
    final PostHogConfig postHogConfig = PostHogConfig.builder(apiKey)
        .host(host.isBlank() ? "https://us.i.posthog.com" : host)
        .debug(debug)
        .build();
    this.posthog = PostHog.with(postHogConfig);
  }

  @Override
  public void capture(final String distinctId, final String event, final Map<String, Object> properties) {
    if (!enabled || posthog == null || distinctId == null || distinctId.isBlank()) {
      return;
    }
    if (event == null || event.isBlank()) {
      return;
    }
    try {
      if (properties == null || properties.isEmpty()) {
        posthog.capture(distinctId, event);
        return;
      }
      final PostHogCaptureOptions.Builder options = PostHogCaptureOptions.builder();
      properties.forEach((key, value) -> {
        if (key != null && !key.isBlank() && value != null) {
          options.property(key, value);
        }
      });
      posthog.capture(distinctId, event, options.build());
    } catch (final Exception e) {
      logger.log(Level.WARNING, "PostHog capture failed for event '" + event + "'", e);
    }
  }

  @Override
  public void identify(final String distinctId, final Map<String, Object> traits) {
    if (!enabled || distinctId == null || distinctId.isBlank()) {
      return;
    }
    capture(distinctId, "$identify", Map.of(
        "distinct_id", distinctId,
        "$set", traits == null ? Map.of() : traits));
  }

  @Override
  public void flush() {
    if (!enabled || posthog == null) {
      return;
    }
    try {
      posthog.flush();
    } catch (final Exception e) {
      logger.log(Level.WARNING, "PostHog flush failed", e);
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void shutdown() {
    if (!enabled || posthog == null) {
      return;
    }
    try {
      posthog.flush();
      posthog.close();
    } catch (final Exception e) {
      logger.log(Level.WARNING, "PostHog close failed", e);
    }
  }
}
