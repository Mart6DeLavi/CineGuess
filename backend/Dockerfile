FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle /workspace/settings.gradle
COPY backend/build.gradle /workspace/backend/build.gradle
COPY backend/src /workspace/backend/src
RUN gradle :backend:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/backend/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
