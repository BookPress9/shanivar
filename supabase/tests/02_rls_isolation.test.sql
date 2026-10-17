-- DEV-42 AC5 (+ AC2 timestamps, AC7 cascade): own-row isolation proven as real API roles.
-- User A acts through role authenticated with a JWT sub; user B's rows are seeded by postgres.
begin;
create extension if not exists pgtap with schema extensions;
select plan(44);

insert into auth.users (id, email) values
  ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'a@test.local'),
  ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'b@test.local');

-- B's rows (postgres owns the tables, so RLS does not apply to this seed)
insert into public.routine_plans (user_id, id, plan) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'current', '{"v":1}');
insert into public.protection_windows (user_id, id, start_time, end_time, start_days) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'w1', '06:00', '07:00', '{1,2}');
insert into public.practice_attempts (user_id, id, practice_id, content_version, started_at, outcome) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'pa1', 'p', '1', now(), 'COMPLETED');
insert into public.self_reports (user_id, id, local_date) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'sr1', current_date);
insert into public.orbit_events (user_id, id, type, local_date, source) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'oe1', 'RETURN_DAY', current_date, 'SELF_REPORTED');

-- ── As user A ──────────────────────────────────────────────────────────────────────────────
set local role authenticated;
set local request.jwt.claims = '{"sub":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"authenticated"}';

-- own rows work (user_id defaults to auth.uid())
select lives_ok($$ insert into public.routine_plans (id, plan, created_at, updated_at) values ('current', '{"v":1}', '2000-01-01', '2000-01-01') $$, 'A inserts own routine_plan');
select lives_ok($$ insert into public.protection_windows (id, start_time, end_time, start_days) values ('w1', '06:00', '07:00', '{1}') $$, 'A inserts own protection_window');
select lives_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome) values ('pa1', 'p', '1', now(), 'ENDED') $$, 'A inserts own practice_attempt');
select lives_ok($$ insert into public.self_reports (id, local_date) values ('sr1', current_date) $$, 'A inserts own self_report');
select lives_ok($$ insert into public.orbit_events (id, type, local_date, source, created_at) values ('oe1', 'RETURN_DAY', current_date, 'SELF_REPORTED', '2000-01-01') $$, 'A inserts own orbit_event');

-- A cannot read B's rows (sees exactly its own)
select results_eq($$ select user_id from public.routine_plans $$,      $$ values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'::uuid) $$, 'A reads only own routine_plans');
select results_eq($$ select user_id from public.protection_windows $$, $$ values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'::uuid) $$, 'A reads only own protection_windows');
select results_eq($$ select user_id from public.practice_attempts $$,  $$ values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'::uuid) $$, 'A reads only own practice_attempts');
select results_eq($$ select user_id from public.self_reports $$,       $$ values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'::uuid) $$, 'A reads only own self_reports');
select results_eq($$ select user_id from public.orbit_events $$,       $$ values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'::uuid) $$, 'A reads only own orbit_events');

-- A cannot insert rows for B
select throws_ok($$ insert into public.routine_plans (user_id, id, plan) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'x', '{}') $$, '42501', null, 'A cannot insert routine_plan as B');
select throws_ok($$ insert into public.protection_windows (user_id, id, start_time, end_time, start_days) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'x', '06:00', '07:00', '{1}') $$, '42501', null, 'A cannot insert protection_window as B');
select throws_ok($$ insert into public.practice_attempts (user_id, id, practice_id, content_version, started_at, outcome) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'x', 'p', '1', now(), 'ENDED') $$, '42501', null, 'A cannot insert practice_attempt as B');
select throws_ok($$ insert into public.self_reports (user_id, id, local_date) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'x', current_date) $$, '42501', null, 'A cannot insert self_report as B');
select throws_ok($$ insert into public.orbit_events (user_id, id, type, local_date, source) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'x', 'RETURN_DAY', current_date, 'SELF_REPORTED') $$, '42501', null, 'A cannot insert orbit_event as B');

-- A cannot update B's rows (0 rows touched)
select is_empty($$ update public.routine_plans set plan = '{"hacked":1}' where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, 'A cannot update B routine_plans');
select is_empty($$ update public.protection_windows set enabled = false where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, 'A cannot update B protection_windows');
select is_empty($$ update public.practice_attempts set active_ms = 1 where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, 'A cannot update B practice_attempts');
select is_empty($$ update public.self_reports set note = 'x' where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, 'A cannot update B self_reports');
select throws_ok($$ update public.orbit_events set source = 'x' where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, '42501', null, 'A cannot update B orbit_events (append-only, DEV-54)');

