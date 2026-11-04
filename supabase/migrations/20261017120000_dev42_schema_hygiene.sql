-- DEV-42 (founder-confirmed): cross-table schema hygiene baseline.
-- Forward-only: the applied 20261013090000 / 20261014090000 migrations are not edited.
-- Covers Frontend gap list items 9 (timestamps) and 12 (evidence_type checks).
-- Size caps and the anon revoke (PM additions) live in their own migrations so each can be reverted alone.

-- AC1: the server keeps self-report and observed evidence apart; a client cannot relabel a row.
alter table public.self_reports
  add constraint self_reports_evidence_type_check check (evidence_type = 'self_report');
alter table public.practice_attempts
  add constraint practice_attempts_evidence_type_check check (evidence_type = 'app_observed');

-- AC2: created_at on every table. orbit_events is append-only: created_at, no updated_at.
-- (client_updated_at for DEV-45 is added in its own migration, 20261017120100.)
alter table public.routine_plans
  add column created_at timestamptz not null default now();
alter table public.protection_windows
  add column created_at timestamptz not null default now();
alter table public.practice_attempts
  add column created_at timestamptz not null default now();
alter table public.self_reports
  add column created_at timestamptz not null default now();
alter table public.orbit_events
  add column created_at timestamptz not null default now();

-- Server-owned timestamps. Invoker rights, fixed empty search_path, not callable through the API
-- (trigger functions cannot be invoked as RPC). On insert the server clock wins over any client value;
-- on update (including the update branch of an upsert) updated_at is bumped and created_at is kept.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    new.created_at := now();
  else
    new.created_at := old.created_at;
  end if;
  new.updated_at := now();
  return new;
end;
$$;

-- Same rule for the append-only table, which has no updated_at.
create or replace function public.set_created_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    new.created_at := now();
  else
    new.created_at := old.created_at;
  end if;
  return new;
end;
$$;

revoke all on function public.set_updated_at() from public, anon, authenticated;
revoke all on function public.set_created_at() from public, anon, authenticated;

do $$
declare t text;
begin
  foreach t in array array['routine_plans','protection_windows','practice_attempts','self_reports'] loop
    execute format('create trigger set_updated_at before insert or update on public.%I
                    for each row execute function public.set_updated_at()', t);
  end loop;
end $$;

create trigger set_created_at before insert or update on public.orbit_events
  for each row execute function public.set_created_at();
