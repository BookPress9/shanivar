# DEV-71: Protection QA suite (permissions, sessions, reboot, recovery)

Owner: Shanivar Tester · Written 2026-10-17 23:00 CEST · Build under test: `main@20fb519` (package `com.shanivar.nativeapp.dev` for debug)
Status: **live**. No-phone checks are done (below). **All 18 phone cases are NOT RUN, pending the founder's phone.** The founder cancelled the emulator (22:55 CEST), so no case is planned for an emulator and none counts as evidence.
Traceability: DEV-71 ACs → the B-ticket ACs (DEV-1..6, DEV-10, DEV-21, DEV-67..70) → the matrix in `/workspace/qa/dev82/ac_test_matrix.md`.

## DEV-71 acceptance criteria
| AC | Text | How it is met | Status |
|---|---|---|---|
| AC1 | Suite executed on real devices with evidence | Phone cases P1–D3 below, on ≥2 OEMs / ≥2 Android versions | **NOT RUN**, pending the founder's phone |
| AC2 | Defects logged with severity | DEV-82 defect register + Jira bugs | Met for the no-phone checks (no new DEV-71 bug; 2 observations routed to existing tickets) |
| Analytics | Verify B3 (DEV-69) events match actions | DEV-69 is not built, so there are no events to check | **BLOCKED** (DEV-69) |
| Privacy | Test data contains no real personal app lists | Use a QA-only YouTube install with no account as the protected app | Rule applies to every case |

## Part 1: no-phone checks (done, with evidence)
Evidence files are in this folder: `unit_results_20fb519.txt`, `unit-full-20fb519.log`, `wording_scan_20fb519.txt`, `static_review.md`.

| ID | Check | Result | Evidence |
|---|---|---|---|
| N1 | Full JVM/Robolectric unit suite on a clean export of main@20fb519 | **PASS**: 518 tests, 0 failures, 0 errors, 1 skipped (SupabaseSyncTest e2e, needs a live stack) | `unit_results_20fb519.txt` |
| N2 | Protection unit classes | **PASS**, 0 failures. FocusScheduleStatusTest 16 · FocusWindowRecurrenceTest 24 · FocusWindowEndHandlerTest 15 · FocusSchedulerRegistryTest 5 · FocusScheduleDraftTest 3 · BootRescheduleCatchUpTest 5 · BootReconcileActionsTest 1 · LocalProtectionTest 6 · DisciplineEngineTest 20 · RoutineProtectionTest 31 · RoutineBoundaryLinkTest 1 · AndroidProtectionGatewayTest 1 · ForegroundAppBlockerTest 5 · SadhanaFocusLockTest 4 (137 tests) | same |
| N3 | Manifest permissions and components | **PASS**: PACKAGE_USAGE_STATS, SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE + FOREGROUND_SERVICE_SPECIAL_USE, `FocusGuardService` `specialUse` with subtype text, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS. BlockOverlayActivity not exported; boot receiver handles BOOT_COMPLETED / TIME_SET / TIMEZONE_CHANGED / MY_PACKAGE_REPLACED (all protected broadcasts). minSdk 26, targetSdk 35. No AccessibilityService, no Device Admin (0002). | `static_review.md` §1 |
| N4 | Reboot catch-up (DEV-21) path | **PASS**: `BootFocusCatchUp.apply` starts local protection only for an occurrence active now, needs no session, always re-arms. The server mirror runs only after a local start. | `static_review.md` §2 |
| N5 | Permission loss → honest state (DEV-3/70) | **PASS** in app UI: `FocusScheduleStatusResolver` puts missing usage/overlay/exact-alarm → NEEDS_DEVICE_ACCESS ahead of ACTIVE. `DisciplineEngine.status()` returns NEEDS_PERMISSION first. `LocalProtectionController.start` refuses when it cannot enforce. | `static_review.md` §3 |
| N6 | Ongoing guard notification while not enforcing | **OBSERVATION → DEV-70** (not filed; this is DEV-70 scope "never show protected when not enforcing" + heartbeat). `FocusGuardService` never re-checks permissions, so after Usage access or overlay is revoked mid-window, the system notification keeps saying "Focus protection is on" while nothing is blocked. Covered on the phone by P1/P2. | `static_review.md` §3 |
| N7 | Stop works offline / signed out; no snooze (DEV-4/10) | **PASS**: `releaseFocus()` calls `guard.stop()` before any network call. `LocalProtectionController.stopNow()` has no API reference. Block screen offers Return / short practice / one-minute pause, with no snooze or "5 more min". | `static_review.md` §4 |
| N8 | Wording scan: protection copy | **PASS**: no "unbreakable/impossible/continuous/guarantee" claim in protection UI. Onboarding says "voluntary and stoppable — never unbreakable". Block screen discloses the brief flash. **Existing:** the block screen's "Choose a short practice" opens BreathingHost, already tracked as **DEV-89** (no duplicate filed). | `wording_scan_20fb519.txt` |
| N9 | Clock moved backwards during a window | **OBSERVATION** (no bug yet): `endsAt` is absolute epoch, so setting the clock back 3 h inside a window keeps blocking 3 h past the window's wall-clock end (Stop still works). Run R6 on the phone before deciding. | `static_review.md` §5 |

