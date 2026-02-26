CREATE TABLE dataset (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    format VARCHAR(50) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, name)
);

CREATE TABLE dataset_version (
    id VARCHAR(36) PRIMARY KEY,
    dataset_id VARCHAR(36) NOT NULL REFERENCES dataset(id) ON DELETE CASCADE,
    version_number BIGINT NOT NULL,
    commit_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    operation VARCHAR(255),
    operation_parameters JSONB,
    UNIQUE(dataset_id, version_number)
);

CREATE TABLE schema_snapshot (
    id VARCHAR(36) PRIMARY KEY,
    dataset_id VARCHAR(36) NOT NULL REFERENCES dataset(id) ON DELETE CASCADE,
    version_number BIGINT NOT NULL,
    schema_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(dataset_id, version_number)
);
