-- Shanivar v1 local-first sync (HANDOFF /v1 contract mapped to Supabase).
-- Every row belongs to auth.uid(); RLS: a user (incl. anonymous guest) reads and writes only own rows.
-- Writes are idempotent upserts on (user_id, id), matching the device outbox keys.

create table public.routine_plans (
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  id text not null default 'current',
  schema_version int not null default 1,
  plan jsonb not null,
  updated_at timestamptz not null default now(),
  primary key (user_id, id)
);

create table public.protection_windows (
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  id text not null,
  start_time time not null,
  end_time time not null,
  start_days int[] not null,
  enabled boolean not null default true,
  packages text[] not null default '{}',
  updated_at timestamptz not null default now(),
  primary key (user_id, id)
);

create table public.practice_attempts (
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  id text not null,
  practice_id text not null,
  content_version text not null,
  started_at timestamptz not null,
  zone_id text,
  intended_seconds int,
  active_ms bigint not null default 0 check (active_ms >= 0),
  outcome text not null check (outcome in ('COMPLETED','ENDED','INTERRUPTED')),
  ended_at timestamptz,
  evidence_type text not null default 'app_observed',
  updated_at timestamptz not null default now(),
  primary key (user_id, id)
);

create table public.self_reports (
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  id text not null,
  local_date date not null,
  intended_minutes int,
  actual_minutes int check (actual_minutes between 1 and 180),
  practice_id text,
  evidence_type text not null default 'self_report',
  updated_at timestamptz not null default now(),
  primary key (user_id, id)
);

create table public.orbit_events (
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  id text not null,
  type text not null,
  local_date date not null,
  source text not null,
  primary key (user_id, id)
);

do $$
declare t text;
begin
  foreach t in array array['routine_plans','protection_windows','practice_attempts','self_reports','orbit_events'] loop
    execute format('alter table public.%I enable row level security', t);
    execute format('create policy own_select on public.%I for select to authenticated using (user_id = (select auth.uid()))', t);
    execute format('create policy own_insert on public.%I for insert to authenticated with check (user_id = (select auth.uid()))', t);
    execute format('create policy own_update on public.%I for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()))', t);
    execute format('create policy own_delete on public.%I for delete to authenticated using (user_id = (select auth.uid()))', t);
    execute format('grant select, insert, update, delete on public.%I to authenticated', t);
  end loop;
end $$;
