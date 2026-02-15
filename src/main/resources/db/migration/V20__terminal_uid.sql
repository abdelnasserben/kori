-- Extension UUID si absente
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Ajout colonne
ALTER TABLE terminals
ADD COLUMN terminal_uid VARCHAR(36);

-- Remplissage
UPDATE terminals
SET terminal_uid = UPPER(
    SUBSTRING(
        'T-' || REPLACE(gen_random_uuid()::text, '-', ''),
        1,
        32
    )
)
WHERE terminal_uid IS NULL;

-- Contraintes
ALTER TABLE terminals
ALTER COLUMN terminal_uid SET NOT NULL;

CREATE UNIQUE INDEX ux_terminals_terminal_uid
ON terminals (terminal_uid);
