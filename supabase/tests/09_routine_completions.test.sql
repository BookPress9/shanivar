-- DEV-72: one completion per step occurrence, server validation, 409 on a stale routine revision, isolation, undo.
begin;
create extension if not exists pgtap with schema extensions;
select plan(24);

select col_is_pk('public', 'routine_step_completions', array['user_id','id'], 'PK is (user_id, occurrence key)');
select policies_are('public', 'routine_step_completions', array['own_select','own_insert','own_update','own_delete'], 'own-row policies');

insert into auth.users (id, email) values
  ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'a@test.local'), ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'b@test.local');
insert into public.routine_step_completions (user_id, id, step_id, scheduled_local_date, completed_at, tz, client_updated_at)
  values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'sb@2026-10-13', 'sb', '2026-10-13', '2026-10-13T08:00:00+02', 'Europe/Berlin', '2026-10-13T08:00:00+02');

set local role authenticated;
set local request.jwt.claims = '{"sub":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"authenticated"}';

-- AC: 5 identical requests -> 1 row (the PostgREST upsert the DEV-45 sender sends)
do $$ begin
  for i in 1..5 loop
    insert into public.routine_step_completions (id, step_id, scheduled_local_date, scheduled_local_time, completed_at, tz, plan_revision, client_updated_at)
    values ('s1@2026-10-13', 's1', '2026-10-13', '07:00', '2026-10-13T07:10:00+02', 'Europe/Berlin', 0, '2026-10-13T07:10:00+02')
    on conflict (user_id, id) do update set completed_at = excluded.completed_at, client_updated_at = excluded.client_updated_at,
      scheduled_local_time = excluded.scheduled_local_time, plan_revision = excluded.plan_revision, tz = excluded.tz;
  end loop;
end $$;
select is((select count(*)::int from public.routine_step_completions where step_id = 's1'), 1, '5 identical requests give 1 row');
-- offline replay of an older mark is ignored (DEV-45 LWW), the row stays as is
insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz, client_updated_at)
  values ('s1@2026-10-13', 's1', '2026-10-13', '2026-10-13T07:05:00+02', 'Europe/Berlin', '2026-10-13T07:05:00+02')
  on conflict (user_id, id) do update set completed_at = excluded.completed_at, client_updated_at = excluded.client_updated_at;
select results_eq($$ select count(*)::int, min(completed_at) from public.routine_step_completions where step_id = 's1' $$,
  $$ values (1, '2026-10-13T07:10:00+02'::timestamptz) $$, 'offline replay: still 1 row, older mark ignored');
select is((select scheduled_local_time from public.routine_step_completions where id = 's1@2026-10-13'), '07:00'::time, 'scheduled time kept');

-- server validation
select throws_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz) values ('x@2026-10-13', 's2', '2026-10-13', now(), 'Europe/Berlin') $$,
  '23514', null, 'occurrence key must be step_id@date');
select throws_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz) values ('s2@2026-10-13', 's2', '2026-10-13', '2026-10-13T07:00:00Z', 'Mars/Base') $$,
  '23514', 'invalid_tz', 'unknown time zone rejected');
select throws_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz) values ('s2@2026-10-13', 's2', '2026-10-13', '2026-10-12T21:00:00Z', 'Europe/Berlin') $$,
  '23514', 'completion_outside_window', 'before the scheduled local date rejected (12 Oct 23:00 Berlin)');
select lives_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz) values ('s2@2026-10-13', 's2', '2026-10-13', '2026-10-14T21:30:00Z', 'Europe/Berlin') $$,
  'late completion by the end of the next local day is allowed (14 Oct 23:30 Berlin)');
select throws_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz) values ('s3@2026-10-13', 's3', '2026-10-13', '2026-10-14T22:30:00Z', 'Europe/Berlin') $$,
  '23514', 'completion_outside_window', 'after the grace day rejected (15 Oct 00:30 Berlin): the occurrence is missed');
