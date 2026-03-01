# ADR-007: IDE Stabilization & Git Workspace Integration

After the initial release of the Code Service (Phase 5), several regressions and architectural gaps were identified regarding IDE connectivity and Git integration.

## Problem Statement
1. **Connectivity Hijacking:** Services were unable to provision containers due to Docker network name mismatches between development environments.
2. **Session Persistence Errors:** Restoring sessions after service restarts failed due to "RUNNING" state leaks in the database without corresponding physical containers.
3. **Git Recognition:** Mounting bare repositories directly into the IDE iframe prevented VS Code's Source Control from functioning, as it lacked a working tree.
4. **Security Friction:** Ownership mismatches between the backend (root) and the IDE user (abc) triggered "Unsafe Repository" warnings.

## Architectural Decision

### 1. Unified Network Orchestration
Every IDE container now joins the `foundry_nebula-net` bridge explicitly. The `DOCKER_NETWORK` environment variable in `docker-compose.yml` serves as the source of truth for the `IdeOrchestrator`.

### 2. Workspace vs. Repository Separation
The `GitManager` now manages two distinct directories for every repository:
- `{id}.git`: The canonical bare repository (storage).
- `{id}-workspace`: A non-bare working tree checkout for the IDE (active development).

### 3. Host Path Translation
To support Docker-in-Docker or host-to-container bind mounts, the `code-service` uses `HOST_GIT_STORAGE_PATH` to translate its internal `/var/nebula/git` paths into absolute host paths before calling the Docker API.

### 4. Git Security Context
IDE containers are provisioned with `GIT_SAFE_DIRECTORIES=/config/workspace` to bypass ownership checks inside the container, ensuring a seamless "Ready to Code" experience.

## Status: Implemented & Verified
Implemented in `services/code-service` and `workspace/mfe-code` following the Stabilization Sprint (2026-03-01).
