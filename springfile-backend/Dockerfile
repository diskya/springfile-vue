# Use an official OpenJDK 17 runtime as a parent image
FROM eclipse-temurin:17-jre-jammy

# Set the working directory inside the container
WORKDIR /app

# Argument to specify the JAR file path (can be overridden during build)
ARG JAR_FILE=target/springfile-0.0.1-SNAPSHOT.jar

# Copy the executable JAR file from the build context to the container
COPY ${JAR_FILE} app.jar

# Make port 8080 available to the world outside this container
# Cloud Run uses the PORT environment variable, which the app reads from application.properties
# Exposing 8080 is standard practice, Cloud Run maps $PORT to this.
EXPOSE 8080

# Run the JAR file when the container launches
# Spring Boot will listen on the port defined by the PORT env var (defaulting to 8080)
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
