package ru.breezeproject.core.database;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseVendorTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @Test
    void defaultsToMysqlWhenUnset() {
        assertEquals(DatabaseVendor.MYSQL, DatabaseVendor.fromConfigValue(null, LOGGER));
        assertEquals(DatabaseVendor.MYSQL, DatabaseVendor.fromConfigValue("", LOGGER));
    }

    @Test
    void parsesPostgresqlCaseInsensitively() {
        assertEquals(DatabaseVendor.POSTGRESQL, DatabaseVendor.fromConfigValue("postgresql", LOGGER));
        assertEquals(DatabaseVendor.POSTGRESQL, DatabaseVendor.fromConfigValue("PostgreSQL", LOGGER));
    }

    @Test
    void unknownValueFallsBackToMysql() {
        assertEquals(DatabaseVendor.MYSQL, DatabaseVendor.fromConfigValue("oracle", LOGGER));
    }

    @Test
    void eachVendorHasItsOwnMigrationsLocation() {
        assertEquals("classpath:migrations/mysql/", DatabaseVendor.MYSQL.migrationsLocation());
        assertEquals("classpath:migrations/postgresql/", DatabaseVendor.POSTGRESQL.migrationsLocation());
    }

    @Test
    void eachVendorHasItsOwnRollbackRoot() {
        assertEquals("rollback/mysql", DatabaseVendor.MYSQL.rollbackResourceRoot());
        assertEquals("rollback/postgresql", DatabaseVendor.POSTGRESQL.rollbackResourceRoot());
    }

    @Test
    void jdbcUrlsUseCorrectDialectPrefix() {
        assertEquals("jdbc:mysql://localhost:3306/breezecore?useSSL=false&autoReconnect=true&characterEncoding=utf8",
                DatabaseVendor.MYSQL.buildJdbcUrl("localhost", 3306, "breezecore"));
        assertEquals("jdbc:postgresql://localhost:5432/breezecore",
                DatabaseVendor.POSTGRESQL.buildJdbcUrl("localhost", 5432, "breezecore"));
    }
}
