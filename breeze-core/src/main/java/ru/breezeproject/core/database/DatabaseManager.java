package ru.breezeproject.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseManager {

    private final Logger logger;
    private HikariDataSource dataSource;
    private DatabaseVendor vendor;

    public DatabaseManager(Logger logger) {
        this.logger = logger;
    }

    public void connect(FileConfiguration config) {
        this.vendor = DatabaseVendor.fromConfigValue(config.getString("database.type"), logger);

        HikariConfig hikariConfig = new HikariConfig();

        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", vendor.defaultPort());
        String name = config.getString("database.name", "breezecore");
        String user = config.getString("database.user", "root");
        String password = config.getString("database.password", "");
        int poolSize = config.getInt("database.pool-size", 10);

        hikariConfig.setDriverClassName(vendor.driverClassName());
        hikariConfig.setJdbcUrl(vendor.buildJdbcUrl(host, port, name));
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setPoolName("BreezeCore-Pool");

        hikariConfig.setConnectionTimeout(10_000);
        hikariConfig.setMaxLifetime(1_800_000);
        hikariConfig.setLeakDetectionThreshold(15_000);

        this.dataSource = new HikariDataSource(hikariConfig);
        logger.info("Connected to " + vendor + " database '" + name + "' at " + host + ":" + port
                + " (pool size " + poolSize + ")");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager.connect() was not called before getConnection()");
        }
        return dataSource.getConnection();
    }

    public HikariDataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager.connect() was not called before getDataSource()");
        }
        return dataSource;
    }

    public DatabaseVendor getVendor() {
        if (vendor == null) {
            throw new IllegalStateException("DatabaseManager.connect() was not called before getVendor()");
        }
        return vendor;
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }
}
