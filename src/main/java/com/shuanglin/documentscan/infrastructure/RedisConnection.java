package com.shuanglin.documentscan.infrastructure;

import com.shuanglin.documentscan.config.ConfigManager;
import io.avaje.inject.Component;
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Redis connection manager.
 *
 * <p>Manages connection to Redis for caching and session storage.</p>
 *
 * @author Shuanglin
 * @since 1.0.0
 */
@Component
public class RedisConnection {

    private static final Logger log = LoggerFactory.getLogger(RedisConnection.class);

    private final ConfigManager.RedisConfig config;
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;

    public RedisConnection(ConfigManager configManager) {
        this.config = configManager.getRedisConfig();
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing Redis connection to {}:{}", config.host(), config.port());
        try {
            RedisURI.Builder builder = RedisURI.builder()
                    .withHost(config.host())
                    .withPort(config.port())
                    .withDatabase(config.database())
                    .withTimeout(java.time.Duration.ofMillis(config.timeout()));

            if (config.password() != null && !config.password().isEmpty()) {
                builder.withAuthentication("default", config.password());
            }

            RedisURI redisUri = builder.build();
            redisClient = RedisClient.create(redisUri);
            connection = redisClient.connect();

            // Verify connection
            RedisCommands<String, String> sync = connection.sync();
            String response = sync.ping();
            if ("PONG".equals(response)) {
                log.info("✓ Redis connection established successfully");
            } else {
                log.error("✗ Redis ping failed");
                throw new RuntimeException("Redis ping failed");
            }
        } catch (Exception e) {
            log.error("✗ Failed to connect to Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Redis connection failed", e);
        }
    }

    @PreDestroy
    public void close() {
        if (connection != null) {
            log.info("Closing Redis connection");
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    /**
     * Get the Redis connection.
     */
    public StatefulRedisConnection<String, String> getConnection() {
        return connection;
    }

    /**
     * Get sync commands interface.
     */
    public RedisCommands<String, String> getCommands() {
        return connection.sync();
    }

    /**
     * Check if connection is healthy.
     */
    public boolean isHealthy() {
        try {
            if (connection == null || !connection.isOpen()) {
                return false;
            }
            String response = connection.sync().ping();
            return "PONG".equals(response);
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}
