# Use a lightweight OpenJDK runtime image
FROM openjdk:22-slim

# Set working directory inside container
WORKDIR /app

# Copy the source code and web pages into the container
COPY src /app/src
COPY web /app/web

# Create class destination directory and compile source files
RUN mkdir bin && javac -d bin src/com/globalexchange/service/ExchangeRateService.java src/com/globalexchange/server/ExchangeServer.java

# Expose default port (Render/Koyeb inject their own, handled dynamically by system environment variables)
EXPOSE 8080

# Command to execute the application
CMD ["java", "-cp", "bin", "com.globalexchange.server.ExchangeServer"]
