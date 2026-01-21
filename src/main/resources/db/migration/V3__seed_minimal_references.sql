-- --------------------------------------------------
-- Minimal reference data for integration tests
-- --------------------------------------------------

-- Agent
INSERT INTO agents (id)
VALUES ('AGENT_001')
ON CONFLICT (id) DO NOTHING;

-- Merchant (ACTIVE)
INSERT INTO merchants (id, status)
VALUES ('MERCHANT_001', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- Terminal linked to merchant
INSERT INTO terminals (id, merchant_id)
VALUES ('TERMINAL_001', 'MERCHANT_001')
ON CONFLICT (id) DO NOTHING;
