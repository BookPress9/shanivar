-- DEV-54: Sadhana record schema (reflections, self_reports, practice_attempts, append-only orbit_events).
begin;
create extension if not exists pgtap with schema extensions;
select plan(36);

-- AC1 structure
select columns_are('public', 'practice_reflections',
  array['user_id','attempt_id','usefulness','note','client_updated_at','created_at','updated_at'],
  'practice_reflections columns (+ DEV-42 client_updated_at)');
select col_is_pk('public', 'practice_reflections', array['user_id','attempt_id'], 'reflection PK is (user_id, attempt_id)');
select col_is_null('public', 'practice_reflections', 'usefulness', 'usefulness is nullable');
select col_is_null('public', 'practice_reflections', 'note', 'note is nullable');
-- AC2 / AC3 / AC4 structure
select col_not_null('public', 'self_reports', 'private_by_default', 'private_by_default not null');
select col_default_is('public', 'self_reports', 'private_by_default', 'true', 'private_by_default defaults to true');
select has_column('public', 'practice_attempts', 'anchor', 'attempt anchor');
select has_column('public', 'practice_attempts', 'anchor_changes', 'attempt anchor_changes');
select col_type_is('public', 'practice_attempts', 'events', 'jsonb', 'attempt events jsonb');
select has_column('public', 'practice_attempts', 'created_at', 'attempt created_at');
select has_column('public', 'orbit_events', 'entity_id', 'orbit entity_id');
select has_column('public', 'orbit_events', 'created_at', 'orbit created_at');
select is((select col_description('public.orbit_events'::regclass, 4)), 'App OrbitEvent.occurrenceDate (local calendar date of the event).',
  'local_date documents the occurrenceDate mapping');
select policies_are('public', 'orbit_events', array['own_select','own_insert'], 'orbit_events keeps only own_select and own_insert');
-- AC7: no table for the local-only Sadhana SelfCheck
select is((select count(*)::int from pg_tables where schemaname = 'public' and tablename ~* 'self_?check'), 0, 'no SelfCheck table (local-only)');

insert into auth.users (id, email) values
  ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'a@test.local'), ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'b@test.local');
insert into public.practice_reflections (user_id, attempt_id, usefulness) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'att-b', 'neutral');
set local role authenticated;
set local request.jwt.claims = '{"sub":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"authenticated"}';

-- AC6: a reflection belongs to its owner only
select lives_ok($$ insert into public.practice_reflections (attempt_id, usefulness) values ('att-a', 'useful') $$, 'A records a reflection');
select lives_ok($$ insert into public.practice_reflections (attempt_id) values ('att-a2') $$, 'a reflection with no usefulness is allowed');
select results_eq($$ select attempt_id from public.practice_reflections order by 1 $$, $$ values ('att-a'), ('att-a2') $$, 'A sees only own reflections');
select throws_ok($$ insert into public.practice_reflections (user_id, attempt_id, usefulness) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'x', 'useful') $$, '42501', null, 'A cannot write a reflection for B');
select is_empty($$ update public.practice_reflections set usefulness = 'useful' where attempt_id = 'att-b' returning 1 $$, 'A cannot update B''s reflection');
select is_empty($$ delete from public.practice_reflections where attempt_id = 'att-b' returning 1 $$, 'A cannot delete B''s reflection');
select throws_ok($$ insert into public.practice_reflections (attempt_id, usefulness) values ('bad', 'great') $$, '23514', null, 'usefulness outside useful/neutral/uncomfortable is rejected');
select throws_ok($$ insert into public.practice_reflections (attempt_id, note) values ('long', repeat('n', 501)) $$, '23514', null, 'reflection note over 500 is rejected');
-- a cleared reflection is a tombstone; the server deletes it (keyed by attempt_id)
select lives_ok($$ insert into public.tombstones (kind, id, client_updated_at) values ('reflection', 'att-a2', now()) $$, 'reflection tombstone accepted');
select results_eq($$ select attempt_id from public.practice_reflections $$, $$ values ('att-a') $$, 'reflection tombstone removed the row');

-- AC2: drift reasons, delete
select lives_ok($$ insert into public.self_reports (id, local_date, kind, drift_reasons) values ('s1', current_date, 'stage1_check', '{thoughts,phone,other}') $$, 'allowed drift reasons pass');
select throws_ok($$ insert into public.self_reports (id, local_date, kind, drift_reasons) values ('s2', current_date, 'stage1_check', '{thoughts,boredom}') $$, '23514', null, 'an invalid drift reason is rejected');
select results_eq($$ select private_by_default from public.self_reports where id = 's1' $$, $$ values (true) $$, 'self-report is private by default');
select lives_ok($$ insert into public.tombstones (kind, id, client_updated_at) values ('self_report', 's1', now()) $$, 'DEV-38 delete as a DEV-45 tombstone');
select is_empty($$ select 1 from public.self_reports where id = 's1' $$, 'self-report tombstone deletes end to end');

-- AC3: attempt columns and the events cap
select throws_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome, anchor) values ('a1', 'p', '1', now(), 'ENDED', 'MOON') $$, '23514', null, 'anchor outside BREATH/ROOM is rejected');
select throws_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome, anchor_changes) values ('a1', 'p', '1', now(), 'ENDED', -1) $$, '23514', null, 'negative anchor_changes is rejected');
select throws_ok($$ insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome, events)
                    values ('a1', 'p', '1', now(), 'ENDED', jsonb_build_array(repeat('e', 32768))) $$, '23514', null, 'an oversized events value is rejected');

-- AC4: append-only orbit with value checks
select lives_ok($$ insert into public.orbit_events (id, type, local_date, source, entity_id) values ('o1', 'PRACTICE_ATTEMPTED', current_date, 'DEVICE_OBSERVED', 'a1') $$, 'valid orbit event');
select throws_ok($$ insert into public.orbit_events (id, type, local_date, source) values ('o2', 'STREAK', current_date, 'SELF_REPORTED') $$, '23514', null, 'invalid orbit type is rejected');
select throws_ok($$ update public.orbit_events set entity_id = 'x' where id = 'o1' $$, '42501', null, 'an orbit update is denied');

select * from finish();
rollback;
