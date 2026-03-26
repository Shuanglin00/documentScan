package com.shuanglin.documentscan.infrastructure;

import com.shuanglin.documentscan.config.ConfigManager;
import io.avaje.inject.Component;
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.database.request.CreateDatabaseReq;
import io.milvus.v2.service.utility.response.CheckHealthResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Milvus vector database connection manager.
 *
 * <p>Manages connection to Milvus for vector storage and retrieval.</p>
 *
 * @author Shuanglin
 * @since 1.0.0
 */
@Component
public class MilvusConnection {

    private static final Logger log = LoggerFactory.getLogger(MilvusConnection.class);

    private final ConnectConfig connectConfig;

    private MilvusClientV2 milvusClient;

    public MilvusConnection(ConfigManager configManager) {
        this.connectConfig = configManager.getMilvusConfig();
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing Milvus connection to {}:{}", connectConfig.getHost(), connectConfig.getPort());
        try {
            ConnectConfig config = ConnectConfig.builder()
                    .uri(connectConfig.getUri())
                    .build();

            milvusClient = new MilvusClientV2(config);

            // Verify connection
            CheckHealthResp checkHealthResp = milvusClient.checkHealth();
            if (checkHealthResp.getIsHealthy()) {
                log.info("✓ Milvus connection established successfully");
            } else {
                log.error("✗ Milvus health check failed: {}", checkHealthResp.getReasons());
                throw new RuntimeException("Milvus health check failed: " + checkHealthResp.getReasons());
            }

            List<String> databaseNames = milvusClient.listDatabases().getDatabaseNames();
            databaseNames.stream().filter(name -> name.equals(connectConfig.getDbName())).findFirst().ifPresentOrElse(name -> {
                log.info("✓ Milvus database '{}' exists", name);
            }, () -> {
                log.info("✓ Creating Milvus database '{}'", connectConfig.getDbName());
                CreateDatabaseReq createDatabaseReq = CreateDatabaseReq.builder()
                        .databaseName(connectConfig.getDbName())
                        .build();
                milvusClient.createDatabase(createDatabaseReq);
            });
        } catch (Exception e) {
            log.error("✗ Failed to connect to Milvus: {}", e.getMessage(), e);
            throw new RuntimeException("Milvus connection failed", e);
        }
    }

    @PreDestroy
    public void close() {
        if (milvusClient != null) {
            log.info("Closing Milvus connection");
            milvusClient.close();
        }
    }

    /**
     * Get the Milvus client instance.
     */
    public MilvusClientV2 getClient () {
        return milvusClient;
    }

    /**
     * Check if connection is healthy.
     */
    public boolean isHealthy() {
        if (milvusClient == null) {
            return false;
        }
        try {
            CheckHealthResp checkHealthResp = milvusClient.checkHealth();
            return checkHealthResp.getIsHealthy();
        } catch (Exception e) {
            log.warn("Milvus health check failed: {}", e.getMessage());
            return false;
        }
    }

}
