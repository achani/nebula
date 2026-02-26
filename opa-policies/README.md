# Open Policy Agent (OPA) — Nebula AuthPolicy

Nebula uses a centralized Authorization service (`AuthPolicy Service`). Individual microservices **do not** embed their own authorization logic. Instead, they make a gRPC or REST call to AuthPolicy, which evaluates **Rego** policies against the user's identity and the requested resource.

## Conventions

1. **File Structure:** `.rego` files should be organized by domain (e.g., `catalog.rego`, `dataset.rego`).
2. **Package Name:** Use `nebula.authz.<domain>`.
3. **Default Rule:** Every policy must define a `default allow = false`.
4. **Input:** The `input` document sent by services will always contain:
   - `input.user.id`
   - `input.user.roles` (from Keycloak)
   - `input.action` (e.g., `read`, `write`, `delete`)
   - `input.resource.type` (e.g., `project`, `dataset`)
   - `input.resource.id`
5. **Reloading:** The AuthPolicy service will watch this git folder (or be pushed to via CI) and hot-reload policies without restarting.

*This folder will be populated starting in Phase 3.*
