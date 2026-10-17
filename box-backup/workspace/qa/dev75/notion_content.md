<callout icon="📱" color="blue_bg">
	**Live.** Shanivar Tester, 2026-10-17 (Europe/Berlin), build main@20fb519. The founder cancelled the emulator, so every phone case is **NOT RUN, pending the founder's phone**. Source files are on the QA box at `/workspace/qa/dev75/` (test_cases.md, static_review.md, phone_checklist.md, unit_results_20fb519.txt, wording_scan_20fb519.txt, dst_check_output.txt).
</callout>
Jira [DEV-75](https://shanivar.atlassian.net/browse/DEV-75) · Parent: DEV-82 test plan
Owner: Shanivar Tester · Written 2026-10-17 23:05 CEST · Build under test: `main@20fb519` (debug package `com.shanivar.nativeapp.dev`) · minSdk 26, targetSdk 35
Status: **live**. No-phone checks are done (below): **3 FAIL → 4 bugs filed (DEV-93, DEV-94, DEV-95, DEV-96)**. **All 14 phone cases are NOT RUN, pending the founder's phone.** The founder cancelled the emulator (22:55 CEST), so nothing is planned for an emulator.

# DEV-75 acceptance criteria
<table fit-page-width="true" header-row="true">
	<tr><td>AC</td><td>Text</td><td>How it is met</td><td>Status</td></tr>
	<tr><td>AC1</td><td>All cases pass or have documented defects</td><td>Phone cases A1–D4 + no-phone checks N1–N11</td><td>**NOT RUN** on the phone. No-phone: 3 FAIL, all documented as bugs</td></tr>
	<tr><td>AC2</td><td>No unresolved critical</td><td>Defect register</td><td>No Critical. **2 High open (DEV-93, DEV-94)** block sign-off under the DoD until fixed and re-verified on the phone</td></tr>
	<tr><td>Analytics</td><td>Compare `alarm\_observations` (DEV-53) with actual</td><td>DEV-53 is not on main</td><td>**BLOCKED** (DEV-53)</td></tr>
	<tr><td>Privacy</td><td>Camera frames for dismissal not stored/sent</td><td>`PhotoMotionDismissalActivity.kt:179` closes the in-memory ImageProxy, with no file write and no upload. QR sends only `matched\_payload` text (`QrDismissalActivity.kt:76`)</td><td>**PASS** (static). Confirm on the phone, A7</td></tr>
</table>

# Part 1: no-phone checks (done, with evidence)
Evidence in this folder: `unit\_results\_20fb519.txt`, `unit-full-20fb519.log`, `wording\_scan\_20fb519.txt`, `dst\_check.jsh` + `dst\_check\_output.txt`, `static\_review.md`.

<table fit-page-width="true" header-row="true">
	<tr><td>ID</td><td>Check</td><td>Result</td><td>Evidence / bug</td></tr>
	<tr><td>N1</td><td>Full unit suite main@20fb519</td><td>**PASS**: 518 / 0 fail / 0 error / 1 skipped</td><td>`unit\_results\_20fb519.txt`</td></tr>
	<tr><td>N2</td><td>Alarm unit classes</td><td>**PASS**, 0 failures. AlarmRecurrenceTest 8 · AlarmRingerTest 7 (incl. hard cap, wake lock) · BootRescheduleCatchUpTest 5 · BootReconcileActionsTest 1 · MovementProgressTrackerTest 3 · GenuineMotionTrackerTest 6 · QrDismissalVerifierTest 5 · PhotoMotionDismissalStateTest 1 · DeviceObservationsTest 7 (43 tests)</td><td>same</td></tr>
	<tr><td>N3</td><td>Manifest: alarm permissions/components</td><td>**PASS with 1 finding**: SCHEDULE\_EXACT\_ALARM + USE\_EXACT\_ALARM, USE\_FULL\_SCREEN\_INTENT, POST\_NOTIFICATIONS, RECEIVE\_BOOT\_COMPLETED, WAKE\_LOCK, VIBRATE, CAMERA, ACTIVITY\_RECOGNITION. AlarmActivity/receivers not exported. `AlarmForegroundService` specialUse. **Finding:** no LOCKED\_BOOT\_COMPLETED / directBootAware → **DEV-95**. Play risk: USE\_EXACT\_ALARM needs the alarm-clock declaration (DEV-67)</td><td>`static\_review.md` §1</td></tr>
	<tr><td>N4</td><td>Scheduling path</td><td>**PASS**: `setAlarmClock` (strongest Doze tier). Checks `canScheduleExactAlarms()` before arming. Setup refuses without exact-alarm, notification or full-screen-intent permission (`ShanivarApp.kt:570-580,677`)</td><td>§2</td></tr>
	<tr><td>N5</td><td>"Armed" state is honest</td><td>**FAIL → DEV-94 (High)**: "Next: <time>", switch ON and Routine "armed" come from stored prefs only. They stay after Android drops the alarm (exact-alarm revoked, failed re-arm on reboot or after a ring). No re-arm on permission re-grant</td><td>§3</td></tr>
	<tr><td>N6</td><td>Accessible dismissal always on the alarm screen (0003)</td><td>**FAIL → DEV-93 (High)**: the alarm screen has only "Start moving". The accessible button appears only if pre-selected or no sensor</td><td>§4</td></tr>
	<tr><td>N7</td><td>Hard cap / no endless ring</td><td>**PASS**: `AlarmRinger.HARD\_CAP\_MILLIS` = 15 min, independent handler. AlarmRingerTest `hardCapEndsTheAlarmAndOutlastsNothing` passes</td><td>§5</td></tr>
	<tr><td>N8</td><td>Back can't dismiss; alarm outranks practice</td><td>**PASS**: `AlarmActivity.kt` back callback swallowed. `BreathingSessionOwner.interrupt` runs after the ringer starts</td><td>§5</td></tr>
	<tr><td>N9</td><td>DST / recurrence math</td><td>**PASS** (java.time mirror of `AlarmRecurrence.nextOccurrence`): fall-back 25 Oct 2026 02:30 rings once (02:30 CEST); the next ring is 26 Oct, also after a 15-min cap or from the repeated hour. Spring-forward 28 Mar 2027 02:30 → 03:30. **Gap:** no DST unit test in AlarmRecurrenceTest</td><td>`dst\_check\_output.txt`</td></tr>
	<tr><td>N10</td><td>Wording scan: alarm copy</td><td>**FAIL → DEV-96 (Low)**: channel description "non-dismissible wake-up alarm"; "Walk to your chosen place" shown to everyone</td><td>`wording\_scan\_20fb519.txt`</td></tr>
	<tr><td>N11</td><td>Reboot path</td><td>**PASS with finding**: `BootRescheduleReceiver` recomputes the next occurrence from the rule (0008) and does not fire a missed alarm late (documented). Before first unlock nothing runs → DEV-95</td><td>§1, §3</td></tr>
</table>

# Part 2: phone cases (all NOT RUN, pending the founder's phone)
Setup: debug build of the RC, signed in or guest as the alarm card needs; sync off or local stack only (never cloud). Alarm volume up. Evidence: phone, Android version, build hash, local time CEST, screenshot or recording (for audio, record video).

<table fit-page-width="true" header-row="true">
	<tr><td>ID</td><td>Traces to</td><td>Case</td><td>Steps on the phone</td><td>Expected</td><td>Status</td></tr>
	<tr><td>A1</td><td>DEV-75 normal, FSI</td><td>Fires from the lock screen</td><td>Alarm now+2 min → lock the phone</td><td>Full-screen alarm over the lock screen, sound on the alarm stream, rising volume</td><td>NOT RUN</td></tr>
	<tr><td>A2</td><td>Doze</td><td>Doze / swiped away</td><td>Swipe Shanivar from Recents; unplug, screen off ≥1 h before the alarm</td><td>Rings on time</td><td>NOT RUN</td></tr>
	<tr><td>A3</td><td>0008, B2, DEV-94/95</td><td>Reboot / update / recurrence</td><td>(a) Reboot + unlock before the alarm. (b) Reinstall the same build. (c) Let a daily alarm ring and dismiss</td><td>Re-armed; "Next" shows tomorrow; rings again</td><td>NOT RUN</td></tr>
	<tr><td>A4</td><td>DEV-75 failure, DEV-94</td><td>Exact alarm revoked</td><td>Schedule → Settings › Alarms & reminders off → reopen Shanivar → wait past the time</td><td>App says the alarm is not armed (today it is expected to still say "Next: …", DEV-94)</td><td>NOT RUN</td></tr>
	<tr><td>A5</td><td>Notifications</td><td>Notifications denied</td><td>Settings › Notifications › Shanivar off → try to schedule; also turn off after scheduling</td><td>Setup refuses with a clear message; after-the-fact state shown honestly</td><td>NOT RUN</td></tr>
	<tr><td>A6</td><td>DEV-75 edge (DST)</td><td>DST + zone change</td><td>Daily alarm **02:30, night of Sat 24 → Sun 25 Oct 2026**; separately change zone by hand</td><td>Exactly one ring at 02:30 local; correct next time after a zone change</td><td>NOT RUN</td></tr>
	<tr><td>A7</td><td>0003, DEV-93</td><td>Dismiss paths</td><td>With accessible movement **off**: alarm → look for the accessible option. Then QR, photo and movement paths</td><td>Accessible option visible on the alarm screen (expected FAIL until DEV-93 is fixed); no photo saved in the Gallery</td><td>NOT RUN</td></tr>
	<tr><td>A8</td><td>Practice</td><td>Alarm during practice</td><td>Start a 60 s practice; alarm fires</td><td>Practice stops, recorded as interrupted; alarm rings</td><td>NOT RUN</td></tr>
	<tr><td>A9</td><td>Hard cap</td><td>Leave it ringing</td><td>Do nothing for 15 min</td><td>Stops at 15 min; recorded as abandoned, not dismissed</td><td>NOT RUN</td></tr>
	<tr><td>A10</td><td>DEV-95</td><td>Reboot, no unlock</td><td>Alarm now+10 min → reboot → do **not** unlock</td><td>Rings on the lock screen (expected FAIL, DEV-95)</td><td>NOT RUN</td></tr>
	<tr><td>D1</td><td>0003</td><td>Movement dismissal</td><td>Walk the step target</td><td>Dismisses at the target; steps taken before the alarm don't count</td><td>NOT RUN</td></tr>
	<tr><td>D2</td><td>DND</td><td>Wakes a person</td><td>Do Not Disturb on, vibrate-only ringer, phone across the room</td><td>Audible and noticeable (alarm stream)</td><td>NOT RUN</td></tr>
	<tr><td>D3</td><td>OEM</td><td>OEM killer</td><td>OEM phone, battery saver on, overnight alarm</td><td>Not suppressed</td><td>NOT RUN</td></tr>
	<tr><td>D4</td><td>Power</td><td>Off across the alarm time</td><td>Power off before, power on after</td><td>No late "ghost" ring; next occurrence armed</td><td>NOT RUN</td></tr>
</table>

**Counts:** 14 phone cases (all NOT RUN) + 11 no-phone checks (8 PASS incl. 2 with findings, 3 FAIL) + 4 AC rows.
**Bugs filed from DEV-75:** DEV-93 (High), DEV-94 (High), DEV-95 (Medium), DEV-96 (Low). Duplicate check: JQL text search for accessible / dismissible / LOCKED\_BOOT\_COMPLETED / direct boot / exact alarm / pendingTriggerAtEpochMillis found none. DEV-3 covers only the focus-schedule side.

# Founder phone checklist
For: Shubham · About 1.5 h of active time, plus one overnight (D3) and the DST night (A6, **Sat 24 → Sun 25 Oct 2026**). No computer needed.
Before you start:
- [ ] Debug build from QA installed (hash: ________). Phone model / Android version: ________
- [ ] Alarm volume up. Shanivar has Alarms & reminders, Notifications and full-screen alerts allowed.
- [ ] For audio checks, take a short **video**. For the rest, a screenshot. Tick PASS or FAIL.
- [ ] Two checks are **expected to fail** today (A7 accessible option, A10 locked reboot). Please run them anyway; they confirm bugs DEV-93 and DEV-95.

**Ringing**
- [ ] A1 Alarm 2 min from now, lock the phone. ✔ The alarm screen appears over the lock screen and the sound gets louder.
- [ ] A2 Swipe Shanivar out of Recents, unplug, screen off for 1 h before an alarm. ✔ Rings on time.
- [ ] A9 Let one alarm ring without touching it. ✔ It stops by itself at 15 min.
- [ ] D2 Do Not Disturb on, ringer on vibrate, phone across the room. ✔ You hear it.

**Repeat and recovery**
- [ ] A3 (a) Restart and unlock before an alarm. (b) Reinstall the same build. (c) Let a daily alarm ring and dismiss it. ✔ Each time the card shows tomorrow's "Next" time and it rings again.
- [ ] A10 Alarm 10 min ahead → restart → **don't unlock** → wait. Note whether it rings. (Expected to fail: DEV-95.)
- [ ] D4 Power off before an alarm, power on after its time. ✔ No late ring; next one is armed.
- [ ] A6 Daily alarm at **02:30**, night of Sat 24 → Sun 25 Oct (clocks go back at 03:00). ✔ Exactly one ring. Also: change the time zone by hand once, and note the "Next" time.

**Permissions**
- [ ] A4 Schedule an alarm, then Settings › Apps › Shanivar › **Alarms & reminders off**. Reopen Shanivar and screenshot the alarm card. Wait past the time. Note: rang / didn't ring, and what the card said. (The card is expected to wrongly say "Next: …": DEV-94.)
- [ ] A5 Notifications off for Shanivar → try to schedule. ✔ It refuses with a clear message.

**Dismissing**
- [ ] A7 With "accessible movement" **off** in settings: when the alarm rings, look for an option that needs no walking. Screenshot. (Expected to fail: DEV-93.) Then try the QR and photo options once each, and check the Gallery: ✔ no photo saved.
- [ ] D1 Walk the step target. ✔ It dismisses at the target.
- [ ] A8 Start a 60-second practice, with an alarm set to ring during it. ✔ Practice stops; alarm rings.

**Phone-specific**
- [ ] D3 OEM phone (if you have one) with battery saver on: overnight alarm. ✔ Rings. DEV-75 needs 2 brands / Android 12–15 to sign off.

Send back: the ticked list + screenshots/videos to QA (Shanivar Tester).

# Static review notes
Read-only, on the clean export `/workspace/qa/build-20fb519`. Paths are relative to `android-app/app/src/main/`.

### 1. Manifest
- L40 SCHEDULE\_EXACT\_ALARM, L46 USE\_EXACT\_ALARM, L51 USE\_FULL\_SCREEN\_INTENT, L54 RECEIVE\_BOOT\_COMPLETED, L58 POST\_NOTIFICATIONS, L60-61 CAMERA / ACTIVITY\_RECOGNITION, L69-70 VIBRATE / WAKE\_LOCK.
- L93 AlarmActivity exported=false singleInstance. L106 AlarmForegroundService specialUse. L112 AlarmFireReceiver exported=false.
- L115-122 boot receiver: BOOT\_COMPLETED, TIME\_SET, TIMEZONE\_CHANGED, MY\_PACKAGE\_REPLACED. **No LOCKED\_BOOT\_COMPLETED, no `directBootAware`; prefs are credential-encrypted** → **DEV-95**.

### 2. Arming
- `AlarmScheduler.kt:90-99` `armNativeAlarm`: on API 31+ it returns false if `!canScheduleExactAlarms()`, otherwise `setAlarmClock(AlarmClockInfo, PendingIntent.getBroadcast(... IMMUTABLE))`.
- `ShanivarApp.kt:570-580` refuses to schedule without exact-alarm or notification permission. `:677` checks `canUseFullScreenIntent()` (API 34+).

### 3. Honest "armed" state → DEV-94
- `AlarmScheduler.kt:43-54` `scheduleRule` returns null **before** writing prefs, so a stale `next\_trigger\_at` survives a failed re-arm.
- `:76` `pendingTriggerAtEpochMillis()` = any stored value > 0 (past or not, permission or not).
- Consumers: `ShanivarApp.kt:305-306` (`alarmArmed`), `:562,615-622` ("Next: …", switch), `V1Home.kt:65`, `AlarmActivity` done screen.
- Re-arm callers that can fail silently: `BootRescheduleReceiver.kt:52-54`, `AlarmActivity.kt:69,176` (`runCatching { advanceToNextOccurrence() }`).
- No receiver for `ACTION\_SCHEDULE\_EXACT\_ALARM\_PERMISSION\_STATE\_CHANGED`.

### 4. Accessible dismissal → DEV-93
- `AlarmActivity.kt:193-205` `AlarmScreen`: text + one button, "Start moving".
- `MovementDismissalActivity.kt:55,61,64,132`: the accessible button shows only when `accessibleMovement` pref is true or `sensorUnavailable`.
- `docs/decisions/0003-movement-first-alarm-dismissal.md`: accessible "always reachable from the alarm screen itself". Website `index.html:115` promises the same.

### 5. Safety rails that pass
- `AlarmRinger.kt:43-60` hard cap 15 min on its own handler. Wake lock is released in `stop()`. Escalating volume (AlarmRingerTest).
- `AlarmActivity.kt` Back swallowed. Hard-cap path records "abandoned" and advances the recurrence after teardown.
- `AlarmFireReceiver.kt` IMPORTANCE\_HIGH channel, CATEGORY\_ALARM, full-screen intent + direct `startActivity` fallback.
- Privacy: `PhotoMotionDismissalActivity.kt:176-181` closes the image in memory. QR sends only the matched text.

### 6. Copy → DEV-96
- `AlarmFireReceiver.kt:35` "Shanivar's non-dismissible wake-up alarm." `:39-40` "Walk to your chosen place to dismiss." `AlarmForegroundService.kt:34` "Get up and move to finish dismissing this alarm."
