package com.incident.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url:}") String jdbcUrl,
            @Value("${spring.datasource.username:}") String username,
            @Value("${spring.datasource.password:}") String password
    ) {
        HikariConfig config = new HikariConfig();

        // Check if DATABASE_URL is in Railway format (postgresql://...)
        if (databaseUrl != null && !databaseUrl.isEmpty() && databaseUrl.startsWith("postgresql://")) {
            try {
                // Parse Railway DATABASE_URL format: postgresql://user:password@host:port/database
                URI dbUri = new URI(databaseUrl);
                
                String host = dbUri.getHost();
                int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
                String path = dbUri.getPath();
                if (path != null && path.startsWith("/")) {
                    path = path.substring(1); // Remove leading slash
                }
                
                String dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + (path != null ? path : "");
                
                String dbUser = "";
                String dbPassword = "";
                
                if (dbUri.getUserInfo() != null && !dbUri.getUserInfo().isEmpty()) {
                    String[] userInfo = dbUri.getUserInfo().split(":", 2);
                    dbUser = userInfo[0];
                    if (userInfo.length > 1) {
                        dbPassword = userInfo[1];
                        // Handle URL-encoded passwords
                        try {
                            dbPassword = java.net.URLDecoder.decode(dbPassword, java.nio.charset.StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            // If decoding fails, use original password
                        }
                    }
                }
                
                config.setJdbcUrl(dbUrl);
                if (!dbUser.isEmpty()) {
                    config.setUsername(dbUser);
                }
                if (!dbPassword.isEmpty()) {
                    config.setPassword(dbPassword);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse DATABASE_URL: " + databaseUrl, e);
            }
        } 
        // Check if DATABASE_URL is already in JDBC format
        else if (databaseUrl != null && !databaseUrl.isEmpty() && databaseUrl.startsWith("jdbc:")) {
            config.setJdbcUrl(databaseUrl);
            // Use separate username/password if provided
            if (username != null && !username.isEmpty()) {
                config.setUsername(username);
            }
            if (password != null && !password.isEmpty()) {
                config.setPassword(password);
            }
        }
        // Use spring.datasource properties (from application.yml)
        else if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
            config.setJdbcUrl(jdbcUrl);
            if (username != null && !username.isEmpty()) {
                config.setUsername(username);
            }
            if (password != null && !password.isEmpty()) {
                config.setPassword(password);
            }
        }
        // Fallback: try to use DATABASE_URL as-is (might be JDBC format from env var)
        else {
            throw new RuntimeException("No valid database configuration found. Please set DATABASE_URL or spring.datasource properties.");
        }

        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}

