# Shanivar — working agreement

**Read this before changing anything.** It is loaded into every agent session automatically, so it
stays short on purpose: non-negotiables, where the real documentation lives, and how we work. It is
an index and a set of rules, not an encyclopaedia. Every line here costs context on every turn —
if something can live in a linked document instead, it should.

Shanivar is a discipline app: alarms you cannot casually dismiss, and app locks you cannot casually
bypass. Two features, deliberately. Everything else is downstream of those working.

**The horizon we are building against:** a million users within two years. That does not mean
building a million-user system today — premature scaling has killed more projects here than late
scaling. It means something narrower and harder: **decisions stay reversible, and invariants stay
written down.** Code gets rewritten cheaply. Undocumented reasons do not survive contact with the
next engineer, human or agent.

---

## 1. Non-negotiables

Break any of these and the product stops being trustworthy. They are not style preferences.

1. **An alarm must never keep ringing after its task is done.** Enforcement that malfunctions is
   worse than no enforcement — it is the one bug that gets the app uninstalled and never
   reinstalled. Silence the ringer *first*, before any teardown that could throw. See the ordering
   comment in `AlarmActivity.dismissAlarm()`.
2. **An alarm must always be escapable eventually.** A hard cap ends it, and an accessible
   non-physical dismissal path always exists. Nobody is ever trapped by a sensor their phone
   lacks or a body that cannot walk.
3. **Never claim enforcement we cannot deliver.** Force-stop, revoked permissions and disabled
   notifications all defeat us; Android has no true un-dismissible alarm. We build "maximally
   inconvenient to bypass" and we say exactly that to users. Never show "Alarm confirmed" over a
   permission that would stop it firing.
4. **No Device Admin / accessibility-service enforcement.** It is stalkerware-adjacent, and it is a
   Play Store removal risk. Declined deliberately — see the enforcement stance.
5. **No secrets in the repo.** `.env` is gitignored in every variant. A predecessor repo leaked a
   committed `.env`; assume anything ever committed is public forever and rotate it.
6. **Never run an Android emulator or Gradle managed-device task on this machine.** It has crashed
   the laptop. `assembleDebug`, `testDebugUnitTest`, `lintDebug` are safe.
   `pixel2Api34DebugAndroidTest` is **forbidden**; `connectedDebugAndroidTest` (real hardware) is
   allowed.
7. **Never commit or push unless asked.**

## 2. Where the truth lives

This file is the door. The rooms are elsewhere — read the relevant one before working in that area.

**Stance and direction**
- [`android-app/ENFORCEMENT_STANCE_2026-09-12.md`](android-app/ENFORCEMENT_STANCE_2026-09-12.md) —
  the founder-level position on alarms and app locks, the escalation spec, the 8 safety invariants,
  and the 8-check physical-device protocol. **The single most important document in the repo.**
- [`android-app/PRODUCT_DIRECTION.md`](android-app/PRODUCT_DIRECTION.md)
- [`android-app/FOUNDER_MOVEMENT_ALARM_DIRECTION_2026-09-12.md`](android-app/FOUNDER_MOVEMENT_ALARM_DIRECTION_2026-09-12.md)

**Contracts between backend and app** — change these together or you break the other side.
- [`android-app/CLAUDE_DISCIPLINE_ENGINE_NATIVE_CONTRACT.md`](android-app/CLAUDE_DISCIPLINE_ENGINE_NATIVE_CONTRACT.md)
- [`android-app/CLAUDE_REFLECTION_SCROLLER_NATIVE_CONTRACT.md`](android-app/CLAUDE_REFLECTION_SCROLLER_NATIVE_CONTRACT.md)

**Testing and distribution**
- [`android-app/LOCAL_TESTING.md`](android-app/LOCAL_TESTING.md) ·
  [`android-app/DEVICE_DISTRIBUTION.md`](android-app/DEVICE_DISTRIBUTION.md)

**Team and process** — `ai_org/` holds the agent roster, squad charters and meeting records.

