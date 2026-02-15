ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS client_code VARCHAR(16);

UPDATE clients c
SET client_code = generated.client_code
FROM (
    SELECT id,
           'C-' || LPAD(ROW_NUMBER() OVER (ORDER BY created_at, id)::text, 6, '0') AS client_code
    FROM clients
) AS generated
WHERE c.id = generated.id
  AND c.client_code IS NULL;

ALTER TABLE clients
    ALTER COLUMN client_code SET NOT NULL;

ALTER TABLE clients
    ADD CONSTRAINT clients_client_code_key UNIQUE (client_code);

CREATE INDEX IF NOT EXISTS idx_clients_client_code
    ON clients (client_code);