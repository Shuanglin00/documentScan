package com.shuanglin.documentscan;

import io.avaje.inject.BeanScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DocumentScan Application Entry Point.
 *
 * <p>Initializes dependency injection container and starts all services.</p>
 *
 * @author Shuanglin
 * @since 1.0.0
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        log.info("========================================");
        log.info("Starting DocumentScan Application");
        log.info("========================================");

        try (BeanScope beanScope = BeanScope.builder().build()) {
            log.info("✓ Dependency injection container initialized");

            // Services are auto-started via @PostConstruct
            // Keep main thread alive
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.info("Application interrupted, shutting down...");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to start application: {}", e.getMessage(), e);
            System.exit(1);
        }

        log.info("========================================");
        log.info("DocumentScan Application stopped");
        log.info("========================================");
    }
}
