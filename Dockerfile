# Multi-stage build for Quarkus (JVM, fast-jar)

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

# Build a runnable Quarkus fast-jar
RUN mvn -B -DskipTests package


FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/target/quarkus-app/ /app/

EXPOSE 8080

# Quarkus listens on 0.0.0.0 via application.properties
ENTRYPOINT ["java","-jar","quarkus-run.jar"]
