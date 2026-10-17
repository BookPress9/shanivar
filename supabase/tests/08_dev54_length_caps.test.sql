-- DEV-54 PM follow-up: length caps on practice_id, content_version, zone_id, attempt_id and orbit type/source/entity_id.
begin;
create extension if not exists pgtap with schema extensions;
select plan(9);
insert into auth.users (id, email) values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'a@test.local');
set local role authenticated;
set local request.jwt.claims = '{"sub":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"authenticated"}';

select lives_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome, zone_id)
                   values ('a0', repeat('p', 128), repeat('v', 64), now(), 'ENDED', repeat('z', 64)) $$, 'attempt at the caps is accepted');
select throws_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome) values ('a1', repeat('p', 129), 'v', now(), 'ENDED') $$, '23514', null, 'practice_id over 128 rejected');
select throws_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome) values ('a2', 'p', repeat('v', 65), now(), 'ENDED') $$, '23514', null, 'content_version over 64 rejected');
select throws_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome, zone_id) values ('a3', 'p', 'v', now(), 'ENDED', repeat('z', 65)) $$, '23514', null, 'zone_id over 64 rejected');
select throws_ok($$ insert into public.self_reports (id, local_date, practice_id) values ('s1', current_date, repeat('p', 129)) $$, '23514', null, 'self_reports.practice_id over 128 rejected');
select throws_ok($$ insert into public.self_reports (id, local_date, attempt_id) values ('s2', current_date, repeat('a', 129)) $$, '23514', null, 'self_reports.attempt_id over 128 rejected');
select throws_ok($$ insert into public.orbit_events (id, type, local_date, source, entity_id) values ('o1', 'RETURN_DAY', current_date, 'CONFIGURED', repeat('e', 129)) $$, '23514', null, 'orbit entity_id over 128 rejected');
select ok(exists (select 1 from pg_constraint where conname = 'orbit_events_type_length_check'), 'orbit type length cap present');
select ok(exists (select 1 from pg_constraint where conname = 'orbit_events_source_length_check'), 'orbit source length cap present');
select * from finish();
rollback;
