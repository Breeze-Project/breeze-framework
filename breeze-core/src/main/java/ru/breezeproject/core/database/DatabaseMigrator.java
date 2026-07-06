package ru.breezeproject.core.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseMigrator {

    private static final Pattern MIGRATION_NAME = Pattern.compile("^V(\\d+)__(.+)\\.sql$");

    private final Logger logger;

    public DatabaseMigrator(Logger logger) {
        this.logger = logger;
    }

    public void migrate(DataSource dataSource, DatabaseVendor vendor) {
        List<JavaMigration> migrations;
        try {
            migrations = discoverMigrations(vendor);
        } catch (IOException e) {
            logger.severe("Failed to discover migration files for vendor " + vendor + ": " + e.getMessage());
            throw new IllegalStateException("Could not discover migration files", e);
        }

        if (migrations.isEmpty()) {
            logger.warning("No migration files found for vendor " + vendor
                    + " under migrations/" + vendor.name().toLowerCase() + "/ — check the plugin jar contents.");
        }

        Flyway flyway = Flyway.configure(getClass().getClassLoader())
                .dataSource(dataSource)
                .javaMigrations(migrations.toArray(new JavaMigration[0]))
                .baselineOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        if (result.success) {
            if (result.migrationsExecuted == 0) {
                logger.info("Database schema up to date (no pending migrations, vendor=" + vendor + ").");
            } else {
                logger.info("Applied " + result.migrationsExecuted + " migration(s) for " + vendor
                        + ", now at version " + result.targetSchemaVersion + ".");
            }
        } else {
            logger.severe("Database migration failed for vendor " + vendor + ". See Flyway output above for details.");
            throw new IllegalStateException("Flyway migration did not complete successfully");
        }
    }

    private List<JavaMigration> discoverMigrations(DatabaseVendor vendor) throws IOException {
        String root = "migrations/" + vendor.name().toLowerCase();
        List<JavaMigration> result = new ArrayList<>();

        ClassLoader classLoader = getClass().getClassLoader();
        URL dirUrl = classLoader.getResource(root);
        if (dirUrl == null) {
            return result;
        }

        List<String[]> found = new ArrayList<>();

        if ("jar".equals(dirUrl.getProtocol())) {
            JarURLConnection connection = (JarURLConnection) dirUrl.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(root + "/") && entryName.endsWith(".sql")) {
                        found.add(new String[]{entryName.substring((root + "/").length()), entryName});
                    }
                }
            }
        } else {
            java.io.File dir = new java.io.File(dirUrl.getFile());
            java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
            if (files != null) {
                for (java.io.File file : files) {
                    found.add(new String[]{file.getName(), root + "/" + file.getName()});
                }
            }
        }

        for (String[] entry : found) {
            String fileName = entry[0];
            String resourcePath = entry[1];
            Matcher matcher = MIGRATION_NAME.matcher(fileName);
            if (matcher.matches()) {
                int version = Integer.parseInt(matcher.group(1));
                String description = matcher.group(2);
                result.add(new SqlFileJavaMigration(version, description, resourcePath, classLoader));
            } else {
                logger.warning("Ignoring file in " + root + "/ that doesn't match V<n>__name.sql: " + fileName);
            }
        }

        return result;
    }
}
