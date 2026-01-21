-- V1__init.sql
-- Phase 2: PostgreSQL schema initialization for KORI

-- -----------------------------
-- Enums as CHECK constraints (simple & portable)
-- -----------------------------

-- -----------------------------
-- Core reference tables
-- -----------------------------

CREATE TABLE IF NOT EXISTS agents (
    id              VARCHAR(64) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS merchants (
    id              VARCHAR(64) PRIMARY KEY,
    status          VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED'))
);

CREATE TABLE IF NOT EXISTS terminals (
    id              VARCHAR(64) PRIMARY KEY,
    merchant_id     VARCHAR(64) NOT NULL REFERENCES merchants(id)
);

-- -----------------------------
-- Clients / Accounts / Cards
-- -----------------------------

CREATE TABLE IF NOT EXISTS clients (
    id              UUID PRIMARY KEY,
    phone_number    VARCHAR(32) NOT NULL UNIQUE,
    status          VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED'))
);

CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL,

    CONSTRAINT fk_accounts_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id)
);

CREATE INDEX idx_accounts_client_id ON accounts(client_id);

CREATE TABLE IF NOT EXISTS cards (
    id                  UUID PRIMARY KEY,
    account_id          UUID NOT NULL REFERENCES accounts(id),
    card_uid            VARCHAR(128) NOT NULL UNIQUE,
    pin                 VARCHAR(64) NOT NULL,
    status              VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','SUSPENDED','LOST','BLOCKED','INACTIVE')),
    failed_pin_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_pin_attempts >= 0)
);

-- -----------------------------
-- Transactions
-- -----------------------------

CREATE TABLE IF NOT EXISTS transactions (
    id                      UUID PRIMARY KEY,
    type                    VARCHAR(64) NOT NULL,
    amount                  NUMERIC(19,2) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL,
    original_transaction_id UUID NULL
);

CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_transactions_original_tx ON transactions(original_transaction_id);

-- -----------------------------
-- Ledger (append-only)
-- -----------------------------

CREATE TABLE IF NOT EXISTS ledger_entries (
    id              UUID PRIMARY KEY,
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    account         VARCHAR(64) NOT NULL,
    entry_type      VARCHAR(16) NOT NULL CHECK (entry_type IN ('CREDIT','DEBIT')),
    amount          NUMERIC(19,2) NOT NULL,
    reference_id    VARCHAR(128) NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ledger_tx ON ledger_entries(transaction_id);
CREATE INDEX IF NOT EXISTS idx_ledger_account_ref ON ledger_entries(account, reference_id);
CREATE INDEX IF NOT EXISTS idx_ledger_created_at ON ledger_entries(created_at);

-- -----------------------------
-- Idempotency
-- -----------------------------

CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    result_type     VARCHAR(256) NOT NULL,
    result_json     TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -----------------------------
-- Audit events
-- -----------------------------

CREATE TABLE IF NOT EXISTS audit_events (
    id              UUID PRIMARY KEY,
    action          VARCHAR(128) NOT NULL,
    actor_type      VARCHAR(32) NOT NULL,
    actor_id        VARCHAR(128) NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    metadata_json   TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_occurred_at ON audit_events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_events(action);

-- -----------------------------
-- Payouts (Phase 1 fix: payout states)
-- (If you didn't add payout domain yet, this table is still fine to have)
-- -----------------------------

CREATE TABLE IF NOT EXISTS payouts (
    id              UUID PRIMARY KEY,
    agent_id        VARCHAR(64) NOT NULL REFERENCES agents(id),
    transaction_id  UUID NOT NULL UNIQUE REFERENCES transactions(id),
    amount          NUMERIC(19,2) NOT NULL,
    status          VARCHAR(32) NOT NULL CHECK (status IN ('REQUESTED','COMPLETED')),
    created_at      TIMESTAMPTZ NOT NULL,
    completed_at    TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_payouts_agent ON payouts(agent_id);

-- -----------------------------
-- Dynamic configuration (fees/commissions/security)
-- Stored in DB in Phase 2; admin endpoints come Phase 3.
-- Single-row tables (id=1) for simplicity.
-- -----------------------------

CREATE TABLE IF NOT EXISTS fee_config (
    id                              INTEGER PRIMARY KEY CHECK (id = 1),

    -- Card enrollment fixed price
    card_enrollment_price           NUMERIC(19,2) NOT NULL,

    -- Card payment fee: percent + min/max
    card_payment_fee_rate           NUMERIC(10,6) NOT NULL,
    card_payment_fee_min            NUMERIC(19,2) NOT NULL,
    card_payment_fee_max            NUMERIC(19,2) NOT NULL,

    -- Merchant withdraw fee: percent + min/max
    merchant_withdraw_fee_rate      NUMERIC(10,6) NOT NULL,
    merchant_withdraw_fee_min       NUMERIC(19,2) NOT NULL,
    merchant_withdraw_fee_max       NUMERIC(19,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS commission_config (
    id                                  INTEGER PRIMARY KEY CHECK (id = 1),

    -- Card enrollment agent commission: fixed
    card_enrollment_agent_commission    NUMERIC(19,2) NOT NULL,

    -- Merchant withdraw agent commission: percent of fee + min/max
    merchant_withdraw_commission_rate   NUMERIC(10,6) NOT NULL,
    merchant_withdraw_commission_min    NUMERIC(19,2) NULL,
    merchant_withdraw_commission_max    NUMERIC(19,2) NULL
);

CREATE TABLE IF NOT EXISTS security_config (
    id                          INTEGER PRIMARY KEY CHECK (id = 1),
    max_failed_pin_attempts     INTEGER NOT NULL CHECK (max_failed_pin_attempts > 0)
);

-- Seed defaults (safe placeholders; adjust as needed)
INSERT INTO fee_config (
    id,
    card_enrollment_price,
    card_payment_fee_rate, card_payment_fee_min, card_payment_fee_max,
    merchant_withdraw_fee_rate, merchant_withdraw_fee_min, merchant_withdraw_fee_max
)
VALUES (
    1,
    500.00,
    0.020000, 10.00, 500.00,
    0.010000, 10.00, 500.00
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO commission_config (
    id,
    card_enrollment_agent_commission,
    merchant_withdraw_commission_rate,
    merchant_withdraw_commission_min,
    merchant_withdraw_commission_max
)
VALUES (
    1,
    200.00,
    0.500000,
    NULL,
    NULL
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO security_config (
    id,
    max_failed_pin_attempts
)
VALUES (1, 3)
ON CONFLICT (id) DO NOTHING;
