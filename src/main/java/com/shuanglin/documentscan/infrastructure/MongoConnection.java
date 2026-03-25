package com.shuanglin.documentscan.infrastructure;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.shuanglin.documentscan.config.ConfigManager;
import io.avaje.inject.Component;
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MongoDB connection factory and manager.
 *
 * <p>Provides thread-safe MongoDB client and database access.</p>
 *
 * @author Shuanglin
 * @since 1.0.0
 */
@Component
public class MongoConnection {

    private static final Logger log = LoggerFactory.getLogger(MongoConnection.class);

    private final ConfigManager.MongoDbConfig config;
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoConnection(ConfigManager configManager) {
        this.config = configManager.getMongoDbConfig();
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing MongoDB connection to: {}", config.database());
        try {
            mongoClient = MongoClients.create(config.uri());
            database = mongoClient.getDatabase(config.database());

            // Verify connection
            database.runCommand(new org.bson.Document("ping", 1));
            log.info("✓ MongoDB connection established successfully");
        } catch (Exception e) {
            log.error("✗ Failed to connect to MongoDB: {}", e.getMessage(), e);
            throw new RuntimeException("MongoDB connection failed", e);
        }
    }

    @PreDestroy
    public void close() {
        if (mongoClient != null) {
            log.info("Closing MongoDB connection");
            mongoClient.close();
        }
    }

    /**
     * Get the MongoDB database instance.
     */
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * Get the MongoDB client instance.
     */
    public MongoClient getClient() {
        return mongoClient;
    }

    /**
     * Check if connection is healthy.
     */
    public boolean isHealthy() {
        try {
            database.runCommand(new org.bson.Document("ping", 1));
            return true;
        } catch (Exception e) {
            log.warn("MongoDB health check failed: {}", e.getMessage());
            return false;
        }
    }
}
