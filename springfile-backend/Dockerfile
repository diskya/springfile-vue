# Stage 1: Build the application using Maven
FROM maven:3.8-openjdk-17 AS builder

# Set the working directory
WORKDIR /build

# Copy the Maven project definition
COPY pom.xml .

# Download dependencies (this layer is cached if pom.xml doesn't change)
# Using dependency:go-offline is faster for subsequent builds if dependencies haven't changed
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Package the application, skipping tests
# This creates the JAR file in /build/target/
RUN mvn package -DskipTests -B

# Stage 2: Create the final runtime image
FROM eclipse-temurin:17-jre-jammy

# Set the working directory inside the container
WORKDIR /app

# Copy the executable JAR file from the builder stage to the final image
COPY --from=builder /build/target/springfile-0.0.1-SNAPSHOT.jar app.jar

# Make port 8080 available to the world outside this container
# Cloud Run uses the PORT environment variable, which the app reads from application.properties
EXPOSE 8080

# Run the JAR file when the container launches
# Spring Boot will listen on the port defined by the PORT env var (defaulting to 8080)
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
