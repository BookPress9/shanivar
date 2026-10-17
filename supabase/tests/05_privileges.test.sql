-- DEV-42 AC4 (PM addition): anon has no privileges on any public table; authenticated has only
-- SELECT/INSERT/UPDATE/DELETE. Iterates over every public table, so new tables are covered too.
begin;
create extension if not exists pgtap with schema extensions;
select plan(5);

select is(
  (select coalesce(array_agg(distinct c.relname::text), '{}')
     from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind in ('r','p','v','m')
      and (has_table_privilege('anon', c.oid, 'SELECT') or has_table_privilege('anon', c.oid, 'INSERT')
        or has_table_privilege('anon', c.oid, 'UPDATE') or has_table_privilege('anon', c.oid, 'DELETE')
        or has_table_privilege('anon', c.oid, 'TRUNCATE') or has_table_privilege('anon', c.oid, 'REFERENCES')
        or has_table_privilege('anon', c.oid, 'TRIGGER'))),
  '{}'::text[], 'anon has no privilege on any public table');

select is(
  (select coalesce(array_agg(distinct c.relname::text), '{}')
     from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind in ('r','p')
      and (has_table_privilege('authenticated', c.oid, 'TRUNCATE') or has_table_privilege('authenticated', c.oid, 'REFERENCES')
        or has_table_privilege('authenticated', c.oid, 'TRIGGER'))),
  '{}'::text[], 'authenticated has no TRUNCATE/REFERENCES/TRIGGER on any public table');

select is(
  (select coalesce(array_agg(t order by t), '{}')
     from unnest(array['routine_plans','protection_windows','practice_attempts','self_reports','practice_reflections']) t
    where not (has_table_privilege('authenticated', 'public.' || t, 'SELECT') and has_table_privilege('authenticated', 'public.' || t, 'INSERT')
           and has_table_privilege('authenticated', 'public.' || t, 'UPDATE') and has_table_privilege('authenticated', 'public.' || t, 'DELETE'))),
  '{}'::text[], 'authenticated keeps select/insert/update/delete the own-row policies need');

-- DEV-54: orbit_events is append-only: select + insert, no update, no delete (cascade only)
select ok(has_table_privilege('authenticated', 'public.orbit_events', 'SELECT') and has_table_privilege('authenticated', 'public.orbit_events', 'INSERT')
          and not has_table_privilege('authenticated', 'public.orbit_events', 'UPDATE') and not has_table_privilege('authenticated', 'public.orbit_events', 'DELETE'),
          'orbit_events: authenticated has select and insert only');

set local role anon;
select throws_ok($$ insert into public.orbit_events (user_id, id, type, local_date, source)
                    values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'x', 'RETURN_DAY', current_date, 'SELF_REPORTED') $$,
                 '42501', 'permission denied for table orbit_events', 'anon insert is refused by privileges, not only by RLS');

select * from finish();
rollback;
