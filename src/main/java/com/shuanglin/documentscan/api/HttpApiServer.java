package com.shuanglin.documentscan.api;

import com.shuanglin.documentscan.config.ConfigManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.avaje.inject.Component;
import io.avaje.inject.PostConstruct;
import io.avaje.inject.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Simple HTTP server for health checks and basic API endpoints.
 *
 * <p>Provides REST endpoints for system monitoring.</p>
 *
 * @author Shuanglin
 * @since 1.0.0
 */
@Component
public class HttpApiServer {

    private static final Logger log = LoggerFactory.getLogger(HttpApiServer.class);

    private final ConfigManager.ServerConfig config;
    private final HealthCheckService healthCheckService;
    private HttpServer server;

    public HttpApiServer(ConfigManager configManager, HealthCheckService healthCheckService) {
        this.config = configManager.getServerConfig();
        this.healthCheckService = healthCheckService;
    }

    @PostConstruct
    public void start() {
        log.info("Starting HTTP API server on {}:{}", config.host(), config.port());

        try {
            server = HttpServer.create(new InetSocketAddress(config.host(), config.port()), 0);
        } catch (IOException e) {
            log.error("Failed to create HTTP server: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create HTTP server", e);
        }

        // Health check endpoint
        server.createContext("/health", new HealthHandler());
        server.createContext("/ready", new ReadyHandler());
        server.createContext("/", new RootHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        log.info("✓ HTTP API server started on port {}", config.port());
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping HTTP API server");
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * Health check handler - comprehensive status check.
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            HealthCheckService.HealthStatus status = healthCheckService.checkHealth();
            int statusCode = status.healthy() ? 200 : 503;

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, statusCode, status.toJson());
        }
    }

    /**
     * Ready check handler - simple liveness check.
     */
    private class ReadyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            boolean alive = healthCheckService.isAlive();
            int statusCode = alive ? 200 : 503;
            String response = alive ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\"}";

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, statusCode, response);
        }
    }

    /**
     * Root handler - basic info.
     */
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = """
                {
                    "name": "DocumentScan API",
                    "version": "1.0.0",
                    "endpoints": [
                        "/health",
                        "/ready"
                    ]
                }
                """;

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, response);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
