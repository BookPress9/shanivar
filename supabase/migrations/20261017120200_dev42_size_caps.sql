-- DEV-42 AC3 (PM addition, revertable alone): payload size caps.
-- note already has check (char_length(note) <= 500) from 20261014090000 and stays as is.
alter table public.routine_plans
  add constraint routine_plans_plan_size_check check (octet_length(plan::text) <= 65536),
  add constraint routine_plans_id_length_check check (char_length(id) <= 128);
alter table public.protection_windows
  add constraint protection_windows_packages_count_check check (cardinality(packages) <= 200),
  add constraint protection_windows_id_length_check check (char_length(id) <= 128);
alter table public.practice_attempts
  add constraint practice_attempts_id_length_check check (char_length(id) <= 128);
alter table public.self_reports
  add constraint self_reports_drift_reasons_count_check check (cardinality(drift_reasons) <= 20),
  add constraint self_reports_id_length_check check (char_length(id) <= 128);
alter table public.orbit_events
  add constraint orbit_events_id_length_check check (char_length(id) <= 128);
