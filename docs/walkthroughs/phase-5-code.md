# Phase 5 Walkthrough: Code Service & Browser IDE

Phase 5 introduces the **Code Service** and **Workspace Frontend (`mfe-code`)**, bringing an embedded Git lifecycle and browser-based IDE (code-server) into the Nebula platform.

## What was Accomplished

### 1. Code Service (`backend`)
- **Domain Models & Database:** Implemented `Repository` and `IdeSession` entities, backed by PostgreSQL migrations (`V1__init_code_schema.sql`).
- **JGit Integration:** Added `GitManager` to dynamically initialize bare Git repositories locally or in object storage when a user creates a project repository.
- **IDE Orchestrator:** Implemented `IdeOrchestrator` using the Docker API (`docker-java`) to provision fully-isolated, ephemeral `linuxserver/code-server` containers per repository.
- **REST APIs:** Added `RepositoryController` to manage the lifecycle of repos, browse files (via JGit tree walks), and launch/stop IDE sessions.
- **API Gateway Routing:** Configured Spring Cloud Gateway to proxy `/api/repos/**` traffic to the Code Service, and dynamically proxy `/ide/**` traffic down to the running code-server instances.
- **Kafka Events:** Emits Avro `RepositoryCreated` messages to the `code.events` topic so the Catalog can eventually track repositories.
- **Testcontainers Integration:** Validated context startup, Kafka interactions, and dependent PostgreSQL infrastructure using local embedded infrastructure.

### 2. Workspace Frontend (`mfe-code` & `shell`)
- **Vite Module Federation:** Bootstrapped the `shell` host application and `mfe-code` remote module using `@originjs/vite-plugin-federation`.
- **Shell Layout:** Designed the primary navigation app shell utilizing `BlueprintJS` dark-themed components, mimicking Palantir Foundry UX.
- **CodeApp Module:** Deployed isolated `RepositoryBrowser` and `IdeLauncher` components inside the remote front-end.
- **Seamless Integrations:** React Router dynamically injects the MFE into the `/repos/*` subpath, launching `code-server` in an isolated nested iframe wrapper.

## How to Test

### 1. Start Support Infrastructure
```bash
docker-compose up -d postgres kafka zookeeper api-gateway config-service
```

### 2. Start Code Service
```bash
cd services/code-service && ./gradlew bootRun
```

### 3. Start Workspace Shell & Remote
Ensure both Vite instances are running concurrently in separate terminals:
```bash
cd workspace/mfe-code && npm run dev -- --port 3001
cd workspace/shell && npm run dev -- --port 3000
```
Visit http://localhost:3000 in your browser. You will see the Nebula Foundry landing page. Click **Open Code Workspace** to trigger the module federation lazy-load and access the repository listing.

## Hardening & Resilience (Stabilization Phase)

After initial deployment, several bugs were identified and fixed to ensure production-grade stability:

### 1. Backend Connectivity & Orchestration
- **Docker Network Alignment:** Fixed `DOCKER_NETWORK` environment variable and host configuration to correctly map service internal names to the `foundry_nebula-net` bridge, resolving 500 errors during container provisioning.
- **Stale Session Handling:** Implemented cleanup logic and manual database purging for stale "RUNNING" sessions that lacked corresponding Docker containers.

### 2. Session & Auth Hardening
- **Wait-for-Recovery:** Added diagnostic logging to `RepositoryController` to track request flow.
- **Global 401 Interceptors:** Configured `QueryClient` in both Shell and Code MFE to automatically detect expired JWT tokens, clear local storage, and redirect the user to login.

### 3. Git Workspace Support (Worktrees)
- **Non-bare Workspaces:** Migrated from mounting bare repositories to mounting full Git **Workspaces** (working trees). This ensures VS Code's Source Control view recognizes the repository metadata.
- **Host Path Mapping:** Implemented absolute path translation between the `code-service` container and the Docker host to support reliable bind mounts.
- **Git Security Warnings:** Resolved the "Unsafe Repository" warning by injecting `GIT_SAFE_DIRECTORIES` into IDE containers, handling UID ownership mismatches between host and container.

### 4. UX Improvements
- **UI Layout Fixes:** Resolved CSS encapsulation issues where the Shell layout was restricted; implemented explicit width/height constraints and cleaned up stale BlueprintJS global overrides.
- **IDE Feedback:** Restored password hint UI in the IDE launcher for better user guidance.

![Git Source Control recognized in IDE](/Users/ajay/.gemini/antigravity/brain/a2c294cc-6467-4093-af78-41f6cb21eb65/ide_git_verification_1772369757895.png)
