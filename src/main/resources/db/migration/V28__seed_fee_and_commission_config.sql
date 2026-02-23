-- Seed default fee and commission configuration

INSERT INTO fee_config (
    id,
    card_enrollment_price,
    card_payment_fee_rate,
    card_payment_fee_min,
    card_payment_fee_max,
    merchant_withdraw_fee_rate,
    merchant_withdraw_fee_min,
    merchant_withdraw_fee_max,
    client_transfer_fee_rate,
    client_transfer_fee_min,
    client_transfer_fee_max,
    merchant_transfer_fee_rate,
    merchant_transfer_fee_min,
    merchant_transfer_fee_max,
    card_payment_fee_refundable,
    merchant_withdraw_fee_refundable,
    card_enrollment_price_refundable
)
VALUES (
    1,
    0.00,
    0.000000,
    0.00,
    0.00,
    0.000000,
    0.00,
    0.00,
    0.000000,
    0.00,
    0.00,
    0.000000,
    0.00,
    0.00,
    false,
    false,
    false
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO commission_config (
    id,
    card_enrollment_agent_commission,
    merchant_withdraw_commission_rate,
    merchant_withdraw_commission_min,
    merchant_withdraw_commission_max
)
VALUES (
    1,
    0.00,
    0.000000,
    0.00,
    0.00
)
ON CONFLICT (id) DO NOTHING;
