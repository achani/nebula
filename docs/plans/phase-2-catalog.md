# Phase 2 — Catalog Service (Core Domain)

## Goal
The Catalog Service acts as the logical namespace layer for the entire Nebula platform. Every entity—Datasets, Repositories, etc.—will belong to a `Project` and be geographically organized inside a `Folder`.

## Key Deliverables

### 1. Project Initialization & Architecture
- **Service Name:** `catalog-service`
- **Application Framework:** Spring Boot 3 (with Java 21 Virtual Threads enabled).
- **Domain Entities:** 
  - `Project` (Root namespace concept)
  - `Folder` (Hierarchical organization inside a project)
  - `CatalogItem` (Abstract concept representing either a dataset, repository, or folder).

### 2. Database & Persistence Layer
- **Database:** PostgreSQL (Schema: `catalog`).
- **Migrations:** Flyway for schema versioning and lifecycle.
- **ORM:** Spring Data JPA / Hibernate.

### 3. REST API
- Expose robust HTTP endpoints with RFC 7807 `ProblemDetail` standard error handling:
  - `POST /api/catalog/projects` - Create project
  - `GET /api/catalog/projects` - List active projects
  - `POST /api/catalog/folders` - Create sub-folders

### 4. Async Event Broadcasting (Kafka)
- **Schema Management:** Define Avro schemas (`ProjectCreated`, `FolderCreated`) stored centrally in the Confluent Schema Registry.
- **Messaging:** Implement a Kafka Producer to broadcast entity creation events to the `catalog.events` topic with high durability guarantees.

### 5. High-Performance Internal RPC (gRPC)
- **gRPC Server:** Set up a `CatalogLookupService` via gRPC.
- **Purpose:** Ensure other internal services (e.g., Code Service, Dataset Service) can rapidly look up Catalog item IDs without the overhead of HTTP/REST serialization.

### 6. Observability
- **Distributed Tracing:** Auto-instrumented via OpenTelemetry Java Agent (pushing to Jaeger over OTLP port 4318).
- **Log Aggregation:** Structured JSON Logback configuration mapped to Prometheus parameters and Loki standard fields (including MDC injecting `correlation_id` propagated from API Gateway).
- **Metrics:** Micrometer integration exposed at `/actuator/prometheus`.

### 7. DevOps & Deployment
- Include `catalog-service` inside `docker-compose.yml` mapped to internal networking.
- Generate standard Kubernetes Helm Chart configurations (Deployment, Service).

### 8. Testing & Validation
- **Unit Testing:** JUnit 5 + Mockito achieving high business logic coverage.
- **Integration Testing:** `@SpringBootTest` configured with Testcontainers to launch ephemeral PostgreSQL and Kafka instances locally for true E2E validations.
- **Contract Testing:** Prepare Pact configurations for future REST/gRPC client integrations.

## Execution Requirements
1. **Never skip testing:** Integration tests must pass before the phase is considered complete.
2. **Follow ADRs:** Generate **ADR-003** before starting the architecture for Polymorphic Catalog Items.
3. **No hard-coded secrets:** Pull dynamic config securely (or leverage global `application.yml` via the `config-service`).
