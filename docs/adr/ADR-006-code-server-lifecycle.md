# ADR 006: Code-Server Session Lifecycle

## Status
Proposed

## Context
In Phase 5, Nebula introduces the **Code Service**, which allows users to view Git repositories and edit code directly in the browser. To achieve a powerful IDE experience, we are integrating [code-server](https://github.com/coder/code-server) (VS Code in the browser).

A critical design decision is how to manage the lifecycle of these `code-server` instances.

Should a session be:
1. **Per-User:** A single persistent IDE container per user, into which multiple repositories can be cloned.
2. **Per-Repository:** An ephemeral IDE container launched specifically for one repository, spun down when idle.
3. **Per-Workspace (Palantir Foundry style):** A combination where a user launches a compute session tied to a specific branch/project.

## Decision
We will use a **Per-Repository (Ephemeral) Session Lifecycle**.

1. When a user clicks "Open in IDE" on a Repository in the Catalog, the `code-service` uses the Docker API (or fabric client) to spin up a new container running `linuxserver/code-server`.
2. The container is injected with the specific Git repository volume.
3. The `code-service` acts as a reverse proxy (or instructs the API Gateway) to route the user's browser traffic to that specific container instance.
4. **Idle Timeout:** If no activity is detected for 30 minutes, the container is killed. The Git volume persists.

## Rationale
- **Isolation:** Each repository defines its own environment. Dependencies for a Spark Scala job won't conflict with a React MFE project.
- **Resource Efficiency:** We don't need to keep heavy IDE JVMs running for all users 24/7. They spin up on demand and spin down when idle.
- **Simplicity of MVP:** Mapping 1 Repository to 1 Container is easier to orchestrate locally via Docker API than managing multi-tenant user volumes.
- **Alignment with K8s:** This maps perfectly to Kubernetes `Pods` in the future.

## Consequences
- The `code-service` must have permission to manage Docker containers (or K8s pods).
- We need a mechanism to map dynamic routing (e.g., `/ide/<repoId>`) to the correct backend container dynamically in the API Gateway.
- Startup time for the IDE will face a "cold start" delay of ~2-5 seconds.
