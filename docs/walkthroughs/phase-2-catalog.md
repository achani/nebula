# Phase 2: Catalog Service (Completed)

**Components Built:**
- Base `catalog-service` skeleton in Spring Boot 3 running on Java 21.
- PostgreSQL Flyway schema defining polymorphic Catalog Items (`projects` and `folders`).
- REST APIs to manage boundaries (`ProjectController`, `FolderController`) following RFC 7807 problem details.
- Kafka integration to publish `ProjectCreated` and `FolderCreated` using Avro schemas and Confluent Schema Registry.
- `CatalogLookupServiceImpl` gRPC endpoint to quickly resolve entity names internally.
- Integration tests using Testcontainers connecting to Postgres and Kafka.
- Production-ready `Dockerfile` and integration into `docker-compose.yml` and `infra/helm`.

**Testing Performed:**
- Automated Unit Tests and Context Load test with Testcontainers.
- Full E2E verification of JWT routing through the `api-gateway` down to `catalog-service`.
- Confirmed Avro event generation inside the local Kafka cluster.
