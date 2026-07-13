package ru.breezeproject.api;

import java.util.Optional;

public final class BreezeApiVersion {
  private record Version(int major, int minor, int patch) {
  }

  public static final String CURRENT = "1.4.0";

  public static boolean isCompatible(final String moduleApiVersion) {
    return parse(moduleApiVersion)
        .map(declared -> isCompatible(declared, parse(CURRENT).orElseThrow()))
        .orElse(false);
  }

  private static boolean isCompatible(final Version declared, final Version current) {
    if (declared.major() != current.major()) {
      return false;
    }
    if (declared.minor() != current.minor()) {
      return declared.minor() < current.minor();
    }
    return declared.patch() <= current.patch();
  }

  private static Optional<Version> parse(final String version) {
    return Optional.ofNullable(version)
        .filter(v -> !v.isBlank())
        .map(v -> v.trim().split("\\."))
        .filter(parts -> parts.length == 3)
        .flatMap(parts -> {
          try {
            return Optional.of(new Version(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])));
          } catch (final NumberFormatException e) {
            return Optional.empty();
          }
        });
  }

  private BreezeApiVersion() {
  }
}
