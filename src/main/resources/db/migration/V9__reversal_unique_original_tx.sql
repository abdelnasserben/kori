CREATE UNIQUE INDEX IF NOT EXISTS ux_transactions_reversal_original_tx
    ON public.transactions (original_transaction_id)
    WHERE type = 'REVERSAL';