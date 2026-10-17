# Supabase: findings and local E2E (13 Oct 2026)

## Findings
- Repo: no supabase/ project existed. `backend/src/lib/supabase.js` reads SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY / SUPABASE_ANON_KEY; DATABASE.md says Postgres on Supabase via the Supavisor pooler. Base schema file `database/schema.sql` is not tracked.
- Laptop: `TheHybridMonk/backend/.env` and `archive_deployment/.env.production` point at cloud project `zpukheowjaxvocbplfmp.supabase.co` (keys present, not printed). That hostname no longer resolves (NXDOMAIN): the project appears deleted or the ref changed. No health check possible. No supabase CLI or Docker on the laptop. Nothing was created or changed in the cloud.

## Local stack (box)
`supabase init` + `supabase start` (CLI 2.120, Docker). Running: db, auth, rest, kong, mailpit. Disabled in config.toml to save memory: realtime, studio, storage, analytics. Anonymous sign-ins enabled (guest).
Box fix needed: legacy iptables FORWARD policy was DROP; set to ACCEPT so containers can reach each other.

## /v1 contract → Supabase
| HANDOFF /v1 | Supabase |
|---|---|
| guest session | Auth anonymous sign-in (`/auth/v1/signup` with `{}`) |
| Google sign-in | Auth Google provider — needs founder's OAuth client id/secret |
| email passwordless | Auth email OTP — needs an SMTP provider for production |
| PUT routine | `routine_plans` upsert |
| protection windows | `protection_windows` upsert |
| practice attempts | `practice_attempts` upsert (app_observed) |
| self-reports | `self_reports` upsert (self_report) |
| orbit | `orbit_events` upsert |
All tables: RLS own-rows-only via auth.uid(), idempotent on (user_id, id). Edge functions: none needed yet (account deletion/export later).

## Client
`SupabaseSync.kt`: SupabaseClient (anon sign-in, upsert, select) + SyncSender for SyncOutbox. Flag: BuildConfig.SUPABASE_URL empty by default = off.

## E2E
`./gradlew testDebugUnitTest --tests '*SupabaseSyncTest' -PsupabaseE2eUrl=http://127.0.0.1:54321 -PsupabaseE2eAnonKey=<local anon key from supabase status>`
Guest sign-in → outbox push (attempt + self-report) → idempotent retry → read back → second guest sees nothing (RLS). Skipped when not configured.

## After a box restart (DEV-45)
`dockerd` is not a system service on the box (no systemd), so the stack is gone after a restart. Bring it back with:
```
scripts/dev/start-local-supabase.sh [worktree-dir] [--reset] [--test]
```
It starts `dockerd` (log `/tmp/dockerd.log`), opens the Docker socket to the box user, sets the iptables FORWARD policy to ACCEPT, runs `supabase start` in the given worktree (default: this repo), and with `--reset` / `--test` re-applies that worktree's migrations and runs the pgTAP tests. It prints URLs only, never keys. A copy lives at `/workspace/bin/start-local-supabase.sh`. Prerequisites (one-time): `sudo apt-get install -y docker.io` and the Supabase CLI (v2.120.0) in `/usr/local/bin`.
All worktrees share `project_id = "shanivar-v3"`, so only one stack runs at a time; `--reset` from a worktree switches the local schema to that branch.

DEV-45 E2E (engine against the local stack): `./gradlew testDebugUnitTest -PsupabaseE2eUrl=http://127.0.0.1:54321 -PsupabaseE2eAnonKey="$ANON_KEY"` with `eval "$(supabase status -o env | grep '^ANON_KEY=')"`, so the key never appears on screen.