## Part 2: phone cases (all NOT RUN, pending the founder's phone)
Setup for every case: debug build of the release candidate installed. Routine V3 on (debug only). Protected app = **YouTube, signed out** (QA data, not a personal list). Sync off or local stack only, never the cloud project. Time zone Europe/Berlin and automatic time on unless the case says otherwise. Evidence per case: phone model, Android version, build hash, local time (CEST), screenshot or screen recording, PASS/FAIL, note.

| ID | Traces to | Case | Steps on the phone | Expected | Status |
|---|---|---|---|---|---|
| P1 | DEV-3, DEV-70 | Usage access revoked mid-window | Start a window → Settings › Apps › Special access › Usage access › Shanivar off → open YouTube → open Shanivar | YouTube is no longer blocked (platform fact). Shanivar shows "Needs device access" with a repair step, never Active. Note what the ongoing notification says (N6). | NOT RUN |
| P2 | DEV-3, DEV-70 | Overlay revoked mid-window | Same, with "Display over other apps" off | Honest state, no false Active; "Stop protection now" still works | NOT RUN |
| P3 | DEV-3 | Exact alarm denied | Settings › Apps › Shanivar › Alarms & reminders off → save a window | Window not Ready; repair path named | NOT RUN |
| P4 | DEV-71 failure case, DEV-68 | Permission denied at onboarding | Fresh install → refuse Usage access and overlay in onboarding | Onboarding continues, plan saved as "Saved, not active", with a clear way to grant later; never claims protection | NOT RUN |
| S1 | DEV-68/69 | Window START/END on time | Window now+2 → now+5 min → open YouTube before, during and after | Blocked from START (brief flash disclosed); free again at END | NOT RUN |
| S2 | DEV-2 | Adjacent windows | Two windows back to back (overlap is rejected at save) | END of the first does not release the second | NOT RUN |
| S3 | DEV-5, DEV-6 | Overnight + full day | 22:00→06:00 window, check before and after midnight; full calendar day | Active across midnight, no 1-minute hole | NOT RUN |
| S4 | DEV-4, DEV-10 | Stop offline, signed out; no snooze | Airplane mode, signed out → "Stop protection now" | Ends at once; block screen never offers snooze / "5 more min" | NOT RUN |
| S5 | DEV-69, DEV-89 | Overlay choices | During a window open YouTube → try each: Return to my day / one-minute pause / short practice / Back | Each goes to the phone home screen, never back into YouTube; Back does not open YouTube. Note what "short practice" shows (DEV-89). | NOT RUN |
| R1 | DEV-21 | Reboot inside / after a window | Restart the phone inside a window, unlock; repeat after the window ended | Inside: protection resumes with no sign-in. After: no protection; next window still armed | NOT RUN |
| R2 | DEV-70 | App swiped away / force-stopped | (a) Swipe Shanivar away from Recents mid-window. (b) Settings › Force stop. | (a) Protection continues. (b) Known bypass: protection stops until the app is opened. Shanivar must say so honestly when reopened. | NOT RUN |
| R3 | DEV-1 | Doze, late START | Unplug, screen off ≥1 h across a START, then wake | No 24 h session; protection only for what is left of the window | NOT RUN |
| R4 | DEV-5 AC4 | Time zone + DST | (a) Manual zone change to Asia/Kolkata mid-window. (b) Night of **Sun 25 Oct 2026** (Berlin falls back 03:00→02:00) with a window across 02:00–03:00 | One boundary per rule; next boundary shown and correct | NOT RUN |
| R5 | B2 | App update | Install the same build again over itself mid-window | Windows and alarm re-armed | NOT RUN |
| R6 | DEV-71 edge case, N9 | Clock change during a window | Automatic time off → move the clock back 3 h, then forward past END | Record actual behaviour; expected: protection follows the window, and Stop always works | NOT RUN |
| D1 | DEV-67 | OEM battery saver / app killer | OEM phone (Xiaomi/Realme/Vivo/Samsung), battery saver on, 2 h window | Guard survives, or the failure is shown | NOT RUN |
| D2 | DEV-67 | Overnight on battery, low-power mode | Overnight window, phone unplugged | Protection holds until END | NOT RUN |
| D3 | DEV-71 scope | Second phone / Android version | Repeat P1–R6 on a second phone (ideally stock Pixel, different Android version) | As above | NOT RUN |

**Counts:** 18 phone cases (all NOT RUN) + 9 no-phone checks (7 PASS, 2 observations routed to DEV-70/DEV-89 or kept for R6) + 4 AC rows (AC1 NOT RUN, AC2 met, analytics BLOCKED by DEV-69, privacy rule).
**Bugs filed from DEV-71:** none new. A real failure needs a phone; the static gaps are already in DEV-70 and DEV-89.
