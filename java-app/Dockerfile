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

# Copy JAR and OpenTelemetry agent from build stage
COPY --from=build /app/target/statsd-demo-1.0.0.jar app.jar
COPY --from=build /app/target/opentelemetry-javaagent.jar opentelemetry-javaagent.jar

EXPOSE 8080

# Run with OpenTelemetry Java agent
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
