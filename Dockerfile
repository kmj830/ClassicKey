# Multi-stage Dockerfile for ClassicKey Spring Boot on Google Cloud Run
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy gradle wrapper and configuration files first for efficient caching
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle

# Grant execution permission
RUN chmod +x gradlew

# Pre-fetch dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and table definition
COPY src src
COPY table.txt ./

# Build bootJar without running tests (tests already verified in CI)
RUN ./gradlew bootJar --no-daemon -x test

# Runtime Stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Google Cloud Run injects the PORT environment variable (defaults to 8080)
ENV PORT=8080
EXPOSE 8080

# Copy executable jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar
COPY table.txt ./

# Optimize JVM memory usage for Cloud Run container limits
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT} -jar app.jar"]
