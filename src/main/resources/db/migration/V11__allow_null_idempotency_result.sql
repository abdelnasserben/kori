ALTER TABLE public.idempotency_records
    ALTER COLUMN result_json DROP NOT NULL;