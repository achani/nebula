# ADR-004: Centralized Authorization via Open Policy Agent (OPA)

## Status
Accepted

## Context
Nebula is designed as a highly distributed data platform with many discrete microservices (Catalog, Dataset, Build, Code, etc.). 

Each of these services needs to make authorization decisions based on the authenticated user's roles and the specific resource they are trying to access. For example:
- Can this user delete the project? (Only Owners)
- Can this user view the dataset inside this folder? (Viewers, Editors, Owners)
- Can this user submit a Spark build job? (Editors, Owners)

### Options Considered

**Option 1: Embedded Spring Security (Method Security)**
- *Approach:* Apply `@PreAuthorize("hasRole('PROJECT_OWNER')")` on individual Controller or Service methods within each Java microservice.
- *Pros:* Extremely easy to implement initially. Built-in to Spring Boot. No network overhead.
- *Cons:* Policy logic becomes deeply coupled to application code. To change a permission, you must recompile and redeploy the specific service. Cross-service audits (e.g., "what can user X do across the platform?") are nearly impossible without reading source code across 10 repositories.

**Option 2: Centralized AuthZ API with custom business logic**
- *Approach:* Build an `authz-service` that exposes REST/gRPC endpoints. Other services ask `can_user_access(userId, resourceId)`.
- *Pros:* Centralizes the logic. Easy to audit. Code is in one place.
- *Cons:* Writing a custom, high-performance rule engine from scratch is difficult and error-prone.

**Option 3: Open Policy Agent (OPA)**
- *Approach:* Use the CNCF-graduated open-source project OPA. Policies are written in a declarative language called **Rego**. The `authpolicy-service` acts as a facade, evaluating Rego files on incoming gRPC queries.
- *Pros:* 
  - Standardized, industry-proven policy engine.
  - Policies are data, not code. We can change permissions on the fly simply by updating the Rego text files without recompiling or redeploying the Java services.
  - Rego allows for extremely complex, data-driven rules (e.g., RBAC, ABAC).
- *Cons:* Learning curve for the Rego language. Slight network overhead for gRPC calls (though OPA evaluates rules in microseconds).

## Decision
We will use **Option 3: Open Policy Agent (OPA)**. 

To maintain the Java ecosystem and enforce strict typing, we will build a Spring Boot 3 microservice (`authpolicy-service`) that embeds the OPA engine (or runs it as a sidecar) and exposes a fast, strongly-typed gRPC API: `AuthorizeRequest` → `AuthorizeResponse`.

**All downstream services (Catalog, Dataset, Build) MUST call the `authpolicy-service` via gRPC before executing any non-idempotent action.** No service is allowed to implement its own bespoke authorization logic.

## Consequences
- **Positive:** Policies can be managed in a dedicated `opa-policies/` Git repository and dynamically loaded onto the platform in real-time. Extremely high auditability.
- **Negative:** We must engineer the gRPC calls in the hot-path to be performant, likely requiring local caching (via Caffeine or Redis) or connection pooling to offset the network hop.
- **Dependencies:** We must introduce `opa` to the local `docker-compose.yml` and Helm charts.
