-- DEV-42 AC4 (PM addition, revertable alone): least privilege for the API roles.
-- anon (no session) gets nothing; guests sign in anonymously and act as authenticated.
-- authenticated keeps only what the own_select/own_insert/own_update/own_delete policies need
-- (drops the Supabase default TRUNCATE, REFERENCES and TRIGGER; TRUNCATE ignores RLS).
do $$
declare t text;
begin
  foreach t in array array['routine_plans','protection_windows','practice_attempts','self_reports','orbit_events'] loop
    execute format('revoke all on public.%I from anon', t);
    execute format('revoke all on public.%I from authenticated', t);
    execute format('grant select, insert, update, delete on public.%I to authenticated', t);
  end loop;
end $$;
