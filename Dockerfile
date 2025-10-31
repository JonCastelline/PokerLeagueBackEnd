# Stage 1: Build the application
FROM gradle:jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]