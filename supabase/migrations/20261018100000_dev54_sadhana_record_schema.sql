-- DEV-54 (founder-confirmed): the server Sadhana record matches the app's record model (Frontend gap list
-- items 4, 5, 7, 8). Forward-only; applied migrations are not edited. The redundant
-- `alter column local_date set not null` in 20261014090000 is harmless and stays.
-- Self-reports, reflections and drift reasons are the person's own reflective notes: never scores, health
-- or medical data, never aggregated or ranked. Sadhana SelfCheck (SadhanaSpine.kt) stays local-only: no table.

-- AC1: practice_reflections (SadhanaRecord.kt PracticeReflectionRecord). One reflection per attempt.
-- client_updated_at: DEV-42 AC2/AC6 baseline for every synced table (DEV-45 last-writer-wins).
create table public.practice_reflections (
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  attempt_id text not null check (char_length(attempt_id) <= 128),
  usefulness text check (usefulness in ('useful','neutral','uncomfortable')),
  note text check (char_length(note) <= 500),
  client_updated_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, attempt_id)
);
alter table public.practice_reflections enable row level security;
create policy own_select on public.practice_reflections for select to authenticated using (user_id = (select auth.uid()));
create policy own_insert on public.practice_reflections for insert to authenticated with check (user_id = (select auth.uid()));
create policy own_update on public.practice_reflections for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
-- a cleared reflection is a tombstone (kind 'reflection'); the server applies it under own_delete
create policy own_delete on public.practice_reflections for delete to authenticated using (user_id = (select auth.uid()));
revoke all on public.practice_reflections from anon, authenticated;
grant select, insert, update, delete on public.practice_reflections to authenticated;
create trigger set_updated_at before insert or update on public.practice_reflections
  for each row execute function public.set_updated_at();

-- AC2: self_reports
alter table public.self_reports
  add column private_by_default boolean not null default true,
  add constraint self_reports_drift_reasons_allowed_check
    check (drift_reasons <@ array['thoughts','sounds','body','phone','sleepy','emotion','other']::text[]);

-- AC3: practice_attempts (SadhanaAttempt.kt). created_at already exists (DEV-42).
alter table public.practice_attempts
  add column anchor text check (anchor in ('BREATH','ROOM')),
  add column anchor_changes int not null default 0 check (anchor_changes >= 0),
  add column events jsonb check (octet_length(events::text) <= 32768);

-- AC4: orbit_events (LivingOrbit.kt OrbitEvent). created_at already exists (DEV-42).
-- Mapping: the app's OrbitEvent.occurrenceDate is stored in local_date (no rename without founder approval).
alter table public.orbit_events
  add column entity_id text check (char_length(entity_id) <= 128),
  add constraint orbit_events_type_check check (type in
    ('ROUTINE_CREATED','ALARM_CONFIGURED','BOUNDARY_SAVED','PRACTICE_ATTEMPTED','RETURN_DAY','INDEPENDENT_RECORD')),
  add constraint orbit_events_source_check check (source in ('CONFIGURED','DEVICE_OBSERVED','SELF_REPORTED'));
comment on column public.orbit_events.local_date is 'App OrbitEvent.occurrenceDate (local calendar date of the event).';
-- Append-only: insert and own select stay; no update; delete only through the auth.users cascade.
drop policy own_update on public.orbit_events;
drop policy own_delete on public.orbit_events;
revoke update, delete on public.orbit_events from authenticated;

-- Sync plumbing (DEV-45): reflections are keyed by attempt_id, orbit events can no longer be deleted.
alter table public.tombstones drop constraint tombstones_kind_check;
alter table public.tombstones add constraint tombstones_kind_check
  check (kind in ('attempt','self_report','window','reflection'));

-- sync_guard: optional TG_ARGV[1] names the key column (default id).
create or replace function public.sync_guard()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
declare k text := to_jsonb(new) ->> coalesce(tg_argv[1], 'id');
begin
  if tg_op = 'UPDATE' and old.client_updated_at is not null
     and (new.client_updated_at is null or new.client_updated_at < old.client_updated_at) then
    return null;
  end if;
  if tg_nargs > 0 and exists (
       select 1 from public.tombstones t
        where t.user_id = new.user_id and t.kind = tg_argv[0] and t.id = k
          and (new.client_updated_at is null or t.client_updated_at >= new.client_updated_at)) then
    return null;
  end if;
  return new;
end;
$$;

create or replace function public.apply_tombstone()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
declare
  tbl text := case new.kind            -- fixed whitelist, never user text in SQL
    when 'attempt' then 'practice_attempts' when 'self_report' then 'self_reports'
    when 'window' then 'protection_windows' when 'reflection' then 'practice_reflections' end;
  col text := case new.kind when 'reflection' then 'attempt_id' else 'id' end;
begin
  if tbl is null then return null; end if;
  execute format('delete from public.%I where user_id = $1 and %I = $2
                    and (client_updated_at is null or client_updated_at <= $3)', tbl, col)
    using new.user_id, new.id, new.client_updated_at;
  return null;
end;
$$;
revoke all on function public.sync_guard() from public, anon, authenticated;
revoke all on function public.apply_tombstone() from public, anon, authenticated;

create trigger sync_guard before insert or update on public.practice_reflections
  for each row execute function public.sync_guard('reflection', 'attempt_id');
