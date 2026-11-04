-- DEV-45 AC5/AC6: server side of the sync conflict rule (decision: device is the authority for its own
-- records; last writer wins on client_updated_at from DEV-42; tombstones beat older upserts).
-- Deletes travel as upserts into public.tombstones (agreed DEV-45/47 contract), never as bare DELETE.
-- Older writes are skipped silently (the statement still succeeds, so the client treats them as acked).

create table public.tombstones (
  user_id uuid not null default auth.uid() references auth.users on delete cascade,
  kind text not null check (kind in ('attempt','self_report','window','orbit')),
  id text not null check (char_length(id) <= 128),
  client_updated_at timestamptz not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, kind, id)
);

-- BEFORE INSERT OR UPDATE on data tables (TG_ARGV[0] = kind) and on tombstones (no argument).
--  * update: an incoming write older than the stored one (or without a client time when the stored row
--    has one) is skipped;
--  * data tables: a write not newer than a tombstone for the same (user_id, kind, id) is skipped.
create or replace function public.sync_guard()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
  if tg_op = 'UPDATE' and old.client_updated_at is not null
     and (new.client_updated_at is null or new.client_updated_at < old.client_updated_at) then
    return null;
  end if;
  if tg_nargs > 0 and exists (
       select 1 from public.tombstones t
        where t.user_id = new.user_id and t.kind = tg_argv[0] and t.id = new.id
          and (new.client_updated_at is null or t.client_updated_at >= new.client_updated_at)) then
    return null;
  end if;
  return new;
end;
$$;

-- AFTER INSERT OR UPDATE on tombstones: the server applies the delete to rows not newer than it.
-- Invoker rights: the delete runs under the caller's own_delete RLS policy.
create or replace function public.apply_tombstone()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
declare tbl text := case new.kind            -- fixed whitelist, never user text in SQL
  when 'attempt' then 'practice_attempts' when 'self_report' then 'self_reports'
  when 'window' then 'protection_windows' when 'orbit' then 'orbit_events' end;
begin
  if tbl is null then return null; end if;
  execute format('delete from public.%I where user_id = $1 and id = $2
                    and (client_updated_at is null or client_updated_at <= $3)', tbl)
    using new.user_id, new.id, new.client_updated_at;
  return null;
end;
$$;

revoke all on function public.sync_guard() from public, anon, authenticated;
revoke all on function public.apply_tombstone() from public, anon, authenticated;

do $$
declare t text; k text;
begin
  for t, k in select * from (values ('practice_attempts','attempt'), ('self_reports','self_report'),
      ('routine_plans','routine'), ('protection_windows','window'), ('orbit_events','orbit')) v loop
    execute format('create trigger sync_guard before insert or update on public.%I
                    for each row execute function public.sync_guard(%L)', t, k);
  end loop;
end $$;

-- DEV-42 baseline on the new table: RLS own rows, server timestamps, cascade, no anon, least privilege.
alter table public.tombstones enable row level security;
create policy own_select on public.tombstones for select to authenticated using (user_id = (select auth.uid()));
create policy own_insert on public.tombstones for insert to authenticated with check (user_id = (select auth.uid()));
create policy own_update on public.tombstones for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create trigger set_updated_at before insert or update on public.tombstones
  for each row execute function public.set_updated_at();
create trigger sync_guard before update on public.tombstones
  for each row execute function public.sync_guard();
create trigger apply_tombstone after insert or update on public.tombstones
  for each row execute function public.apply_tombstone();
revoke all on public.tombstones from anon, authenticated;
-- upsert = insert + update (+ select for the conflict target); no client delete of tombstones
grant select, insert, update on public.tombstones to authenticated;
