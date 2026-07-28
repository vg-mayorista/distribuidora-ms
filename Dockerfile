# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy maven wrapper and pom.xml first for caching dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build final jar
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Production stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root app user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
