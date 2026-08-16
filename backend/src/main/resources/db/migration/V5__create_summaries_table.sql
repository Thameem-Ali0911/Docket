CREATE TABLE summaries (
    id BIGSERIAL PRIMARY KEY,
    document_id INTEGER NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    summary_text TEXT,
    failed_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_summaries_document_id ON summaries(document_id);
