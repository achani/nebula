# Phase 4 - Dataset Service Implementation Plan

## Goal Description
Build the `dataset-service` which acts as the control plane for datasets stored as Delta Lake tables on MinIO. It will handle creating datasets, managing their metadata in PostgreSQL, maintaining their schemas, and querying Delta transaction logs for time travel.

## Proposed Changes

### `schema-registry` (Avro Schemas)
- #### [NEW] `schema-registry/DatasetCreated.avsc`
- #### [NEW] `schema-registry/DatasetUpdated.avsc`
- #### [NEW] `schema-registry/SchemaChanged.avsc`
- #### [NEW] `schema-registry/ProjectDeleted.avsc`

### `dataset-service` (Spring Boot Application)
- #### [NEW] `services/dataset-service/build.gradle`
  Spring Boot 3, Spark 3.5 (local), Delta Core, MinIO/S3A, PostgreSQL, Kafka, OPA client.
- #### [NEW] `services/dataset-service/src/main/resources/application.yml`
  Configuration for Kafka, DB, OPA, MinIO.
- #### [NEW] `services/dataset-service/src/main/resources/db/migration/V1__init_dataset_schema.sql`
  Tables for `dataset`, `dataset_version`, `schema_snapshot`.
- #### [NEW] `services/dataset-service/src/main/java/com/nebula/dataset/config/SparkConfig.java`
  Configures a lightweight local `SparkSession` with S3A MinIO credentials.
- #### [NEW] `services/dataset-service/src/main/java/com/nebula/dataset/domain/...`
  Domain entities mapping to the database.
- #### [NEW] `services/dataset-service/src/main/java/com/nebula/dataset/api/...`
  REST Controllers exposing CRUD for datasets and versions.
- #### [NEW] `services/dataset-service/src/main/java/com/nebula/dataset/service/...`
  Business logic orchestrating Spark, DB persistence, and Kafka.

### `api-gateway`
- #### [MODIFY] `services/api-gateway/src/main/resources/application.yml`
  Adds `/api/datasets/**` route.

### Testing & Verification
- #### [NEW] `services/dataset-service/src/test/java/com/nebula/dataset/DatasetIntegrationTest.java`
  E2E Testcontainers test orchestrating PostgreSQL, MinIO, and Kafka.

## Verification Plan
1. Call `/api/datasets` to create a dataset.
2. Verify Delta Lake structure in MinIO.
3. Call `/api/datasets/{id}/versions` to view the time travel log.