select throws_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz) values ('s4@' || current_date, 's4', current_date, now() + interval '1 hour', 'UTC') $$,
  '23514', 'completed_in_future', 'a completion in the future is rejected');
select throws_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz) values ('bad id@2026-10-13', 'bad id', '2026-10-13', '2026-10-13T07:00:00Z', 'UTC') $$,
  '23514', null, 'step_id must be an app id (no free text)');
select throws_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz, plan_revision) values ('s5@2026-10-13', 's5', '2026-10-13', '2026-10-13T07:00:00Z', 'UTC', -1) $$,
  '23514', null, 'negative plan_revision rejected');

-- isolation
select results_eq($$ select id from public.routine_step_completions order by id $$, $$ values ('s1@2026-10-13'), ('s2@2026-10-13') $$, 'A sees only own completions');
select throws_ok($$ insert into public.routine_step_completions (user_id, id, step_id, scheduled_local_date, completed_at, tz) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'z@2026-10-13', 'z', '2026-10-13', '2026-10-13T07:00:00Z', 'UTC') $$,
  '42501', null, 'A cannot complete a step for B');
select is_empty($$ update public.routine_step_completions set tz = 'UTC' where id = 'sb@2026-10-13' returning 1 $$, 'A cannot update B''s completion');
select is_empty($$ delete from public.routine_step_completions where id = 'sb@2026-10-13' returning 1 $$, 'A cannot delete B''s completion');

-- undo = tombstone; a late replay of the mark does not resurrect it
select lives_ok($$ insert into public.tombstones (kind, id, client_updated_at) values ('step_completion', 's1@2026-10-13', '2026-10-13T07:30:00+02') $$, 'undo via tombstone');
select is_empty($$ select 1 from public.routine_step_completions where id = 's1@2026-10-13' $$, 'undo removed the completion');
insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz, client_updated_at)
  values ('s1@2026-10-13', 's1', '2026-10-13', '2026-10-13T07:10:00+02', 'Europe/Berlin', '2026-10-13T07:10:00+02')
  on conflict (user_id, id) do update set completed_at = excluded.completed_at, client_updated_at = excluded.client_updated_at;
select is_empty($$ select 1 from public.routine_step_completions where id = 's1@2026-10-13' $$, 'replayed mark after undo does not resurrect it');
-- re-completing after the undo (newer client time) is a new completion
select lives_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz, client_updated_at)
  values ('s1@2026-10-13', 's1', '2026-10-13', '2026-10-13T07:40:00+02', 'Europe/Berlin', '2026-10-13T07:40:00+02') $$, 'complete again after undo');

-- 409 on a stale revision (simulates DEV-52's routine_plans.revision inside this transaction)
reset role;
alter table public.routine_plans add column revision int not null default 0;
insert into public.routine_plans (user_id, id, plan, revision)
  values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'current', '{"activities":[{"id":"s1","enabled":true},{"id":"s6","enabled":false}]}', 3);
set local role authenticated;
select throws_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz, plan_revision) values ('s9@2026-10-13', 's9', '2026-10-13', '2026-10-13T07:00:00Z', 'UTC', 2) $$,
  'PT409', 'stale_plan_revision', 'step removed in a newer revision: 409');
select throws_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz, plan_revision) values ('s6@2026-10-13', 's6', '2026-10-13', '2026-10-13T07:00:00Z', 'UTC', 2) $$,
  'PT409', 'stale_plan_revision', 'step disabled in a newer revision: 409');
select lives_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz, plan_revision) values ('s1@2026-10-14', 's1', '2026-10-14', '2026-10-14T07:00:00Z', 'UTC', 2) $$,
  'older revision but the step still exists: accepted');
select lives_ok($$ insert into public.routine_step_completions (id, step_id, scheduled_local_date, completed_at, tz, plan_revision) values ('s9@2026-10-14', 's9', '2026-10-14', '2026-10-14T07:00:00Z', 'UTC', 3) $$,
  'current revision: accepted');

select * from finish();
rollback;
