# Stage 1: Build the application
FROM amazoncorretto:17 AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew bootWar --no-daemon

# Stage 2: Run the application
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.war app.war

# Create uploads directory
RUN mkdir -p /app/uploads

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.war"]
