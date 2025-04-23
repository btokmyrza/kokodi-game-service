# Build stage
FROM gradle:8.6-jdk21 AS build

WORKDIR /app
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src

RUN gradle shadowJar --no-daemon

FROM amazoncorretto:21

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port if needed
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
