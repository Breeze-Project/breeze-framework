package ru.breezeproject.core.database;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

public class RollbackRunner {
  private record DownScript(int version, String name, String resourcePath) {
  }

  private static final Pattern DOWN_SCRIPT_NAME = Pattern.compile("^V(\\d+)__(.+)\\.down\\.sql$");

  private final Logger logger;

  public RollbackRunner(final Logger logger) {
    this.logger = logger;
  }

  public void rollbackTo(final DataSource dataSource, final DatabaseVendor vendor, final int targetVersion) {
    try (Connection connection = dataSource.getConnection()) {
      final int currentVersion = readCurrentFlywayVersion(connection, vendor);
      final List<DownScript> allDiscovered = discoverDownScripts(vendor);

      for (int v = targetVersion + 1; v <= currentVersion; v++) {
        final int version = v;
        if (allDiscovered.stream().noneMatch(s -> s.version() == version)) {
          throw new IllegalStateException("Refusing rollback: no down script found for V" + version + " (vendor="
              + vendor + "). No changes were made.");
        }
      }

      final List<DownScript> toRun = allDiscovered.stream()
          .filter(s -> s.version() > targetVersion && s.version() <= currentVersion)
          .sorted(Comparator.comparingInt(DownScript::version).reversed())
          .toList();

      for (final DownScript script : toRun) {
        runScript(connection, script);
        removeFromFlywayHistory(connection, script.version());
        logger.info("Rolled back migration V" + script.version() + " (" + script.name()
            + ") for vendor " + vendor);
      }
    } catch (SQLException | IOException e) {
      throw new IllegalStateException("Failed to roll back database migrations", e);
    }
  }

  private int readCurrentFlywayVersion(final Connection connection, final DatabaseVendor vendor) throws SQLException {
    final String castType = vendor == DatabaseVendor.MYSQL ? "UNSIGNED" : "INTEGER";
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(
            "SELECT COALESCE(MAX(CAST(version AS " + castType + ")), 0) AS v FROM flyway_schema_history "
                + "WHERE success = true")) {
      return rs.next() ? rs.getInt("v") : 0;
    }
  }

  private void removeFromFlywayHistory(final Connection connection, final int version) throws SQLException {
    try (var preparedStatement = connection.prepareStatement(
        "DELETE FROM flyway_schema_history WHERE version = ?")) {
      preparedStatement.setInt(1, version);
      preparedStatement.executeUpdate();
    }
  }

  private void runScript(final Connection connection, final DownScript script) throws SQLException, IOException {
    final String sql = readResource(script.resourcePath());
    connection.setAutoCommit(false);
    try (Statement statement = connection.createStatement()) {
      for (final String rawStatement : sql.split(";")) {
        final String trimmed = rawStatement.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("--")) {
          continue;
        }
        statement.execute(trimmed);
      }
      connection.commit();
    } catch (final SQLException e) {
      connection.rollback();
      throw e;
    } finally {
      connection.setAutoCommit(true);
    }
  }

  private List<DownScript> discoverDownScripts(final DatabaseVendor vendor) throws IOException {
    final List<DownScript> result = new ArrayList<>();
    final String root = vendor.rollbackResourceRoot();
    final URL dirUrl = getClass().getClassLoader().getResource(root);
    if (dirUrl == null) {
      return result;
    }

    if ("jar".equals(dirUrl.getProtocol())) {
      final JarURLConnection connection = (JarURLConnection) dirUrl.openConnection();
      try (JarFile jarFile = connection.getJarFile()) {
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          final JarEntry entry = entries.nextElement();
          final String entryName = entry.getName();
          if (entryName.startsWith(root + "/") && entryName.endsWith(".sql")) {
            addIfMatches(result, entryName.substring((root + "/").length()), entryName);
          }
        }
      }
    } else {
      final java.io.File dir = new java.io.File(dirUrl.getFile());
      final java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
      if (files != null) {
        for (final java.io.File file : files) {
          addIfMatches(result, file.getName(), root + "/" + file.getName());
        }
      }
    }
    return result;
  }

  private void addIfMatches(final List<DownScript> result, final String fileName, final String resourcePath) {
    final Matcher matcher = DOWN_SCRIPT_NAME.matcher(fileName);
    if (matcher.matches()) {
      result.add(new DownScript(Integer.parseInt(matcher.group(1)), matcher.group(2), resourcePath));
    } else {
      logger.warning("Ignoring file in rollback/ that doesn't match V<n>__name.down.sql: " + fileName);
    }
  }

  private String readResource(final String path) throws IOException {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new IOException("Rollback resource not found: " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
