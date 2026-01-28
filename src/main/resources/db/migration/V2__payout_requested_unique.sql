-- Prevent multiple REQUESTED payouts per agent (race-condition safe)
-- This is a business invariant that must be locked at DB level.

create unique index if not exists uq_payouts_agent_requested
    on public.payouts (agent_id)
    where status = 'REQUESTED';
