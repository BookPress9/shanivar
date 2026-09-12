# Product Owner: first prototype APK — delivery tracker

Role stood up 2026-09-10, by the Staff Manager, at founder request: drive toward a first prototype
APK the founder can actually install and use, delegating backend work to Claude and frontend/UI
work to Codex. This sits above the individual feature squads (`reflection_scroller_squad.md`,
`discipline_engine_squad.md`) — those are done at the backend/native-engine layer; this tracks what
still separates that from a phone-in-hand experience.

## Definition of done for v0 (not a full release)
The founder installs a debug APK on their own physical Android phone (emulator is permanently
banned on the dev machine — see memory) and, end to end against the live backend:
1. Signs in, sees Today, reads a daily note.
2. Runs a Practice session.
3. Opens "Reflect with Shanivar" and has a real scrolling exchange.
4. Starts a real Focus lock and releases it; sets a real alarm and dismisses it for real (QR or
   photo+motion).
Explicitly out of scope for this bar: payments/pricing, WhatsApp nudges, audio narration, visual
polish — `PRODUCT_DIRECTION.md` already defers these correctly.

## Status as of this pass (verified today, not carried over from memory)
- **Backend**: healthy. `/api/health` answers on both `localhost:5000` and the LAN IP.
- **Device reachability — was broken, fixed today.** `android-app/gradle.properties` hardcoded
  `shanivarApiUrl=http://10.0.2.2:5000/api`, which only resolves inside an emulator. Since the
  emulator is permanently banned on this machine, a debug APK built before today's fix could not
  reach the backend **at all** on a real phone — not just for discipline engine, for everything
  (`ShanivarApi` reads `BuildConfig.API_URL` as its one source of truth, so this blocked login, the
  daily note, practices, and the reflection scroller too, not only Focus/Rhythm). Repointed it at
  the dev machine's current LAN IPv4 (`192.168.1.11`) and proved it end to end: `curl` against that
  address returns a live health response, and a from-scratch clean rebuild (cleared `app/build` —
  OneDrive's file sync was corrupting Gradle's incremental-compile cache mid-build, causing one
  stale build with a leftover port baked in; a clean rebuild fixed it, not a code change) now bakes
  in the correct URL, confirmed by reading the generated `BuildConfig.java` directly.
  - **Caveat**: this LAN IP is only reachable while the phone is on the same Wi-Fi as this machine
    and the backend is running (`npm start` in `backend/`). It will need updating if this machine's
    IP changes networks — `gradle.properties` now has a comment explaining exactly how.
