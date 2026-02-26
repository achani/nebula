# Nebula — Multiphase Implementation Plan

> **Goal:** Build a production-grade, distributed data analytics platform incrementally.
> Each phase is **independently deployable**, **testable end-to-end locally**, and leaves `main` in a releasable state.
> Every phase ends with a tagged Git release (`v0.1.0`, `v0.2.0`, ...) to enable rollback to any point.

---

## Guiding Principles

- **Local-first dev:** The entire stack (Kafka, PostgreSQL, Spark, MinIO, Keycloak, Jaeger) runs via `docker-compose` on a laptop. No cloud required.
- **No phase skips:** Observability, security, and testing are built in from Phase 1, not bolted on later.
- **Feature flags:** New MFEs and services are toggled off by default until their phase is complete.
- **ADR per phase:** Every significant architectural decision is captured in `docs/adr/` before code is written.
- **Git strategy:** `main` is always deployable. Work happens in short-lived feature branches, merged via PR after CI passes. Each phase is tagged.

---

## Phase 0 — Foundations & Local Dev Environment

**Goal:** Every developer can run the full infrastructure stack locally with a single command. No application code yet.

### Deliverables
- `docker-compose.yml` bringing up:
  - **PostgreSQL 16** (one instance, multiple databases simulating schema-per-service)
  - **Apache Kafka** + Zookeeper + **Confluent Schema Registry** (OSS)
  - **MinIO** (S3-compatible object store — Delta Lake target)
  - **Keycloak** (pre-seeded realm `nebula`, client `workspace`, admin user)
  - **Jaeger** (all-in-one image)
  - **Prometheus** + **Grafana** (with provisioned datasources)
  - **Loki** + **Promtail**
  - **HashiCorp Vault** (dev mode)
- `infra/helm/` — umbrella chart skeleton + per-service chart stubs.
- `infra/k8s/` — Istio `PeerAuthentication` (STRICT mTLS) policy stubs.
- `Makefile` with targets: `up`, `down`, `reset`, `status`.
- `README.md` with local dev setup instructions.
- Repo structure initialized: `services/`, `workspace/`, `schema-registry/`, `opa-policies/`, `infra/`.
- GitHub Actions CI skeleton: lint, build, test jobs (no-op stubs initially).
- **ADR-001:** Monorepo vs polyrepo decision.
- **ADR-002:** Istio as service mesh (justification over Eureka/Consul).

### Test Gate
```bash
make up
curl http://localhost:9000/minio/health/live     # MinIO OK
curl http://localhost:16686                       # Jaeger UI loads
curl http://localhost:8080/realms/nebula          # Keycloak realm exists
```

**Git tag:** `v0.0.1-infra`

---

## Phase 1 — API Gateway + AuthN Skeleton

**Goal:** A working API Gateway that validates JWTs from Keycloak and routes to a stub backend. This is the security perimeter for everything upstream.

### Deliverables
- `services/api-gateway/` — Spring Cloud Gateway
  - JWT validation filter (delegated to Keycloak JWKS endpoint)
  - `X-Correlation-ID` injection filter
  - Route stubs for all planned services
  - `/actuator/health` liveness + readiness probes
  - OTel Java agent wired → Jaeger
  - Structured JSON logging (Logback) with `trace_id`, `span_id`, `correlation_id`
  - Micrometer metrics → Prometheus
- `services/config-service/` — Spring Cloud Config Server backed by local Git repo (`config/`)
- Keycloak realm export script (reproducible realm setup).
- Helm chart for both services.
- Unit tests + integration test with Testcontainers (Keycloak).

### Test Gate
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/nebula/protocol/openid-connect/token \
  -d "client_id=workspace&grant_type=password&username=admin&password=admin" | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8090/api/catalog/ping  # → 200
