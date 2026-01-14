# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml first to leverage Docker cache
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health check and DejaVu fonts
RUN apk add --no-cache curl fontconfig ttf-dejavu

# Create non-root user
RUN addgroup -g 1001 jaspergroup && \
    adduser -D -u 1001 -G jaspergroup jasperuser

# Set working directory
WORKDIR /app


# Copy and import certificate
COPY src/main/resources/verderp.crt /tmp/verderp.crt
RUN keytool -import -trustcacerts -cacerts -storepass changeit -noprompt -alias verderp -file /tmp/verderp.crt && \
    rm /tmp/verderp.crt

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy font configuration
COPY --from=builder /app/src/main/resources/fonts-system.xml /app/fonts-system.xml

# Create directory for reports
RUN mkdir -p /app/relatorios && chown -R jasperuser:jaspergroup /app

# Switch to non-root user
USER jasperuser

# Environment variables
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SERVER_PORT=8013 \
    SPRING_PROFILES_ACTIVE=docker

# Expose port
EXPOSE 8013

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8013/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]