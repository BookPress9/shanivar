-- DEV-42 AC3/AC5 (PM addition): oversized payloads fail, payloads at the limit pass.
begin;
create extension if not exists pgtap with schema extensions;
select plan(14);

insert into auth.users (id, email) values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'a@test.local');
set local role authenticated;
set local request.jwt.claims = '{"sub":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"authenticated"}';

-- plan: 65536 bytes max (jsonb text form)
select throws_ok($$ insert into public.routine_plans (id, plan) values ('big', jsonb_build_object('x', repeat('a', 65536))) $$, '23514', null, 'routine_plans.plan over 64 KiB fails');
select lives_ok($$ insert into public.routine_plans (id, plan) values ('ok', jsonb_build_object('x', repeat('a', 65536 - 9))) $$, 'routine_plans.plan of exactly 64 KiB passes');
select is((select octet_length(plan::text) from public.routine_plans where id = 'ok'), 65536, 'boundary row is exactly 65536 bytes');

-- packages: 200 max
select throws_ok($$ insert into public.protection_windows (id, start_time, end_time, start_days, packages)
                    values ('big', '06:00', '07:00', '{1}', array(select 'p' || g from generate_series(1, 201) g)) $$, '23514', null, '201 packages fail');
select lives_ok($$ insert into public.protection_windows (id, start_time, end_time, start_days, packages)
                   values ('ok', '06:00', '07:00', '{1}', array(select 'p' || g from generate_series(1, 200) g)) $$, '200 packages pass');

-- drift_reasons: 20 max
select throws_ok($$ insert into public.self_reports (id, local_date, drift_reasons) values ('big', current_date, array_fill('other'::text, array[21])) $$, '23514', null, '21 drift_reasons fail');
select lives_ok($$ insert into public.self_reports (id, local_date, drift_reasons) values ('ok', current_date, array_fill('other'::text, array[20])) $$, '20 drift_reasons pass');

-- note: 500 max (unchanged)
select throws_ok($$ insert into public.self_reports (id, local_date, note) values ('note', current_date, repeat('n', 501)) $$, '23514', null, '501-char note fails');

-- id: 128 chars max on every table
select throws_ok($$ insert into public.routine_plans (id, plan) values (repeat('i', 129), '{}') $$, '23514', null, 'routine_plans.id over 128 fails');
select throws_ok($$ insert into public.protection_windows (id, start_time, end_time, start_days) values (repeat('i', 129), '06:00', '07:00', '{1}') $$, '23514', null, 'protection_windows.id over 128 fails');
select throws_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome) values (repeat('i', 129), 'p', '1', now(), 'ENDED') $$, '23514', null, 'practice_attempts.id over 128 fails');
select throws_ok($$ insert into public.self_reports (id, local_date) values (repeat('i', 129), current_date) $$, '23514', null, 'self_reports.id over 128 fails');
select throws_ok($$ insert into public.orbit_events (id, type, local_date, source) values (repeat('i', 129), 'RETURN_DAY', current_date, 'SELF_REPORTED') $$, '23514', null, 'orbit_events.id over 128 fails');
select lives_ok($$ insert into public.orbit_events (id, type, local_date, source) values (repeat('i', 128), 'RETURN_DAY', current_date, 'SELF_REPORTED') $$, '128-char id passes');

select * from finish();
rollback;
