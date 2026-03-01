# Phase 5 - Code Service & Browser IDE Implementation Plan

## Goal Description
Build the `code-service` to manage Git repositories and launch in-browser VS Code environments (`code-server`). Introduce the `mfe-code` micro-frontend so users can browse files and commit history directly in the Workspace UI.

## Proposed Changes

### 1. `code-service` (Backend Microservice)
- #### [NEW] `services/code-service/build.gradle`
  Spring Boot 3, Kafka, PostgreSQL, JGit, and Docker Java API client (for launching containers).
- #### [NEW] `services/code-service/src/main/resources/db/migration/V1__init_code_schema.sql`
  Stores repository metadata, branch states, and active IDE session mapping.
- #### [NEW] `services/code-service/src/main/java/com/nebula/code/domain/...`
  Domain entities for `Repository`, `Branch`, `Commit`, and `IdeSession`.
- #### [NEW] `services/code-service/src/main/java/com/nebula/code/service/GitManager.java`
  Uses JGit to initialize bare repositories on disk (`/var/nebula/git`), commit files, and retrieve history.
- #### [NEW] `services/code-service/src/main/java/com/nebula/code/service/IdeOrchestrator.java`
  Uses the Docker API to spin up `linuxserver/code-server` containers mapped to the repository path.
- #### [NEW] `services/code-service/src/main/java/com/nebula/code/api/RepositoryController.java`
  REST endpoints for repo CRUD, listing files, and getting commit logs.
- #### [NEW] `services/code-service/src/main/java/com/nebula/code/api/IdeController.java`
  Endpoints to request an IDE startup, get its status, and retrieve the forwarding URL.

### 2. Events & Registry
- #### [NEW] `schema-registry/RepositoryCreated.avsc`
  Avro schema to notify the `catalog-service` to create a new `Item` of type `REPOSITORY`.

### 3. API Gateway & Routing
- #### [MODIFY] `services/api-gateway/src/main/resources/application.yml`
  Add dynamic routing rules to forward `/ide/{sessionId}/**` traffic to the dynamically provisioned IDE containers.

### 4. `mfe-code` (Frontend Workspace)
- #### [NEW] `workspace/mfe-code/`
  Bootstrap a new React Webpack Module Federation remote.
- #### [NEW] `workspace/mfe-code/src/components/RepositoryBrowser.tsx`
  UI to display the Git file tree and commit history using BlueprintJS.
- #### [NEW] `workspace/mfe-code/src/components/IdeLauncher.tsx`
  "Open in IDE" button that triggers the backend to spin up the container and then redirects or opens an iframe to the IDE URL.
- #### [MODIFY] `workspace/shell/webpack.config.js`
  Register the new `mfe-code` remote so it can be mounted inside projects.

## Verification Plan

### Automated Tests
- Unit tests for `GitManager` ensuring JGit creates bare repos correctly.
- Integration test for `RepositoryController` verifying API responses and Kafka event emission.

### Manual Verification
1. Boot the platform with `make up`.
2. Ensure the Workspace React app loads.
3. Click "Create Repository" in a project.
4. Verify the repo appears in the Catalog.
5. Click "Open IDE".
6. Verify a new `code-server` Docker container spins up on the host.
7. Verify the browser successfully loads the VS Code UI via the API Gateway routing.
