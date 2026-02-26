FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src
RUN ./gradlew build -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN curl -L -o opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.2.0/opentelemetry-javaagent.jar
ENV JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar"
ENV OTEL_SERVICE_NAME="authpolicy-service"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_METRICS_EXPORTER="none"
ENV OTEL_LOGS_EXPORTER="none"
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://jaeger:4318"
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
