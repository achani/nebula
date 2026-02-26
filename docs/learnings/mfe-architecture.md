# Micro-Frontend (MFE) Architecture and Deployment Models

**Question:**
How does MFE architecture work? Should each MFE be implemented inside the backend service repository and hosted together with the rest of the service? If the MFE is implemented as a sub-directory inside the workspace folder, won't it have to be maintained separately and potentially get out of sync with the main service (e.g., schema changes)? What are the best practices?

---

**Answer:**

This touches on one of the most debated architectural decisions in Micro-Frontend (MFE) design: **Where should the frontend code live relative to the backend service?**

Storing the MFE *inside* the backend service repository (e.g., `services/catalog-service/src/main/ui`) is a common approach that directly addresses the sync issue. However, industry standards for large-scale platforms often favor decoupling them.

Let's break down how MFE architecture works, the two primary models for repository structure, and the best practices for preventing schema drift.

### How Micro-Frontend Architecture Works (Module Federation)

In a traditional Single Page Application (SPA), the entire frontend (Catalog UI, Build UI, Settings UI) is compiled into one massive JavaScript bundle at build time. If the Catalog team updates a button, the entire monolithic app must be rebuilt and redeployed.

**Micro-Frontends** solve this by splitting the frontend into independent, deployable units. We use **Webpack Module Federation** (the industry standard) to achieve this at runtime:

1. **The Host (Shell App):** This is the core frame of the application (in our case, `workspace/shell`). It handles global concerns: the top navigation bar, routing, authentication (Keycloak JWT), and the global layout.
2. **The Remotes (MFEs):** These are the individual feature UIs (e.g., `workspace/mfe-catalog`). They are built and deployed as completely independent web servers hosting static JS files.
3. **Runtime Stitching:** When a user navigates to `/projects` in the Shell app, the Shell dynamically fetches the compiled JavaScript chunk for `mfe-catalog` over the network and injects it into the DOM. The Shell app literally doesn't have the Catalog code at build time.

---

### Model 1: Backend-Coupled MFEs (Monolithic Repository per Domain)

In this model, the repository looks like this:
```text
services/catalog-service/
├── src/main/java/      # Spring Boot backend
├── src/main/ui/        # React MFE for Catalog
└── build.gradle        # Builds BOTH the Java app and compiles the React app
```
The React app gets bundled into the Spring Boot `.jar` (in the `src/main/resources/static` folder) and Spring Boot serves the HTML/JS directly.

**Pros:**
- **Perfect Synchronization:** A commit that changes the Java API schema (e.g., adding `description` to Project) identically changes the React UI in the exact same PR. They deploy together.
- **True Cross-Functional Teams:** A "Catalog Data Team" owns everything from the database tables up to the pixels on the screen.

**Cons:**
- **Build Times:** You have to run `npm install` and Node.js Webpack builds during your Java Gradle compile. It makes backend CI pipelines extremely slow.
- **Technology Mixing:** Backend devs hate dealing with Node.js artifacts in their Java builds, and Frontend devs hate booting up PostgreSQL and JVMs just to tweak CSS.
- **Scaling:** If you need 10 horizontal pods of the MFE for traffic spikes, you are forced to autoscale 10 heavy Java/JVM/Postgres-connected pods as well, wasting compute resources.

---

### Model 2: Dedicated Frontend Workspace (Nebula Platform Approach)

In the Nebula plan, we separated them by execution environments:
```text
services/
├── catalog-service/    # Pure Java/Spring (Deployment A)
workspace/
├── shell/              # Pure React (Deployment B)
├── mfe-catalog/        # Pure React (Deployment C)
```
In this model, the MFEs are served by lightweight Nginx web servers (or CloudFront/S3 in production), completely decoupled from the Spring Boot JVMs.

**Pros:**
- **Independent Autoscaling:** The frontend assets can be cached globally on CDNs. The backend only scales based on actual API traffic.
- **Developer Experience (DX):** Frontend engineers can work entirely inside the `workspace/` folder running `npm run dev` and mock the APIs. Backend engineers never have to look at `package.json`.
- **Decoupled Deployments:** If you just want to fix a typo on the Catalog UI button, you build and deploy `mfe-catalog`. You don't have to restart the Java backend and drop active database connections.

**Cons (The Synchronization Gotcha):**
- **Schema Drift:** If the `catalog-service` team changes an API payload, the `mfe-catalog` team must coordinate to update their types, otherwise the UI breaks at runtime.

### Best Practices to Prevent Drift in Model 2

Since industry practice heavily favors **Model 2** (Dedicated Frontend Workspace) for performance and dev-experience reasons, we mitigate the schema drift issue using **Contract-Driven Development**:

1. **OpenAPI / Swagger Generation:** The Spring Boot backend automatically generates a `swagger.json` file.
2. **TypeScript Generation:** The `workspace/mfe-catalog` runs a code-generator (like `openapi-typescript-codegen`) that points to the backend API and generates strict TypeScript interfaces. If the backend changes a field name, the React build fails instantly in CI, catching the drift before deployment.
3. **Pact Contract Testing:** We write tests asserting exactly what JSON the MFE expects. If the backend accidentally breaks the contract, the **backend's** CI pipeline fails before it is allowed to merge.

By relying on strict contracts and OpenAPI generation, we get the best of both worlds: decoupling the heavy JVMs from the lightweight UI, while still enforcing schema consistency!
