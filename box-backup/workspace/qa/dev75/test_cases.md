# DEV-75: Alarm and notification behaviour on supported real devices

Owner: Shanivar Tester · Written 2026-10-17 23:05 CEST · Build under test: `main@20fb519` (debug package `com.shanivar.nativeapp.dev`) · minSdk 26, targetSdk 35
Status: **live**. No-phone checks are done (below): **3 FAIL → 4 bugs filed (DEV-93, DEV-94, DEV-95, DEV-96)**. **All 14 phone cases are NOT RUN, pending the founder's phone.** The founder cancelled the emulator (22:55 CEST), so nothing is planned for an emulator.

## DEV-75 acceptance criteria
| AC | Text | How it is met | Status |
|---|---|---|---|
| AC1 | All cases pass or have documented defects | Phone cases A1–D4 + no-phone checks N1–N11 | **NOT RUN** on the phone. No-phone: 3 FAIL, all documented as bugs |
| AC2 | No unresolved critical | Defect register | No Critical. **2 High open (DEV-93, DEV-94)** block sign-off under the DoD until fixed and re-verified on the phone |
| Analytics | Compare `alarm_observations` (DEV-53) with actual | DEV-53 is not on main | **BLOCKED** (DEV-53) |
| Privacy | Camera frames for dismissal not stored/sent | `PhotoMotionDismissalActivity.kt:179` closes the in-memory ImageProxy, with no file write and no upload. QR sends only `matched_payload` text (`QrDismissalActivity.kt:76`) | **PASS** (static). Confirm on the phone, A7 |

## Part 1: no-phone checks (done, with evidence)
Evidence in this folder: `unit_results_20fb519.txt`, `unit-full-20fb519.log`, `wording_scan_20fb519.txt`, `dst_check.jsh` + `dst_check_output.txt`, `static_review.md`.

| ID | Check | Result | Evidence / bug |
|---|---|---|---|
| N1 | Full unit suite main@20fb519 | **PASS**: 518 / 0 fail / 0 error / 1 skipped | `unit_results_20fb519.txt` |
| N2 | Alarm unit classes | **PASS**, 0 failures. AlarmRecurrenceTest 8 · AlarmRingerTest 7 (incl. hard cap, wake lock) · BootRescheduleCatchUpTest 5 · BootReconcileActionsTest 1 · MovementProgressTrackerTest 3 · GenuineMotionTrackerTest 6 · QrDismissalVerifierTest 5 · PhotoMotionDismissalStateTest 1 · DeviceObservationsTest 7 (43 tests) | same |
| N3 | Manifest: alarm permissions/components | **PASS with 1 finding**: SCHEDULE_EXACT_ALARM + USE_EXACT_ALARM, USE_FULL_SCREEN_INTENT, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, VIBRATE, CAMERA, ACTIVITY_RECOGNITION. AlarmActivity/receivers not exported. `AlarmForegroundService` specialUse. **Finding:** no LOCKED_BOOT_COMPLETED / directBootAware → **DEV-95**. Play risk: USE_EXACT_ALARM needs the alarm-clock declaration (DEV-67) | `static_review.md` §1 |
| N4 | Scheduling path | **PASS**: `setAlarmClock` (strongest Doze tier). Checks `canScheduleExactAlarms()` before arming. Setup refuses without exact-alarm, notification or full-screen-intent permission (`ShanivarApp.kt:570-580,677`) | §2 |
| N5 | "Armed" state is honest | **FAIL → DEV-94 (High)**: "Next: <time>", switch ON and Routine "armed" come from stored prefs only. They stay after Android drops the alarm (exact-alarm revoked, failed re-arm on reboot or after a ring). No re-arm on permission re-grant | §3 |
| N6 | Accessible dismissal always on the alarm screen (0003) | **FAIL → DEV-93 (High)**: the alarm screen has only "Start moving". The accessible button appears only if pre-selected or no sensor | §4 |
| N7 | Hard cap / no endless ring | **PASS**: `AlarmRinger.HARD_CAP_MILLIS` = 15 min, independent handler. AlarmRingerTest `hardCapEndsTheAlarmAndOutlastsNothing` passes | §5 |
| N8 | Back can't dismiss; alarm outranks practice | **PASS**: `AlarmActivity.kt` back callback swallowed. `BreathingSessionOwner.interrupt` runs after the ringer starts | §5 |
| N9 | DST / recurrence math | **PASS** (java.time mirror of `AlarmRecurrence.nextOccurrence`): fall-back 25 Oct 2026 02:30 rings once (02:30 CEST); the next ring is 26 Oct, also after a 15-min cap or from the repeated hour. Spring-forward 28 Mar 2027 02:30 → 03:30. **Gap:** no DST unit test in AlarmRecurrenceTest | `dst_check_output.txt` |
| N10 | Wording scan: alarm copy | **FAIL → DEV-96 (Low)**: channel description "non-dismissible wake-up alarm"; "Walk to your chosen place" shown to everyone | `wording_scan_20fb519.txt` |
| N11 | Reboot path | **PASS with finding**: `BootRescheduleReceiver` recomputes the next occurrence from the rule (0008) and does not fire a missed alarm late (documented). Before first unlock nothing runs → DEV-95 | §1, §3 |

