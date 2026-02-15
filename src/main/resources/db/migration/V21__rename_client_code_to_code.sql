ALTER TABLE clients
RENAME COLUMN client_code TO code;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'i'
      AND c.relname = 'ux_clients_client_code'
      AND n.nspname = 'public'
  ) THEN
    EXECUTE 'ALTER INDEX public.ux_clients_client_code RENAME TO ux_clients_code';
  END IF;
END $$;
