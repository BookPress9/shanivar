-- DEV-72 (founder-confirmed): routine completion semantics and idempotent completion.
-- One row per step occurrence (user, step, scheduled local date): the occurrence key is the idempotency
-- key, so 5 identical requests or an offline replay give one row. Undo = DEV-45 tombstone (kind
-- 'step_completion'). Missed and recovered are derived, never stored (docs/decisions/0021).
-- Completions are facts (done / not done), never scores or rewards (DEV-16 out of scope).

create table public.routine_step_completions (
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  -- occurrence key "<step_id>@<scheduled_local_date>"
  id text not null check (char_length(id) <= 128),   -- fits the tombstones id cap
  step_id text not null check (step_id ~ '^[A-Za-z0-9_.:-]{1,117}$'),
  scheduled_local_date date not null,
  scheduled_local_time time,
  completed_at timestamptz not null,
  tz text not null check (char_length(tz) between 1 and 64),
  -- routine revision the device's plan had when the step was marked (RoutineSyncStore.serverRevision; 0 = none yet)
  plan_revision int not null default 0 check (plan_revision >= 0),
  client_updated_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, id),
  constraint routine_step_completions_occurrence_key_check check (id = step_id || '@' || scheduled_local_date::text)
);
alter table public.routine_step_completions enable row level security;
create policy own_select on public.routine_step_completions for select to authenticated using (user_id = (select auth.uid()));
create policy own_insert on public.routine_step_completions for insert to authenticated with check (user_id = (select auth.uid()));
create policy own_update on public.routine_step_completions for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy own_delete on public.routine_step_completions for delete to authenticated using (user_id = (select auth.uid()));
revoke all on public.routine_step_completions from anon, authenticated;
grant select, insert, update, delete on public.routine_step_completions to authenticated;
create trigger set_updated_at before insert or update on public.routine_step_completions
  for each row execute function public.set_updated_at();
create trigger sync_guard before insert or update on public.routine_step_completions
  for each row execute function public.sync_guard('step_completion');

-- Server validation. Errors: 23514 (check_violation, -> dead letter) for an invalid completion;
-- PT409 (PostgREST -> HTTP 409) for a completion made against a stale routine revision.
create or replace function public.validate_step_completion()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
declare
  local_day date;
  server_rev int;
  server_plan jsonb;
begin
  if not exists (select 1 from pg_catalog.pg_timezone_names where name = new.tz) then
    raise exception 'invalid_tz' using errcode = '23514';
  end if;
  if new.completed_at > now() + interval '5 minutes' then
    raise exception 'completed_in_future' using errcode = '23514';
  end if;
  -- complete = marked on the scheduled local date or by the end of the next local day (grace)
  local_day := (new.completed_at at time zone new.tz)::date;
  if local_day < new.scheduled_local_date or local_day > new.scheduled_local_date + 1 then
    raise exception 'completion_outside_window' using errcode = '23514';
  end if;
  -- Stale revision (active once DEV-52 adds routine_plans.revision; read through jsonb so this works before):
  -- marked against an older revision AND the step is no longer an enabled activity of the current plan.
  select (to_jsonb(rp) ->> 'revision')::int, rp.plan into server_rev, server_plan
    from public.routine_plans rp where rp.user_id = new.user_id and rp.id = 'current';
  if server_rev is not null and new.plan_revision < server_rev and not exists (
       select 1 from jsonb_array_elements(coalesce(server_plan -> 'activities', '[]'::jsonb)) a
        where a ->> 'id' = new.step_id and coalesce((a ->> 'enabled')::boolean, true)) then
    raise exception 'stale_plan_revision' using errcode = 'PT409',
      detail = json_build_object('current_revision', server_rev)::text;
  end if;
  return new;
end;
$$;
revoke all on function public.validate_step_completion() from public, anon, authenticated;
create trigger validate_step_completion before insert or update on public.routine_step_completions
  for each row execute function public.validate_step_completion();

-- DEV-45 plumbing: undo is a tombstone of kind 'step_completion'.
alter table public.tombstones drop constraint tombstones_kind_check;
alter table public.tombstones add constraint tombstones_kind_check
  check (kind in ('attempt','self_report','window','reflection','step_completion'));

create or replace function public.apply_tombstone()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
declare
  tbl text := case new.kind            -- fixed whitelist, never user text in SQL
    when 'attempt' then 'practice_attempts' when 'self_report' then 'self_reports'
    when 'window' then 'protection_windows' when 'reflection' then 'practice_reflections'
    when 'step_completion' then 'routine_step_completions' end;
  col text := case new.kind when 'reflection' then 'attempt_id' else 'id' end;
begin
  if tbl is null then return null; end if;
  execute format('delete from public.%I where user_id = $1 and %I = $2
                    and (client_updated_at is null or client_updated_at <= $3)', tbl, col)
    using new.user_id, new.id, new.client_updated_at;
  return null;
end;
$$;
revoke all on function public.apply_tombstone() from public, anon, authenticated;