## Part 2: phone cases (all NOT RUN, pending the founder's phone)
Setup: debug build of the RC, signed in or guest as the alarm card needs; sync off or local stack only (never cloud). Alarm volume up. Evidence: phone, Android version, build hash, local time CEST, screenshot or recording (for audio, record video).

| ID | Traces to | Case | Steps on the phone | Expected | Status |
|---|---|---|---|---|---|
| A1 | DEV-75 normal, FSI | Fires from the lock screen | Alarm now+2 min → lock the phone | Full-screen alarm over the lock screen, sound on the alarm stream, rising volume | NOT RUN |
| A2 | Doze | Doze / swiped away | Swipe Shanivar from Recents; unplug, screen off ≥1 h before the alarm | Rings on time | NOT RUN |
| A3 | 0008, B2, DEV-94/95 | Reboot / update / recurrence | (a) Reboot + unlock before the alarm. (b) Reinstall the same build. (c) Let a daily alarm ring and dismiss | Re-armed; "Next" shows tomorrow; rings again | NOT RUN |
| A4 | DEV-75 failure, DEV-94 | Exact alarm revoked | Schedule → Settings › Alarms & reminders off → reopen Shanivar → wait past the time | App says the alarm is not armed (today it is expected to still say "Next: …", DEV-94) | NOT RUN |
| A5 | Notifications | Notifications denied | Settings › Notifications › Shanivar off → try to schedule; also turn off after scheduling | Setup refuses with a clear message; after-the-fact state shown honestly | NOT RUN |
| A6 | DEV-75 edge (DST) | DST + zone change | Daily alarm **02:30, night of Sat 24 → Sun 25 Oct 2026**; separately change zone by hand | Exactly one ring at 02:30 local; correct next time after a zone change | NOT RUN |
| A7 | 0003, DEV-93 | Dismiss paths | With accessible movement **off**: alarm → look for the accessible option. Then QR, photo and movement paths | Accessible option visible on the alarm screen (expected FAIL until DEV-93 is fixed); no photo saved in the Gallery | NOT RUN |
| A8 | Practice | Alarm during practice | Start a 60 s practice; alarm fires | Practice stops, recorded as interrupted; alarm rings | NOT RUN |
| A9 | Hard cap | Leave it ringing | Do nothing for 15 min | Stops at 15 min; recorded as abandoned, not dismissed | NOT RUN |
| A10 | DEV-95 | Reboot, no unlock | Alarm now+10 min → reboot → do **not** unlock | Rings on the lock screen (expected FAIL, DEV-95) | NOT RUN |
| D1 | 0003 | Movement dismissal | Walk the step target | Dismisses at the target; steps taken before the alarm don't count | NOT RUN |
| D2 | DND | Wakes a person | Do Not Disturb on, vibrate-only ringer, phone across the room | Audible and noticeable (alarm stream) | NOT RUN |
| D3 | OEM | OEM killer | OEM phone, battery saver on, overnight alarm | Not suppressed | NOT RUN |
| D4 | Power | Off across the alarm time | Power off before, power on after | No late "ghost" ring; next occurrence armed | NOT RUN |

**Counts:** 14 phone cases (all NOT RUN) + 11 no-phone checks (8 PASS incl. 2 with findings, 3 FAIL) + 4 AC rows.
**Bugs filed from DEV-75:** DEV-93 (High), DEV-94 (High), DEV-95 (Medium), DEV-96 (Low). Duplicate check: JQL text search for accessible / dismissible / LOCKED_BOOT_COMPLETED / direct boot / exact alarm / pendingTriggerAtEpochMillis found none. DEV-3 covers only the focus-schedule side.
