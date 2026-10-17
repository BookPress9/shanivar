# Claude build handoff - Routine V3

Prepared 30 September 2026. Owner: Claude for backend/native integration, coordinated with Codex for Compose review. Authority: founder instruction and [decision 0014](../docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md).

## Outcome

Build the founder-selected **Routine + Sadhana** shell and the complete **Routine** experience shown in the [interactive V3 reference](wireframes/shanivar-v3-wireframe/shanivar-v3/index.html). Do not build Sadhana progression in this phase. Existing Steady remains reachable from Sadhana without new levels, unlocks or content.

The Phase 1 result lets a person:

1. state a usual wake time and intended sleep time;
2. optionally configure a wake alarm;
3. select distracting apps once;
4. create, edit, enable, disable and delete several recurring protection windows;
5. add and edit personal activities such as Gym, reading or a longer Sadhana, including time, duration and occurrence;
6. see the actual resulting day in Routine;
7. understand whether each device capability is saved, ready, active or needs attention;
8. stop protection locally and regain essential access.

## Read before editing

1. [Working agreement](../CLAUDE.md)
2. [Decision 0014](../docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md)
3. [V3 visual and interaction handoff](wireframes/shanivar-v3-wireframe/shanivar-v3/README.md)
4. [Routine test guide](ROUTINE_V3_TEST_GUIDE_2026-09-30.md)
5. [Enforcement stance](ENFORCEMENT_STANCE_2026-09-12.md)
6. [Technical map, Focus defects](../SHANIVAR_TECHNICAL_MAP.md#2-verified-defects-in-the-focus-scheduler)
7. [Alarm recurrence decision](../docs/decisions/0008-alarm-recurrence-recomputed-not-pregenerated.md)
8. [Focus schedule decision](../docs/decisions/0010-focus-schedule-when-then-commitment.md)

Do not run an emulator or a Gradle managed-device task. Do not commit or push unless the founder asks.

## Scope boundaries

### Build now

- two-item bottom navigation: `Routine`, `Sadhana`;
- Routine as the normal launch destination;
- V3 onboarding: wake anchor, intended-sleep anchor, optional alarm/protection setup;
- Routine timeline and setup/status cards;
- wake-alarm entry using existing engine and safety paths;
- categorized installed-app selection;
- multiple protection windows and recurrence;
- editable personal activities with `Every day`, `Weekly` and `Custom` occurrence;
- permission, exact-alarm and recovery flows;
- deterministic local status and local exportable diagnostics;
- existing Steady entry inside Sadhana.

### Do not build in Phase 1

- Sadhana chapters as a progression system;
- XP, ranks, scores, badges, streak-loss pressure or leaderboards;
- new breathing/practice scripts;
- advanced-practice unlocking;
- AI recommendations, dosha inference, cortisol/body-clock claims;
- push campaigns, social features, paid-plan UI or family reporting;
- account-required device scheduling;
- a new navigation framework solely for two destinations.

## Accepted visual contract

Use V3's black, cream-paper, sage and muted-gold system. Preserve its direct coaching voice and large editorial headings. The source of truth is the wireframe package, including its reduced-motion rule and light/dark foreground pairs.

Compose requirements:

- support 200% font scaling without clipped copy or unreachable controls;
- use at least 48 dp controls and semantic selected/disabled states;
- keep the bottom destinations visible on ordinary routes and absent on full-screen alarm, block and practice surfaces;
- keep `End` reachable in active practice and `Stop protection` reachable without completing content;
- render routine content from state, never from the sample times or sample app labels in the wireframe;
- respect reduced motion; the Sadhana Saturn animation is not part of the Routine build requirement.

Final-wireframe fidelity checklist:

- Rhythm eyebrow reads `YOUR DAY`; do not restore `TWO ANCHORS`.
- Rhythm support copy reads `Not an ideal morning. Your actual life.`; do not restore `Not someone else's routine.`
- Routine displays an `Edit Routine` action beside the wake/sleep summary.
- Edit Routine edits both anchors and personal activities in one scrollable flow.
- App Protection places its add-window `+` after the window list, aligned bottom right. Do not place it inside the first-window header.
- Ordinary screens keep exactly two bottom destinations: Routine and Sadhana.
- Sample times, activity labels and apps in the wireframe remain fixtures; native UI renders stored state.

## Target information architecture

```text
First run
  Welcome
  -> Wake + intended sleep
  -> Optional guardrails
       -> Wake alarm
       -> App selection + protection windows
  -> Routine

Returning launch
  Routine
    -> Edit wake/sleep anchors
    -> Edit personal activities
         -> Name + time + duration
         -> Every day | Weekly | Custom days
    -> Alarm setup/status
    -> App protection setup/status
    -> Permission repair
    -> Essential access / stop

Bottom navigation
  Routine | Sadhana
```

Settings remains a top-bar route. Alarm ringing, dismissal, block overlay, permission return and recovery outrank the shell.

## Domain model

Create one versioned local configuration model rather than spreading new V3 fields across unrelated Compose state:

```kotlin
data class RoutinePlanV1(
    val wakeTime: LocalTime?,
    val intendedSleepTime: LocalTime?,
    val activities: List<RoutineActivityV1>,
    val protectedPackages: Set<String>,
    val windows: List<ProtectionWindowV1>,
)

data class RoutineActivityV1(
    val id: String,
    val title: String,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val days: Set<Int>,            // 0=Sun ... 6=Sat
    val enabled: Boolean,
)

data class ProtectionWindowV1(
    val id: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val startDays: Set<Int>,       // 0=Sun ... 6=Sat, matching existing API convention
    val enabled: Boolean,
)
```

The alarm remains owned by `AlarmPlan`/`AlarmScheduler`; do not duplicate its armed state in `RoutinePlanV1`. Compose a `RoutineSnapshot` from the alarm scheduler, routine repository, Focus schedule registry and Android permission checks.

Use a bounded, schema-versioned local JSON record in SharedPreferences for this small configuration. Do not add Room solely for Routine configuration. Decode failure must preserve safety: stop claiming readiness, keep recovery available and show `Needs attention`.

Wake and intended-sleep time currently exist in both `Rhythm` and `BreathingPreferences`. Migrate to one explicit Routine owner and provide a compatibility read/migration. Do not leave two independently editable clocks.

## Personal-activity editor

The final wireframe makes Routine a personal calendar. `Edit Routine` must edit both anchors and personal activities. Each activity has a user-editable name, start time, duration and occurrence. The UI presents three occurrence choices: `Every day`, `Weekly` and `Custom`. `Weekly` selects one weekday; `Custom` selects one or more weekdays. Limit the first build to 12 activities, require a non-blank title and valid time, and preserve stable IDs across edits.

The quick-add choices `Gym`, `Read a book` and `Extend Sadhana` are editable starting points. They are not a fixed activity taxonomy and they do not imply a health prescription. A generic bottom-right `+` creates a blank personal activity. The App Protection screen uses its own bottom-right `+` to create another window.

Personal activities are calendar entries. Saving one does not arm the wake alarm, create a Focus rule, unlock Sadhana content or claim completion. Keep any later reminder or completion-tracking capability behind a separate contract and explicit user choice. The Routine timeline combines wake/sleep anchors, personal activities and device-backed events, while labeling device status only on device-backed events.

## Modular delivery contract

The current Android app is one Gradle `app` module with a mostly flat package. Do not begin this release with a broad multi-module rewrite. Create feature boundaries inside the existing module first, keep dependencies pointing inward, and extract Gradle modules later only when build time or team ownership justifies it.

Use these logical areas:

```text
routine/model       pure RoutinePlan, activity, recurrence and snapshot models
routine/data        versioned local persistence and migration
routine/domain      validation, occurrence calculation and timeline composition
routine/ui          Routine screen and Edit Routine state/events
alarm               existing alarm engine behind AlarmGateway
protection          app catalogue, window registry, scheduler and recovery
sadhana             existing reviewed Steady entry; no Phase 2 progression
shell               two-destination navigation and route ownership
```

Package names may follow the repository convention, but source files must preserve these boundaries. Compose must consume immutable screen state and emit events. It must not read SharedPreferences, calculate recurrence, inspect package permissions or call schedulers directly.

Define small interfaces at the boundary:

```kotlin
interface RoutinePlanRepository {
    fun observe(): Flow<RoutinePlanV1>
    suspend fun save(plan: RoutinePlanV1): Result<Unit>
}

interface AlarmGateway {
    fun observeStatus(): Flow<AlarmStatus>
}

interface ProtectionGateway {
    fun observeStatus(): Flow<ProtectionStatus>
    suspend fun save(config: ProtectionConfig): Result<Unit>
    suspend fun stopNow(): Result<Unit>
}
```

`RoutineSnapshotComposer` is the only component that joins Routine plan data with alarm and protection status for the timeline. Feature code communicates through typed models and interfaces, never by reaching into another feature's Compose state or storage keys.

Each feature exposes one immutable UI-state type and a small event surface. Route-level coordinators collect flows and perform navigation; reusable composables receive values and callbacks. Keep platform objects (`Context`, receivers, services, `PackageManager`, alarm APIs) in adapters. Pure domain code uses Kotlin/JDK types so it can run in local unit tests.

Keep optimizations local and measurable:

- one pull request/slice changes one logical area unless a versioned interface must change;
- interface changes include migration notes and contract tests;
- performance work records a baseline and the same measurement after the change;
- feature flags switch complete routes or adapters, not scattered visual branches;
- pure recurrence, validation and timeline functions remain deterministic and unit-testable;
- no speculative abstraction: add an interface at an actual I/O or feature boundary, not around every class;
- preserve old persisted data through explicit schema migrations; never make UI redesigns silently rewrite engine state.

This structure is the extension seam for later reminders, activity completion, Routine suggestions and Sadhana milestones. Those additions should extend a boundary or introduce a new adapter without changing alarm safety or protection scheduling.

## Protection-window semantics

- A window with `start < end` ends on the same selected day.
- A window with `start > end` is overnight; the selected day owns the start. Example: Monday 22:00-06:00 means Monday night through Tuesday 06:00.
- `start == end` is invalid; do not encode “all day” as `00:00-23:59` because that creates a one-minute gap. If all-day is later required, model it explicitly.
- V3 windows share the single selected app set.
- Limit the first build to eight windows, matching the wireframe.
- Reject overlapping enabled windows in the first release. Explain which two overlap. Adjacent windows are allowed. This avoids a premature multi-owner release problem while preserving the V3 use cases.
- Custom week needs at least one start day.
- Times use the current device zone. On zone/time change, recompute from the rule; do not reinterpret stored local times as instants.

## Scheduler migration - blocking work

The current `FocusScheduler` persists one rule and owns one fixed PendingIntent. `FocusScheduleCard` calls `firstOrNull()`. This cannot implement V3.

Replace it with a bounded registry that:

- stores all enabled local rules;
- arms a uniquely identifiable next boundary per rule, or one deterministic global next-boundary alarm that recomputes the complete registry;
- includes the expected rule ID, boundary action and scheduled instant in persisted state;
- treats a delayed START received after its window ended as stale and skips it;
- recomputes current active state before every start/release action;
- advances after every delivery, including failures;
- recovers after boot, app update, time-zone change, clock change and process death;
- does not depend on `ShanivarSession.api` being warm;
- never lets an earlier END release protection owned by another active window. This should be impossible while overlap is rejected, but still test ownership explicitly;
- exposes `nextBoundary`, `armed`, `active` and `problem` to the UI without inventing state.

Device enforcement and immediate recovery must work locally. Network/account sync is secondary. If retaining server schedules, introduce an explicit sync adapter and conflict status; do not block local save/arming on sign-in or a Render response. Never tell the user a schedule is active based only on a successful HTTP response.

Before expanding the registry, fix the current 24-hour delayed-START defect and add regression tests. Preserve the current no-retroactive-alarm rule; scheduled Focus may resume during a valid active window after boot, but must never start after that window ended.

## App selection

The V3 categories are presentation metadata, not a new mental-health taxonomy:

- Social
- Feeds & video
- Communities
- Entertainment
- Easy escape routes

Show installed eligible apps only, with app label and icon from `PackageManager`. Keep a reviewed exclusion policy for Shanivar itself, launchers, settings, dialer/phone, emergency/system packages and packages the engine cannot safely overlay. Do not request broad package visibility without confirming the Play policy path. If production-safe installed-app discovery cannot be shipped yet, retain a clearly labelled curated catalogue and expand it through versioned data; never show an uninstalled app as protected.

Selection is stored by package name. If a selected app is removed, keep the rule but display it as unavailable; if reinstalled, it may become eligible again after a status refresh.

## Alarm integration

Reuse `AlarmCard`, `AlarmPlan`, `AlarmScheduler` and the existing dismissal Activities. Fit the UI to V3 without changing these invariants:

- silence first;
- hard cap always ends ringing;
- accessible non-physical dismissal exists;
- capability and exact-alarm status are truthful;
- no routine edit can alter a ringing alarm;
- saving intended sleep never changes an alarm;
- no server is required to fire or dismiss.

The Routine timeline reads the next actual alarm trigger. `Alarm configured` requires an armed local trigger, not a filled form.

## Routine timeline

Build the timeline from `RoutineSnapshot`:

- wake anchor and actual alarm status;
- optional daylight suggestion labelled as guidance, not a measured protocol;
- each enabled protection window in the next 24 hours;
- each enabled personal activity in the next waking cycle, with occurrence and duration;
- one optional Sadhana entry;
- intended-sleep anchor.

Sort using occurrence instants relative to now so overnight schedules display correctly. Do not show “today” for an event that belongs to the next waking cycle. Empty, permission-missing and partial-error states need a direct repair action.

The sample manifesto copy in V3 is accepted direction. It may challenge the task; it may not shame the person or infer why they failed.

## State and status copy

| Engine state | Required UI meaning |
| --- | --- |
| No plan | `Not configured` + setup action |
| Saved, missing capability | `Needs device access` + exact repair action |
| Armed next boundary | `Ready` + next change time |
| Inside valid window with engine confirmation | `Active` + end time |
| Stale/corrupt/sync conflict/arming failure | `Needs attention` + safe explanation |
| Disabled | `Off` |

Do not collapse `Saved`, `Ready` and `Active` into one green state.

## Suggested implementation slices

### R0 - correctness before UI

- Add delayed-boundary and cold-process regression tests.
- Fix the 24-hour START bug.
- Implement overnight recurrence and eliminate the all-day gap representation.
- Make recovery local and process-independent.

### R1 - shell and local plan

- Replace `ShanivarTab` with Routine/Sadhana.
- Make Routine default; remove Dashboard UI and stale back behavior.
- Replace/rename `THREE_TAB_NAV` with a V3 flag or make the accepted shell the default after validation.
- Add `RoutinePlanV1`, `RoutineActivityV1`, migration and `RoutineSnapshot` behind `RoutinePlanRepository`.
- Persist wake/sleep anchors once.

### R1.5 - personal calendar editor

- Build Edit Routine for anchors and personal activities.
- Add quick starts for Gym, Read a book and Extend Sadhana plus the generic bottom-right add action.
- Validate and persist time, duration and Every day/Weekly/Custom recurrence.
- Compose activities into the Routine timeline without claiming alarm or Focus enforcement.

### R2 - multiple windows

- Add the registry, overlap validation, recurrence presets and custom days.
- Convert the first existing schedule into one V3 window without data loss.
- Build installed/curated app categories and package status.

### R3 - V3 Compose

- Match Routine, Edit Routine, alarm, app selection, bottom-right window add control, permission and recovery states.
- Preserve current engine boundaries and busy-state navigation lock.
- Add large-text, semantics and reduced-motion behavior.

### R4 - evidence and activation

- Complete the test guide.
- Run unit, lint and debug assembly.
- Run the mandatory physical-device matrix; no emulator.
- Record exact build/config/version and unresolved gates.
- Only then make V3 the default build path.

## Definition of done

Claude may call Phase 1 implementation complete only when:

- every item in the final-wireframe fidelity checklist is represented in Compose;
- Routine plan migration is idempotent and personal activities survive process death;
- alarm and protection engines remain independently recoverable and truthful;
- the Routine editor cannot mutate engine state except through the relevant gateway;
- unit, lint and assembly commands pass with results recorded;
- the required physical-device cases are recorded or the build is explicitly labelled gated;
- one feature can be replaced with a fake repository/gateway in tests without initializing the other feature engines;
- the final implementation note names each boundary changed, its public models/interfaces and any follow-up optimization supported by measured evidence.

## Files expected to change

Likely native files: `ShanivarShell.kt`, `ShanivarApp.kt`, `DailyRhythm.kt` or new Routine model/repository/editor/composer files, `FocusScheduleCard.kt`, `FocusScheduler.kt`, `FocusScheduleReceiver.kt`, `FocusWindowRecurrence.kt`, `BootRescheduleReceiver.kt` and related tests. Keep the first implementation inside the existing app module; change Gradle structure only through a separate measured decision.

Backend changes are optional if Routine is local-first. If sync is retained or expanded, update `RoutineApi.kt`, discipline routes/schema and backend tests together. Do not change contracts implicitly.

Do not edit Steady scripts, the VBT catalogue or Sadhana progression as part of this task.

## Completion evidence

Claude's final handoff must state:

- files changed and migration behavior;
- which Routine features are implemented versus fixture-only;
- exact test commands and results;
- physical-device make/model/Android version for every device result;
- remaining Play policy and device-compatibility risks;
- whether V3 is default or still gated;
- no claim that browser screenshots prove Android scheduling or blocking.

