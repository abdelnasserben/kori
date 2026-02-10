CREATE INDEX IF NOT EXISTS idx_ledger_entries_client_owner_tx
    ON ledger_entries (account_type, owner_ref, transaction_id);

CREATE INDEX IF NOT EXISTS idx_ledger_entries_merchant_owner_tx
    ON ledger_entries (account_type, owner_ref, transaction_id);

CREATE INDEX IF NOT EXISTS idx_transactions_created_at_id
    ON transactions (created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_terminals_merchant_status_created_id
    ON terminals (merchant_id, status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_terminals_merchant_created_id
    ON terminals (merchant_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_cards_client_created_id
    ON cards (client_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_audit_events_terminal_last_seen
    ON audit_events (actor_type, actor_id, occurred_at DESC);