CREATE TABLE IF NOT EXISTS documents (
    id SERIAL PRIMARY KEY,
    workspace_id INTEGER NOT NULL REFERENCES workspaces(id),
    type VARCHAR(50) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
