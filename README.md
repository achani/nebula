# Nebula — Distributed Data Analytics Platform

> Inspired by Palantir Foundry. Built for learning. Engineered for scale.

Nebula is a microservices-first, event-driven data analytics platform. User data is stored in **Delta Lake** (on MinIO), and platform metadata is managed in isolated **PostgreSQL** schemas per service. All asynchronous communication flows through **Kafka**.

## Prerequisites for Local Development

To run the full stack locally, you need:
- **Docker** and **Docker Compose** (Docker Desktop ≥24)
- **RAM:** Minimum 16 GB for the full stack (8 GB for `dev-lite`)
- **Make:** standard `make` utility
- **mc CLI:** MinIO client for browsing local object storage
- **jq:** Command-line JSON processor (for local scripts)

## Quick Start
Our goal is "one command to bootstrap everything." 

To bring up the entire infrastructure stack (PostgreSQL, Kafka, Schema Registry, MinIO, Keycloak, Jaeger, Prometheus, Grafana, Loki, Vault, and local Spark):

```bash
make up
```

Wait ~30 seconds for all containers to report health. Then you can access the core infrastructure:

| Component | URL | Credentials / Notes |
|---|---|---|
| **Keycloak** | http://localhost:8080 | `admin / admin` |
| **Jaeger UI** | http://localhost:16686 | No auth |
| **MinIO Console** | http://localhost:9001 | `minioadmin / minioadmin` |
| **Grafana** | http://localhost:3001 | `admin / admin` |
| **Prometheus** | http://localhost:9090 | No auth |
| **Spark UI** | http://localhost:8082 | No auth |

## Useful `make` Targets

- `make up`: Starts the full docker-compose stack in the background.
- `make lite`: Starts a memory-optimized compose stack (removes Vault, Loki, Spark Worker). Ideal for 8GB laptops.
- `make down`: Stops all containers.
- `make reset`: **DELETES ALL DATA volumes** and restarts the stack fresh.
- `make status`: Shows the health status of all containers.
- `make logs`: Tails logs for all infra containers.

## Repository Structure

```
nebula/
├── services/           # Backend Spring Boot microservices
├── workspace/          # Frontend React micro-frontend shell and remotes
├── schema-registry/    # Avro schemas for Kafka events
├── opa-policies/       # Open Policy Agent (Rego) authorization policies
├── config/             # Spring Cloud Config backing git repository
├── infra/              # Local dev config, Helm charts, K8s manifests
└── docs/               # Architecture Decision Records (ADRs) and notes
```

## Architectural Guidelines
See `GEMINI.md` for the comprehensive platform rules, tech stack, and microservice definitions.
See `PLAN.md` for the incremental multi-phase implementation plan.
