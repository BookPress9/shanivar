# Claude to Grok developer transfer: Shanivar V3

**Prepared:** 4 October 2026  
**Recipient:** Grok Bot `app.shanivar`  
**Integrator:** Codex  
**Repository:** `https://github.com/BookPress9/shanivar`  
**Local branch:** `main`

This document transfers the current Shanivar V3 product and Android developer context that exists on the founder's laptop but is not fully present in Grok's GitHub checkout. Treat the laptop worktree described here as the current integration source. Do not overwrite it with the older remote state.

## 1. Source-of-truth warning

The GitHub repository is behind the laptop.

| State | Commit |
|---|---|
| Laptop `HEAD` | `5f5b1be8ec2221df3d97f4a29e40d6e21eec64b3` |
| GitHub `origin/main` at transfer time | `33d0ce45e1fc5869e00b927654df40cb1665406a` |

The laptop is one commit ahead and also contains uncommitted developer, collaboration, research, and marketing work. A clean GitHub clone therefore cannot be used to infer the current V3 state. The V3 work must first be reviewed and integrated from the local folder. No commit or push is implied by this handoff.

## 2. Exact local locations

Repository root:

```text
C:\Users\Thehy\OneDrive\Documents\Vibecoding Projects\Hybrid monk draft\TheHybridMonk
```

Android project root:

```text
C:\Users\Thehy\OneDrive\Documents\Vibecoding Projects\Hybrid monk draft\TheHybridMonk\android-app
```

Android application source:

```text
C:\Users\Thehy\OneDrive\Documents\Vibecoding Projects\Hybrid monk draft\TheHybridMonk\android-app\app\src
```

Backend project root:

```text
C:\Users\Thehy\OneDrive\Documents\Vibecoding Projects\Hybrid monk draft\TheHybridMonk\backend
```

V3 wireframe root:

```text
C:\Users\Thehy\OneDrive\Documents\Vibecoding Projects\Hybrid monk draft\TheHybridMonk\android-app\wireframes\shanivar-v3-wireframe\shanivar-v3
```

Agent handoff root:

```text
C:\Users\Thehy\OneDrive\Documents\Vibecoding Projects\Hybrid monk draft\TheHybridMonk\ai_org\collaboration
```

These Windows paths describe the founder's laptop. Grok's cloud computer will not gain filesystem access merely by knowing them. They are provenance and navigation references for the founder/Codex bridge until the local state is committed and pushed or transferred as an archive/patch.

## 3. Read order for Grok

Read these files in this order. The absolute path to every file is the repository root above plus the relative path shown.

1. `AGENTS.md` — current authority, collaboration rules, and the replacement of Claude by Grok.
2. `SHANIVAR_STANCE.md` — current product and intellectual stance.
3. `docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md` — accepted V3 decision; highest-authority V3 product contract.
4. `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md` — detailed historical Claude-to-implementation contract, preserved as technical evidence.
5. `android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md` — acceptance and regression guide.
6. `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md` — visual/interaction intent.
7. `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/index.html` — clickable V3 prototype.
8. `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/app.js` and `style.css` — prototype behavior and styling.
9. `SHANIVAR_TECHNICAL_MAP.md` and `GPT_TECH_BRIDGE.md` — repository architecture and inherited technical context.
10. `CLAUDE_TECHNICAL_INTEGRATION_V1.md` — older Claude integration record; use only where it does not conflict with decision 0014.
11. `android-app/ENFORCEMENT_STANCE_2026-09-12.md` — device-protection truth requirements.
12. `docs/decisions/0008-device-enforcement-boundary.md` and `docs/decisions/0010-device-state-authority-and-recovery.md` — earlier enforcement decisions retained by V3.
13. `android-app/README.md` — build and project entry point.

If any older document conflicts with decision 0014, follow decision 0014. A filename containing `CLAUDE` identifies provenance, not current authority.

## 4. Accepted V3 product direction

V3 has exactly two top-level destinations: **Routine** and **Sadhana**. Dashboard is removed. Returning users launch into Routine.

