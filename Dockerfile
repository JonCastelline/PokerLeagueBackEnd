# Use a base image with Java 17 (or your project's Java version)
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Gradle wrapper files
COPY gradlew .
RUN chmod +x gradlew
COPY gradle gradle

# Copy the build.gradle and settings.gradle files
COPY build.gradle .
COPY settings.gradle .

# Copy the source code
COPY src src

# Build the application
# Use --no-daemon to prevent Gradle from running a daemon, which is better for Docker builds
# Use -x test to skip tests during the Docker build, as tests should be run in CI
RUN ./gradlew bootJar -x test

# Expose the port your Spring Boot application runs on (default is 8080)
EXPOSE 8080

# Set the entry point to run the JAR file
# Activate the 'prod' profile when running the application
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "build/libs/pokerleaguebackend-0.0.1-SNAPSHOT.jar"]
