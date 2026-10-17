# Shanivar technical map

> **Direction update (30 September 2026):** this remains the verified engineering baseline and defect register. Its navigation description predates [decision 0014](docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md), which removes Dashboard and authorizes the Routine / Sadhana shell. Use the [Routine V3 handoff](android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md) for new navigation and scheduler work.

**Verified by inspection and execution on 29 September 2026.** Owner: Claude (technical integration).
This file supersedes any capability claim in older handoffs. Where a document and the code disagree,
the code wins and this file records the code.

Labels: **SHIPPED** (reachable by a user of the current default build) · **DORMANT** (implemented and
tested, but not reachable in the default build) · **PARTIAL** (works with material gaps) ·
**MOCK** (stub or fixture only) · **PLANNED** (documented, not built) · **ABSENT** (nothing exists).

The distinction that matters most in this repo: a great deal of code is **DORMANT**, not absent.
`BuildConfig.STREAMLINED_LAUNCH = true` compiles out the four-tab shell, and with it the reflection
scroller, the practice player, the daily insight card and every AI call the app was ever wired to
make. Reading `backend/src/routes/` gives a wildly inflated picture of the product.

---

## 0. The single most important finding

> **The shipped Android app makes zero AI calls.**

The only AI-backed endpoint the client can reach is `GET /insights/today` (`ShanivarApi.today()`),
called from `refreshNote()` at `ShanivarApp.kt:568-570` — inside `LegacyTabShell`, which begins at
line 556 and is compiled out. The Gemini integration is real, tested and deployed server-side, and
no user of the current build ever touches it.

Everything GPT is designing about a knowing, contextual teacher is therefore not an *extension* of a
working AI product. It is the **first** AI product. Plan accordingly: there is no established
prompt-latency budget, no observability, no cost curve and no evaluation harness, because nothing in
production has needed one yet.

---

## 1. Capability map

### 1.1 Client platforms

| Field | Detail |
|---|---|
| **Current state** | **SHIPPED** Android only. Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01), `minSdk 26`, `targetSdk 35`, `compileSdk 35`, `applicationId com.shanivar.nativeapp` (`.dev` suffix on debug), `versionName 0.1.0` |
| **Relevant files** | `android-app/app/build.gradle.kts`, `android-app/app/src/main/java/com/shanivar/nativeapp/` (52 Kotlin files) |
| **Dependencies** | Compose, activity-compose 1.9.3, lifecycle-viewmodel-compose, CameraX, ML Kit barcode (QR dismissal) |
| **Technical debt** | Debug-signed only — **no release `signingConfig` exists anywhere**. Release variant points at `https://api.thehybridmonk.com/api`, which does not resolve |
| **Risks** | No Play Store path until signing + domain exist. No iOS. `frontend/` on disk is an untracked Next.js leftover (**0 files in git**) — not a product surface |
| **Product implication** | Every GPT concept is Android-first by necessity. An iOS claim has no implementation behind it at all |
| **Recommended next move** | Keep Android-only through the pilot. Do not start iOS before the Android loop is device-verified |

### 1.2 Backend

| Field | Detail |
|---|---|
| **Current state** | **SHIPPED** Node/Express on Render (`https://hybridmonk-api.onrender.com`). 19 route modules, 3,779 LOC in `routes/`. Health verified 200 in 22.6s (free-tier cold start) |
| **Relevant files** | `backend/src/index.js` (mounts 19 routers), `backend/src/db.js`, `backend/src/middleware/auth.js` |
| **Dependencies** | express 4.21, pg 8.13, jsonwebtoken, bcryptjs, express-rate-limit, xss, circular-natal-horoscope-js, moment-timezone, tz-lookup, @supabase/supabase-js |
| **Technical debt** | Only 4 of 19 routers are used by the client (`auth`, `users`, `discipline`, `insights`). The other 15 (`gamification`, `billing`, `ecommerce`, `community`, `journey`, `content`, `habits`, `journal`, `practices`, `reflections`, `chart`, `ai`, `analytics`, `telemetry`, `whatsapp`) are unreachable from the product |
| **Risks** | Free tier sleeps; ~23s first request. `Dockerfile` and `railway.json` exist on disk but are **not tracked in git**, so Render is using its native Node build — the committed deploy story and the actual one differ |
| **Product implication** | There is far more backend than product. New work should assume most of it is dead weight, not foundation |
| **Recommended next move** | Decide per-router: keep, freeze or delete. An unused `billing`/`ecommerce` router is attack surface and reader-confusion with no upside |

