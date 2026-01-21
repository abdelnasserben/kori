-- Prevent UPDATE/DELETE on ledger_entries (append-only guarantee)

CREATE OR REPLACE FUNCTION kori_ledger_no_update_delete()
RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'ledger_entries is append-only: % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ledger_no_update ON ledger_entries;
DROP TRIGGER IF EXISTS trg_ledger_no_delete ON ledger_entries;

CREATE TRIGGER trg_ledger_no_update
BEFORE UPDATE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION kori_ledger_no_update_delete();

CREATE TRIGGER trg_ledger_no_delete
BEFORE DELETE ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION kori_ledger_no_update_delete();
