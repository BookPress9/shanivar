-- DEV-54 PM follow-up (revertable alone): length caps on the text fields of the tables DEV-54 touches.
-- Ids 128 (as DEV-42), content_version / zone_id 64, orbit type / source 32 (already bounded by the
-- DEV-54 value checks; the explicit cap keeps the rule uniform for DEV-50..53).
alter table public.practice_attempts
  add constraint practice_attempts_practice_id_length_check check (char_length(practice_id) <= 128),
  add constraint practice_attempts_content_version_length_check check (char_length(content_version) <= 64),
  add constraint practice_attempts_zone_id_length_check check (char_length(zone_id) <= 64);
alter table public.self_reports
  add constraint self_reports_practice_id_length_check check (char_length(practice_id) <= 128),
  add constraint self_reports_attempt_id_length_check check (char_length(attempt_id) <= 128);
alter table public.orbit_events
  add constraint orbit_events_type_length_check check (char_length(type) <= 32),
  add constraint orbit_events_source_length_check check (char_length(source) <= 32);
