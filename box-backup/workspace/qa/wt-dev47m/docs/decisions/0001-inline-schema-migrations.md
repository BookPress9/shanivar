# 0001 — Schema changes run inline at boot, not through a migration runner

- **Date:** 2026-09-12
- **Status:** Accepted, with a defined expiry (see *Revisit when*)

## Context

The backend applies its schema with 44 `CREATE TABLE IF NOT EXISTS` / `ALTER TABLE` statements
executed at module load, spread across 5 route files (`db.js`, `discipline.js`, `reflections.js`,
`telemetry.js`, `ecommerce.js`). There is no migration runner, no ordered migration files, and no
record anywhere of which schema version a given database is on.

This was not designed. It accreted because no migration runner existed when the first table was
needed, and each subsequent table followed the local convention.

It has genuinely earned its place so far: a single Render instance pointed at a fresh Supabase
database brings itself fully up without a deploy step, which is why the movement contract landed on
production without a manual migration.

## Options

1. **Keep inline DDL.** Zero ceremony, self-healing, no deploy step. Breaks on multiple instances.
2. **Adopt a migration runner now** (node-pg-migrate, Knex, Umzug). Correct long-term; costs a day
   and touches all 5 route files while two agents are actively editing them.
3. **Hybrid** — freeze inline DDL for existing tables, require new tables to go through a runner.
   Two conventions at once, which is how codebases become unreadable.

## Decision

Option 1 for now. Option 2 before the trigger below. Explicitly not option 3.

## Consequences

Accepted, with eyes open:

- **Concurrent instances race.** Two containers booting together run the same DDL simultaneously.
  Postgres will serialise most of it, but `ALTER TABLE` under contention can deadlock a boot.
- **`CREATE TABLE IF NOT EXISTS` silently no-ops against an existing, drifted table.** Adding a
  column to the `CREATE` block does nothing on a live database — the change must be written a
  second time as an explicit `ALTER TABLE`. This trap has already cost one debugging cycle and is
  the single most likely way a future schema change appears to work and doesn't.
- **No rollback, and no way to ask production what version it is on.**
- The startup path grows slower with every table.

## Revisit when — any one of these

- A **second backend instance** is provisioned (horizontal scale, blue-green, or a worker). This is
  the hard trigger; the racing problem is real from instance two onward.
- Table count passes **~15** (currently 10: `discipline_preferences`, `alarm_schedules`,
  `alarm_dismissal_events`, `alarm_fire_events`, `focus_sessions`, `reflection_sessions`,
  `reflection_cards`, `reflection_responses`, `reflection_idempotency_keys`, `user_measurements`).
- Any change requires a **data backfill**, which inline idempotent DDL fundamentally cannot express.
- A schema change needs to be **reverted** in production.

## Notes for whoever does the migration

Write the runner to adopt the existing schema as migration `0000` (baseline) rather than trying to
recreate it — production already has these tables, and replaying the inline DDL against it will
no-op misleadingly rather than fail loudly.