### 1.3 Authentication

| Field | Detail |
|---|---|
| **Current state** | **SHIPPED** Custom JWT. `bcryptjs` password hashing, `jsonwebtoken` verification, bearer token in `Authorization` |
| **Relevant files** | `backend/src/middleware/auth.js`, `backend/src/routes/auth.js`, `android-app/.../ShanivarApi.kt`, `AccountState.kt`, `ShanivarSession.kt` |
| **Technical debt** | `ShanivarSession.api` is a best-effort in-process session bridge. If the process is killed with no cached session, background receivers cannot authenticate |
| **Risks** | Refuses to boot without `JWT_SECRET` in production (good). No refresh tokens, no rotation, no device binding, no OAuth |
| **Product implication** | Works offline-first only because the feature set barely needs the server. Any AI feature requires a live authenticated call and inherits the session-bridge gap |
| **Recommended next move** | Persist a refresh credential before background work depends on authenticated calls (see 1.11) |

### 1.4 Database

| Field | Detail |
|---|---|
| **Current state** | **SHIPPED** Supabase Postgres through the Supavisor pooler (`aws-1-ap-northeast-2.pooler.supabase.com:5432`, user `postgres.<ref>`) via `pg` Pool |
| **Relevant files** | `backend/src/db.js` exports `{ query, pool }`; per-route inline DDL |
| **Technical debt** | **44 inline `CREATE TABLE` / `ALTER TABLE` statements executed at boot across 5 route files. No migration runner, no version history, no rollback** |
| **Risks** | Concurrent instances race; `CREATE TABLE IF NOT EXISTS` silently no-ops against a drifted table. The direct `db.<ref>.supabase.co` host is IPv6-only and Render's egress is IPv4-only (`ENETUNREACH`) — already cost one deploy |
| **Product implication** | **This is the hard ceiling on the knowledge architecture.** A source ledger, provenance tables and any embedding store all need real migrations. Adding them as inline boot DDL would multiply the existing trap |
| **Recommended next move** | **P0.** Introduce a migration runner *before* the first knowledge table, not after. This is the cheapest it will ever be |

### 1.5 AI providers

| Field | Detail |
|---|---|
| **Current state** | **DORMANT.** Google Gemini via raw `fetch` (no SDK). Default model `gemini-3.6-flash` (env-overridable), 20s timeout in `ai.js`, 15s in `insights.js`. Deterministic rule-based fallback when `GEMINI_API_KEY` is unset |
| **Relevant files** | `backend/src/routes/ai.js` (251 LOC), `backend/src/routes/insights.js` (187), `backend/src/utils/reflectionGeneration.js` |
| **Technical debt** | Single provider, single model class, no routing, no caching, no token accounting, no structured-output contract, no eval harness, no prompt versioning. Timeouts fall back to rule-based text — correct behavior, but invisible to any metric |
| **Risks** | Provider lock-in to one Gemini model string. **Whether `GEMINI_API_KEY` is set on Render is UNVERIFIED** — it needs dashboard access I do not have, and every AI route is auth-gated so it cannot be probed read-only |
| **Product implication** | GPT's "Shanivar knows the practitioner" work lands on an AI layer that has never served a real user. Treat cost, latency and quality as entirely unmeasured |
| **Recommended next move** | **P1.** Founder/ops: confirm the production key. Then build the smallest latency+cost spike (see spike S-2) before designing prompt architecture |

### 1.6 Retrieval / RAG

| Field | Detail |
|---|---|
| **Current state** | **ABSENT.** Grep across `backend/src` for `embedding`, `pgvector`, `vector(` returns nothing. No vector store, no chunking, no reranking, no hybrid search |
| **Relevant files** | None |
| **Product implication** | The six-layer knowledge architecture GPT proposes has **no** existing foundation. That is an advantage: it can be designed correctly rather than retrofitted onto a flat embedding pool |
| **Recommended next move** | **Do not build a vector store yet.** See §9 of the integration report — 8–50 curated practices with relational provenance validate the experience without any retrieval infrastructure |

### 1.7 Astrology

