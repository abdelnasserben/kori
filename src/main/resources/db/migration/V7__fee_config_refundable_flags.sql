ALTER TABLE IF EXISTS fee_config
    ADD COLUMN IF NOT EXISTS card_payment_fee_refundable boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS merchant_withdraw_fee_refundable boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS card_enrollment_price_refundable boolean NOT NULL DEFAULT false;