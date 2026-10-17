# Frontend → Backend alignment (Shanivar Frontend, 17 Oct 2026)
Design source of truth: android-app/wireframes/initiation-v1 and sadhana-v1 (HANDOFF.md "Data model"), plus decision 0018.
Reviewed: supabase/migrations 20261013090000_v1_local_first_sync.sql, 20261014090000_stage1_self_check.sql (HEAD b372079).

## Already aligned
- Own-rows RLS on auth.uid(), anonymous guests ok; idempotent (user_id,id) upserts.
- practice_attempts matches SadhanaAttempt / SyncPayloads.attempt exactly.
- self_reports Stage 1 columns match Stage1.payload (SadhanaStage1.kt) and decision 0018.
- UTC + zone_id + local_date approach follows HANDOFF.

## Requested changes (priority order)
1. profiles/onboarding_drafts (Identity/Auth/Foundation; InitiationOnboarding.kt OnboardingDraft): user_id PK, display_name 1-60, identity_mode (none/guest/google/email), checkpoint, schema_version, completed_at, revision, created_at, updated_at, own-rows RLS.
2. routine_plans: add revision int not null default 0, time_zone text not null (IANA), practice_local time; RPC put_routine(plan, time_zone, practice_local, expected_revision) returning canonical row and raising conflict on mismatch (RoutineSync.kt expects 409).
3. Protection windows: one home only (either plan.windows+protectedPackages, or the table with packages dropped/shared). Add time_zone, revision, created_at; desired_enabled vs enabled (RoutinePlan.kt ProtectionWindowV1).
4. orbit_events (LivingOrbit.kt): add entity_id, created_at; check type in (ROUTINE_CREATED, ALARM_CONFIGURED, BOUNDARY_SAVED, PRACTICE_ATTEMPTED, RETURN_DAY, INDEPENDENT_RECORD), source in (CONFIGURED, DEVICE_OBSERVED, SELF_REPORTED); local_date vs occurrence_date (rename or document mapping); drop own_update policy (append-only).
5. practice_reflections (sadhana Return; SadhanaRecord.kt PracticeReflectionRecord): user_id, attempt_id, usefulness (useful/neutral/uncomfortable, nullable), note nullable, created_at, updated_at; PK (user_id, attempt_id).
6. guidance_preferences: user_id PK, detail (full/reduced/minimal), changed_by (user/suggestion), reminders (normal/light/off), revision, updated_at.
7. self_reports: add private_by_default bool not null default true; check drift_reasons ⊆ (thoughts, sounds, body, phone, sleepy, emotion, other); support delete. Drop redundant `set not null`.
8. practice_attempts: add anchor (BREATH/ROOM), anchor_changes int, events jsonb, created_at (SadhanaAttempt.kt).
9. updated_at trigger + created_at on every table.
10. alarm_configs (device_id, wake_local, repeat_days, desired_enabled, config_revision) and alarm_observations (config_revision, status off/configured/permission_required/scheduled/failed, next_fire_at, reason, observed_at) — DeviceObservations.kt.
11. device_observations: device_id, config_revision, usage_access, overlay_access, alarm_capability, effective_protection_state, observed_at (device stays authority).
12. evidence_type checks: app_observed / self_report.
13. Later: practice_definitions, content_review_manifests, source_records — founder-approved/wireframe text only, no breath pacing.
14. Add empty supabase/seed.sql or disable [db.seed] (file referenced but missing). Do not copy mock_server.js / practices.js content.
15. Auth: Google + email OTP once founder credentials + SMTP available.
16. Edge functions: account deletion and export (SUPABASE_E2E.md).
17. Keep legacy Node backend (backend/src: practices.js /generate, mock_server.js, ai.js, insights.js) away from the new app: it has breath pacing, energy/nervous-system claims, XP/streaks, "Brahma Labs" branding.

## Not needed
- Sadhana SelfCheck in SadhanaSpine.kt is local-only by founder decision (13 Oct) — no table unless founder changes this.

## Frontend side (my work, FYI)
Android currently only queues attempt + stage1_check rows; routine/window/orbit/onboarding payloads, independent-practice sync and outbox deletes are still to build.
