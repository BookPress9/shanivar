<callout icon="📱" color="blue_bg">
	**Live.** Shanivar Tester, 2026-10-17 (Europe/Berlin), build main@20fb519. The founder cancelled the emulator, so every phone case is **NOT RUN, pending the founder's phone**. Source files are on the QA box at `/workspace/qa/dev71/` (test_cases.md, static_review.md, phone_checklist.md, unit_results_20fb519.txt, wording_scan_20fb519.txt).
</callout>
Jira [DEV-71](https://shanivar.atlassian.net/browse/DEV-71) · Parent: DEV-82 test plan
Owner: Shanivar Tester · Written 2026-10-17 23:00 CEST · Build under test: `main@20fb519` (package `com.shanivar.nativeapp.dev` for debug)
Status: **live**. No-phone checks are done (below). **All 18 phone cases are NOT RUN, pending the founder's phone.** The founder cancelled the emulator (22:55 CEST), so no case is planned for an emulator and none counts as evidence.
Traceability: DEV-71 ACs → the B-ticket ACs (DEV-1..6, DEV-10, DEV-21, DEV-67..70) → the matrix in `/workspace/qa/dev82/ac\_test\_matrix.md`.

# DEV-71 acceptance criteria
<table fit-page-width="true" header-row="true">
	<tr><td>AC</td><td>Text</td><td>How it is met</td><td>Status</td></tr>
	<tr><td>AC1</td><td>Suite executed on real devices with evidence</td><td>Phone cases P1–D3 below, on ≥2 OEMs / ≥2 Android versions</td><td>**NOT RUN**, pending the founder's phone</td></tr>
	<tr><td>AC2</td><td>Defects logged with severity</td><td>DEV-82 defect register + Jira bugs</td><td>Met for the no-phone checks (no new DEV-71 bug; 2 observations routed to existing tickets)</td></tr>
	<tr><td>Analytics</td><td>Verify B3 (DEV-69) events match actions</td><td>DEV-69 is not built, so there are no events to check</td><td>**BLOCKED** (DEV-69)</td></tr>
	<tr><td>Privacy</td><td>Test data contains no real personal app lists</td><td>Use a QA-only YouTube install with no account as the protected app</td><td>Rule applies to every case</td></tr>
</table>

# Part 1: no-phone checks (done, with evidence)
Evidence files are in this folder: `unit\_results\_20fb519.txt`, `unit-full-20fb519.log`, `wording\_scan\_20fb519.txt`, `static\_review.md`.

<table fit-page-width="true" header-row="true">
	<tr><td>ID</td><td>Check</td><td>Result</td><td>Evidence</td></tr>
	<tr><td>N1</td><td>Full JVM/Robolectric unit suite on a clean export of main@20fb519</td><td>**PASS**: 518 tests, 0 failures, 0 errors, 1 skipped (SupabaseSyncTest e2e, needs a live stack)</td><td>`unit\_results\_20fb519.txt`</td></tr>
	<tr><td>N2</td><td>Protection unit classes</td><td>**PASS**, 0 failures. FocusScheduleStatusTest 16 · FocusWindowRecurrenceTest 24 · FocusWindowEndHandlerTest 15 · FocusSchedulerRegistryTest 5 · FocusScheduleDraftTest 3 · BootRescheduleCatchUpTest 5 · BootReconcileActionsTest 1 · LocalProtectionTest 6 · DisciplineEngineTest 20 · RoutineProtectionTest 31 · RoutineBoundaryLinkTest 1 · AndroidProtectionGatewayTest 1 · ForegroundAppBlockerTest 5 · SadhanaFocusLockTest 4 (137 tests)</td><td>same</td></tr>
	<tr><td>N3</td><td>Manifest permissions and components</td><td>**PASS**: PACKAGE\_USAGE\_STATS, SYSTEM\_ALERT\_WINDOW, FOREGROUND\_SERVICE + FOREGROUND\_SERVICE\_SPECIAL\_USE, `FocusGuardService` `specialUse` with subtype text, RECEIVE\_BOOT\_COMPLETED, POST\_NOTIFICATIONS. BlockOverlayActivity not exported; boot receiver handles BOOT\_COMPLETED / TIME\_SET / TIMEZONE\_CHANGED / MY\_PACKAGE\_REPLACED (all protected broadcasts). minSdk 26, targetSdk 35. No AccessibilityService, no Device Admin (0002).</td><td>`static\_review.md` §1</td></tr>
	<tr><td>N4</td><td>Reboot catch-up (DEV-21) path</td><td>**PASS**: `BootFocusCatchUp.apply` starts local protection only for an occurrence active now, needs no session, always re-arms. The server mirror runs only after a local start.</td><td>`static\_review.md` §2</td></tr>
	<tr><td>N5</td><td>Permission loss → honest state (DEV-3/70)</td><td>**PASS** in app UI: `FocusScheduleStatusResolver` puts missing usage/overlay/exact-alarm → NEEDS\_DEVICE\_ACCESS ahead of ACTIVE. `DisciplineEngine.status()` returns NEEDS\_PERMISSION first. `LocalProtectionController.start` refuses when it cannot enforce.</td><td>`static\_review.md` §3</td></tr>
	<tr><td>N6</td><td>Ongoing guard notification while not enforcing</td><td>**OBSERVATION → DEV-70** (not filed; this is DEV-70 scope "never show protected when not enforcing" + heartbeat). `FocusGuardService` never re-checks permissions, so after Usage access or overlay is revoked mid-window, the system notification keeps saying "Focus protection is on" while nothing is blocked. Covered on the phone by P1/P2.</td><td>`static\_review.md` §3</td></tr>
	<tr><td>N7</td><td>Stop works offline / signed out; no snooze (DEV-4/10)</td><td>**PASS**: `releaseFocus()` calls `guard.stop()` before any network call. `LocalProtectionController.stopNow()` has no API reference. Block screen offers Return / short practice / one-minute pause, with no snooze or "5 more min".</td><td>`static\_review.md` §4</td></tr>
	<tr><td>N8</td><td>Wording scan: protection copy</td><td>**PASS**: no "unbreakable/impossible/continuous/guarantee" claim in protection UI. Onboarding says "voluntary and stoppable — never unbreakable". Block screen discloses the brief flash. **Existing:** the block screen's "Choose a short practice" opens BreathingHost, already tracked as **DEV-89** (no duplicate filed).</td><td>`wording\_scan\_20fb519.txt`</td></tr>
	<tr><td>N9</td><td>Clock moved backwards during a window</td><td>**OBSERVATION** (no bug yet): `endsAt` is absolute epoch, so setting the clock back 3 h inside a window keeps blocking 3 h past the window's wall-clock end (Stop still works). Run R6 on the phone before deciding.</td><td>`static\_review.md` §5</td></tr>
</table>

# Part 2: phone cases (all NOT RUN, pending the founder's phone)
Setup for every case: debug build of the release candidate installed. Routine V3 on (debug only). Protected app = **YouTube, signed out** (QA data, not a personal list). Sync off or local stack only, never the cloud project. Time zone Europe/Berlin and automatic time on unless the case says otherwise. Evidence per case: phone model, Android version, build hash, local time (CEST), screenshot or screen recording, PASS/FAIL, note.

<table fit-page-width="true" header-row="true">
	<tr><td>ID</td><td>Traces to</td><td>Case</td><td>Steps on the phone</td><td>Expected</td><td>Status</td></tr>
	<tr><td>P1</td><td>DEV-3, DEV-70</td><td>Usage access revoked mid-window</td><td>Start a window → Settings › Apps › Special access › Usage access › Shanivar off → open YouTube → open Shanivar</td><td>YouTube is no longer blocked (platform fact). Shanivar shows "Needs device access" with a repair step, never Active. Note what the ongoing notification says (N6).</td><td>NOT RUN</td></tr>
	<tr><td>P2</td><td>DEV-3, DEV-70</td><td>Overlay revoked mid-window</td><td>Same, with "Display over other apps" off</td><td>Honest state, no false Active; "Stop protection now" still works</td><td>NOT RUN</td></tr>
	<tr><td>P3</td><td>DEV-3</td><td>Exact alarm denied</td><td>Settings › Apps › Shanivar › Alarms & reminders off → save a window</td><td>Window not Ready; repair path named</td><td>NOT RUN</td></tr>
	<tr><td>P4</td><td>DEV-71 failure case, DEV-68</td><td>Permission denied at onboarding</td><td>Fresh install → refuse Usage access and overlay in onboarding</td><td>Onboarding continues, plan saved as "Saved, not active", with a clear way to grant later; never claims protection</td><td>NOT RUN</td></tr>
	<tr><td>S1</td><td>DEV-68/69</td><td>Window START/END on time</td><td>Window now+2 → now+5 min → open YouTube before, during and after</td><td>Blocked from START (brief flash disclosed); free again at END</td><td>NOT RUN</td></tr>
	<tr><td>S2</td><td>DEV-2</td><td>Adjacent windows</td><td>Two windows back to back (overlap is rejected at save)</td><td>END of the first does not release the second</td><td>NOT RUN</td></tr>
	<tr><td>S3</td><td>DEV-5, DEV-6</td><td>Overnight + full day</td><td>22:00→06:00 window, check before and after midnight; full calendar day</td><td>Active across midnight, no 1-minute hole</td><td>NOT RUN</td></tr>
	<tr><td>S4</td><td>DEV-4, DEV-10</td><td>Stop offline, signed out; no snooze</td><td>Airplane mode, signed out → "Stop protection now"</td><td>Ends at once; block screen never offers snooze / "5 more min"</td><td>NOT RUN</td></tr>
	<tr><td>S5</td><td>DEV-69, DEV-89</td><td>Overlay choices</td><td>During a window open YouTube → try each: Return to my day / one-minute pause / short practice / Back</td><td>Each goes to the phone home screen, never back into YouTube; Back does not open YouTube. Note what "short practice" shows (DEV-89).</td><td>NOT RUN</td></tr>
	<tr><td>R1</td><td>DEV-21</td><td>Reboot inside / after a window</td><td>Restart the phone inside a window, unlock; repeat after the window ended</td><td>Inside: protection resumes with no sign-in. After: no protection; next window still armed</td><td>NOT RUN</td></tr>
	<tr><td>R2</td><td>DEV-70</td><td>App swiped away / force-stopped</td><td>(a) Swipe Shanivar away from Recents mid-window. (b) Settings › Force stop.</td><td>(a) Protection continues. (b) Known bypass: protection stops until the app is opened. Shanivar must say so honestly when reopened.</td><td>NOT RUN</td></tr>
	<tr><td>R3</td><td>DEV-1</td><td>Doze, late START</td><td>Unplug, screen off ≥1 h across a START, then wake</td><td>No 24 h session; protection only for what is left of the window</td><td>NOT RUN</td></tr>
	<tr><td>R4</td><td>DEV-5 AC4</td><td>Time zone + DST</td><td>(a) Manual zone change to Asia/Kolkata mid-window. (b) Night of **Sun 25 Oct 2026** (Berlin falls back 03:00→02:00) with a window across 02:00–03:00</td><td>One boundary per rule; next boundary shown and correct</td><td>NOT RUN</td></tr>
	<tr><td>R5</td><td>B2</td><td>App update</td><td>Install the same build again over itself mid-window</td><td>Windows and alarm re-armed</td><td>NOT RUN</td></tr>
	<tr><td>R6</td><td>DEV-71 edge case, N9</td><td>Clock change during a window</td><td>Automatic time off → move the clock back 3 h, then forward past END</td><td>Record actual behaviour; expected: protection follows the window, and Stop always works</td><td>NOT RUN</td></tr>
	<tr><td>D1</td><td>DEV-67</td><td>OEM battery saver / app killer</td><td>OEM phone (Xiaomi/Realme/Vivo/Samsung), battery saver on, 2 h window</td><td>Guard survives, or the failure is shown</td><td>NOT RUN</td></tr>
	<tr><td>D2</td><td>DEV-67</td><td>Overnight on battery, low-power mode</td><td>Overnight window, phone unplugged</td><td>Protection holds until END</td><td>NOT RUN</td></tr>
	<tr><td>D3</td><td>DEV-71 scope</td><td>Second phone / Android version</td><td>Repeat P1–R6 on a second phone (ideally stock Pixel, different Android version)</td><td>As above</td><td>NOT RUN</td></tr>
</table>

**Counts:** 18 phone cases (all NOT RUN) + 9 no-phone checks (7 PASS, 2 observations routed to DEV-70/DEV-89 or kept for R6) + 4 AC rows (AC1 NOT RUN, AC2 met, analytics BLOCKED by DEV-69, privacy rule).
**Bugs filed from DEV-71:** none new. A real failure needs a phone; the static gaps are already in DEV-70 and DEV-89.

# Founder phone checklist
For: Shubham · About 2.5 h of active time, plus one overnight (D2) and one DST night (R4b, **Sun 25 Oct 2026**). No computer or cables needed.
Before you start:
- [ ] Install the debug build QA sends you (release-candidate hash: ________). Write down the phone model and Android version: ________
- [ ] Install **YouTube** and stay signed out. It is the test app, so no personal app list is involved.
- [ ] Shanivar: grant Usage access, Display over other apps, Alarms & reminders and Notifications. Keep automatic time and time zone on.
- [ ] For each line: tick PASS or FAIL and take a screenshot (Power + Volume down). If something looks wrong, a 10-second screen recording is better.

Tip: a "test window" means Routine › App Protection › a window starting 2 minutes from now and lasting 5 minutes, with YouTube selected.

**Permissions**
- [ ] P1 Start a test window. Settings › Usage access › Shanivar **off**. Open YouTube, then Shanivar. ✔ Shanivar says it needs access (not "Active"). Note what the Shanivar notification says. Turn access back on afterwards.
- [ ] P2 Same, with **Display over other apps** off. ✔ Not "Active"; "Stop protection now" still works.
- [ ] P3 Settings › Apps › Shanivar › **Alarms & reminders off**. Save a window. ✔ Window is not "Ready"; the app tells you what to turn on.
- [ ] P4 (fresh install, or Clear storage) In onboarding, **refuse** Usage access and overlay. ✔ You can finish; it says saved but not active and shows how to fix it later.

**Sessions**
- [ ] S1 Test window: open YouTube before, during and after. ✔ Blocked only during (a short flash is OK); free after.
- [ ] S2 Two windows back to back (e.g. 5 min then 5 min). ✔ Still blocked after the first ends.
- [ ] S3 A 22:00→06:00 window. Check at 23:59 and 00:01. ✔ Blocked both times.
- [ ] S4 Airplane mode on, signed out, mid-window: tap **Stop protection now**. ✔ YouTube opens at once; the block screen never offers "snooze" or "5 more minutes".
- [ ] S5 Mid-window open YouTube and try each button (Return to my day / one-minute pause / short practice / Back). ✔ Each lands on the phone's home screen, never back in YouTube. Note what "short practice" shows.

**Recovery**
- [ ] R1 Restart the phone mid-window and unlock it. ✔ YouTube still blocked, no sign-in needed. Restart again after the window ended. ✔ Not blocked.
- [ ] R2 (a) Swipe Shanivar out of Recents mid-window. ✔ Still blocked. (b) Settings › Apps › Shanivar › **Force stop**. Note whether YouTube opens, then open Shanivar and note what it says.
- [ ] R3 Unplug, screen off for 1 h across a window START, then use YouTube. ✔ Blocked only until the window's own end.
- [ ] R4 (a) Mid-window, change the time zone by hand to India. Note the next window time shown. (b) **Night of Sat 24 → Sun 25 Oct**: window 01:30–03:30. ✔ One start and one end, at the right local times.
- [ ] R5 Reinstall the same build over itself mid-window. ✔ Window still active and next one armed.
- [ ] R6 Automatic time off. Mid-window, move the clock **back 3 h**, then forward past the end. Note what happens. ✔ Stop always works. Turn automatic time back on.

**Phone-specific**
- [ ] D1 With battery saver on, run a 2-hour window and check YouTube every ~30 min. ✔ Blocked throughout, or Shanivar shows that protection stopped.
- [ ] D2 Overnight window, phone unplugged in low-power mode. ✔ Blocked in the morning until the end time.
- [ ] D3 If you have a second phone (different brand or Android version), repeat P1–R6 on it. DEV-71 needs 2 phones to sign off.

Send back: screenshots + this list with ticks to QA (Shanivar Tester). QA files FAILs as bugs, with severity.

# Static review notes
Read-only review on a clean `git archive` export (`/workspace/qa/build-20fb519`). No source changed. Paths are relative to `android-app/app/src/main/`.

### 1. Manifest (`AndroidManifest.xml`)
- L11 `PACKAGE\_USAGE\_STATS` (Settings grant), L19 `SYSTEM\_ALERT\_WINDOW`, L24/L32 `FOREGROUND\_SERVICE` + `FOREGROUND\_SERVICE\_SPECIAL\_USE`, L54 `RECEIVE\_BOOT\_COMPLETED`, L58 `POST\_NOTIFICATIONS`.
- L101-105 `FocusGuardService` exported=false, `foregroundServiceType="specialUse"`, subtype "Detects and blocks a user-selected distracting app during a focus session the user started". Play review must accept it (DEV-67).
- L88 `BlockOverlayActivity` exported=false, singleInstance, excludeFromRecents.
- L115-122 `BootRescheduleReceiver` exported=true, but every action it accepts is a protected system broadcast, and the code re-filters with `RECONCILE\_ACTIONS`. OK.
- L72-79 `<queries>` limits visibility to the 6 catalogue apps. This matches `ProtectionAppCatalog` (no full app inventory read). Privacy: good.
- No AccessibilityService and no DeviceAdmin (decision 0002). OK.

### 2. Reboot / clock catch-up: `BootRescheduleReceiver.kt`
- L74-79 `BootFocusCatchUp.apply` uses `FocusWindowRecurrence.activeWindowEnd` + `LocalProtectionController` with no session. L141 always re-arms (`runCatching`).
- L84 server mirror only `if (outcome.started && api != null)`. Matches DEV-21. Unit coverage: BootRescheduleCatchUpTest (5), BootReconcileActionsTest (1), all pass.

### 3. Permission loss
- `LocalProtection.kt:68-83` `start()` refuses when `!canEnforce()` (usage && overlay).
- `DisciplineEngine.kt:57-60` NEEDS\_PERMISSION before any ACTIVE. L66-72 re-asserts the guard before claiming ACTIVE.
- `FocusScheduleStatus.kt:73-101` and `RoutineProtection.kt:251-262`: missing access → NEEDS\_DEVICE\_ACCESS ahead of ACTIVE. Covered by FocusScheduleStatusTest (16) and RoutineProtectionTest (31).
- **Observation (DEV-70 scope):** `FocusGuardService.kt:62-76` poll loop never re-checks permissions. `queryEvents` returns nothing without Usage access, and `startActivity` fails silently without overlay. The ongoing notification (L118 "Focus protection is on") stays up. There is no heartbeat and no `protection\_failure` event. This is exactly DEV-70, so no duplicate bug was filed.

### 4. Stop and no-snooze
- `DisciplineEngine.releaseFocus()` L115-127: `guard.stop()` first, server release best effort.
- `LocalProtectionController.stopNow()` has no API type. `RoutineProtectionScreen.kt:185` "Stop protection now".
- `BlockOverlayActivity.kt:105-115`: Return to my day / Choose a short practice / Take a one-minute pause. No snooze. Back goes home (L40).
- "Choose a short practice" → `BreathingHost(BLOCK\_OVERLAY)` (L49): already **DEV-89**.

### 5. Clock change
- `FocusGuardService` stores absolute `endsAt` (epoch ms). Moving the clock forward past END stops it (L66). Moving it back extends blocking in wall-clock terms. `TIME\_SET` runs `BootFocusCatchUp`, which can start protection but never shortens an active one. Not filed: Stop always works and the product impact is unclear. Phone case R6 records the actual behaviour.
