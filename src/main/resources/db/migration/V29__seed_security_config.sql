-- Seed default security configuration if missing

INSERT INTO security_config (id, max_failed_pin_attempts)
VALUES (1, 3)
ON CONFLICT (id) DO NOTHING;