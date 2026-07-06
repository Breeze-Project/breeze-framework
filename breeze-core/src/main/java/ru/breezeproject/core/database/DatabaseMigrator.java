package ru.breezeproject.core.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;
import java.util.logging.Logger;

public class DatabaseMigrator {
  private final Logger logger;

  public DatabaseMigrator(Logger logger) {
    this.logger = logger;
  }

  public void migrate(DataSource dataSource, DatabaseVendor vendor) {
    Flyway flyway = Flyway.configure(getClass().getClassLoader())
        .dataSource(dataSource)
        .locations(vendor.migrationsLocation())
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
      throw new IllegalStateException("Flyway migration did not complete successfully");
    }
  }
}
