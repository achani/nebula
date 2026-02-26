# Nebula Phase 2 — Task Checklist

## Architecture & Setup
- [x] Create **ADR-003** for the polymorphic Catalog Item model
- [x] Initialize Spring Boot Gradle project (`services/catalog-service`)
- [x] Connect to `config-service` and API Gateway routing

## Database & Persistence
- [x] Configure PostgreSQL connectivity + Flyway migrations
- [x] Define Flyway schema V1 (`projects`, `folders`, `catalog_items`)
- [x] Create JPA Entities (`Project`, `Folder`, `CatalogItem`)
- [x] Create Spring Data Repositories

## REST API
- [x] Implement `ProjectController` (Create, List, Get)
- [x] Implement `FolderController` (Create, List within Project)
- [x] Implement global exception handler (RFC 7807 `ProblemDetail`)

## Kafka & Avro (Async)
- [x] Define Avro schema `CatalogEvent.avsc` (ProjectCreated, FolderCreated) in `schema-registry/`
- [x] Add Avro Gradle plugin to compile schema into Java POJOs
- [x] Configure Spring Kafka Producer connected to Schema Registry
- [x] Broadcast events upon Project/Folder creation

## gRPC Server (Sync)
- [x] Add gRPC Spring Boot starter dependencies
- [x] Define `catalog.proto` (Internal Lookup Service)
- [x] Implement gRPC `CatalogLookupServiceImpl`

## Observability & DevOps
- [x] Add JSON Logback, Micrometer, and OTel Java SDK settings
- [x] Create `Dockerfile` (Eclipse Temurin Jammy)
- [x] Append `catalog-service` to `docker-compose.yml`
- [x] Populate Helm charts (`infra/helm/catalog-service`)

## Testing
- [x] Unit tests for Domain/Service layer
- [x] Integration tests using Testcontainers (PostgreSQL + Kafka)

## Verification
- [x] Start full stack via `make up`
- [x] Create a Project via API Gateway (`POST /api/catalog/projects`)
- [x] Verify `ProjectCreated` Avro event in Kafka
- [ ] Verify logs with `correlation_id` and traces in Jaeger
