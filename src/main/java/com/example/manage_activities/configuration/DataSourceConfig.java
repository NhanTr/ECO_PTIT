package com.example.manage_activities.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.logging.Logger;

/**
 * Configuration class for multi-datasource setup with automatic failover
 * Primary: EC2 MySQL
 * Fallback: Localhost MySQL
 */
@Configuration
public class DataSourceConfig {
    private static final Logger LOGGER = Logger.getLogger(DataSourceConfig.class.getName());

    @Value("${spring.datasource.url}")
    private String primaryUrl;

    @Value("${spring.datasource.username}")
    private String primaryUsername;

    @Value("${spring.datasource.password}")
    private String primaryPassword;

    @Value("${spring.datasource-fallback.url}")
    private String fallbackUrl;

    @Value("${spring.datasource-fallback.username}")
    private String fallbackUsername;

    @Value("${spring.datasource-fallback.password}")
    private String fallbackPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        // Try primary datasource first
        try {
            LOGGER.info("🔄 Testing Primary DataSource (EC2) connection...");
            if (testConnectionBeforeCreating(primaryUrl, primaryUsername, primaryPassword)) {
                LOGGER.info("✅ Primary DataSource (EC2) is available, using it");
                return createDataSource(primaryUrl, primaryUsername, primaryPassword);
            }
        } catch (Exception e) {
            LOGGER.warning("❌ Primary DataSource (EC2) failed: " + e.getMessage());
        }

        // Try fallback datasource
        try {
            LOGGER.info("🔄 Testing Fallback DataSource (Localhost) connection...");
            if (testConnectionBeforeCreating(fallbackUrl, fallbackUsername, fallbackPassword)) {
                LOGGER.info("✅ Fallback DataSource (Localhost) is available, switching to it");
                return createDataSource(fallbackUrl, fallbackUsername, fallbackPassword);
            }
        } catch (Exception e) {
            LOGGER.warning("❌ Fallback DataSource (Localhost) failed: " + e.getMessage());
        }

        LOGGER.severe("❌ Both datasources failed! Creating primary datasource anyway (will likely fail)");
        return createDataSource(primaryUrl, primaryUsername, primaryPassword);
    }

    /**
     * Test connection BEFORE creating HikariDataSource
     */
    private boolean testConnectionBeforeCreating(String url, String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, username, password);
            boolean valid = conn.isValid(2);
            conn.close();
            return valid;
        } catch (Exception e) {
            LOGGER.warning("Connection test failed for " + url + ": " + e.getMessage());
            return false;
        }
    }

    private DataSource createDataSource(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000);
        return new HikariDataSource(config);
    }
}
