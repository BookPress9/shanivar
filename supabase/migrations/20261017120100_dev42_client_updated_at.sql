-- DEV-42 / PM decision (17 Oct): client_updated_at on every synced table for the DEV-45 conflict rule.
-- Client clock, stamped by the app when it enqueues the outbox entry. Nullable: older builds and
-- rows synced before this migration carry no client time (the server never invents one).
-- Includes the append-only orbit_events (PM decision; AC2 text says "created_at only" there).
-- Own migration so it can be reverted alone.
alter table public.routine_plans      add column client_updated_at timestamptz;
alter table public.protection_windows add column client_updated_at timestamptz;
alter table public.practice_attempts  add column client_updated_at timestamptz;
alter table public.self_reports       add column client_updated_at timestamptz;
alter table public.orbit_events       add column client_updated_at timestamptz;
