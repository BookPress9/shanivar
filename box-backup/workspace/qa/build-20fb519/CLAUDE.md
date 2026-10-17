# Shanivar — working agreement

> **Collaboration supersession — 4 October 2026:** Claude is no longer an active collaborator. Codex is lead integrator and the Grok Bot named `app.shanivar` handles newly assigned bounded work through [AGENTS.md](AGENTS.md) and the [collaboration queue](ai_org/collaboration/README.md). Historical Claude deliverables remain evidence and their safety constraints remain active unless a later accepted decision changes them. Do not infer that old Claude ownership starts a worker or blocks Codex from maintaining the project.

**Foundation research (2026-09-29):** Read the [V1 report](research/deep-foundation/SHANIVAR_DEEP_FOUNDATION_REPORT_V1.md) and [manual stance](SHANIVAR_STANCE.md) before changing philosophical framing or navigation. The founder expands international research and rejects spiritual ranking. Research conclusions remain PROPOSED; this report does not authorize a wholesale app redesign or release new practices.

**Steady visual handoff (2026-09-25):** the new breathing feature is named **Steady**. Claude must read the [wireframe contract](android-app/wireframes/steady/README.md) and [clickable prototype](android-app/wireframes/steady/index.html) alongside the breathing build handoff before implementing its UI. This supersedes the working label Breathe & reset, not existing behavior or internal identifiers.

**Third pillar, built 2026-09-25:** the VBT breathing feature from the [Claude handoff](android-app/CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md), [test guide](android-app/VBT_BREATHING_TEST_GUIDE_2026-09-25.md) and [decision 0011](docs/decisions/0011-vbt-breathing-third-pillar.md) is implemented and its automated checks pass — read [the release evidence](android-app/VBT_BREATHING_RELEASE_EVIDENCE_2026-09-25.md) before claiming anything about it, because the content review, the accessibility checks and every physical-device gate are still NOT RUN. It extends the practice-library deferral below only for the eight specified starter practices. The player holds no reference to any enforcement type, and a test fails the build if that changes.

**Read this before changing anything.** It is loaded into every agent session automatically, so it
stays short on purpose: non-negotiables, where the real documentation lives, and how we work. It is
an index and a set of rules, not an encyclopaedia. Every line here costs context on every turn —
if something can live in a linked document instead, it should.

Shanivar serves **worried adult children introducing their parents, and younger people using it
for their own focus and awareness of thinking**. Parents may choose a respectful Lord Shani framing;
faith is an invitation, never an enforcement mechanism or an assumption about all parents.
V1 requires both alarms and app locks/focus modes: begin a chosen action, protect time,
notice distraction and optionally review what helped. Both tools work independently. Every participant controls their own account and
session; a child receives no private activity report. All ages are the intended audience. The first adult cohort is a validation sequence;
report school-age readiness gaps before claiming support for every age. See
[`android-app/INITIAL_MVP_SCOPE_2026-09-13.md`](android-app/INITIAL_MVP_SCOPE_2026-09-13.md).

The [product manager review](ai_org/product_manager_mvp_review_2026-09-13.md) prioritizes family
acquisition learning while retaining young self-use. Reuse a local pause before signup (explicitly
no blocking), keep the parent in control, and make optional Shani framing discoverable. Measure
voluntary repeat by audience; do not confuse helper interest with parent adoption.

Astrology, the reflection scroller, the practice library, gamification, WhatsApp, family
dashboards and paid-plan UI are later capabilities. Existing code may remain, but it must not add
navigation, onboarding or claims to the initial experience. Do not delete tested safety code merely
to simplify the surface.

The founder's [v1 decision](docs/decisions/0007-v1-alarms-and-focus.md) restores alarms over the earlier
PM recommendation. Read the [marketing handoff](marketing/shanivar/v1-product-direction-handoff.md)
and [competitor patterns](research/shanivar-v1-alarm-focus-patterns.md) before implementing Claude's task.

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

**Accepted product direction (2026-09-30):** the founder-selected [V3 wireframe](android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md) replaces the provisional three-tab shell with **Routine / Sadhana**. Routine is the normal launch destination; Dashboard is removed. Phase 1 Routine is authorized through [decision 0014](docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md) and the [Claude build handoff](android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md). Read the [test guide](android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md) before editing scheduling. Phase 2 Sadhana progression remains collaborative and unresolved; decision 0013 still controls its gamification and intensive-practice boundaries.

**Technical reality (read before trusting any capability claim below)**
- [`SHANIVAR_TECHNICAL_MAP.md`](SHANIVAR_TECHNICAL_MAP.md) — all 20 capability areas labelled
  SHIPPED/DORMANT/PARTIAL/ABSENT, verified 2026-09-29, plus six verified focus-scheduler defects.
  Key finding: the shipped app makes **zero AI calls**, and there is **no `strings.xml`**.
- [`GPT_TECH_BRIDGE.md`](GPT_TECH_BRIDGE.md) — GPT's research concepts translated into engineering
  terms, each with a class, options and a recommendation. Three need founder decisions; eleven do not.
- [`CLAUDE_TECHNICAL_INTEGRATION_V1.md`](CLAUDE_TECHNICAL_INTEGRATION_V1.md) — proposed knowledge, AI,
  memory, personalization and navigation architecture; platform feasibility; spikes; P0-P3 roadmap.

**Stance and direction**
- [`android-app/INITIAL_MVP_SCOPE_2026-09-13.md`](android-app/INITIAL_MVP_SCOPE_2026-09-13.md) —
  the current launch scope, both audience paths, plain tone and defer list. It controls
  pitch/usability work when older documents describe a broader MVP.
