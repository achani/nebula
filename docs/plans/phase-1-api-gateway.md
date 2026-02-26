# Phase 1: API Gateway + Config Service + AuthN Skeleton

Build the central configuration server and the single ingress API Gateway for the Nebula platform. The Gateway will serve as the strict security perimeter, validating JWTs via Keycloak and injecting correlation IDs before routing traffic downstream.

## User Review Required

No breaking changes or significant deviations from the architecture guidelines. Please review the proposed components below and confirm if you are ready to proceed with the execution.

## Proposed Changes

### Config Service

The Config Service will act as a centralized property source for all Spring Boot services, pulling configuration from the local `config/` directory.

#### [NEW] `services/config-service/`
- Bootstrap a Spring Boot 3 application with Java 21 and Gradle (`build.gradle`).
- Add the `spring-cloud-config-server` dependency.
- Enable `@EnableConfigServer`.
- Configure `application.yml` to read from the local `config/` directory (native profile).
- Add `Dockerfile` for containerization.
- Add OTel auto-instrumentation, Logback JSON logging, and Micrometer Prometheus metrics.

#### [NEW] `config/`
- Create default global configuration properties (`application.yml`) to be shared across services (e.g., Kafka brokers, Keycloak issuer URL, Zipkin/Jaeger endpoints).
- Create service-specific configuration files (e.g., `api-gateway.yml`).

### API Gateway

The single ingress point for Nebula. It will handle routing, JWT validation, and correlation ID injection.

#### [NEW] `services/api-gateway/`
- Bootstrap a Spring Boot 3 + **Spring WebFlux** (Netty) application with Java 21 and Gradle (`build.gradle`).
- Add dependencies: `spring-cloud-starter-gateway`, `spring-boot-starter-oauth2-resource-server`, `spring-cloud-starter-config`.
- Configure `bootstrap.yml` to connect to the `config-service`.
- **Security:** Configure Spring Security WebFlux to require authenticated requests and validate JWTs using Keycloak's JWKS endpoint.
- **Filters:** Implement a global filter to generate and inject `X-Correlation-ID` into downstream request headers and MDC.
- **Routing:** Define route stubs for all planned services (Catalog, Dataset, Build, etc.) in `api-gateway.yml` (served by Config Service).
- Add `Dockerfile`.
- Add OTel Java agent, structured Logback configuration (including `trace_id` and `correlation_id`), and Micrometer.

### Testing

#### [NEW] `services/api-gateway/src/test/`
- Write unit tests for the Correlation ID filter.
- Write a `@SpringBootTest` integration test using **Testcontainers** to spin up a Keycloak instance, obtain a real JWT, and verify the Gateway's security layer allows authorized requests and rejects unauthorized ones (401).

### Infrastructure Updates

#### [MODIFY] `docker-compose.yml`
- Add `config-service` and `api-gateway` definitions to the main docker-compose file. Ensure they are attached to `nebula-net`.

#### [MODIFY] `infra/helm/`
- Populate the `templates/` for the `api-gateway` and `config-service` Helm charts with basic Deployments and Services.

## Verification Plan

### Automated Tests
- Run `./gradlew test` in both `config-service` and `api-gateway`. Ensure unit and Testcontainers integration tests pass.

### Manual Verification
1. Run `make up`. Ensure the new Spring Boot containers start cleanly within Docker.
2. Obtain a token from Keycloak using a password grant: `curl -d "client_id=workspace&grant_type=password&username=admin&password=admin" ...`
3. Send an unauthorized request to `http://localhost:8090/api/catalog/ping` expecting a `401 Unauthorized`.
4. Send an authorized request with the Bearer token expecting the request to reach the gateway and potentially return a 404 (since the downstream service doesn't exist yet but the gateway allows it through).
5. Check Jaeger to verify spans correlate correctly.
6. Check Loki to verify structured JSON logs have `correlation_id`, `trace_id`, and `span_id`.
