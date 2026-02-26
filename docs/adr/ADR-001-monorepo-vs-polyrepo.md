# ADR 001: Monorepo vs Polyrepo for Nebula

**Date:** 2026-02-26  
**Status:** Accepted  

## Context
Nebula is a microservices-based platform comprising ~10 backend Spring Boot services and a React Micro-Frontend workspace. We need to decide how to version and store this source code.

## Decision
We will use a **Monorepo** approach for the entire Nebula platform. All microservices, frontend applications, infrastructure code (Helm, K8s, docker-compose), and shared assets (Avro schemas, OPA policies) will live in a single Git repository.

## Rationale
1. **Simplified Local Development:** A developer only needs to clone one repository to run `make up` and have the entire stack working locally.
2. **Cross-Service Refactoring:** Changes to shared contracts (e.g., updating an Avro schema in `schema-registry/` and updating the Producer/Consumer services) can happen in a single atomic Git commit spanning multiple services.
3. **Unified CI/CD:** One GitHub Actions pipeline can orchestrate the build matrix for all services, ensuring the entire stack compiles together cleanly.
4. **Learning Context:** For a project designed to teach distributed architecture, having all code co-located lowers the cognitive load of navigating between repositories.

## Consequences
- **Positive:** Easier dependency management, synchronized releases across services, and atomic cross-cutting changes.
- **Negative:** The repository will grow large over time. The CI build times will increase unless we implement sparse checkouts or intelligent build caching (e.g., Gradle Build Cache, Turborepo for JS).
- **Mitigation:** We will ensure `docker-compose.dev-lite.yml` remains viable for lower-spec laptops even as the repo grows.
