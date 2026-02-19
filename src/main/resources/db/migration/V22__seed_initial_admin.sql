-- Create the first admin if none exists.

INSERT INTO admins (id, username, status, created_at)
SELECT gen_random_uuid(), 'super@admin', 'ACTIVE', now()
WHERE NOT EXISTS (SELECT 1 FROM admins);
