CREATE TABLE repository (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    default_branch VARCHAR(100) DEFAULT 'main',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, name)
);

CREATE TABLE ide_session (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES repository(id) ON DELETE CASCADE,
    container_id VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL, -- e.g., STARTING, RUNNING, STOPPED
    last_accessed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ide_session_repo ON ide_session(repository_id);
