FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy JAR only - NO OpenTelemetry agent needed (using StatsD)
COPY --from=build /app/target/statsd-demo-1.0.0.jar app.jar

EXPOSE 8080

# Run without OpenTelemetry agent - pure StatsD via Micrometer
ENTRYPOINT ["java", "-jar", "app.jar"]
