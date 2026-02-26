# ADR-003: Polymorphic Catalog Item Data Model

## Status
Accepted

## Context
The Nebula platform needs a centralized `catalog-service` to manage the logical hierarchy of all data assets.
The foundational unit of organization is a `Project`. Within a project, users can create `Folders`. Inside folders (or at the project root), they can create datasets, code repositories, dashboards, and potentially other future resource types.

We need a flexible data model that allows:
1. Building a unified "tree view" of a Project's contents (folders and their children) efficiently.
2. Managing access control (RBAC) hierarchically (e.g., granting read access to a folder implies access to its contents).
3. Easily adding new types of assets in the future (e.g., ML Models) without completely redesigning the core catalog schema.

## Options Considered

### 1. Separate Tables per Entity Type
`folders` table, `datasets` table, `repositories` table, etc., all carrying a `project_id` and `parent_folder_id`.
* **Pros:** Strongly typed schemas for each entity type. Simple to understand in isolation.
* **Cons:** Extremely difficult to query a unified "tree" view of a folder's mixed contents. E.g., `SELECT * FROM folders UNION SELECT * FROM datasets UNION...`. Pagination and sorting across types become a nightmare. Global uniqueness of IDs is harder to enforce without UUIDs.

### 2. Single Table Inheritance (STI)
A single `catalog_items` table containing columns for *every* possible attribute of every type, with a `type` discriminator column (e.g., `FOLDER`, `DATASET`, `REPOSITORY`).
* **Pros:** Trivial to query tree hierarchies (`SELECT * FROM catalog_items WHERE parent_id = ?`). Simple to enforce global ID uniqueness.
* **Cons:** Schema becomes bloated with sparse columns (many NULLs). Cannot easily enforce NOT NULL constraints on type-specific fields at the database level.

### 3. Class Table Inheritance (CTI) / Polymorphic Base Table
A base `catalog_items` table holding common metadata (ID, `project_id`, `parent_id`, `name`, `type`, `created_at`, `updated_at`). 
Specific entity tables (`datasets`, `repositories`) carry a foreign key back to the `catalog_items` table (usually using the same ID as the primary key).
* **Pros:** 
  - Easy to build tree views by querying just the `catalog_items` table.
  - Strongly typed specific tables (e.g., `datasets` can have `format` strictly NOT NULL).
  - Clean schema separation.
  - Global ID uniqueness enforced by the base table.
* **Cons:** Requires JOINs to fetch full entity details.

## Decision
We will use **Option 3: Class Table Inheritance (Polymorphic Base Table)**.

The `catalog_items` table will act as the canonical namespace and hierarchy manager. It will define the tree structure via a self-referencing `parent_id` and establish ownership via `project_id`.

Every asset—whether it's a `Folder`, `Dataset`, or `Repository`—will first have an entry in `catalog_items`. If the item requires extended metadata (like `format` for a Dataset), a corresponding row will exist in a specialized table (e.g., `datasets`) linking back to the `catalog_items.id`.

### Schematic Overview:
* `projects` (id, name, description)
* `catalog_items` (id, project_id (FK), parent_id (FK, self), name, type (ENUM), created_by, created_at)
  * `Folder` is simply a `catalog_item` where `type = 'FOLDER'`. It requires no extended table initially.
  * *Future:* `datasets` (id (FK to catalog_items), format, external_path, schema_version)
  * *Future:* `repositories` (id (FK to catalog_items), git_url, default_branch)

## Consequences
* **Positive:** Tree rendering in the Workspace MFE will be highly performant, requiring a simple query against `catalog_items`.
* **Positive:** Adding a new entity type in the future just requires adding a new ENUM value to `type` and creating an optional extension table.
* **Negative:** Creating a complex entity (like a Dataset in Phase 4) will require coordinating inserts across two tables (`catalog_items` and `datasets`). We must rely on database transactions (Spring `@Transactional`) to ensure consistency.
