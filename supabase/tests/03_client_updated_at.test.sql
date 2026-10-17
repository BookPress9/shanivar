-- DEV-42 / PM decision: client_updated_at on every synced table, stored exactly as the client sent it.
begin;
create extension if not exists pgtap with schema extensions;
select plan(6);

select is(
  (select coalesce(array_agg(t order by t), '{}')
     from unnest(array['routine_plans','protection_windows','practice_attempts','self_reports','orbit_events']) t
    where not exists (select 1 from information_schema.columns k
                       where k.table_schema = 'public' and k.table_name = t and k.column_name = 'client_updated_at'
                         and k.data_type = 'timestamp with time zone' and k.is_nullable = 'YES')),
  '{}'::text[], 'every synced table has nullable client_updated_at timestamptz');

insert into auth.users (id, email) values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'a@test.local');
set local role authenticated;
set local request.jwt.claims = '{"sub":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"authenticated"}';

insert into public.routine_plans (id, plan, client_updated_at) values ('current', '{}', '2026-10-17T08:00:00Z');
insert into public.protection_windows (id, start_time, end_time, start_days, client_updated_at) values ('w1', '06:00', '07:00', '{1}', '2026-10-17T08:00:00Z');
insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome, client_updated_at) values ('pa1', 'p', '1', now(), 'ENDED', '2026-10-17T08:00:00Z');
insert into public.self_reports (id, local_date) values ('sr1', current_date);
insert into public.orbit_events (id, type, local_date, source, client_updated_at) values ('oe1', 'RETURN_DAY', current_date, 'SELF_REPORTED', '2026-10-17T08:00:00Z');

select results_eq($$ select client_updated_at from public.routine_plans $$, $$ values ('2026-10-17T08:00:00Z'::timestamptz) $$, 'routine_plans keeps client time');
select results_eq($$ select client_updated_at from public.practice_attempts $$, $$ values ('2026-10-17T08:00:00Z'::timestamptz) $$, 'practice_attempts keeps client time');
select results_eq($$ select client_updated_at from public.orbit_events $$, $$ values ('2026-10-17T08:00:00Z'::timestamptz) $$, 'orbit_events keeps client time');
select results_eq($$ select client_updated_at from public.self_reports $$, $$ values (null::timestamptz) $$, 'older payloads without client time still sync (null)');
-- an upsert carries the newer client time; updated_at stays server-owned
insert into public.routine_plans (id, plan, client_updated_at) values ('current', '{"v":2}', '2026-10-17T09:00:00Z')
  on conflict (user_id, id) do update set plan = excluded.plan, client_updated_at = excluded.client_updated_at;
select results_eq($$ select client_updated_at, updated_at from public.routine_plans $$, $$ values ('2026-10-17T09:00:00Z'::timestamptz, now()) $$, 'upsert stores the new client time, server sets updated_at');

select * from finish();
rollback;
