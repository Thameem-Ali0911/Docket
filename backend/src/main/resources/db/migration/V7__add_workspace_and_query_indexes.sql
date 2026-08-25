-- Migration V7: Add missing indexes on documents, users, and foreign key query paths
-- Closes performance bottleneck identified in architectural evaluation

CREATE INDEX IF NOT EXISTS idx_documents_workspace_id ON documents(workspace_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_workspace_uploaded ON documents(workspace_id, uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_workspace_id ON users(workspace_id);