- **Debug APK built and current**: `android-app/app/build/outputs/apk/debug/app-debug.apk` (~33MB,
  today's clean build). 58/58 JVM unit tests passing, `lintDebug` clean, both reverified after the
  clean rebuild — not stale numbers carried over from the discipline-engine reconciliation.
- **Reflection Scroller**: wired end to end in the UI already (`ShanivarApp.kt`'s "Reflect with
  Shanivar" button, `ReflectionNavigation`, `ReflectionScreen`) — Codex has already done this
  integration. Will work for real once a phone can reach the backend, which is now true.
- **Discipline Engine (Focus/Rhythm/alarms): backend and native engine are both complete and
  tested in isolation, but neither is called from the app's UI yet.** Confirmed by grep, not
  assumed: `AlarmScheduler.schedule()` is only ever called from `BootRescheduleReceiver` (which
  reschedules an alarm that was already scheduled — nothing schedules one the first time), and
  `DisciplineEngine.startFocus`/`releaseFocus` are never called outside `DisciplineEngine.kt`
  itself. The Focus tab still says "App lock is not connected in this build" and the Rhythm screen
  still says "Alarm not scheduled. Saving records your preference only." — both literally true,
  unchanged since those screens were first written. This is the single biggest gap between "backend
  feature complete" and "founder can use it."

## Punch list

### P0 — blocks the v0 bar above
1. **[Codex]** Wire the Focus tab to the real `DisciplineEngine` (`status()` / `startFocus()` /
   `releaseFocus()`) in place of the static copy in `FocusScreen` (`ShanivarApp.kt`).
2. **[Codex]** Wire the Rhythm screen's alarm section to `AlarmScheduler.schedule()` and the backend
   `/api/discipline/preferences` + `/api/discipline/alarms` routes, in place of the local-only
   `saveRhythm`/`loadRhythm` SharedPreferences round trip.
3. **[Codex]** Build the permission-grant onboarding (usage access, overlay, exact alarm, camera) —
   already scoped step by step in `android-app/CLAUDE_DISCIPLINE_ENGINE_NATIVE_CONTRACT.md`. Without
   this, #1 and #2 will just silently fail on a real device.
4. **[Claude]** Device reachability — done today, see above.

### P1 — real, but doesn't block a founder-only prototype
5. **[Claude]** Auth token is in-memory only (not persisted to disk) — the founder will need to
   sign in again after any process death (app-lock overlay killed in the background, a reboot,
   Android reclaiming memory). Worth fixing before wider testing, not before the founder's own first
   look.
6. **[Both]** Real-device verification of everything in #1–#3 once Codex's wiring lands. The
   emulator is permanently banned on this machine, so this can only happen by sideloading the APK
   onto a real phone, or via a cloud device lab (e.g. Firebase Test Lab) — not by anything runnable
   here.

### Founder decisions still open (flagged, not guessed)
- Focus-session duration bounds and staleness-grace period (currently 5–480 min + 30 min,
  placeholder values from the backend agent).
- Paid-plan/pricing for the app-lock entitlement gate (the gate is real and wired to the existing
  `membership` column; there is no actual plan to sell yet, so nobody is blocked today).

### Decided, 2026-09-11
Local Wi-Fi testing now (already wired — `gradle.properties` points at this machine's LAN IP);
migrate to a hosted, scalable backend later. Note for that later migration: Supabase already hosts
the Postgres DB today — the piece that still needs its own hosting when this happens is the Express
API itself (`backend/`), not Supabase, which isn't a drop-in host for a custom Node server. Options
already sitting in `archive_deployment/` from an earlier iteration (Render, Docker+nginx, AWS) are a
starting point when that's actually prioritized, not before.

### New gap found, 2026-09-11: no self-registration path exists
`ShanivarApi.kt` has a `login()` method and no `register()` method; `AccountDialog` in
`ShanivarApp.kt` has only email/password fields wired to sign-in. There is no way for a new user to
create their own account from inside the app today — the backend's `/api/auth/register` route is
real and complete (requires `email`, `username`, `password`, `phone`; everything else optional), but
nothing in the native client calls it. **[Codex]**: build a basic create-account screen — this is
the actual trigger for "start with a basic UI" from the founder's own fallback instruction, not
something a document can route around long-term.

**Immediate workaround, shipped this cycle so family testing isn't blocked on that UI landing**:
the founder pre-creates each family member's account directly against the live backend, one command
per person, then hands out the resulting email/password:
```powershell
Invoke-RestMethod -Uri "http://localhost:5000/api/auth/register" -Method Post -ContentType "application/json" -Body (@{
  email = "aunt.priya@example.com"; username = "priya"; password = "TempPass123!"
  display_name = "Priya"; phone = "+910000000000"
} | ConvertTo-Json)
```
`phone` is required by the route (WhatsApp opt-in linkage, not used yet) but can be a placeholder;
birth data fields are optional and were intentionally left out here — omitting them just means that
account's reflections run in general (non-chart) mode rather than personalized, which is the correct
fallback already built in, not an error.

Family-facing install guide published as an artifact (plain-language, no self-registration assumed):
https://claude.ai/code/artifact/236838bb-fe10-47f6-9e24-df7e375bcce8

## Deployed, 2026-09-12
Backend is live at `https://hybridmonk-api.onrender.com` running the current MVP code, and the
Android debug build points at it — the same-Wi-Fi constraint for family testing is gone.

The blocker that had to be solved to get there, worth recording because it silently predated all
of this: Supabase's direct `db.<ref>.supabase.co` endpoint resolves **IPv6-only**, and Render's
egress is IPv4-only, so the deployed service could never reach the database (`ENETUNREACH`). The
old March deployment had been failing this way too — nobody noticed because the old `/api/health`
returned `ok` unconditionally. Fixed by pointing `DATABASE_URL` at Supabase's Supavisor pooler,
`aws-1-ap-northeast-2.pooler.supabase.com:5432` (session mode, user `postgres.<ref>`), which does
resolve to IPv4. The region was identified by matching the IPv6 prefix against AWS's published
ranges, not guessed — and note the project is on the `aws-1-` pooler cluster, not the legacy
`aws-0-` one, which rejects it with "tenant not found."

Two hardening fixes landed with the deploy, both caught by a production-readiness audit and both
invisible in local testing: `app.set('trust proxy', 1)` (without it, every request behind Render's
proxy shares one rate-limit bucket, so the 20-per-15min auth limiter becomes a platform-wide
lockout), and `/api/health` now runs a real `SELECT` so a deploy cannot go green with a dead
database. That second one immediately proved itself by correctly failing the first deploy attempt.

Verified against production, not locally: registration, login, the lock-targets catalog, the
broadened six-app default block list, full focus-session lifecycle including duplicate rejection
and idempotent release, alarm creation with empty-days validation, dismissal recording, reflection
session creation, daily insight generation, and cross-user isolation. All test accounts removed
afterward (109 real users untouched).

Code now lives in a fresh repository, `BookPress9/shanivar`, with clean history — the predecessor
repo's history contains a committed `.env`. Render still deploys from that old repo; repointing it
is the one remaining manual step.

## Immediate next action available right now
Install `android-app/app/build/outputs/apk/debug/app-debug.apk` on a real Android phone on the same
Wi-Fi as this machine, with `backend/` running (`npm start`). Sign-in, Today, Practice, and the
Reflection Scroller will work against the live backend today. Focus and Rhythm will still show as
"not connected" until Codex lands items 1–3 above.
