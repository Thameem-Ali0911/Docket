-- Phase 10: LLM usage tracking & per-workspace budget guard
-- Tracks per-workspace LLM call counts (rolling daily window) for denial-of-wallet protection.
-- Adds a human_corrected_json column to extractions so corrections don't overwrite original AI output.

-- Track per-workspace daily LLM invocation counts
CREATE TABLE IF NOT EXISTS llm_usage (
    id              BIGSERIAL    PRIMARY KEY,
    workspace_id    INTEGER      NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    usage_date      DATE         NOT NULL DEFAULT CURRENT_DATE,
    call_count      INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uq_llm_usage_workspace_date UNIQUE (workspace_id, usage_date)
);

CREATE INDEX IF NOT EXISTS idx_llm_usage_workspace_date
    ON llm_usage (workspace_id, usage_date);

-- Add human-correction column to extractions (preserves original AI output)
ALTER TABLE extractions
    ADD COLUMN IF NOT EXISTS human_corrected_json  TEXT,
    ADD COLUMN IF NOT EXISTS correction_note        VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS corrected_at           TIMESTAMPTZ;

-- Add daily budget cap column to workspaces (default 100 LLM calls/day)
ALTER TABLE workspaces
    ADD COLUMN IF NOT EXISTS daily_llm_budget INTEGER NOT NULL DEFAULT 100;