curl http://localhost:8090/api/catalog/ping                                      # → 401
```

**Git tag:** `v0.1.0`

---

## Phase 2 — Catalog Service (Core Domain)

**Goal:** The logical namespace layer — Projects, Folders, Datasets, Repos — is the backbone of the entire platform.

### Deliverables
- `services/catalog-service/` — Spring Boot 3 (virtual threads)
  - Domain model: `Project`, `Folder`, `CatalogItem` (polymorphic: dataset | repo | folder)
  - REST API: CRUD for Projects and Folders
  - PostgreSQL persistence (Flyway migrations, schema: `catalog`)
  - Kafka producer: `ProjectCreated`, `FolderCreated` events (Avro schemas in `schema-registry/`)
  - gRPC server: `CatalogLookupService` (fast path for other services to resolve item IDs)
  - OTel, structured logging, Micrometer metrics
  - Resilience4j circuit breaker on outbound calls
  - `@SpringBootTest` integration tests with Testcontainers (PostgreSQL + Kafka)
  - Pact contract test stubs
- Avro schema for `CatalogEvent` in `schema-registry/`.
- Helm chart.
- **ADR-003:** Polymorphic catalog item model.

### Test Gate
```bash
TOKEN=...
curl -X POST http://localhost:8090/api/catalog/projects \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name": "my-project", "description": "First project"}'
curl -H "Authorization: Bearer $TOKEN" http://localhost:8090/api/catalog/projects
# Verify Kafka topic "catalog.events" receives ProjectCreated (kafka-console-consumer)
```

**Git tag:** `v0.2.0`

---

## Phase 3 — AuthPolicy Service (Authorization Plane)

**Goal:** Centralized, policy-based authorization. Every service calls this before acting on a resource. No service embeds its own authz logic.

### Deliverables
- `services/authpolicy-service/` — Spring Boot 3 + OPA
  - gRPC API: `AuthorizeRequest` → `AuthorizeResponse` (allow/deny + reason)
  - REST API: `/v1/authorize` (fallback for non-gRPC callers)
  - OPA engine: Rego policies loaded from `opa-policies/` at startup + live reload
  - Initial policies: project-level RBAC (owner, editor, viewer)
  - Testcontainers integration tests
- `opa-policies/` — initial Rego policies for Catalog resources.
- Catalog Service updated: all mutating endpoints call AuthPolicy via gRPC.
- **ADR-004:** OPA as policy engine (vs. Spring Security method security).

### Test Gate
```bash
# As viewer, delete a project → 403 (denied by OPA)
# As owner, delete project → 200
# Verify AuthPolicy gRPC call in Jaeger trace for the request
```

**Git tag:** `v0.3.0`

---

## Phase 4 — Dataset Service + Delta Lake Integration

**Goal:** Datasets backed by Delta Lake on MinIO with time-travel and schema evolution working end-to-end locally.

### Deliverables
- `services/dataset-service/` — Spring Boot 3
  - Domain model: `Dataset`, `DatasetVersion`, `SchemaSnapshot`
  - REST API: create/read/delete datasets; list versions; time-travel read (by version or timestamp)
  - Delta Lake ops via **Spark 3.5 in local mode** (embedded `SparkSession` for metadata; actual transforms via Build Service)
  - MinIO as backing store (S3A connector)
  - PostgreSQL schema `dataset` — dataset metadata, versions, schema history
  - Kafka consumer: `ProjectDeleted` → cascade cleanup
  - Kafka producer: `DatasetCreated`, `DatasetUpdated`, `SchemaChanged` events
  - Testcontainers (PostgreSQL + MinIO + Kafka)
- **ADR-005:** Embedded Spark local mode for metadata ops vs. remote Spark cluster.
- Helm chart.

### Test Gate
```bash
curl -X POST http://localhost:8090/api/datasets \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"projectId": "...", "name": "transactions", "format": "DELTA"}'
# Write data via mc CLI → Delta log created in MinIO
mc cp transactions.parquet local/nebula-data/my-project/transactions/
curl http://localhost:8090/api/datasets/{id}/versions  # → version list with time-travel
```

**Git tag:** `v0.4.0`

---

## Phase 5 — Code Service + Browser IDE

**Goal:** Git repositories appear in the Catalog as first-class items. Developers write Spark job code *inside the platform* before submitting jobs in Phase 6.

### Deliverables
- `services/code-service/` — Spring Boot 3 + JGit
  - REST API: create repo (registers in Catalog as `REPOSITORY` item), clone, list branches, list commits
  - Git storage: bare repos on MinIO (or local volume for dev)
  - Provisions a **code-server** (VS Code in browser) instance per repo on demand (Docker API / K8s Job)
  - Session management: idle timeout, port allocation
- `workspace/mfe-code/` — Code browser MFE
  - File tree, commit history view
  - "Open in IDE" button → launches code-server in browser
- **ADR-006:** code-server session lifecycle (per-user vs. per-repo).
- Helm chart.

### Test Gate
- Create a repo via REST → appears in Catalog as `REPOSITORY` item
- Browse files and commit history via API
- "Open in IDE" → code-server launches, repo is open, can commit from browser

**Git tag:** `v0.5.0`

---

## Phase 6 — Build Service + Spark Job Submission

**Goal:** Users submit Spark jobs (written in the Code Service) that read/write Delta tables on MinIO. Live status streams to the Workspace via WebSockets.

### Deliverables
- `services/build-service/` — Spring Boot 3 (WebFlux for reactive streaming)
  - REST API: submit job, get status, list jobs
  - Spark job submission via **Spark REST API** (standalone cluster in docker-compose)
  - **Live log streaming:** Spark driver logs → Kafka topic `build.job-logs` → WebSocket `/ws/builds/{jobId}/logs`
  - Job lifecycle Kafka events: `JobSubmitted`, `JobRunning`, `JobCompleted`, `JobFailed`
  - PostgreSQL schema `build` — job history, status, metadata
- `docker-compose.yml` updated: **Spark standalone cluster** (master + 1 worker) → MinIO.
- Example Spark job (in Code Service repo): reads Delta table, applies transform, writes back.
- Testcontainers integration test for job lifecycle.
- **ADR-007:** Spark standalone (local dev) → Kubernetes Spark Operator (production path).

### Test Gate
```bash
JOB_ID=$(curl -X POST http://localhost:8090/api/builds/jobs \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"datasetId": "...", "jobClass": "com.nebula.example.WordCount"}' | jq -r .jobId)
wscat -c "ws://localhost:8090/ws/builds/$JOB_ID/logs"
# → Live log lines stream as Spark job runs against MinIO data
```

**Git tag:** `v0.6.0`

---

## Phase 7 — Workspace (Micro-Frontend Shell + MFEs)

**Goal:** A working browser portal stitching together all services via Module Federation. Real-time job status visible in the UI.

### Deliverables
- `workspace/shell/` — React 18 + TypeScript (strict) + Module Federation host
  - Keycloak JS adapter (login / token refresh)
  - Shared `useWebSocket` hook (STOMP over SockJS)
  - BlueprintJS design system + global CSS tokens
  - Left nav: Projects tree (Catalog), top bar: user/logout
  - Routes: `/projects/{id}` → `mfe-catalog`, `/builds` → `mfe-build`, `/code` → `mfe-code`
  - Global `<NonIdealState>`, `<Spinner>`, `<Toaster>`
- `workspace/mfe-catalog/` — Project tree, folder/dataset browsing, version history table
- `workspace/mfe-build/` — Job submission form, live log viewer (WebSocket), job history table
- `workspace/mfe-code/` — File tree, commit history, "Open in IDE" button (code-server iframe)
- Playwright e2e: login → create project → browse datasets → write code → submit job → see live logs.

### Test Gate
- Browser: login → create project → browse tree → create dataset → open repo in IDE → submit Spark job → watch logs stream live

**Git tag:** `v0.7.0`

---

## Phase 8 — Scheduling Service

**Goal:** Jobs triggered on a cron schedule or by platform events (e.g., `DatasetUpdated`).

### Deliverables
- `services/scheduling-service/` — Spring Boot 3 + Quartz (clustered, PostgreSQL-backed)
  - REST API: create/update/delete schedules; manual trigger
  - Trigger types: `CRON` (time-based), `EVENT` (Kafka consumer)
  - On trigger: publishes `JobRequested` → Build Service picks up and submits
  - Quartz job store: PostgreSQL schema `scheduling`
  - Kafka consumer: `DatasetUpdated` → evaluate dependent schedules → trigger
- `workspace/mfe-scheduling/` — Schedule management UI (cron builder, event trigger config, history)
- **ADR-008:** Quartz cluster vs. Temporal/Airflow — document tradeoffs.

### Test Gate
```bash
curl -X POST http://localhost:8090/api/schedules \
  -d '{"jobClass": "com.nebula.example.WordCount", "trigger": {"type": "CRON", "cron": "0 * * * * ?"}}'
# → After 1 minute: Build Service receives JobRequested → job runs → logs stream
```

**Git tag:** `v0.8.0`

---

## Phase 9 — Notification Service + Platform Hardening

**Goal:** Close the observability and reliability loop: notifications, alerting, chaos testing, and full Pact contract coverage.

### Deliverables
- `services/notification-service/` — Spring Boot 3 (Kafka consumer)
  - Consumes all platform events (job completion, schema change, schedule failure)
  - Fan-out: in-app (WebSocket push), email (SMTP), webhook
  - User notification preferences — PostgreSQL schema `notification`
- **Hardening across all services:**
  - Resilience4j bulkheads + timeouts audited and tuned
  - Istio retry + circuit-break policies applied in Helm charts
  - HPA for every service (min: 2 replicas, scale on CPU ≥70%)
  - Grafana dashboards: per-service RED metrics (Rate, Errors, Duration)
  - Alertmanager rules: job failure rate > 10%, p99 latency > 2s
  - Chaos test: kill a pod mid-request → circuit breaker fires, graceful fallback returns
- Pact contract tests: all REST + gRPC contracts published and verified.
- Full Playwright e2e suite covering Phases 0–9 together.

### Test Gate
- Kill Catalog pod → Gateway returns `503` with RFC 7807 `ProblemDetail` (no stack trace)
- Restart pod → traffic resumes automatically (Istio handles)
- Submit a failing job → in-app notification appears in Workspace within 5s

**Git tag:** `v0.9.0`

---

## Rollback Strategy

| Mechanism | How |
|---|---|
| Git tags | Every phase tagged. `git checkout v0.3.0` + `helm rollback` restores that state. |
| Helm history | `helm rollback <release> <revision>` for K8s rollbacks. |
| Database migrations | Flyway versioned, forward-only (no destructive drops within a phase). |
| Docker images | Tagged with Git SHA + phase version. Old images retained in registry. |
| Delta Lake | Time travel on all user datasets — data is never deleted, only new versions written. |

---

## Local Dev Stack Summary

```
docker-compose up -d
```

| Component | Port | Purpose |
|---|---|---|
| PostgreSQL | 5432 | All service schemas |
| Kafka | 9092 | Async messaging |
| Schema Registry | 8081 | Avro schemas |
| MinIO | 9000/9001 | Delta Lake storage |
| Keycloak | 8080 | AuthN |
| Spark Master | 7077/8082 | Job submission |
| Spark Worker | 8083 | Job execution |
| Jaeger | 16686 | Distributed tracing |
| Prometheus | 9090 | Metrics scraping |
| Grafana | 3001 | Dashboards |
| Loki | 3100 | Log aggregation |
| Vault | 8200 | Secrets (dev mode) |

> **`docker-compose.dev-lite.yml`** profile (no Vault, no Loki, 1 Kafka broker) for 8 GB laptops.

---

## Learning Milestones per Phase

| Phase | Core Concepts Learned |
|---|---|
| 0 | Docker Compose, Kafka, MinIO, Keycloak, Jaeger, Prometheus — wiring the platform |
| 1 | API Gateway patterns, JWT validation, OTel instrumentation, Correlation IDs |
| 2 | Domain-Driven Design, Kafka Avro event schemas, gRPC server, Flyway migrations |
| 3 | Policy-as-code (OPA/Rego), centralized authorization, gRPC client patterns |
| 4 | Delta Lake internals, time travel, schema evolution, Spark local mode, S3A |
| 5 | JGit, container lifecycle management, browser-based IDE integration |
| 6 | Spark standalone, reactive streaming (WebFlux), WebSocket + Kafka bridge |
| 7 | Micro-frontend architecture, Module Federation, real-time UI with WebSockets |
| 8 | Distributed schedulers, Quartz clustering, event-driven job triggers |
| 9 | Chaos engineering, Istio traffic policies, Resilience4j tuning, SRE alerting |
