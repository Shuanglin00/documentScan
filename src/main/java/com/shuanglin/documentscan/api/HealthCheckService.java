package com.shuanglin.documentscan.api;

import com.shuanglin.documentscan.infrastructure.MilvusConnection;
import com.shuanglin.documentscan.infrastructure.MongoConnection;
import com.shuanglin.documentscan.infrastructure.RedisConnection;
import io.avaje.inject.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check service for monitoring system status.
 *
 * <p>Provides health status of all database connections and system components.</p>
 *
 * @author Shuanglin
 * @since 1.0.0
 */
@Component
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final MongoConnection mongoConnection;
    private final MilvusConnection milvusConnection;
    private final RedisConnection redisConnection;

    public HealthCheckService(
            MongoConnection mongoConnection,
            MilvusConnection milvusConnection,
            RedisConnection redisConnection) {
        this.mongoConnection = mongoConnection;
        this.milvusConnection = milvusConnection;
        this.redisConnection = redisConnection;
    }

    /**
     * Perform comprehensive health check.
     */
    public HealthStatus checkHealth() {
        boolean mongoHealthy = mongoConnection.isHealthy();
        boolean milvusHealthy = milvusConnection.isEnabled() ? milvusConnection.isHealthy() : true;
        boolean redisHealthy = redisConnection.isHealthy();

        boolean overallHealthy = mongoHealthy && milvusHealthy && redisHealthy;

        Map<String, Object> details = new HashMap<>();
        details.put("mongodb", mongoHealthy ? "UP" : "DOWN");
        details.put("milvus", milvusConnection.isEnabled() ? (milvusHealthy ? "UP" : "DOWN") : "DISABLED");
        details.put("redis", redisHealthy ? "UP" : "DOWN");

        return new HealthStatus(overallHealthy, details);
    }

    /**
     * Simple ping check.
     */
    public boolean isAlive() {
        return true;
    }

    /**
     * Health status record.
     */
    public record HealthStatus(boolean healthy, Map<String, Object> details) {

        public String toJson() {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"").append(healthy ? "UP" : "DOWN").append("\",");
            json.append("\"details\":{");

            boolean first = true;
            for (Map.Entry<String, Object> entry : details.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }

            json.append("}}");
            return json.toString();
        }
    }
}
