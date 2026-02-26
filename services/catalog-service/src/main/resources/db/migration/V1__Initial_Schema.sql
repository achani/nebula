CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (name)
);

CREATE TABLE catalog_items (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES catalog_items(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Ensure a project cannot have two items with the same name at the root
CREATE UNIQUE INDEX idx_root_catalog_item_name 
ON catalog_items(project_id, name) 
WHERE parent_id IS NULL;

-- Ensure a folder cannot have two children with the same name
CREATE UNIQUE INDEX idx_child_catalog_item_name 
ON catalog_items(parent_id, name) 
WHERE parent_id IS NOT NULL;

CREATE TABLE folders (
    id UUID PRIMARY KEY REFERENCES catalog_items(id) ON DELETE CASCADE
);
