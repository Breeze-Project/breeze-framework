package ru.breezeproject.core.database;

import java.util.logging.Logger;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

public class DatabaseMigrator {
  private final Logger logger;

  public DatabaseMigrator(final Logger logger) {
    this.logger = logger;
  }

  public void migrate(final DataSource dataSource, final DatabaseVendor vendor) {
    final Flyway flyway = Flyway.configure(getClass().getClassLoader())
        .dataSource(dataSource)
        .locations(vendor.migrationsLocation())
        .baselineOnMigrate(true)
        .load();

    final MigrateResult result = flyway.migrate();

    if (result.success) {
      if (result.migrationsExecuted == 0) {
        logger.info("Database schema up to date (no pending migrations, vendor=" + vendor + ").");
      } else {
        logger.info("Applied " + result.migrationsExecuted + " migration(s) for " + vendor
            + ", now at version " + result.targetSchemaVersion + ".");
      }
    } else {
      throw new IllegalStateException("Flyway migration did not complete successfully");
    }
  }
}
