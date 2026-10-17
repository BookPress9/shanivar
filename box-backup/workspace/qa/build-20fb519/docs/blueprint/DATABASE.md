# Database — Shanivar

**Scope:** the backend Postgres only. Sadhana, Sankalp, Routine plan, web-app data are on-device (see `SECURITY.md` §6). Sources: `backend/src/db.js`, `backend/src/routes/*.js`, decisions 0001, 0004.

## 1. Database Engine
PostgreSQL on Supabase (`db.js:15-17` `DATABASE_URL`, SSL `rejectUnauthorized:false` for Supabase, pool max 10, `db.js:21-25`). All connections via the Supavisor pooler, never the direct host (0004).

## 2. Naming Conventions (observed, not enforced by tooling)
Tables snake_case plural (`focus_sessions`); columns snake_case; PK `id UUID DEFAULT uuid_generate_v4()`; FK `<entity>_id` (`user_id` → `users(id) ON DELETE CASCADE`); timestamps `created_at`/`updated_at TIMESTAMPTZ DEFAULT NOW()`; indexes `<table>_<col>_idx` or `_key`. DATE parsed as raw 'YYYY-MM-DD' string (`db.js:11`).

## 3. Tables (inline DDL; file:line of definition)
| Table | Purpose | Defined |
|---|---|---|
| `users` | accounts; extra columns added inline (checkins, membership, preferred_language, birth_*_input) | base DDL UNRESOLVED; ALTERs `db.js:43-49`; `ecommerce.js:17` |
| `discipline_preferences` | one row per user: wake time, days, dismissal method, step target, focus minutes, language | `discipline.js:63-82` |
| `focus_sessions` | protection sessions; one active per user | `discipline.js:84-104` |
| `alarm_schedules` | one editable alarm per user | `discipline.js:106-121` |
| `alarm_dismissal_events` | verified dismissals | `discipline.js:123-134` |
| `focus_schedules` | recurring window + when-then commitment (0010) | `discipline.js:147-162` |
| `alarm_fire_events` | telemetry: delivery path, device model | `telemetry.js:39-62` |
| `reflection_sessions/_cards/_responses/_idempotency_keys` | reflection flow | `reflections.js:39-95` |
| `user_measurements` | ecommerce webhook (tailoring orders) | `ecommerce.js:18-25` |
| `practice_completions`, `habit_logs`, `user_chart` | referenced in code/comments; DDL not in repo | `db.js:51-70` comment; UNRESOLVED |
- **UNRESOLVED:** `backend/scripts/setup-supabase.js:95` reads `../../database/schema.sql`; no `database/` directory or `.sql` file is tracked. Where is the base schema (users etc.) defined?
- **UNRESOLVED:** decision 0001 counts 10 tables; `SHANIVAR_TECHNICAL_MAP.md:274` counts 44 inline DDL statements. Which legacy tables (journal, community, gamification, habits, billing) exist in production?

## 4. Relationships
`users` 1—n `focus_sessions`, `alarm_schedules`, `alarm_dismissal_events`, `alarm_fire_events`, `reflection_sessions`, `focus_schedules`; 1—1 `discipline_preferences` (`user_id UNIQUE`). `alarm_schedules` 1—n `alarm_dismissal_events` / `alarm_fire_events` (`ON DELETE SET NULL`). `reflection_sessions` 1—n cards, responses (CASCADE).

## 5. Constraints
Unique: one active focus session per user (partial index, `discipline.js:103`); `reflection_cards (session_id,index)`; `reflection_responses (session_id,card_id)`; `alarm_fire_events (user_id, client_event_id)` where not null; `practice_completions` per user/practice/UTC day (`db.js:70-72`). CHECK: dismissal_method in movement/accessible/qr/photo_motion; `step_target` 1-200; `focus_minutes` 5-120; language en/hi; `whatsapp_tone` direct/gentle.

## 6. Indexes
Per-user lookups (`*_user_id_idx`), `focus_sessions_user_status_idx`, `alarm_fire_events_manufacturer_idx` (reliability by device). Reasons are in the DDL comments.

## 7. Data Validation
Server-side route validation; `xss` sanitizer (`utils/sanitize.js`). UNRESOLVED: per-route rules are not catalogued.

## 8. Soft Delete
No. Hard delete via `ON DELETE CASCADE`. UNRESOLVED: is there an account-deletion endpoint? (needed for SOP-09 delete-on-request).

## 9. Auditing
Dismissals and alarm fires are recorded as events. No created_by/updated_by/deleted_by. Reflection idempotency is stored per operation.

## 10. Sensitive Data
Email, password hash, birth date/time/location inputs (`db.js:47-49`), reflection responses (free text), device model. Protection: `SECURITY.md`. UNRESOLVED: encryption at rest beyond Supabase defaults; retention periods.

## 11. Migration Rules (reality)
- **No migration runner (DEV-9).** Schema changes are idempotent `CREATE/ALTER ... IF NOT EXISTS` run at process boot (`db.js:36`, `discipline.js:60`, `telemetry.js:37`, `reflections.js:37`) — decision 0001.
- Inline DDL cannot backfill data or revert; `CREATE TABLE IF NOT EXISTS` is a no-op on an existing table, so columns need separate `ALTER ... ADD COLUMN IF NOT EXISTS` (`telemetry.js:30` comment).
- Reopen triggers: second instance, ~15 tables, any backfill, any revert (0001 "Revisit when"). Runner must adopt existing schema as migration `0000`.
- Every schema change: document here, add the DDL, test, never break production rows. Docs-only tasks never run DDL.

## 12. Seed Data
UNRESOLVED: none documented. `backend/src/mock_server.js` runs an in-memory mock (`npm run dev:mock`).
