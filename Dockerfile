# Stage 1: Compile the application using the JDK image
FROM eclipse-temurin:21 AS builder
WORKDIR /app
COPY src /app/src
RUN mkdir bin && javac -d bin src/com/globalexchange/service/ExchangeRateService.java src/com/globalexchange/server/ExchangeServer.java

# Stage 2: Run the application using the lightweight JRE image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/bin /app/bin
COPY web /app/web

# Expose default port
EXPOSE 8080

# Command to execute the application
CMD ["java", "-cp", "bin", "com.globalexchange.server.ExchangeServer"]
