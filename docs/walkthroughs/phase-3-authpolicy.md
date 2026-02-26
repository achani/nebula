# Phase 3 Completion Walkthrough: AuthPolicy Service

## Goal
The goal of Phase 3 was to build the `authpolicy-service`, a centralized authorization engine powered by Open Policy Agent (OPA), and retrofit the `catalog-service` to query this new service before allowing modifications (like creating or deleting Projects/Folders).

**Components Built:**
- New `authpolicy-service` built with Spring Boot 3 + gRPC and OpenTelemetry instrumentation.
- Declarative Rego policy engine integration (`opa-policies/rbac.rego`) providing dynamic role-based rule enforcement.
- Integrated the official `authpolicy.proto` protobuf schema into both services.
- `AuthPolicyClient.java` built inside `catalog-service` to forward required permissions synchronously via gRPC using Resilience4j circuit breakers to fail-fast.
- `GlobalExceptionHandler` mapping `ForbiddenException` to standard 403 JSON responses.

**Testing Performed:**
- Evaluated isolated OPA configurations natively. 
- Sent direct POST payload to the API simulating an invalid role, catching the `CallNotPermittedException` generating a fast 403. 
- Restored valid roles, traced JSON properties through the gRPC translation layer, and verified a `201 Created` response when OPA evaluated `allow=true`.
