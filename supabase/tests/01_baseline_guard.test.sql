-- DEV-42 baseline guard. Runs over EVERY table in public, so each new table (DEV-47..DEV-55)
-- fails this file until it carries the same baseline: RLS on, server created_at, timestamp trigger,
-- user-owned rows cascade from auth.users.
begin;
create extension if not exists pgtap with schema extensions;
select plan(12);

-- AC1: evidence_type checks
select col_has_check('public', 'self_reports', 'evidence_type', 'self_reports.evidence_type has a check');
select col_has_check('public', 'practice_attempts', 'evidence_type', 'practice_attempts.evidence_type has a check');

-- AC2: shared timestamp function, safe definition
select has_function('public', 'set_updated_at', '{}'::name[], 'set_updated_at() exists');
select is(
  (select p.prosecdef or p.proconfig is null or not ('search_path=""' = any(p.proconfig))
     from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname = 'set_updated_at'),
  false, 'set_updated_at() is security invoker with an empty search_path');

select is(
  (select coalesce(array_agg(c.relname::text order by c.relname), '{}')
     from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind = 'r'
      and not exists (select 1 from information_schema.columns k
                       where k.table_schema = 'public' and k.table_name = c.relname and k.column_name = 'created_at'
                         and k.data_type = 'timestamp with time zone' and k.is_nullable = 'NO'
                         and k.column_default = 'now()')),
  '{}'::text[], 'every public table has created_at timestamptz not null default now()');

select is(
  (select coalesce(array_agg(c.relname::text order by c.relname), '{}')
     from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind = 'r'
      and exists (select 1 from pg_attribute a where a.attrelid = c.oid and a.attname = 'updated_at' and not a.attisdropped)
      and not exists (select 1 from pg_trigger t join pg_proc p on p.oid = t.tgfoid
                       where t.tgrelid = c.oid and not t.tgisinternal and p.proname = 'set_updated_at'
                         and (t.tgtype & 16) <> 0 and (t.tgtype & 2) <> 0)),
  '{}'::text[], 'every table with updated_at has a BEFORE UPDATE set_updated_at() trigger');

select is(
  (select coalesce(array_agg(c.relname::text order by c.relname), '{}')
     from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind = 'r'
      and not exists (select 1 from pg_attribute a where a.attrelid = c.oid and a.attname = 'updated_at' and not a.attisdropped)
      and not exists (select 1 from pg_trigger t join pg_proc p on p.oid = t.tgfoid
                       where t.tgrelid = c.oid and not t.tgisinternal and p.proname = 'set_created_at')),
  '{}'::text[], 'every table without updated_at (append-only) keeps created_at server-owned via set_created_at()');

select hasnt_column('public', 'orbit_events', 'updated_at', 'append-only orbit_events has created_at but no updated_at');

-- RLS on everywhere (AC6 guard)
select is(
  (select coalesce(array_agg(c.relname::text order by c.relname), '{}')
     from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind in ('r','p') and not c.relrowsecurity),
  '{}'::text[], 'RLS is enabled on every public table');

-- AC7: user_id -> auth.users on delete cascade
select is(
  (select coalesce(array_agg(c.relname::text order by c.relname), '{}')
     from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind = 'r'
      and exists (select 1 from pg_attribute a where a.attrelid = c.oid and a.attname = 'user_id' and not a.attisdropped)
      and not exists (select 1 from pg_constraint f
                       where f.conrelid = c.oid and f.contype = 'f' and f.confrelid = 'auth.users'::regclass
                         and f.confdeltype = 'c'
                         and f.conkey = array[(select attnum from pg_attribute where attrelid = c.oid and attname = 'user_id')])),
  '{}'::text[], 'every table with user_id references auth.users on delete cascade');

select ok(
  (select count(*) from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind = 'r') >= 5,
  'guard sees the 5 sync tables (and any newer ones)');

-- No exposed security definer function without a fixed search_path
select is(
  (select coalesce(array_agg(p.proname::text order by p.proname), '{}')
     from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.prosecdef
      and (p.proconfig is null or not exists (select 1 from unnest(p.proconfig) s where s like 'search_path=%'))),
  '{}'::text[], 'no security definer function in public without a fixed search_path');

select * from finish();
rollback;
