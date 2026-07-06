package ru.breezeproject.core.database;

import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;

class SqlFileJavaMigration implements JavaMigration {

    private final int version;
    private final String description;
    private final String resourcePath;
    private final ClassLoader classLoader;

    SqlFileJavaMigration(int version, String description, String resourcePath, ClassLoader classLoader) {
        this.version = version;
        this.description = description;
        this.resourcePath = resourcePath;
        this.classLoader = classLoader;
    }

    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion(String.valueOf(version));
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Integer getChecksum() {
        return null;
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    @Override
    public void migrate(Context context) throws Exception {
        String sql = readResource();
        try (Statement statement = context.getConnection().createStatement()) {
            for (String rawStatement : sql.split(";")) {
                String trimmed = rawStatement.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                statement.execute(trimmed);
            }
        }
    }

    private String readResource() throws IOException {
        try (InputStream in = classLoader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Migration resource not found: " + resourcePath);
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
}
