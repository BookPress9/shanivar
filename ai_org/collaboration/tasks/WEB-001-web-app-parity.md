# WEB-001 · Web app parity on the Shanivar Supabase contract

- **State:** REVIEW
- **Owner:** Codex
- **Started:** 2026-10-17

## Goal

Turn `web-app/` into a responsive, installable web application that carries the same Routine, Sadhana, records and account/sync behavior as the current native product where browsers support it. Use the existing Supabase schema and authentication endpoints. Make no backend, migration or native-app changes.

## Owned paths

- `web-app/**`
- `ai_org/collaboration/tasks/WEB-001-web-app-parity.md`
- `ai_org/collaboration/reports/WEB-001-codex.md`
- Web-app documentation and continuity entries required for this task

## Inputs

- `SHANIVAR_STANCE.md`
- decisions 0016, 0017 and 0019
- `android-app/app/src/main/java/com/shanivar/nativeapp/`
- `supabase/migrations/`
- existing `web-app/`

## Deliverables

1. Responsive browser UI for account entry, Routine, Sadhana, records/Orbit and settings.
2. Local-first persistence and an authenticated Supabase outbox using the existing tables and conflict contract.
3. Installable PWA behavior and offline shell.
4. Clear browser capability language for device alarms and application blocking.
5. Automated tests for core rules, payloads and offline sync behavior.
6. A review report with validation, limitations and handoff.

## Acceptance checks

- No file under `supabase/`, `backend/` or `android-app/` changes.
- Guest authentication uses Supabase anonymous auth; email code and Google use existing Auth endpoints when configured.
- Sync writes omit `user_id`, include `client_updated_at`, use authenticated RLS, and preserve queued writes offline.
- Routine and Sadhana remain usable locally without a configured backend or network.
- A browser never claims that a native alarm fired or another application was blocked.
- Keyboard, narrow/mobile and desktop layouts are reviewable.
- Tests pass and a production-shaped static serve smoke test succeeds.
