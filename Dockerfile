# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootWar --no-daemon \
    && cp build/libs/*-SNAPSHOT.war /workspace/app.war

FROM eclipse-temurin:17-jre-jammy AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 church \
    && useradd --uid 10001 --gid church --no-create-home church \
    && mkdir -p /app/uploads && chown -R church:church /app
WORKDIR /app
COPY --from=build --chown=church:church /workspace/app.war /app/app.war
USER church
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl --fail --silent --output /dev/null http://127.0.0.1:8080/login || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.war"]
