ALTER TABLE public.idempotency_records
    ADD COLUMN status character varying(16) NOT NULL DEFAULT 'COMPLETED';

UPDATE public.idempotency_records
SET status = CASE
    WHEN result_json IS NULL THEN 'IN_PROGRESS'
    ELSE 'COMPLETED'
END;