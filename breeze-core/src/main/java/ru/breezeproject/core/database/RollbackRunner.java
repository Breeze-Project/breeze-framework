package ru.breezeproject.core.database;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class RollbackRunner {

    private static final Pattern DOWN_SCRIPT_NAME = Pattern.compile("^V(\\d+)__(.+)\\.down\\.sql$");

    private final Logger logger;

    public RollbackRunner(Logger logger) {
        this.logger = logger;
    }

    public boolean rollbackTo(DataSource dataSource, DatabaseVendor vendor, int targetVersion) {
        try (Connection connection = dataSource.getConnection()) {
            int currentVersion = readCurrentFlywayVersion(connection, vendor);
            List<DownScript> allDiscovered = discoverDownScripts(vendor);

            for (int v = targetVersion + 1; v <= currentVersion; v++) {
                int version = v;
                if (allDiscovered.stream().noneMatch(s -> s.version() == version)) {
                    logger.severe("Refusing rollback: no down script found for V" + version
                            + " (vendor=" + vendor + "). No changes were made.");
                    return false;
                }
            }

            List<DownScript> toRun = allDiscovered.stream()
                    .filter(s -> s.version() > targetVersion && s.version() <= currentVersion)
                    .sorted(Comparator.comparingInt(DownScript::version).reversed())
                    .toList();

            for (DownScript script : toRun) {
                runScript(connection, script);
                removeFromFlywayHistory(connection, script.version());
                logger.info("Rolled back migration V" + script.version() + " (" + script.name()
                        + ") for vendor " + vendor);
            }
            return true;

        } catch (SQLException | IOException e) {
            logger.severe("Failed to roll back database migrations: " + e.getMessage());
            return false;
        }
    }

    private int readCurrentFlywayVersion(Connection connection, DatabaseVendor vendor) throws SQLException {
        String castType = vendor == DatabaseVendor.MYSQL ? "UNSIGNED" : "INTEGER";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COALESCE(MAX(CAST(version AS " + castType + ")), 0) AS v FROM flyway_schema_history "
                             + "WHERE success = true")) {
            return rs.next() ? rs.getInt("v") : 0;
        }
    }

    private void removeFromFlywayHistory(Connection connection, int version) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM flyway_schema_history WHERE version = '" + version + "'");
        }
    }

    private void runScript(Connection connection, DownScript script) throws SQLException, IOException {
        String sql = readResource(script.resourcePath());
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            for (String rawStatement : sql.split(";")) {
                String trimmed = rawStatement.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                statement.execute(trimmed);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private List<DownScript> discoverDownScripts(DatabaseVendor vendor) throws IOException {
        List<DownScript> result = new ArrayList<>();
        String root = vendor.rollbackResourceRoot();
        URL dirUrl = getClass().getClassLoader().getResource(root);
        if (dirUrl == null) {
            return result;
        }

        if ("jar".equals(dirUrl.getProtocol())) {
            JarURLConnection connection = (JarURLConnection) dirUrl.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(root + "/") && entryName.endsWith(".sql")) {
                        addIfMatches(result, entryName.substring((root + "/").length()), entryName);
                    }
                }
            }
        } else {
            java.io.File dir = new java.io.File(dirUrl.getFile());
            java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
            if (files != null) {
                for (java.io.File file : files) {
                    addIfMatches(result, file.getName(), root + "/" + file.getName());
                }
            }
        }
        return result;
    }

    private void addIfMatches(List<DownScript> result, String fileName, String resourcePath) {
        Matcher matcher = DOWN_SCRIPT_NAME.matcher(fileName);
        if (matcher.matches()) {
            result.add(new DownScript(Integer.parseInt(matcher.group(1)), matcher.group(2), resourcePath));
        } else {
            logger.warning("Ignoring file in rollback/ that doesn't match V<n>__name.down.sql: " + fileName);
        }
    }

    private String readResource(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Rollback resource not found: " + path);
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }
            return builder.toString();
        }
    }

    private record DownScript(int version, String name, String resourcePath) {
    }
}
