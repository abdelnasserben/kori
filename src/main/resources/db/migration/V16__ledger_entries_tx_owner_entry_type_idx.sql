CREATE INDEX IF NOT EXISTS idx_ledger_tx_account_owner_entry_type
    ON ledger_entries (transaction_id, account_type, owner_ref, entry_type);