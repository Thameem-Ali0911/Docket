CREATE TABLE templates (
    id BIGSERIAL PRIMARY KEY,
    workspace_id INTEGER NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    document_id INTEGER NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(workspace_id, document_type)
);

CREATE TABLE anomaly_flags (
    id BIGSERIAL PRIMARY KEY,
    document_id INTEGER NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    field_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_anomaly_flags_document_id ON anomaly_flags(document_id);
