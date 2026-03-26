# Multi-stage build for DocumentScan Application
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine

# Install necessary tools
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 documentscan && \
    adduser -u 1001 -G documentscan -s /bin/sh -D documentscan

WORKDIR /app

# Copy the built JAR from builder stage (exclude original-*.jar)
COPY --from=builder /build/target/documentScan-*.jar app.jar

# 配置文件已打包在 JAR 中 (src/main/resources/application.conf)
# 如需覆盖配置，请使用 volume mount: -v /path/to/custom.conf:/app/config/application.conf

# Set proper ownership
RUN chown -R documentscan:documentscan /app

USER documentscan

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+OptimizeStringConcat \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=${APP_ENV}"

# Run the application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