Phase 1 delivers the two-destination shell and Routine. Sadhana progression remains a later product decision; do not add XP, ranks, streaks, dosha/cortisol claims, AI recommendations, or new progression mechanics. The existing Steady breathing experience is the only implemented Sadhana content.

Routine is an actual personal day rather than an idealized template. It owns:

- editable wake and sleep anchors;
- editable personal activities with name, time, duration, and recurrence;
- the device-backed alarm;
- installed-app selection;
- multiple protection windows;
- same-day and overnight recurrence;
- an honest chronological timeline;
- truthful permission and device-enforcement states;
- immediate local stop and recovery.

The local device is authoritative for scheduling and recovery. A warm app process, signed-in session, network connection, payment state, practice, or successful server response must not be required for a scheduled boundary or immediate stop.

The required user-visible states are `DRAFT`, `SAVED_LOCAL`, `NEEDS_DEVICE_ACCESS`, `READY`, `ACTIVE`, `NEEDS_ATTENTION`, and `OFF`.

The visual contract uses black, cream, sage, and muted gold; large editorial headings; controls that remain usable at 200% font scale; and touch targets of at least 48 dp. The final Routine prototype includes `YOUR DAY`, the line `Not an ideal morning. Your actual life.`, `Edit Routine`, and a bottom-right add-window control.

## 5. What is implemented and what is not

### Implemented baseline

- Existing V1 Android app and Steady breathing pillar.
- Alarm, focus guard, one recurring focus schedule, installed-app selection, and the older shell scaffold.
- The accepted V3 decision, build handoff, test guide, and interactive wireframe.
- A local, currently uncommitted reliability slice described in section 6.

### Not yet implemented

The complete native Routine V3 feature is not present. Repository search found no native implementation of `RoutinePlanV1`, `RoutineActivityV1`, `ProtectionWindowV1`, `RoutineSnapshot`, `RoutineSnapshotComposer`, the final `RoutineScreen`, or the final `Edit Routine` flow.

`android-app/app/build.gradle.kts` still defines `THREE_TAB_NAV = false`. `ShanivarApp.kt` still gates the old `ShanivarShell.kt` behind that flag. This is stale V2/scaffold language and does not satisfy the accepted two-destination V3 contract. `V1Home.kt` remains the current default home.

Do not report V3 as built merely because the wireframe exists or because the reliability fixes below pass unit tests.

## 6. Local uncommitted developer slice

The following product-code changes exist only in the laptop worktree at transfer time. They continue the V3 technical handoff. Their exact original authorship has not been independently reconstructed, so review the diff rather than relying on an author label.

### New files

- `android-app/app/src/main/java/com/shanivar/nativeapp/LocalProtection.kt`
- `android-app/app/src/test/java/com/shanivar/nativeapp/LocalProtectionTest.kt`

`LocalProtectionController` creates a device-only enforcement path with no API, session, account, or network dependency. It reads the existing `focus_guard` device record, starts only when Usage Access and overlay permissions are available, and stops immediately and idempotently.

### Modified Android files

- `android-app/app/src/main/java/com/shanivar/nativeapp/FocusWindowRecurrence.kt`
  - accepts overnight windows;
  - defines the selected weekday as owning the start;
  - rejects equal start and end times;
  - adds `activeWindowEnd` so a delayed START cannot create a false 24-hour lock.
- `android-app/app/src/main/java/com/shanivar/nativeapp/FocusScheduleCard.kt`
  - accepts overnight input and rejects equal times with clear copy.
- `android-app/app/src/main/java/com/shanivar/nativeapp/FocusScheduleReceiver.kt`
  - performs the local device action first;
  - ignores a START delivered after its occurrence ended;
  - avoids stopping a later active occurrence;
  - advances scheduling independently of server mirroring;
  - mirrors to the server only as best effort when a session is available.
- `android-app/app/src/main/java/com/shanivar/nativeapp/BootRescheduleReceiver.kt`
  - uses the actual active occurrence end during reboot catch-up.
