CREATE INDEX IF NOT EXISTS idx_audit_events_meta_transaction_id
    ON audit_events (((metadata_json::jsonb ->> 'transactionId')));

CREATE INDEX IF NOT EXISTS idx_audit_events_meta_terminal_uid
    ON audit_events (((metadata_json::jsonb ->> 'terminalUid')));

CREATE INDEX IF NOT EXISTS idx_audit_events_meta_card_uid
    ON audit_events (((metadata_json::jsonb ->> 'cardUid')));

CREATE INDEX IF NOT EXISTS idx_clients_phone_number
    ON clients (phone_number);