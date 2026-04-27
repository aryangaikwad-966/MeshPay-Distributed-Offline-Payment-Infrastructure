# Multi-stage Docker build for MeshPay
# Stage 1: Build

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and project files
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Give execute permission to Maven wrapper
RUN chmod +x mvnw

# Build the application (skip tests for faster builds in CI)
COPY src ./src
RUN ./mvnw clean package -DskipTests -e

# Stage 2: Runtime

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1000 meshpay && \
    adduser -D -u 1000 -G meshpay meshpay

# Copy built JAR from builder stage
COPY --from=builder /app/target/meshpay-*.jar app.jar

# Change ownership to non-root user
RUN chown -R meshpay:meshpay /app

# Switch to non-root user
USER meshpay

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# Expose port
EXPOSE 8080

# JVM Optimization flags
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+UseStringDeduplication"

# Enable production profile by default
ENV SPRING_PROFILES_ACTIVE=production

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
