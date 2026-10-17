# Cloud apply PLAN: DEV-42 + DEV-45 to shanivar-prototype (ref katleeaentaugfszpmus)

Status: PLAN ONLY. Nothing has been applied, repaired or written in the cloud project. Prepared 17 Oct 2026
(Europe/Berlin) by Backend. Source of truth for the SQL: branch `dev-45-sync-engine` (head after the DEV-45
follow-up commit), which already contains the DEV-42 migrations. DEV-54 (`dev-54-sadhana-record-schema`)
is NOT part of this plan.

## 1. Current state (read-only, gathered 17 Oct ~22:50 Europe/Berlin)
- Supabase MCP `list_migrations` (cloud history):
  | cloud version | name |
  |---|---|
  | 20261017194817 | v1_local_first_sync |
  | 20261017194825 | stage1_self_check |
  The repo names the same two migrations `20261013090000_v1_local_first_sync.sql` and
  `20261014090000_stage1_self_check.sql` (they were applied through the MCP, which stamps the apply time).
  The CLI therefore sees the cloud versions as unknown remote migrations and the repo files as unapplied.
- MCP `list_tables` (public): routine_plans, protection_windows, practice_attempts, self_reports,
  orbit_events, all RLS on, columns exactly as in the two base migrations (no created_at,
  client_updated_at, tombstones yet).
- Read-only counts (MCP `execute_sql`, SELECT only): every public table 0 rows, rows violating any DEV-42
  check/cap 0, `auth.users` 0 rows.

## 2. Backup first (Free plan: no automated backups / PITR)
```
supabase link --project-ref katleeaentaugfszpmus          # asks for the DB password; do not paste it anywhere
supabase db dump --linked -f backup/cloud-schema-$(date +%Y%m%d-%H%M).sql
supabase db dump --linked --data-only -f backup/cloud-data-$(date +%Y%m%d-%H%M).sql
supabase db dump --linked --role-only -f backup/cloud-roles-$(date +%Y%m%d-%H%M).sql
```
(or `pg_dump "$DB_URL" -Fc -f cloud-$(date +%F).dump` with the session-pooler URL; keep the dump and
DB URL out of git and out of chat.) With 0 rows today the dump is mostly schema, still take it.

## 3. Align the migration history (no schema change)
Pre-check that the cloud schema really equals the two base migrations:
```
supabase db diff --linked --schema public      # expected: no differences vs 20261013090000 + 20261014090000
```
(run from a worktree checked out at a commit that has only the two base migrations, or compare the dump
from step 2 with `supabase db reset` output of those two files locally). If it is not empty: STOP.

Then rewrite the history table only (CLI, recommended):
```
supabase migration repair --linked --status reverted 20261017194817 20261017194825
supabase migration repair --linked --status applied  20261013090000 20261014090000
supabase migration list --linked               # local and remote columns must match for both rows
```
MCP-equivalent: there is no repair tool in the Supabase MCP, and `apply_migration` always records a new
apply-time version (it would recreate the mismatch). The only MCP route is a write to
`supabase_migrations.schema_migrations` via `execute_sql` (update version 20261017194817 -> 20261013090000,
20261017194825 -> 20261014090000). That is a write: not allowed under Backend's rules; use the CLI, or
the PM runs it explicitly.

## 4. Migrations to apply, in order (from dev-45-sync-engine)
1. `20261017120000_dev42_schema_hygiene.sql` (created_at, evidence_type checks, set_updated_at / set_created_at triggers)
2. `20261017120100_dev42_client_updated_at.sql` (client_updated_at on the 5 synced tables)
3. `20261017120200_dev42_size_caps.sql` (PM: plan 64 KB, packages 200, drift_reasons 20, ids 128)
4. `20261017120300_dev42_revoke_anon.sql` (PM: revoke all from anon, least privilege for authenticated)
5. `20261018090000_dev45_sync_conflicts.sql` (tombstones, sync_guard LWW, apply_tombstone)
```
supabase db push --linked --dry-run            # must list exactly these five, in this order
supabase db push --linked
```
(Apply with the CLI, not MCP `apply_migration`, so versions stay equal to the repo files.)

## 5. Pre-checks (read-only, run right before step 4; expected all 0)
```sql
select 'routine_plans' t, count(*) total, count(*) filter (where octet_length(plan::text) > 65536 or char_length(id) > 128) violating from public.routine_plans
union all select 'protection_windows', count(*), count(*) filter (where cardinality(packages) > 200 or char_length(id) > 128) from public.protection_windows
union all select 'practice_attempts', count(*), count(*) filter (where char_length(id) > 128 or evidence_type <> 'app_observed') from public.practice_attempts
union all select 'self_reports', count(*), count(*) filter (where char_length(id) > 128 or cardinality(drift_reasons) > 20 or evidence_type <> 'self_report') from public.self_reports
union all select 'orbit_events', count(*), count(*) filter (where char_length(id) > 128) from public.orbit_events;
```
Result on 17 Oct: all totals 0, violating 0. Any violating row > 0: STOP and decide per row (no silent fix).
Also check in the dashboard (read-only): Auth -> Anonymous sign-ins enabled (DEV-45 guest), and no custom
grants to anon on public tables that 20261017120300 would remove unexpectedly.

## 6. Rollback
- All five migrations are additive (columns, constraints, triggers, functions, one new table, revokes).
  Rollback = restore the step-2 dump into a fresh project, or a hand-written down migration in reverse
  order (drop tombstones + sync_guard/apply_tombstone triggers & functions; re-grant anon; drop the caps
  and evidence checks; drop client_updated_at / created_at columns; drop set_updated_at / set_created_at
  triggers), then `supabase migration repair --status reverted <version>` for each.
- With 0 rows there is no data at risk today; re-check counts if the app has been used since.

## 7. Post-apply verification
1. `supabase migration list --linked`: 7 rows, local = remote.
2. Advisors: MCP `get_advisors` (security and performance; read-only). Expected: no new lint (the local
   splinter run on the same SQL is 0 findings).
3. RLS / privileges (read-only SQL): every public table `relrowsecurity = true`; policies own_select /
   own_insert / own_update / own_delete on the five tables and own_select / own_insert / own_update on
   tombstones; `has_table_privilege('anon', t, 'SELECT')` false for every public table;
   triggers `set_updated_at` and `sync_guard` present on the five tables, `apply_tombstone` on tombstones.
4. AC10 e2e against the cloud (anon key from the dashboard, passed only as a Gradle property, never
   committed or logged):
   `bash gradlew testDebugUnitTest --tests '*SupabaseSyncTest*' -PsupabaseE2eUrl=https://katleeaentaugfszpmus.supabase.co -PsupabaseE2eAnonKey=<anon key>`
   (guest sign-in, push, 3 sends = 1 row, LWW, tombstone, second guest isolation). It creates two
   anonymous users and a few rows; delete those users in the dashboard afterwards (cascade removes rows).
5. pgTAP is local-only (`supabase test db` needs a superuser test DB); do not run it against the cloud.

## 8. Who / when
Needs the DB password and an explicit go from the PM/founder. Backend has not linked the CLI to the cloud
project and has applied nothing.
