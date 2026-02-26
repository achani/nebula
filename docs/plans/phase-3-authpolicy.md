# Phase 3 - AuthPolicy Service Implementation Plan

## Goal Description
Build the `authpolicy-service` which acts as the centralized PEP (Policy Enforcement Point) and PDP (Policy Decision Point) for the Nebula platform. It will use OPA (Open Policy Agent) to evaluate Rego policies and return allow/deny decisions via gRPC.

## Proposed Changes

### `opa-policies`
- #### [NEW] `opa-policies/authz.rego`
  Initial project-level RBAC Rego policies mapping actions (read, create, delete) to roles (viewer, editor, owner).

### `authpolicy-service`
- #### [NEW] `services/authpolicy-service/build.gradle`
  Spring Boot 3, gRPC server, Resilience4j, and standard platform dependencies.
- #### [NEW] `services/authpolicy-service/src/main/proto/authpolicy.proto`
  gRPC definitions for `AuthorizeRequest` and `AuthorizeResponse`.
- #### [NEW] `services/authpolicy-service/src/main/java/com/nebula/authpolicy/service/OpaEvaluatorService.java`
  Service to send requests to the OPA REST API (`http://localhost:8181/v1/data/nebula/authz`) and parse the decision.
- #### [NEW] `services/authpolicy-service/src/main/java/com/nebula/authpolicy/api/AuthPolicyGrpcEndpoint.java`
  gRPC endpoint implementing `AuthPolicyServiceGrpc.AuthPolicyServiceImplBase` to handle incoming authorization checks.

### `catalog-service`
- #### [MODIFY] `services/catalog-service/build.gradle`
  Add gRPC client dependencies and protobuf generation.
- #### [NEW] `services/catalog-service/src/main/java/com/nebula/catalog/infrastructure/AuthPolicyClient.java`
  gRPC client with Resilience4j circuit breakers to call the `authpolicy-service`.
- #### [MODIFY] `services/catalog-service/src/main/java/com/nebula/catalog/api/ProjectController.java`
  Enforce AuthPolicy checks for creating and reading projects.

### `docker-compose.yml`
- #### [MODIFY] `docker-compose.yml`
  Add the `opa` container and the `authpolicy-service` container.

## Verification Plan
1. Send a request to `ProjectController` as an unauthorized user -> verify `403 Forbidden`.
2. Send a request to `ProjectController` as an authorized user -> verify `200` or `201`.
3. Check Jaeger traces to ensure the `catalog-service` -> `authpolicy-service` gRPC call is traced successfully.
