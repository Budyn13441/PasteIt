# Build stage
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy gradle files
COPY gradle ./gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .

# Copy source code
COPY src ./src

# Build the application
RUN ./gradlew clean build -x test

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create a volume mount point for virtual filesystem storage
RUN mkdir -p /root/stashes

# Copy the built jar from builder stage
COPY --from=builder /build/build/libs/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
