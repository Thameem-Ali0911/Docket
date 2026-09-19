-- Phase 12.6: Billing Simulation & Stripe Test Mode
-- Adds subscription plan tier, status, simulated Stripe IDs, and billing period to workspaces.
-- Creates simulated_invoices table to record test mode billing events and receipts.

-- Add billing/subscription columns to workspaces
ALTER TABLE workspaces
    ADD COLUMN IF NOT EXISTS plan_tier              VARCHAR(30)  NOT NULL DEFAULT 'FREE',
    ADD COLUMN IF NOT EXISTS subscription_status    VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS stripe_customer_id     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS billing_period_start   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS billing_period_end     TIMESTAMPTZ  NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days');

-- Create simulated_invoices table
CREATE TABLE IF NOT EXISTS simulated_invoices (
    id              BIGSERIAL    PRIMARY KEY,
    workspace_id    INTEGER      NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    invoice_number  VARCHAR(50)  NOT NULL UNIQUE,
    amount_cents    INTEGER      NOT NULL DEFAULT 0,
    currency        VARCHAR(10)  NOT NULL DEFAULT 'USD',
    status          VARCHAR(30)  NOT NULL DEFAULT 'PAID',
    description     VARCHAR(255),
    period_start    TIMESTAMPTZ,
    period_end      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_simulated_invoices_workspace
    ON simulated_invoices (workspace_id, created_at DESC);
