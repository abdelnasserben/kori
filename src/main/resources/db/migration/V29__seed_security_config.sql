-- Seed default security configuration if missing

INSERT INTO security_config (
    id,
    max_failed_pin_attempts,
)
SELECT
    1,
    3,
WHERE NOT EXISTS (
    SELECT 1 FROM security_config WHERE id = 1
);