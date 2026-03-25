package com.shuanglin.documentscan.infrastructure;

import com.shuanglin.documentscan.config.ConfigManager;
import io.avaje.inject.Component;
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

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

    private final ConfigManager.MilvusConfig config;
    private MilvusServiceClient milvusClient;

    public MilvusConnection(ConfigManager configManager) {
        this.config = configManager.getMilvusConfig();
    }

    @PostConstruct
    public void initialize() {
        if (!config.enabled()) {
            log.info("Milvus is disabled in configuration");
            return;
        }

        log.info("Initializing Milvus connection to {}:{}", config.host(), config.port());
        try {
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(config.host())
                    .withPort(config.port())
                    .withDatabaseName(config.dbName())
                    .withConnectTimeout(10, TimeUnit.SECONDS)
                    .withKeepAliveTime(30, TimeUnit.SECONDS)
                    .build();

            milvusClient = new MilvusServiceClient(connectParam);

            // Verify connection
            R<io.milvus.grpc.CheckHealthResponse> response = milvusClient.checkHealth();
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("✓ Milvus connection established successfully");
            } else {
                log.error("✗ Milvus health check failed: {}", response.getMessage());
                throw new RuntimeException("Milvus health check failed: " + response.getMessage());
            }
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
    public MilvusServiceClient getClient() {
        return milvusClient;
    }

    /**
     * Check if connection is healthy.
     */
    public boolean isHealthy() {
        if (milvusClient == null || !config.enabled()) {
            return false;
        }
        try {
            R<io.milvus.grpc.CheckHealthResponse> response = milvusClient.checkHealth();
            return response.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.warn("Milvus health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if Milvus is enabled.
     */
    public boolean isEnabled() {
        return config.enabled();
    }
}
