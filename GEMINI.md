# Agentic Coding Rules: Nebula — Distributed Data Analytics Platform
> Inspired by Palantir Foundry. Built for learning. Engineered for scale.

---

## 🧭 Platform Philosophy
Nebula is a **microservices-first**, **event-driven** data analytics platform. Every design decision must favor:
- **Loose coupling** between services (async messaging preferred over sync HTTP for non-critical paths).
- **High availability** via multiple replicas, health checks, and circuit breakers.
- **Observability** as a first-class citizen (traces, metrics, logs for every service).
- **Delta Lake** as the canonical storage format for all user data. Internal platform metadata lives in PostgreSQL.
- **Open source first** — prefer Palantir OSS and CNCF-graduated projects.

---

## 🛠 Tech Stack

### Frontend — Workspace (Micro-Frontend Portal)
- **Framework:** React 18+, TypeScript (strict mode).
- **UI Library:** [BlueprintJS](https://blueprintjs.com/) — dense, data-rich, Jira/Confluence aesthetic.
- **Micro-Frontends:** [Module Federation](https://webpack.js.org/concepts/module-federation/) (Webpack 5) — each service contributes its own UI shell.
- **State Management:** Zustand (local) + React Query (server state).
- **Routing:** React Router v6.
- **Build:** Vite (per MFE shell) + TypeScript path aliases.
- **Real-time:** WebSockets (STOMP over SockJS) for live job status, event feeds.
- **Testing:** Vitest + React Testing Library + Playwright (e2e).

### Backend — Microservices
- **Language & Runtime:** Java 21 (Virtual Threads / Loom enabled).
- **Framework:** Spring Boot 3.x (with Spring WebFlux where reactive I/O is needed).
- **Build Tool:** Gradle (multi-module, one root build per service).
- **Service Mesh & Discovery:** [Istio](https://istio.io/) on Kubernetes — handles mTLS, retries, circuit breaking, load balancing, and sidecar injection. No client-side Eureka/Ribbon.
- **API Gateway:** [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway) as the single ingress point — routing, rate limiting, auth token forwarding.
- **Async Messaging:** Apache Kafka — all inter-service events flow through Kafka topics. Use Avro schemas with a Schema Registry (Confluent OSS).
- **Sync Communication:** gRPC (for performance-critical internal calls, e.g., AuthPolicy decisions). REST/JSON for all external-facing APIs.
- **Configuration Management:** Spring Cloud Config Server backed by a Git repo. All services pull config at startup and support live refresh via Spring Cloud Bus (Kafka actuator).

### Data Layer
- **Platform Metadata (internal):** PostgreSQL 16 (one schema per service — no shared databases across service boundaries).
- **User Data Storage:** Delta Lake on object storage (MinIO for local/dev, S3-compatible for prod).
- **Data Processing:** Apache Spark 3.5+ (submitted via the Build Service).
- **Dataset Versioning:** Delta Lake transaction log (time travel, schema evolution).

### Infrastructure & Orchestration
- **Container Runtime:** Docker.
- **Orchestration:** Kubernetes (K8s) — each service is a `Deployment` with `HorizontalPodAutoscaler`.
- **Service Mesh:** Istio (sidecar proxy: Envoy). Handles: mTLS, circuit breaking, retries, traffic shifting.
- **Package Manager:** Helm (one chart per service, umbrella chart for the full platform).
- **CI/CD:** GitHub Actions → build image → push to registry → Helm upgrade.

### Observability Stack (OpenTelemetry-first)
- **Distributed Tracing:** OpenTelemetry SDK (Java agent, auto-instrumentation) → [Jaeger](https://www.jaegertracing.io/).
- **Metrics:** Micrometer → Prometheus → Grafana dashboards.
- **Logging:** Structured JSON logs via Logback (SLF4J) with **trace_id** and **span_id** injected into every log line via the OTel agent. Aggregated with [Loki](https://grafana.com/oss/loki/) + Grafana.
- **Alerting:** Alertmanager (Prometheus rules) → PagerDuty / Slack webhook.
- **Health Checks:** Spring Boot Actuator `/actuator/health` (liveness + readiness probes in K8s).

### Auth & Security
- **AuthN:** OAuth 2.0 / OpenID Connect via [Keycloak](https://www.keycloak.org/) (OSS identity provider).
- **AuthZ:** Policy-based, managed by the **AuthPolicy Service** (OPA — Open Policy Agent). All services call AuthPolicy for access decisions; no service embeds its own authz logic.
- **Token Transport:** JWT in `httpOnly` cookies for browser clients. Bearer token in `Authorization` header for service-to-service (mTLS + short-lived tokens via Istio SPIFFE).
- **Secrets:** Kubernetes Secrets + [HashiCorp Vault](https://www.vaultproject.io/) (dynamic DB credentials, API keys). Never hardcode secrets.

---

## 🏗 Microservices Catalogue

| Service | Responsibility | Key Tech |
|---|---|---|
| **Workspace** | Micro-frontend portal, event bus, shell app | React, Module Federation, WebSockets |
| **API Gateway** | Single ingress, routing, rate limiting, token forwarding | Spring Cloud Gateway |
| **Catalog Service** | Logical namespace (Projects → folders → datasets/repos). Permissions at project level | Spring Boot, PostgreSQL |
| **Dataset Service** | Dataset lifecycle, Delta Lake time travel, schema tracking | Spring Boot, Delta Lake, Spark |
| **Build Service** | Submit/track Spark jobs, stream logs/status to Workspace | Spring Boot, Spark REST API, Kafka, WebSockets |
| **Scheduling Service** | Time-based and event-triggered job scheduling | Spring Boot, Quartz Scheduler, Kafka |
| **Code Service** | Git repo management, browser IDE integration (e.g., code-server / VS Code Server) | Spring Boot, JGit, code-server |
| **AuthPolicy Service** | Policy-based AuthZ decisions for all services | Spring Boot, OPA (Open Policy Agent) |
| **Config Service** | Centralized configuration server | Spring Cloud Config |
| **Notification Service** | Fan-out events (email, in-app, webhooks) | Spring Boot, Kafka consumer |

> **More services will be added.** Every new service MUST follow the patterns in this document.

---

## 🔗 Inter-Service Communication Rules

1. **Async (preferred):** Use Kafka for all fire-and-forget, event-driven communication (e.g., "job completed", "dataset updated").
2. **Sync — gRPC:** For low-latency, strongly typed internal calls (e.g., AuthPolicy authorization checks, Catalog lookups in hot paths).
3. **Sync — REST:** Only for external-facing or developer APIs. Never call another service's DB directly.
4. **No shared databases.** Each service owns its PostgreSQL schema exclusively.
5. **Idempotency keys** required on all Kafka consumers. Consumers MUST be idempotent.
6. **Circuit Breakers:** Use [Resilience4j](https://resilience4j.readme.io/) on all sync outbound calls. Fallback behavior is required.
7. **Correlation IDs:** The API Gateway injects `X-Correlation-ID` into every request. All services MUST propagate it downstream and include it in every log line.

---

## 🏭 Production Standards

### Error Handling
- Services expose RFC 7807 `ProblemDetail` (Spring Boot 3 built-in) for all REST errors.
- gRPC services use standard gRPC status codes + rich error details.
- React Error Boundaries wrap every MFE shell. Unknown errors show a BlueprintJS `NonIdealState` fallback.

### Logging
- All log lines: structured JSON with fields `{timestamp, level, service, trace_id, span_id, correlation_id, message}`.
- Log levels: ERROR for actionable faults, WARN for degraded states, INFO for lifecycle events, DEBUG/TRACE never in production.
- No PII or secrets in logs.

### Tracing
- OTel Java Agent auto-instruments Spring Boot, gRPC, Kafka, JDBC.
- Every cross-service call carries W3C `traceparent` / `tracestate` headers.
- Span names: `ServiceName.OperationName` (e.g., `BuildService.submitSparkJob`).

### Security
- mTLS between all services (enforced by Istio `PeerAuthentication` policy — `STRICT` mode).
- JWT validated at the API Gateway before forwarding. Services trust the forwarded identity header.
- All Docker images are non-root. Use distroless base images where possible.
- OPA policies stored in a dedicated Git repo, loaded into AuthPolicy Service at startup.
- Automated SAST via `spotbugs` + `semgrep` in CI.

### Testing Standards
- **Unit:** JUnit 5 + Mockito. Target ≥80% line coverage per service.
- **Integration:** `@SpringBootTest` + Testcontainers (PostgreSQL, Kafka, MinIO spun up in-process).
- **Contract Testing:** [Pact](https://docs.pact.io/) for REST and gRPC service contracts.
- **E2E:** Playwright against a local `docker-compose` environment.
- Tests MUST pass before any commit.

### Resilience Patterns
- **Circuit Breaker:** Resilience4j on all sync clients (open after 50% failure in 10s window).
- **Retry with backoff:** Exponential backoff + jitter, max 3 retries.
- **Bulkhead:** Separate thread pools for downstream calls.
- **Timeout:** All outbound calls have explicit timeouts. Default: 5s REST, 2s gRPC.
- **HPA:** Every K8s Deployment has a `HorizontalPodAutoscaler` (min: 2 pods, scale on CPU ≥70%).

---

## 📁 Repository & Module Structure

```
nebula/
├── workspace/                  # React micro-frontend portal
│   ├── shell/                  # Root shell app (React + Module Federation host)
│   ├── mfe-catalog/            # Catalog MFE remote
│   ├── mfe-build/              # Build Service MFE remote
│   └── ...
├── services/
│   ├── api-gateway/            # Spring Cloud Gateway
│   ├── catalog-service/
│   ├── dataset-service/
│   ├── build-service/
│   ├── scheduling-service/
│   ├── code-service/
│   ├── authpolicy-service/
│   ├── config-service/
│   └── notification-service/
├── infra/
│   ├── helm/                   # Helm charts (one per service + umbrella)
│   ├── k8s/                    # Raw K8s manifests (Istio policies, RBAC)
│   └── terraform/              # Cloud infra (if applicable)
├── schema-registry/            # Avro schemas for Kafka topics
├── opa-policies/               # OPA Rego policy files
└── docker-compose.yml          # Full local dev environment
```

Each `services/<name>/` follows:
```
<service>/
├── src/main/java/com/nebula/<service>/
│   ├── api/          # REST controllers or gRPC stubs
│   ├── domain/       # Entities, value objects, domain services
│   ├── application/  # Use cases / command handlers
│   ├── infrastructure/ # Repos, Kafka producers/consumers, external clients
│   └── config/       # Spring @Configuration classes
├── src/test/
├── build.gradle
└── Dockerfile
```

---

## 🔄 The Agile Loop (Strict Protocol)

1. **Plan Check:** Reference `PLAN.md` before every task. If a `PLAN.md` doesn't exist for the feature, create one first.
2. **Design First:** For any new service or cross-service feature, produce a short Architecture Decision Record (ADR) in `docs/adr/`.
3. **Code:** Write type-safe, documented, idiomatic Java 21 / TypeScript code. Follow the module structure above.
4. **Test:** Write unit + integration tests. Run the full test suite (`./gradlew test` for Java, `npm test` for UI). Tests MUST pass.
5. **Observe:** Ensure every new code path emits: (a) a structured log line, (b) an OTel span, and (c) a Micrometer metric counter/timer.
6. **Git:** Commit ONLY after tests pass. Format: `feat(scope): description` (Conventional Commits). Scope = service name (e.g., `feat(build-service): add spark job streaming`).
7. **Handoff:** Ask: "Step [X] complete. Proceed to [Y]?"
8. **Artifact Persistence:** All implementation plans, task lists, and verification walkthroughs MUST be saved directly into the repository under `docs/plans/` and `docs/walkthroughs/` for the corresponding phase (e.g., `docs/plans/phase-X-plan.md`, `docs/plans/phase-X-tasks.md`, `docs/walkthroughs/phase-X-walkthrough.md`). Do not leave them only in the agent's ephemeral memory.

---

## 🖥 UI / Workspace Standards

- **Aesthetic:** Dense, data-rich Palantir/Jira aesthetic using BlueprintJS components.
- **No inline styles.** Use CSS Modules or styled-components with a design token system.
- **Accessibility:** All interactive components must be keyboard-navigable and ARIA-labelled.
- **Real-time updates:** Job status, log streaming, dataset sync events use WebSocket subscriptions via a shared `useWebSocket` hook in the shell.
- **MFE Contract:** Each MFE remote MUST export a `mount(containerId, props)` function and handle its own routing within its mount point.
- **Error States:** Use `<NonIdealState>` for empty/error states. Use `<Spinner>` for loading. Use `<Toaster>` for transient notifications.

---

## ⚙️ Local Development Setup

```bash
# 1. Start all backing services (Kafka, PostgreSQL, MinIO, Keycloak, Jaeger, Grafana)
docker-compose up -d

# 2. Start a service (example)
cd services/catalog-service && ./gradlew bootRun

# 3. Start the Workspace shell
cd workspace/shell && npm install && npm run dev
```

All services must have a `docker-compose.override.yml` for local dev overrides (e.g. DEBUG logging, relaxed auth).