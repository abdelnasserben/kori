CREATE INDEX IF NOT EXISTS idx_transactions_created_id_desc ON transactions(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_occurred_id_desc ON audit_events(occurred_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_agents_created_id_desc ON agents(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_clients_created_id_desc ON clients(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_merchants_created_id_desc ON merchants(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_tx_account_type ON ledger_entries(transaction_id, account_type);