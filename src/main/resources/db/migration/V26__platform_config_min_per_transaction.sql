ALTER TABLE IF EXISTS platform_config
    ADD COLUMN IF NOT EXISTS client_transfer_min_per_transaction numeric(19,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS merchant_transfer_min_per_transaction numeric(19,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS merchant_withdraw_min_per_transaction numeric(19,2) NOT NULL DEFAULT 0.00;