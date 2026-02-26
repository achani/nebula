# Phase 4 - Dataset Service Implementation Walkthrough

## Overview
Phase 4 of Nebula focused on building the `dataset-service`, which acts as the control plane for datasets stored as **Delta Lake** tables on MinIO. 

## Key Changes
1. **Avro Schemas**: Created missing event schemas in the `schema-registry/`:
   - `DatasetCreated.avsc`, `DatasetUpdated.avsc`, `SchemaChanged.avsc`, `ProjectDeleted.avsc`.
2. **Bootstrapping the Service**: Initialized the Spring Boot 3 app, resolving dependencies for embedded Spark 3.5, Delta Lake, and Hadoop AWS for MinIO integration.
3. **Database Schema & Domain Model**:
   - Added Flyway migration (`V1__init_dataset_schema.sql`) for `dataset`, `dataset_version`, and `schema_snapshot` tables.
   - Built JPA entities and repositories for tracking dataset persistence and schema state.
4. **Embedded Spark & Delta Lake Integration**:
   - Implemented `SparkConfig.java` to spin up a lightweight `local[*]` Spark session configured to write data to MinIO via the `s3a://` endpoint.
   - Built the `DeltaLakeService.java` wrapper to handle creating empty Delta tables, recovering schema JSON, and enabling "time-travel" querying.
5. **Kafka Events**:
   - Configured `DatasetEventProducer` to fan-out dataset lifecycle events.
   - Added `ProjectEventConsumer` to listen to `ProjectDeleted` events and execute cascading deletes for associated datasets.
6. **REST API & AuthZ**:
   - `DatasetController` handles HTTP API endpoints, including table creation, querying versions, and returning schema data.
   - Integrated with the Phase 3 `authpolicy-service` to enforce OPA policies before executing dataset logic.
7. **Infrastructure setup**:
   - Added a `dataset-service` Helm chart duplicate.
   - Updated `docker-compose.yml` adding the `nebula-dataset-service`.
   - Populated Spring Cloud `config/dataset-service.yml`.

## How to Verify
In your terminal, start the entire backend:
```bash
make up
```

Wait until all services are healthy (including `dataset-service`), then grab the admin token and hit the API to provision a new Dataset:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/nebula/protocol/openid-connect/token \
  -d "client_id=workspace&grant_type=password&username=admin&password=admin" | jq -r .access_token)

# Assuming a project has been created, we test creating a delta table
curl -X POST http://localhost:8090/api/datasets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"projectId": "<uuid>", "name": "transactions", "format": "DELTA"}'
```

Verify the underlying MinIO storage state:
```bash
# Verify Delta Lake structure was built
mc ls local/nebula-data/<projectId>/<datasetId>/_delta_log/
```

Confirm time-travel version querying is possible:
```bash
# Grab the dataset versions to see the initial version 0 Delta history
curl http://localhost:8090/api/datasets/<datasetId>/versions
```
