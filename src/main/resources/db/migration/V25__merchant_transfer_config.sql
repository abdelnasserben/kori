ALTER TABLE IF EXISTS fee_config
    ADD COLUMN IF NOT EXISTS merchant_transfer_fee_rate numeric(10,6) NOT NULL DEFAULT 0.000000,
    ADD COLUMN IF NOT EXISTS merchant_transfer_fee_min numeric(19,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS merchant_transfer_fee_max numeric(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE IF EXISTS platform_config
    ADD COLUMN IF NOT EXISTS merchant_transfer_max_per_transaction numeric(19,2) NOT NULL DEFAULT 999999999.99,
    ADD COLUMN IF NOT EXISTS merchant_transfer_daily_max numeric(19,2) NOT NULL DEFAULT 999999999.99;