| Field | Detail |
|---|---|
| **Current state** | **DORMANT** (backend SHIPPED, product ABSENT). Real deterministic astronomy: `circular-natal-horoscope-js` + `moment-timezone` + `tz-lookup`. Natal chart, planets, houses, moon phase, current transits, all offline and API-key-free |
| **Relevant files** | `backend/src/utils/astrology.js` (`computeNatalChart`, `computeCurrentTransits`, `summarizeTransitsForPrompt`), `backend/src/routes/chart.js` (129 LOC) |
| **Technical debt** | The Android client never calls `/chart`. `chart.js` used to ask Gemini and was deliberately changed to deterministic calculation — good provenance hygiene already in place |
| **Risks** | None active, because nothing reaches it |
| **Product implication** | GPT's three-layer separation (calculation / tradition / generated reflection) is **already half-built and on the right side of the line**: calculation is deterministic and separable, and the prompt-summary function is a distinct step |
| **Recommended next move** | Leave dormant. When reintroduced, keep the existing calculation/interpretation split and add a named-tradition interpretation record between them |

### 1.8 Reflection

| Field | Detail |
|---|---|
| **Current state** | **DORMANT.** Backend `reflections.js` (776 LOC — second-largest route). Android `ReflectionScreen.kt`, `ReflectionState.kt`, `ReflectionStore.kt`, `ReflectionContract.kt`, `ReflectionNavigation.kt`. Gated behind `!STREAMLINED_LAUNCH` |
| **Relevant files** | `ShanivarApp.kt:193` is the gate |
| **Notable** | `EncryptedReflectionStore` uses AndroidKeyStore AES/GCM (`AES/GCM/NoPadding`, 128-bit tag, per-file IV) — **real encryption at rest, already built** |
| **Product implication** | GPT's "messages from Shanivar" vertical card concept has a tested predecessor here, including a native contract doc. Reviving it is cheaper than a new build, but it was built to a different (pre-0009) product stance |
| **Recommended next move** | Read `CLAUDE_REFLECTION_SCROLLER_NATIVE_CONTRACT.md` before designing card delivery. Reuse the encryption, re-examine the content stance |

### 1.9 Practice

| Field | Detail |
|---|---|
| **Current state** | **SHIPPED** (breathing, 2026-09-25) + **DORMANT** (legacy techniques) |
| **Shipped** | 8 practices in `app/src/main/assets/breathing-v1.json` (`vbt-breathe-2026-09-25.1`), validated at load by `BreathingCatalog.kt`; recommender, schedule, session reducer, journal; 66 tests |
| **Dormant** | `assets/techniques.json` (12 legacy scripts) reachable only via `PracticePlayer` in the legacy shell |
| **Relevant files** | `BreathingModels/Catalog/Recommender/Schedule/SessionState/SessionOwner/TonePlayer/Preferences/Screen.kt`, `docs/decisions/0011`, `0012` |
| **Technical debt** | Two unrelated practice content formats. Breathing strings are hardcoded Kotlin, not resources. No practice-history UI |
| **Risks** | Content review, accessibility checks and **all** physical-device gates are NOT RUN — see `VBT_BREATHING_RELEASE_EVIDENCE_2026-09-25.md` |
| **Product implication** | This is the working template for "knowledge → experience" with provenance. GPT's practice map should target this schema, not `techniques.json` |
| **Recommended next move** | Treat `breathing-v1.json` as the canonical practice-object shape and generalize it (§9) |

### 1.10 Navigation

| Field | Detail |
|---|---|
| **Current state** | **SHIPPED** Single-Home. `V1Home.kt` holds a `rememberSaveable` string route (`home`/`focus`/`alarm`/`breathe`/`pause`/`settings`/`helper`) inside a `Crossfade(tween(180))` |
| **Relevant files** | `V1Home.kt`, `ShanivarApp.kt:200`, `MainActivity.kt` |
| **Technical debt** | No navigation library, no real back stack (one `BackHandler` returning to `home`), no deep links, no typed routes. State is strings compared with `==` |
| **Risks** | Fine at 7 routes. Becomes unmanageable at ~15, and a contextual "one relevant next action" Home needs more than a flat switch |
| **Product implication** | GPT's "interface should disappear" direction is *compatible* with what exists — a single Home is already the shape. But a contextual next-action engine is new logic, not a navigation rewrite |
| **Recommended next move** | **P1.** Add a pure `HomeState` resolver (testable, no Compose) that decides the next action. Do not adopt Navigation-Compose yet — route count does not justify it |

### 1.11 Local storage

