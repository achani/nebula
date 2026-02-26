# Phase 1 Completion Walkthrough: Config Service, API Gateway, and AuthN Skeleton

## Goal
The goal of Phase 1 was to establish the foundational backend services that guard the entry point of the Nebula data platform. This involved building a centralized **Config Service** to externalize all configuration properties and an **API Gateway** acting as the single enforcement point for JWT authentication, distributed tracing, and request correlation.

## Changes Made
- **Config Service**:
  - Implemented `spring-cloud-config-server` configured to read from the local file system (`config/` repository folder).
  - Defined global configurations like Keycloak endpoints, Kafka cluster brokers, and Jaeger OpenTelemetry URLs (`application.yml`).
  - Containerized with Eclipse Temurin (Jammy) and injected the OpenTelemetry Java Agent.

- **API Gateway**:
  - Implemented a Spring Cloud Gateway (WebFlux-based) service.
  - Added global routing mappings for future services (`catalog-service`, `dataset-service`, `build-service`, etc.).
  - Integrated `spring-boot-starter-oauth2-resource-server` and `spring-security-oauth2-jose`.
  - Added a global `SecurityWebFilterChain` requiring all endpoints (except `/actuator/**`) to present a valid Bearer JWT.
  - Dynamically resolved the Keycloak `jwk-set-uri` property from the remote Config Service.
  - Added a global `CorrelationIdFilter` ensuring that every incoming request gets an `X-Correlation-ID` header and automatically propagates it to backend services and the MDC context logger.

- **DevOps & Infrastructure**:
  - Added both services to the `docker-compose.yml` stack with explicit `depends_on` logic.
  - Created base Helm charts for deploying both services to Kubernetes.
  - Resolved tricky OpenTelemetry protocol misconfigurations involving gRPC vs HTTP/Protobuf ports.

## What Was Tested
- **Testcontainers Integration Tests**: 
  - Wrote `ApiGatewayIntegrationTest` which dynamically launches a Keycloak container holding the `nebula-realm.json` and verifies that requests without real JWTs return `401 Unauthorized`.
- **Local `docker-compose` E2E Test**:
  - Validated that the Config Server successfully distributes `application.yml` via its API.
  - Hand-crafted an HTTP request hitting `http://localhost:8090/api/catalog/ping` confirming it was instantly blocked with a `401 Unauthorized`.
  - Obtained a real JWT (`grant_type=password`) through local Keycloak and verified that hitting the API Gateway with it correctly passed authentication and routed the request downstream!
  - Evaluated the API Gateway Docker logs to confirm `trace_id`, `span_id`, and `correlation_id` are natively appearing in the `stdout` JSON payload for the Loki log aggregator.

We are now ready for backend business logic. Let's start Phase 2: Building the Catalog & Dataset Services!