- [`android-app/CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md`](android-app/CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md) —
  Claude's concrete implementation and verification task.
- [`docs/decisions/0009-behavioral-return-loop.md`](docs/decisions/0009-behavioral-return-loop.md) —
  the founder-approved V1 behavioral loop (Choose → Prepare → Protect → Notice → Review → Adjust →
  Leave). No AI coach, feed, gamification, or engagement mechanics — read this before adding
  anything that reminds, nudges, or motivates.
- [`docs/decisions/0010-focus-schedule-when-then-commitment.md`](docs/decisions/0010-focus-schedule-when-then-commitment.md) —
  recurring focus schedules (all day / a few days a week / custom times) and why the "reason to
  keep going" is a user-authored when-then commitment, never generated copy.
- [`docs/decisions/0012-breathing-session-owner-is-process-scoped.md`](docs/decisions/0012-breathing-session-owner-is-process-scoped.md) —
  why the breathing session lives in a process-scoped object instead of a ViewModel: it is reached
  from three different Activities and only one session may ever be active.
- [`android-app/CODEX_NEXT_TASK_2026-09-19.md`](android-app/CODEX_NEXT_TASK_2026-09-19.md) —
  the focus-schedule UI handoff: backend, client and native engine are built and tested; the
  screen is the open item.
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

- **Repository** — `BookPress9/shanivar` is the **only** repo; `origin/main` is the trunk. The
  `legacy-hybridmonk` remote and the `legacy-main` branch are the retired earlier project, kept for
  history only — never push product work there. Render is confirmed deploying from `shanivar` now
  (verified 2026-09-24 — see [decision 0005](docs/decisions/0005-single-repo-shanivar.md)); credential
  rotation and deleting the legacy repo are the two steps still open.
- **Backend** — Node/Express, `pg` Pool against Supabase Postgres. `db.js` exports `{ query, pool }`.
  Deployed on Render at `https://hybridmonk-api.onrender.com`. Health: `GET /api/health` (runs a
  real `SELECT 1`; a health check that doesn't touch the database is a lie).
- **Database connection** — must go through the Supavisor pooler
  (`aws-1-ap-northeast-2.pooler.supabase.com:5432`, user `postgres.<ref>`). The direct
  `db.<ref>.supabase.co` host is **IPv6-only** and Render's egress is IPv4-only — using it fails
  with `ENETUNREACH`. This has already cost one deploy.
- **App** — Kotlin / Jetpack Compose. `minSdk 26`, `targetSdk 35`. Default launch surface is the
  single-Home streamlined build (`BuildConfig.STREAMLINED_LAUNCH = true`), executed 2026-09-18 —
  see [`android-app/README.md`](android-app/README.md)'s "Streamlined build" section for what
  changed, and [decision 0008](docs/decisions/0008-alarm-recurrence-recomputed-not-pregenerated.md)
  for the alarm-recurrence model. The four-tab shell is kept as `LegacyTabShell`, reachable by
  flipping that flag.
- **Tests** — 160 Android unit (verified 2026-09-25), 305 backend (verified 2026-09-19; `backend/`
  has not been touched since). Re-check both before trusting them — they move every time a package
  adds tests. Plus 4 instrumented classes that **have never executed** (they were written against
  the forbidden emulator target).

## 4. Known debt — and when to pay it

Written down so nobody rediscovers these the hard way, and so nobody "fixes" them prematurely.

| Debt | Why it's tolerable now | Pay it before |
|---|---|---|
| 44 inline `CREATE TABLE` / `ALTER TABLE` statements run at boot across 5 route files; no migration runner, no version history | Genuinely self-healing on a single instance; kept us shipping | **Any second backend instance.** Concurrent instances race, and `CREATE TABLE IF NOT EXISTS` silently no-ops against a drifted table — a real trap already hit once |
| Render free tier, ~22.6s cold start | Fine for a 20-person cohort | First cohort that includes strangers |
| Release build points at `https://api.thehybridmonk.com/api`, which **does not resolve** | We test debug builds | Any release APK reaching a human |
| Debug build defaults to `http://10.0.2.2:5000/api` (emulator-only alias) | Overridable per build | — build with `-PshanivarApiUrl=https://hybridmonk-api.onrender.com/api` for physical devices |
| Focus card's "app to pause" picker is a fixed 6-app allowlist (`KnownDistractingApps`), not a full installed-app list | Avoids also having to exclude system/communication apps from an arbitrary list | A tester reports a distracting app that isn't one of the six |
| Breathing UI strings live in Kotlin, not `strings.xml`, and the practice journal has no history screen | Matches how the rest of the V1 surface is written; the journal is still capped and deletable | Externalising the strings is a prerequisite for the reviewed Hindi script, so pay it with that work |
| The skippable session review doesn't distinguish a session reaching full duration from an early manual end — same copy either way | Simplification made under the MVP streamline task; still better than no review | The pilot's own feedback says the distinction matters |

## 5. How to work here

**Write down why, not what.** The code says what. A comment earns its place by explaining the
reason a line is load-bearing — the race it prevents, the platform quirk it works around, the
ordering that must not change. That is the difference between code that survives being revisited
and code that gets "cleaned up" into a regression.

**Use the straight line.** For every initial-launch screen, state the result the parent chose, the
current status and the next required action. Insight that does not change the next action is noise.
Use direct verbs and concrete commitments; never turn ownership into blame. This adapts the
implementation-over-information and current-state-to-result principles described by
[Straight-Line Leadership](https://www.straightlineleadership.com/en/); it does not copy the
author's prose or aggressive coaching style.

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
