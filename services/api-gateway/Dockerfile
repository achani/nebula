# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# Build the application jar
RUN ./gradlew clean build -x test --no-daemon

# Download the OpenTelemetry Java agent for automatic tracing
RUN apt-get update && apt-get install -y wget && \
  wget -O opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
WORKDIR /workspace

# Copy OTel agent and the spring boot fat jar
COPY --from=builder /workspace/opentelemetry-javaagent.jar .
COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar app.jar

ENV OTEL_SERVICE_NAME="api-gateway"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_METRICS_EXPORTER="none"
ENV OTEL_LOGS_EXPORTER="none"
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://jaeger:4318"
ENV JAVA_TOOL_OPTIONS="-javaagent:/workspace/opentelemetry-javaagent.jar"

ENTRYPOINT ["java", "-jar", "/workspace/app.jar"]
