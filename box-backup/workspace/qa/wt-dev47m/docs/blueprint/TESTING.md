# Testing — Shanivar

Sources: `android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md`, `VBT_BREATHING_TEST_GUIDE_2026-09-25.md`, `ENFORCEMENT_STANCE_2026-09-12.md`, `android-app/LOCAL_TESTING.md`, `web-app/README.md`. Never write "passed" for a check that was not run.

## 1. Testing Goals
Critical functionality: (1) alarm fires, silences, never traps (invariants 1-8); (2) app protection starts/stops truthfully with immediate local recovery; (3) Routine recurrence incl. overnight/DST; (4) Sadhana rules (`SADHANA_CALCULATIONS.md`) and practice stop/skip; (5) local data stays local and deletable.

## 2. Test Types
- **Unit/Robolectric (Android):** `android-app/app/src/test/java/com/shanivar/nativeapp/` (e.g. `BreathingHistoryTest`, `BreathingDefaultDurationTest`, `InitiationSittingTest`, `SankalpStoreTest`, `BreathingScheduleTest`). Last recorded run: 160/160 on 2026-09-25 (`DEVICE_DISTRIBUTION.md`) — stale for current code; re-run.
- **Instrumented:** `src/androidTest/` has 4 files; "4 instrumented tests never run" (`SHANIVAR_TECHNICAL_MAP.md:275`). Emulator is not authorized; hardware only (`ROUTINE_V3_TEST_GUIDE_2026-09-30.md:3`). A managed device `pixel2Api34` is configured (`android-app/app/build.gradle.kts:48`) and referenced in `LOCAL_TESTING.md:24`. UNRESOLVED: is any emulator/managed-device run permitted?
- **Backend:** Jest + supertest, `backend/tests/*.test.js`; last recorded 305/305 on 2026-09-19 (`DEVICE_DISTRIBUTION.md`).
- **Web app:** `node --test test/` in `web-app/` (no dependencies).
- **Physical device:** DEV-7 (Routine V3 device QA) never run (`marketing/.../README.md:7`; `ROUTINE_V3_DEVICE_QA_2026-10-05.md`). Eight-check hardware protocol: `ENFORCEMENT_STANCE_2026-09-12.md`.
- **E2E:** none automated.

## 3. Testing Tools
Unit: JUnit 4.13.2, Robolectric 4.14.1, coroutines-test, mockwebserver (`android-app/app/build.gradle.kts:70-74`). Compose UI test. Backend: Jest 30, supertest. Web: node:test.

## 4. Test File Structure
As above. No `tests/unit|integration|e2e` split is used.

## 5. Critical User Flows
- **Alarm dismissal:** schedule → fires → movement/QR/photo/accessible → silenced → recorded. Expected: ringer stops first; sensor missing shows accessible path immediately; 15-min watchdog.
- **Protection window:** set window → grant access → ready → active → blocked app shows overlay with a path to stop. Expected status wording truthful (`wireframe README:66-75`).
- **Sadhana practice:** choose practice → 3 min default → End any time → row saved; counts as practised only if ended by timer or user and active >= 90 s, never if interrupted (`BreathingSessionState.kt:198-208`). Expected: no counts/streaks shown.
- **First sitting:** Stop → Settle → Scan → Stay → Ask (Sankalp <=60 chars) → Close; steps move at 30 s and 120 s (`InitiationSitting.kt:34-35`). Internal test only.
- **Waitlist:** submit form → `/thanks.html` → entry appears in Netlify Forms with `source` value (`website/README.md`).
- **Data deletion:** web "Delete all my data on this device"; Android `clearAll()` empties breathing namespace only (`VBT_BREATHING_RELEASE_EVIDENCE_2026-09-25.md:24`). UNRESOLVED: single "delete everything" path on Android.

## 6. Coverage
No numeric target set. UNRESOLVED: practical target. Prioritise safety invariants, recurrence maths, Sadhana rules, data deletion.

## 7. Commands
```
# Android (from repo root; Windows form used by the founder's machine)
./android-app/gradlew.bat -p android-app testDebugUnitTest lintDebug assembleDebug "-PshanivarApiUrl=https://hybridmonk-api.onrender.com/api"   # android-app/README.md:73
# Backend
cd backend && npm test            # jest
# Web app
cd web-app && node --test test/
```
Type check: none separate (Kotlin compile). JS lint: none. UNRESOLVED: the Java/Android SDK setup for running Gradle on the agent box is not documented or verified.

## 8. Definition of Done
- [ ] Tests pass (state which and when)
- [ ] Build passes (debug)
- [ ] Lint passes
- [ ] Critical flow verified; hardware items listed as NOT RUN if not run
- [ ] No blocking errors; no unintended files changed
- [ ] Docs and copy inventory updated
