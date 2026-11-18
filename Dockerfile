# Use a lightweight Java 17 runtime
FROM eclipse-temurin:17-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled JAR file from your target folder to the container
COPY target/bank-sample-app-0.0.1-SNAPSHOT.jar app.jar

# Expose the secure port
EXPOSE 8443

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]