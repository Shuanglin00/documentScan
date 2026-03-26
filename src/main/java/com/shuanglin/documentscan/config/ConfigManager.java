package com.shuanglin.documentscan.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.avaje.inject.Component;
import io.milvus.v2.client.ConnectConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration manager for loading application configuration.
 * Uses Typesafe Config (HOCON) format with environment variable overrides.
 *
 * <p>Configuration precedence (highest to lowest):
 * <ol>
 *   <li>Environment variables</li>
 *   <li>System properties</li>
 *   <li>application.conf in working directory</li>
 *   <li>application.conf in classpath</li>
 *   <li>reference.conf</li>
 * </ol>
 *
 * @author Shuanglin
 * @since 1.0.0
 */
@Component
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    private final Config config;

    public ConfigManager() {
        this.config = loadConfig();
        log.info("Configuration loaded successfully. Environment: {}",
                config.getString("app.environment"));
    }

    /**
     * Load configuration with environment variable overrides.
     */
    private Config loadConfig() {
        Config loadedConfig = ConfigFactory.load();

        // Check for environment-specific config file
        String env = System.getenv("APP_ENV");
        if (env != null && !env.isEmpty()) {
            try {
                Config envConfig = ConfigFactory.load("application-" + env);
                loadedConfig = envConfig.withFallback(loadedConfig);
                log.info("Loaded environment-specific config for: {}", env);
            } catch (Exception e) {
                log.warn("No environment-specific config found for: {}", env);
            }
        }

        return loadedConfig.resolve();
    }

    /**
     * Get the root configuration.
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Get configuration at a specific path.
     */
    public Config getConfig(String path) {
        return config.getConfig(path);
    }

    /**
     * Get string value at path.
     */
    public String getString(String path) {
        return config.getString(path);
    }

    /**
     * Get string value at path with default.
     */
    public String getString(String path, String defaultValue) {
        if (config.hasPath(path)) {
            return config.getString(path);
        }
        return defaultValue;
    }

    /**
     * Get int value at path.
     */
    public int getInt(String path) {
        return config.getInt(path);
    }

    /**
     * Get int value at path with default.
     */
    public int getInt(String path, int defaultValue) {
        if (config.hasPath(path)) {
            return config.getInt(path);
        }
        return defaultValue;
    }

    /**
     * Get boolean value at path.
     */
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    /**
     * Get boolean value at path with default.
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        if (config.hasPath(path)) {
            return config.getBoolean(path);
        }
        return defaultValue;
    }

    // ==================== Convenience Methods ====================

    /**
     * Get MongoDB configuration.
     */
    public MongoDbConfig getMongoDbConfig() {
        return new MongoDbConfig(
                getString("mongodb.uri"),
                getString("mongodb.database"),
                getBoolean("mongodb.auto-index-creation")
        );
    }

    /**
     * Get Milvus configuration.
     */
    public ConnectConfig getMilvusConfig () {
        return ConnectConfig.builder()
                .uri(getString("milvus.host") + ":" + getString("milvus.port"))
                .dbName(getString("milvus.db-name"))
                .build();
    }

    /**
     * Get Redis configuration.
     */
    public RedisConfig getRedisConfig() {
        return new RedisConfig(
                getString("redis.host"),
                getInt("redis.port"),
                getString("redis.password", null),
                getInt("redis.database"),
                getInt("redis.timeout")
        );
    }

    /**
     * Get server configuration.
     */
    public ServerConfig getServerConfig() {
        return new ServerConfig(
                getString("server.host"),
                getInt("server.port")
        );
    }

    // ==================== Configuration Records ====================

    public record MongoDbConfig(String uri, String database, boolean autoIndexCreation) {}
    public record MilvusConfig(String host, int port, String dbName, String collectionName, boolean enabled) {}
    public record RedisConfig(String host, int port, String password, int database, int timeout) {}
    public record ServerConfig(String host, int port) {}
}
