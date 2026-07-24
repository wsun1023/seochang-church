# Stage 1: Build the application using Rocky Linux
FROM rockylinux:9 AS builder
WORKDIR /app

# Install JDK 17 and findutils (for xargs) using dnf
RUN dnf install -y java-17-openjdk-devel findutils && dnf clean all

COPY . .
RUN yum install -y findutils
RUN chmod +x ./gradlew
RUN ./gradlew bootWar --no-daemon

# Stage 2: Run the application using Rocky Linux minimal
FROM rockylinux:9-minimal
WORKDIR /app

# Install JRE 17 (headless for lighter weight) using microdnf
RUN microdnf install -y java-17-openjdk-headless && microdnf clean all

COPY --from=builder /app/build/libs/*.war app.war

# Create uploads directory
RUN mkdir -p /app/uploads

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.war"]