- `android-app/app/src/main/java/com/shanivar/nativeapp/DisciplineEngine.kt`
  - stops local enforcement before best-effort server release;
  - uses the local device record for truthful offline status.
- `android-app/app/src/test/java/com/shanivar/nativeapp/FocusWindowRecurrenceTest.kt`
- `android-app/app/src/test/java/com/shanivar/nativeapp/FocusScheduleDraftTest.kt`
- `android-app/app/src/test/java/com/shanivar/nativeapp/DisciplineEngineTest.kt`

### Modified backend files

- `backend/src/routes/discipline.js`
  - accepts an end time earlier than start as an overnight schedule;
  - rejects equal start and end times.
- `backend/tests/discipline.test.js`
  - covers the updated schedule contract.

### Known review point

`BootRescheduleReceiver.kt` still obtains `ShanivarSession.api` before starting catch-up protection. That conflicts with the stronger V3 rule that reboot/process recovery must be local and network-independent. Treat this as unfinished until the receiver uses the local controller first and mirrors second, matching `FocusScheduleReceiver.kt`.

## 7. Verification recorded on 4 October 2026

From `android-app`:

```powershell
.\gradlew.bat testDebugUnitTest
```

Result: `BUILD SUCCESSFUL`; `:app:testDebugUnitTest` passed/up-to-date.

From `backend`:

```powershell
npm test -- --runInBand tests/discipline.test.js
```

Result: 1 suite passed, 46 tests passed.

These checks validate the present unit-test scope. They do not constitute device QA for exact alarms, reboot recovery, Usage Access, overlay behavior, OEM process killing, TalkBack, 200% font scale, or the unbuilt Routine V3 UI.

## 8. Required implementation sequence

1. Preserve and finish the device-authority reliability slice, including local-first reboot catch-up.
2. Replace or rename the stale `THREE_TAB_NAV` flag and migrate to exactly Routine and Sadhana.
3. Add `RoutinePlanV1`, `RoutineActivityV1`, `ProtectionWindowV1`, versioned local persistence, and migration behind repository interfaces.
4. Compose alarm, routine, protection, permission, and enforcement truth through `RoutineSnapshotComposer`.
5. Build the native Routine timeline from real stored data.
6. Build `Edit Routine` for anchors and personal activities.
7. Support multiple protection windows, installed-app selection, recurrence, overnight semantics, and clear permission recovery.
8. Preserve Steady as the current Sadhana content without inventing progression.
9. Run the automated checks and the full `ROUTINE_V3_TEST_GUIDE_2026-09-30.md` device matrix.
10. Update documentation only after implemented and tested behavior supports the claim.

## 9. Grok operating boundary

Until Grok receives a synced repository snapshot, Grok should perform review and planning against this transfer, not make merge-ready edits against `origin/main`. Code based only on `33d0ce4` risks deleting or duplicating laptop work.

For every assigned implementation task, Grok must receive explicit owned paths and return:

- the commit/base hash it read;
- files changed;
- tests and exact results;
- unresolved conflicts or missing local inputs;
- a report under `ai_org/collaboration/reports/<task-id>-grok.md`.

Grok must not edit central stance or decision status. Codex integrates and accepts work. The founder approves material product-direction changes and public release.

## 10. Intake response requested from Grok

After reading this transfer, return a short report with:

1. the newest commit and files actually accessible to Grok;
2. confirmation that GitHub is missing the local V3 state;
3. a dependency-ordered implementation plan for Phase 1;
4. conflicts or ambiguities found between decision 0014, the Claude build handoff, tests, wireframe, and current code;
5. no code changes unless Codex assigns owned paths from a synchronized base.

Use this exact statement if the remote checkout is still at `33d0ce4`:

> I have the V3 transfer context, but my checkout does not contain the laptop's current integration state. I will not create merge-ready V3 code from the stale base. I am returning an intake review and waiting for a synchronized task boundary.
