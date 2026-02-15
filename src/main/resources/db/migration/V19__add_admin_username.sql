-- Ajout colonne
ALTER TABLE admins
ADD COLUMN username VARCHAR(16);

-- Remplissage
UPDATE admins
SET username = 'admin.' || id
WHERE username IS NULL;

-- Contraintes
ALTER TABLE admins
ALTER COLUMN username SET NOT NULL;

CREATE UNIQUE INDEX ux_admins_username
ON admins (username);