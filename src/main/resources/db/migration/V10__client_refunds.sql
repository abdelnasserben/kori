CREATE TABLE client_refunds (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL,
    transaction_id UUID NOT NULL UNIQUE,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_reason VARCHAR(255)
);

CREATE INDEX idx_client_refunds_client ON client_refunds(client_id);
CREATE INDEX idx_client_refunds_status ON client_refunds(status);