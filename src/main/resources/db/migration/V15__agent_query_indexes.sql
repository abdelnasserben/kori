CREATE INDEX IF NOT EXISTS idx_ledger_entries_agent_owner_tx
    ON ledger_entries (owner_ref, account_type, transaction_id);

CREATE INDEX IF NOT EXISTS idx_audit_events_actor_occurred_id
    ON audit_events (actor_type, actor_id, occurred_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_clients_phone_number
    ON clients (phone_number);

CREATE INDEX IF NOT EXISTS idx_cards_card_uid
    ON cards (card_uid);

CREATE INDEX IF NOT EXISTS idx_terminals_id_text
    ON terminals ((id::text));