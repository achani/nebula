# Nebula Phase 3 — Task Checklist

## Architecture & Setup
- [x] Read and review **ADR-004** (OPA Authorization).
- [x] Initialize Spring Boot Gradle project (`services/authpolicy-service`).
- [x] Create `opa-policies/rbac.rego` defining core permissions.

## gRPC API & OPA Integration
- [x] Define `authpolicy.proto` (`AuthorizeRequest` -> `AuthorizeResponse`).
- [x] Compile protobuf into Java Client/Server stubs.
- [x] Implement `OpaEvaluatorService` to query the Rego engine.
- [x] Implement `@GrpcService` endpoint exposing the evaluator.

## Retrofit: Catalog Service
- [x] Update `catalog-service/build.gradle` to compile `authpolicy.proto` as a client.
- [x] Create `AuthPolicyClient` in Catalog Service with Resilience4j Circuit Breaker.
- [x] Inject `AuthPolicyClient` into `ProjectService` to block unauthorized creations.
- [x] Inject `AuthPolicyClient` into `FolderService` to block unauthorized creations.

## Observability & DevOps
- [x] Add JSON Logback, Micrometer, and OTel Java SDK settings to `authpolicy-service`.
- [x] Create `Dockerfile` (Eclipse Temurin Jammy) running Spring Boot.
- [x] Update `docker-compose.yml` to include `opa` (sidecar/standalone) and `authpolicy-service`.
- [x] Map `opa-policies/` directory as a mounted volume in docker-compose.

## Testing & Verification
- [x] Unit tests for OPA evaluation logic.
- [x] Integration tests using Testcontainers (OPA + Spring Boot).
