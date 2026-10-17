-- DEV-45 AC5/AC6 server side: idempotent upserts, last writer wins on client_updated_at,
-- tombstones (public.tombstones) delete rows and beat older upserts. Runs as a real guest JWT.
begin;
create extension if not exists pgtap with schema extensions;
select plan(22);

insert into auth.users (id, email) values
  ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'a@test.local'),
  ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'b@test.local');
set local role authenticated;
set local request.jwt.claims = '{"sub":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"authenticated"}';

-- PostgREST merge-duplicates upsert, as the engine sends it
prepare up_sr(text, int, timestamptz) as
  insert into public.self_reports (id, local_date, actual_minutes, client_updated_at) values ($1, '2026-10-17', $2, $3)
  on conflict (user_id, id) do update set local_date = excluded.local_date, actual_minutes = excluded.actual_minutes,
    client_updated_at = excluded.client_updated_at;
prepare tomb(text, text, timestamptz) as
  insert into public.tombstones (kind, id, client_updated_at) values ($1, $2, $3)
  on conflict (user_id, kind, id) do update set client_updated_at = excluded.client_updated_at;

-- AC5: 3 sends give 1 row (also the crash-after-ack resend case: same payload again)
execute up_sr('s1', 5, '2026-10-17T08:00:00Z');
execute up_sr('s1', 5, '2026-10-17T08:00:00Z');
execute up_sr('s1', 5, '2026-10-17T08:00:00Z');
select is((select count(*) from public.self_reports where id = 's1'), 1::bigint, 'three identical sends give one row');

-- AC6: last writer wins on client_updated_at
execute up_sr('s1', 9, '2026-10-17T09:00:00Z');
select is((select actual_minutes from public.self_reports where id = 's1'), 9, 'newer write applies');
select lives_ok($$ execute up_sr('s1', 2, '2026-10-17T07:00:00Z') $$, 'older write is accepted without error (acked)');
select is((select actual_minutes from public.self_reports where id = 's1'), 9, 'older write is ignored server-side');
select lives_ok($$ insert into public.self_reports (id, local_date, actual_minutes) values ('s1', '2026-10-17', 3)
                   on conflict (user_id, id) do update set actual_minutes = excluded.actual_minutes,
                     client_updated_at = excluded.client_updated_at $$, 'write without client time is accepted');
select is((select actual_minutes from public.self_reports where id = 's1'), 9, 'write without client time never overwrites a stamped row');
execute up_sr('s1', 9, '2026-10-17T09:00:00Z');
select is((select count(*) from public.self_reports where id = 's1'), 1::bigint, 'equal-time resend is harmless');

-- AC5: a tombstone after an upsert ends with no row
select lives_ok($$ execute tomb('self_report', 's1', '2026-10-17T10:00:00Z') $$, 'tombstone upsert accepted');
select is((select count(*) from public.self_reports where id = 's1'), 0::bigint, 'tombstone after upsert: no row');
select lives_ok($$ execute tomb('self_report', 's1', '2026-10-17T10:00:00Z') $$, 'tombstone resend is idempotent');
select is((select count(*) from public.tombstones where id = 's1'), 1::bigint, 'one tombstone row per key');

-- AC6: tombstones beat older upserts (a late, older write cannot resurrect the row)
select lives_ok($$ execute up_sr('s1', 9, '2026-10-17T09:30:00Z') $$, 'late older upsert accepted (acked)');
select is((select count(*) from public.self_reports where id = 's1'), 0::bigint, 'late older upsert does not resurrect');
select lives_ok($$ insert into public.self_reports (id, local_date) values ('s1', '2026-10-17') on conflict do nothing $$, 'unstamped insert accepted');
select is((select count(*) from public.self_reports where id = 's1'), 0::bigint, 'unstamped insert does not resurrect');

-- a tombstone for a row the server never saw is stored and still beats an older upsert
execute tomb('attempt', 'a-never', '2026-10-17T10:00:00Z');
insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome, client_updated_at)
  values ('a-never', 'p', '1', now(), 'ENDED', '2026-10-17T09:00:00Z');
select is((select count(*) from public.practice_attempts where id = 'a-never'), 0::bigint, 'tombstone before the upsert arrives still wins');

-- an older tombstone never deletes a newer row; an older tombstone does not roll back a newer one
insert into public.practice_attempts (id, practice_id, content_version, started_at, outcome, client_updated_at)
  values ('a2', 'p', '1', now(), 'ENDED', '2026-10-17T12:00:00Z');
execute tomb('attempt', 'a2', '2026-10-17T11:00:00Z');
select is((select count(*) from public.practice_attempts where id = 'a2'), 1::bigint, 'older tombstone keeps a newer row');
execute tomb('attempt', 'a-never', '2026-10-17T08:00:00Z');
select is((select client_updated_at from public.tombstones where id = 'a-never'), '2026-10-17T10:00:00Z'::timestamptz, 'older tombstone resend keeps the newer tombstone');

-- isolation: tombstones are own-rows only and cannot delete another user's rows
reset role;
insert into public.self_reports (user_id, id, local_date, client_updated_at) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'b1', '2026-10-17', '2026-10-17T08:00:00Z');
set local role authenticated;
set local request.jwt.claims = '{"sub":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","role":"authenticated"}';
select throws_ok($$ insert into public.tombstones (user_id, kind, id, client_updated_at) values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'self_report', 'b1', now()) $$,
                 '42501', null, 'A cannot write a tombstone for B');
execute tomb('self_report', 'b1', '2030-01-01T00:00:00Z');
reset role;
select is((select count(*) from public.self_reports where id = 'b1'), 1::bigint, 'A''s tombstone with B''s id does not touch B''s row');
set local role authenticated;
set local request.jwt.claims = '{"sub":"bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb","role":"authenticated"}';
select is_empty($$ select 1 from public.tombstones $$, 'B sees none of A''s tombstones');
select throws_ok($$ delete from public.tombstones $$, '42501', null, 'clients cannot delete tombstones');

select * from finish();
rollback;