## 3. The system as it is today

Describing what exists, not what we wish existed.

- **Backend** — Node/Express, `pg` Pool against Supabase Postgres. `db.js` exports `{ query, pool }`.
  Deployed on Render at `https://hybridmonk-api.onrender.com`. Health: `GET /api/health` (runs a
  real `SELECT 1`; a health check that doesn't touch the database is a lie).
- **Database connection** — must go through the Supavisor pooler
  (`aws-1-ap-northeast-2.pooler.supabase.com:5432`, user `postgres.<ref>`). The direct
  `db.<ref>.supabase.co` host is **IPv6-only** and Render's egress is IPv4-only — using it fails
  with `ENETUNREACH`. This has already cost one deploy.
- **App** — Kotlin / Jetpack Compose. `minSdk 26`, `targetSdk 35`.
- **Tests** — 296 backend, 69 Android unit, 4 instrumented classes that **have never executed**
  (they were written against the forbidden emulator target).

## 4. Known debt — and when to pay it

Written down so nobody rediscovers these the hard way, and so nobody "fixes" them prematurely.

| Debt | Why it's tolerable now | Pay it before |
|---|---|---|
| 44 inline `CREATE TABLE` / `ALTER TABLE` statements run at boot across 5 route files; no migration runner, no version history | Genuinely self-healing on a single instance; kept us shipping | **Any second backend instance.** Concurrent instances race, and `CREATE TABLE IF NOT EXISTS` silently no-ops against a drifted table — a real trap already hit once |
| Render free tier, ~22.6s cold start | Fine for a 20-person cohort | First cohort that includes strangers |
| Release build points at `https://api.thehybridmonk.com/api`, which **does not resolve** | We test debug builds | Any release APK reaching a human |
| Debug build defaults to `http://10.0.2.2:5000/api` (emulator-only alias) | Overridable per build | — build with `-PshanivarApiUrl=https://hybridmonk-api.onrender.com/api` for physical devices |
| `POST_NOTIFICATIONS` never requested at runtime | Invisible below Android 13 | **Now.** On Android 13+ the alarm never appears at all |

## 5. How to work here

**Write down why, not what.** The code says what. A comment earns its place by explaining the
reason a line is load-bearing — the race it prevents, the platform quirk it works around, the
ordering that must not change. That is the difference between code that survives being revisited
and code that gets "cleaned up" into a regression.

**Record decisions, not just changes.** Anything that closes off an alternative — a schema shape,
a platform API declined, an enforcement boundary — goes in
[`docs/decisions/`](docs/decisions/README.md) as `NNNN-short-title.md`: the context, the options,
what we chose, and what would make us revisit. Six months from now the diff will show what changed
and this will show why. Read the existing ones before proposing a change in their area — most
wasted effort goes into rediscovering a dead end someone already walked down.

**Two agents share this codebase.** Claude owns the backend, Codex owns the Compose frontend, and
both have edited the alarm surface. Announce before any large refactor of shared files
(`AlarmActivity`, `AlarmRinger`, `AlarmForegroundService`, `ShanivarApi`, the manifest,
`routes/discipline.js`).

**Verify claims before reporting them.** Read the raw output; don't trust a wrapper script or
another agent's summary. Every false "it works" in this project so far came from trusting a
derived report over the primary source.

**Tests are the documentation that cannot go stale.** Anything in section 1 gets a test. See
`AlarmRingerTest.kt` — it tests the escalation curve and the safety ceilings precisely because a
quiet mistake there either fails to wake someone or refuses to stop.

## 6. Commands known safe

```bash
# Backend
cd backend && npm test

# Android (from android-app/)
gradlew assembleDebug -PshanivarApiUrl=https://hybridmonk-api.onrender.com/api
gradlew testDebugUnitTest
gradlew lintDebug
gradlew connectedDebugAndroidTest   # physical device only — never the managed device

# Production
curl https://hybridmonk-api.onrender.com/api/health
```