| Field | Detail |
|---|---|
| **Current state** | **SHIPPED** SharedPreferences namespaces: `rhythm`, `focus_guard`, `breathing_preferences`, `session_reviews`, `reflections`, alarm/focus scheduler state. One AndroidKeyStore-encrypted file for reflection sessions |
| **Relevant files** | `DailyRhythm.kt`, `BreathingPreferences.kt`, `AlarmScheduler.kt`, `FocusScheduler.kt`, `ReflectionStore.kt`, `SessionReview.kt` |
| **Technical debt** | No Room/SQLite. JSON-in-SharedPreferences for the breathing journal (bounded to 100) and session reviews. No schema versioning for any of it |
| **Risks** | Unbounded growth is guarded only where someone remembered to guard it. `allowBackup="false"` means no cloud restore — deliberate for privacy, worth stating aloud |
| **Product implication** | Any real practice/observation history needs Room. SharedPreferences is already at its limit |
| **Recommended next move** | **P2.** Room when observation history becomes a product feature. Not before |

### 1.12 Cloud storage

| Field | Detail |
|---|---|
| **Current state** | **PARTIAL** Postgres rows only. No object storage, no file uploads, no audio/media hosting, no CDN |
| **Product implication** | Voice-guided practice — the obvious next step for "eyes closed" practice — has **nowhere to put audio files**. That is a genuine blocker, not a detail |
| **Recommended next move** | **P2.** Supabase Storage is already available on the existing project when audio is real |

### 1.13 Notifications

| Field | Detail |
|---|---|
| **Current state** | **PARTIAL.** Local only: two foreground-service notifications and a full-screen-intent alarm. **No FCM, no push, no server-initiated messaging** (grep for `firebase`/`FCM` returns nothing) |
| **Relevant files** | `AlarmForegroundService.kt`, `FocusGuardService.kt`, `AlarmFireReceiver.kt`, manifest `POST_NOTIFICATIONS` |
| **Risks** | `USE_FULL_SCREEN_INTENT` is auto-revoked on Android 14+ without a completed Play Console "core functionality: alarms" declaration |
| **Product implication** | Shanivar **cannot currently reach a user who is not holding the phone**, except via a locally scheduled alarm. Any "Shanivar notices something and reaches out" concept needs push infrastructure that does not exist |
| **Recommended next move** | **P2, and challenge it.** Push is the standard engagement lever and 0009 bans engagement mechanics. Local scheduling may be the *better* fit for the stance, not just the cheaper one |

### 1.14 App-lock / focus

| Field | Detail |
|---|---|
| **Current state** | **PARTIAL (shipped, with verified defects).** `UsageStatsManager` polled every **1500ms** by a `specialUse` foreground service; `SYSTEM_ALERT_WINDOW` overlay Activity on detection. Fixed 6-package allowlist |
| **Relevant files** | `FocusGuardService.kt` (`POLL_INTERVAL_MS = 1500`), `ForegroundAppBlocker.kt` (pure decision logic), `BlockOverlayActivity.kt`, `FocusScheduler.kt`, `FocusScheduleReceiver.kt`, `FocusWindowRecurrence.kt`, `FocusScheduleCard.kt` |
| **Technical debt** | See §2 — **six verified reliability defects**, one of which can create a 24-hour focus session |
| **Risks** | Detection latency ~1.5s (a flash of the blocked app is expected and disclosed). Defeated by force-stop, permission revocation, browser versions, another device. No Device Admin by decision 0002 |
| **Product implication** | This is Shanivar's strongest utility and its least reliable subsystem. GPT's three-mode redesign is sound *after* §2 is fixed, not before |
| **Recommended next move** | **P0.** Fix `FOCUS-1` (24h session) immediately. Then the rest of §2. Do not market persistence until then |

### 1.15 Scheduling

| Field | Detail |
|---|---|
| **Current state** | **SHIPPED** `AlarmManager.setAlarmClock` for alarms (user-visible, Doze-exempt), `setExactAndAllowWhileIdle` for focus boundaries (silent background toggle). Recomputed-not-pregenerated recurrence (decision 0008) |
| **Relevant files** | `AlarmScheduler.kt`, `AlarmRecurrence.kt`, `FocusScheduler.kt`, `FocusWindowRecurrence.kt`, `BootRescheduleReceiver.kt` |
| **Notable** | Boot behavior is a documented deliberate asymmetry: alarms do **not** fire retroactively; focus **does** resume mid-window (decision 0010) |
| **Technical debt** | `FocusWindowRecurrence` returns null when `startTime >= endTime` → **no overnight windows**, which removes the entire sleep-preparation use case |
| **Product implication** | The recurrence engine is the most trustworthy code in the repo. Extending it to overnight windows is a contained change with a clear test surface |
| **Recommended next move** | **P1.** Overnight semantics with the start-day convention GPT proposes (Mon 22:00→Tue 07:00 belongs to Monday) |