-- A cannot delete B's rows
select is_empty($$ delete from public.routine_plans where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, 'A cannot delete B routine_plans');
select is_empty($$ delete from public.protection_windows where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, 'A cannot delete B protection_windows');
select is_empty($$ delete from public.practice_attempts where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, 'A cannot delete B practice_attempts');
select is_empty($$ delete from public.self_reports where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, 'A cannot delete B self_reports');
select throws_ok($$ delete from public.orbit_events where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' returning 1 $$, '42501', null, 'A cannot delete B orbit_events (cascade only, DEV-54)');

-- user_id cannot be changed (neither to B nor to an unknown id)
select throws_ok($$ update public.routine_plans set user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' $$, '42501', null, 'A cannot move own routine_plan to B');
select throws_ok($$ update public.self_reports set user_id = 'cccccccc-cccc-4ccc-8ccc-cccccccccccc' $$, '42501', null, 'A cannot change own self_report user_id');
select throws_ok($$ update public.orbit_events set user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' $$, '42501', null, 'A cannot move own orbit_event to B');

-- evidence_type spoofing fails (insert, update and upsert)
select throws_ok($$ insert into public.self_reports (id, local_date, evidence_type) values ('spoof', current_date, 'app_observed') $$, '23514', null, 'self_report cannot be labelled app_observed');
select throws_ok($$ update public.self_reports set evidence_type = 'app_observed' where id = 'sr1' $$, '23514', null, 'self_report cannot be relabelled app_observed');
select throws_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome, evidence_type) values ('pa1', 'p', '1', now(), 'ENDED', 'self_report')
                    on conflict (user_id, id) do update set evidence_type = excluded.evidence_type $$, '23514', null, 'practice_attempt cannot be upserted as self_report');

-- AC2: server timestamps win over client values on insert
select results_eq($$ select created_at, updated_at from public.routine_plans $$, $$ values (now(), now()) $$, 'insert ignores client created_at/updated_at');
select results_eq($$ select created_at from public.orbit_events $$, $$ values (now()) $$, 'orbit_events insert ignores client created_at');

-- ── No session ─────────────────────────────────────────────────────────────────────────────
reset role;
set local role authenticated;
set local request.jwt.claims = '{"role":"authenticated"}';
select is_empty($$ select 1 from public.routine_plans union all select 1 from public.protection_windows union all
                   select 1 from public.practice_attempts union all select 1 from public.self_reports union all
                   select 1 from public.orbit_events $$, 'authenticated without a sub sees no rows');

reset role;
set local role anon;
set local request.jwt.claims = '{"role":"anon"}';
-- anon has no table privileges at all (DEV-42 AC4): reading is refused, so no rows come back
select throws_ok($$ select 1 from public.routine_plans union all select 1 from public.protection_windows union all
                    select 1 from public.practice_attempts union all select 1 from public.self_reports union all
                    select 1 from public.orbit_events $$, '42501', null, 'no session (anon) gets no rows: permission denied');
select throws_ok($$ insert into public.self_reports (user_id, id, local_date) values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'anon', current_date) $$, '42501', null, 'no session (anon) cannot insert');

-- ── Back as postgres: B untouched, timestamps on update, cascade ───────────────────────────
reset role;
select results_eq($$ select plan from public.routine_plans where user_id = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb' $$, $$ values ('{"v":1}'::jsonb) $$, 'B routine_plan unchanged');
select results_eq($$ select (select count(*) from public.protection_windows) + (select count(*) from public.practice_attempts)
                          + (select count(*) from public.self_reports) + (select count(*) from public.orbit_events)
                     where true $$, $$ values (8::bigint) $$, 'B rows all still present (4 of A + 4 of B)');

-- backdate A's rows without firing triggers, then update/upsert as A
set local session_replication_role = replica;
update public.routine_plans set created_at = '2001-01-01', updated_at = '2001-01-01' where user_id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
update public.self_reports  set created_at = '2001-01-01', updated_at = '2001-01-01' where user_id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
update public.orbit_events  set created_at = '2001-01-01' where user_id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
set local session_replication_role = origin;

set local role authenticated;
set local request.jwt.claims = '{"sub":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"authenticated"}';
update public.routine_plans set plan = '{"v":2}', created_at = '2030-01-01', updated_at = '2000-01-01';
-- PostgREST-style upsert (merge-duplicates) of an existing self_report
insert into public.self_reports (id, local_date, note) values ('sr1', current_date, 'second write')
  on conflict (user_id, id) do update set note = excluded.note, local_date = excluded.local_date;
select throws_ok($$ update public.orbit_events set created_at = '2030-01-01' $$, '42501', null, 'orbit_events cannot be updated at all (DEV-54)');
select results_eq($$ select created_at, updated_at from public.routine_plans $$, $$ values ('2001-01-01'::timestamptz, now()) $$, 'update bumps updated_at and keeps created_at');
select results_eq($$ select created_at, updated_at, note from public.self_reports $$, $$ values ('2001-01-01'::timestamptz, now(), 'second write'::text) $$, 'upsert bumps updated_at and keeps created_at');
select results_eq($$ select created_at from public.orbit_events $$, $$ values ('2001-01-01'::timestamptz) $$, 'orbit_events created_at cannot be rewritten');

-- AC7: deleting the auth user deletes all of that user's rows
reset role;
delete from auth.users where id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';
select is(
  (select count(*) from public.routine_plans where user_id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa')
  + (select count(*) from public.protection_windows where user_id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa')
  + (select count(*) from public.practice_attempts where user_id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa')
  + (select count(*) from public.self_reports where user_id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa')
  + (select count(*) from public.orbit_events where user_id = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'),
  0::bigint, 'on delete cascade removes all of A''s rows');
select is((select count(*) from public.routine_plans), 1::bigint, 'B rows survive A''s deletion');

select * from finish();
rollback;
