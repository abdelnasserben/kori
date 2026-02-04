ALTER TABLE public.idempotency_records
    ADD COLUMN request_hash character varying(64) NOT NULL DEFAULT '',
    ADD COLUMN expires_at timestamp(6) with time zone;

UPDATE public.idempotency_records
SET expires_at = COALESCE(created_at + interval '24 hours', now() + interval '24 hours')
WHERE expires_at IS NULL;