### 1.16 Analytics

| Field | Detail |
|---|---|
| **Current state** | **ABSENT in the app.** One local, append-only JSON self-report (`SessionReview.kt` → `session_reviews` prefs). Backend `analytics.js` (45 LOC) and `telemetry.js` (196 LOC) exist and the client never calls either |
| **Technical debt** | No event taxonomy, no funnel, no crash reporting, no ANR reporting, no structured logs, no dashboards, no error aggregation |
| **Risks** | **A pilot will produce almost no diagnostic signal.** If a tester's alarm fails to fire, there will be no record of it anywhere |
| **Product implication** | GPT's agency metrics (returned to task, voluntary repeat, useful departure) cannot be measured by anything that exists today |
| **Recommended next move** | **P1.** Minimum viable: crash reporting + a tiny local event log the tester can export on request. Consent-gated, no silent upload. See §19 |

### 1.17 Feature flags

| Field | Detail |
|---|---|
| **Current state** | **PARTIAL** Exactly one compile-time flag: `BuildConfig.STREAMLINED_LAUNCH`. No remote config, no per-user flags, no A/B framework, no kill switch |
| **Product implication** | Every experiment GPT proposes (practice-first vs context-first, return-only vs grounding block screen) currently requires a **separate build per arm** |
| **Recommended next move** | **P1 if experiments are real.** A tiny local flag store with debug-menu overrides is enough for counterbalanced pilot studies; a remote-config service is not needed at pilot scale |

### 1.18 Testing

| Field | Detail |
|---|---|
| **Current state** | **SHIPPED and genuinely good.** 160 Android JVM tests (JUnit4 + Robolectric 4.14.1 + Compose UI test), 305 backend Jest tests across 22 suites. Both verified green 29 September 2026 |
| **Technical debt** | 4 instrumented (`androidTest`) classes have **never executed once** — written against the forbidden managed-device target |
| **Risks** | **Zero physical-device verification of anything.** Emulators and managed-device Gradle tasks are prohibited on this machine (they crashed it) |
| **Product implication** | Logic correctness is well covered; feature correctness on real hardware is entirely unverified. These are not the same claim and the gap is the project's biggest risk |
| **Recommended next move** | **P0.** Attach one physical phone. `connectedDebugAndroidTest` is permitted; the eight-check protocol in `ENFORCEMENT_STANCE_2026-09-12.md` has never run |

### 1.19 Deployment

| Field | Detail |
|---|---|
| **Current state** | **PARTIAL** Render free tier, deploying from `BookPress9/shanivar` (verified 2026-09-24 via a route that exists only in this repo returning 401 not 404). Root directory `backend`, native Node build |
| **Technical debt** | **No CI/CD whatsoever** — no `.github/` directory exists. Nothing runs tests on push. Every verification in this repo is a human running Gradle locally |
| **Risks** | `Dockerfile` and `railway.json` are untracked on disk, so the committed deploy story is not the real one. APK distribution is a local Python HTTP server |
| **Product implication** | Two agents share this repo with no automated gate between them |
| **Recommended next move** | **P1.** A single CI workflow running `npm test` + `testDebugUnitTest` + `lintDebug` on push. Cheap, and it is the only thing that will catch cross-agent regressions |

### 1.20 Internationalization

| Field | Detail |
|---|---|
| **Current state** | **ABSENT.** `res/values/` contains **only `colors.xml`**. There is no `strings.xml` anywhere in the project. Every user-facing string is hardcoded in Kotlin |
| **Relevant files** | `preferred_language` is persisted server-side (`RoutineApi.kt:64,185`) and passed to `AlarmCard`, but no client-side string ever changes as a result |
| **Product implication** | **This is a hard blocker on the international-audience objective.** GPT's multilingual/transliteration research cannot reach the product through any existing mechanism. The breathing catalogue declares `scriptLanguage` and a `languageFallback` statement — content is ready for translation, the *app* is not |
| **Recommended next move** | **P1.** Extract strings to resources before any translation work begins. Doing it once, early, is a day; doing it across a grown surface is a week |

