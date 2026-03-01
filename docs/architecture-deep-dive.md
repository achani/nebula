# Nebula Platform Architecture Deep-Dive

Nebula is a distributed, microservices-first data analytics platform. This document explains how the components interact to deliver the workspace and IDE experience.

---

## 1. Frontend: The Micro-Frontend (MFE) Architecture

Nebula uses a **Host-Remote** architecture based on **Vite Module Federation**.

- **Workspace Shell (Host)**: Located in `workspace/shell`. It is the main entry point (`localhost:5173`).
  - **AuthN**: Handles the initial login via Keycloak and stores the JWT in `localStorage`.
  - **Layout**: Provides the global Navbar, Sidebar, and Toast notifications (using BlueprintJS).
  - **Routing**: Orchestrates high-level routes (`/`, `/repos`, `/datasets`).
- **Code MFE (Remote)**: Located in `workspace/mfe-code`.
  - **Isolation**: Exposes `CodeApp.tsx` which is dynamically loaded by the Shell only when the user visits `/repos`.
  - **Communication**: Shares state (like Auth tokens) with the Shell but manages its own internal data fetching (React Query).

---

## 2. API Ingress & Routing

All frontend requests directed at `/api/**` or `/ide/**` are sent to the **API Gateway** (`localhost:8090`).

- **Spring Cloud Gateway**:
  - **Routing**: Maps `/api/repos/**` -> `code-service`.
  - **IDE Routing**: Dynamically maps `/ide/{containerId}/**` to the specific port of a running IDE container.
  - **Identity**: Forwards the `Authorization: Bearer <JWT>` header to downstream services.

---

## 3. Backend: The Code Service

The `code-service` is responsible for repository management and IDE orchestration.

- **Repository Management**:
  - Uses **JGit** to manage bare Git repositories on disk (`/var/nebula/git`).
  - Stores metadata (project IDs, names) in **PostgreSQL** (`code_db`).
- **IDE Orchestration (`IdeOrchestrator`)**:
  - Connects to the host's **Docker Socket** via `docker-java`.
  - **On-Demand Provisioning**: When "Launch IDE" is clicked, it pulls `linuxserver/code-server`, creates a container, and mounts the repository's Git directory into the container's workspace.
  - **Networking**: Containers are attached to the `foundry_nebula-net` bridge network so they can communicate with the platform.
- **Event-Driven Integration**:
  - After a repository is created, it publishes a `RepositoryCreated` event to **Kafka**.
  - Other services (like the Catalog Service) consume this event to update their logical project views.

---

## 4. Security & Authorization

Nebula employs a **Zero Trust** security model.

- **Authentication (AuthN)**: Handled by **Keycloak** (OIDC). Tokens are signed JWTs containing user roles.
- **Authorization (AuthZ)**:
  - **AuthPolicy Service**: A dedicated service that wraps **Open Policy Agent (OPA)**.
  - **gRPC Interface**: services call `AuthPolicyService.authorize()` over gRPC for low-latency permission checks.
  - **RBAC Policy**: Defined in `rbac.rego` (Rego language). For example, it checks if a user has the `edit` permission on a specific `projectId` before allowing an IDE launch.

---

## 5. Summary of the "IDE Launch" Flow

1.  **Browser**: User clicks "Launch IDE".
2.  **Code MFE**: Sends `POST /api/repos/{id}/ide/launch` with JWT.
3.  **Gateway**: Routes request to `code-service`.
4.  **Code Service**:
    - Validates JWT.
    - Calls `authpolicy-service` (gRPC) to check permissions in OPA.
    - `IdeOrchestrator` starts a docker container.
    - Returns a `proxyUrl` (e.g., `/ide/ide-abc-123/`).
5.  **Browser**: Renders an `iframe` pointing to the `proxyUrl`.
6.  **Gateway**: Routes `iframe` requests directly to the new container's internal port.
