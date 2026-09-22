# Multi-stage Dockerfile for ClassicKey Spring Boot Application
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy gradle wrapper and build files first for caching
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle

# Grant execution permission
RUN chmod +x gradlew

# Pre-fetch dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and table definition
COPY src src
COPY table.txt ./

# Build bootJar
RUN ./gradlew bootJar --no-daemon -x test

# Runtime Stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Environment variable for Render port
ENV PORT=8080
EXPOSE ${PORT}

# Copy jar from builder
COPY --from=builder /app/build/libs/*.jar app.jar
COPY table.txt ./

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