---

## 2. Verified defects in the focus scheduler

GPT's `CLOCK_FOCUS_RESEARCH.md` flagged six reliability gaps from static inspection. **I verified all
six against the code.** All are real. One is materially worse than described.

| ID | Defect | Verified where | Severity |
|---|---|---|---|
| **FOCUS-1** | A delayed `START` delivery can begin a **24-hour** focus session. `minutesUntilWindowEnd()` takes `nextBoundary(...)` and treats it as the window's end regardless of which boundary it actually is. If delivery slips past the window's own end, `nextBoundary` returns the *next occurrence's START*, the difference is clamped to `24*60`, and `startFocus(1440, …)` is called — the backend accepts it because `MAX_FOCUS_MINUTES` is now 1440 | `FocusScheduleReceiver.kt:62-67`, `BreathingSchedule`-independent; backend `discipline.js` `MAX_FOCUS_MINUTES` | **P0** |
| **FOCUS-2** | No overnight windows. `if (startTime >= endTime) return null` removes sleep-preparation entirely | `FocusWindowRecurrence.kt:41` | **P1** |
| **FOCUS-3** | "All day" is `00:00`–`23:59`, leaving a one-minute daily gap, and still depends on a successful next-boundary delivery. Must not be called persistent protection | `FocusScheduleCard.kt:31-40` | **P1** (copy), P2 (semantics) |
| **FOCUS-4** | Background boundaries depend on nullable in-process `ShanivarSession.api`. After process death with no cached session, the window silently does not start or end — the schedule advances anyway | `FocusScheduleReceiver.kt:33-45` (documented as an accepted v1 gap) | **P1** |
| **FOCUS-5** | One schedule, and `END` calls `releaseFocus()` unconditionally. Adding overlapping or manual rules risks releasing protection owned by a different rule | `FocusScheduleReceiver.kt:42` | **P1 before multi-schedule** |
| **FOCUS-6** | Saving reports a *scheduled boundary*, not verified future enforcement. Needs distinct `Scheduled` / `Active` / `Needs attention` / `Off` states derived from real permission + guard state | `FocusScheduleCard.kt`, `DisciplineStatus` | **P1** |

FOCUS-1 is the one to fix this week. It is a small, well-understood change — evaluate
`FocusWindowRecurrence.isActiveAt(...)` at delivery time and skip a stale `START` entirely rather
than computing a duration from the wrong boundary — and its failure mode is a user losing access to
their apps for a day.

---

## 3. Cross-cutting debt ranked by what it blocks

| Debt | Blocks | Priority |
|---|---|---|
| No migration runner (44 inline boot DDL) | The entire knowledge/provenance architecture | **P0** |
| No physical-device verification; 4 instrumented tests never run | Any honest reliability claim; the pilot | **P0** |
| FOCUS-1 | User trust; a day-long accidental lockout | **P0** |
| No `strings.xml` | International audience; Hindi parents | **P1** |
| No analytics/crash reporting | Learning anything from the pilot | **P1** |
| No CI | Cross-agent regressions | **P1** |
| No release signing; `api.thehybridmonk.com` NXDOMAIN | Play Store; any non-sideload distribution | **P1** |
| Unrotated leaked credentials (GitHub PAT, Render key) | Outside testers | **P0 (ops, not code)** |
| 15 unused backend routers | Nothing, but they mislead every reader | P2 |
| SharedPreferences-as-database | Real observation history | P2 |
| No object storage | Voice-guided practice | P2 |

---

## 4. What this map does not establish

- **Nothing here is device-verified.** Every behavioral claim is from source inspection plus JVM and
  Robolectric tests. A Robolectric test proving a tone player received `stop()` is not evidence that a
  speaker went quiet.
- **Whether `GEMINI_API_KEY` is set in production is unknown.** It needs Render dashboard access.
- **Whether Play Console declarations exist** for `USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT` and the
  two `specialUse` foreground services is unknown and is a release-blocking question.
- **No iOS feasibility is assessed here.** See §16 of the integration report for why a claim would be
  irresponsible without a FamilyControls entitlement review.

---

Maintained alongside [`GPT_TECH_BRIDGE.md`](GPT_TECH_BRIDGE.md) and
[`CLAUDE_TECHNICAL_INTEGRATION_V1.md`](CLAUDE_TECHNICAL_INTEGRATION_V1.md). Update this file when the
code changes, not when a plan changes.
