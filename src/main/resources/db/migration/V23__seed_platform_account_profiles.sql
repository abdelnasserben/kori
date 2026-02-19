-- Seed platform account profiles (idempotent)

INSERT INTO account_profiles (created_at, status, account_type, owner_ref)
SELECT now(), 'ACTIVE', v.account_type, 'SYSTEM'
FROM (VALUES
  ('PLATFORM_FEE_REVENUE'),
  ('PLATFORM_CLEARING'),
  ('PLATFORM_CLIENT_REFUND_CLEARING'),
  ('PLATFORM_BANK')
) AS v(account_type)
WHERE NOT EXISTS (
  SELECT 1
  FROM account_profiles ap
  WHERE ap.account_type = v.account_type
    AND ap.owner_ref = 'SYSTEM'
);
