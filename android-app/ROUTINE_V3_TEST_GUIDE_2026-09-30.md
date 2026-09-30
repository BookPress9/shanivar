# Routine V3 test guide

Prepared 30 September 2026. This guide validates the accepted Phase 1 Routine build. It does not authorize an emulator; use unit/Robolectric checks and real Android hardware only.

## Automated gates

### Navigation and migration

- Exactly two primary destinations exist: Routine and Sadhana.
- Returning launch opens Routine; a Sadhana deep link opens Sadhana.
- Back from Sadhana returns according to the accepted shell contract without resurrecting Dashboard.
- Existing users retain alarm, selected apps and their first focus schedule after migration.
- Migration is idempotent and corrupted legacy data yields `Needs attention`, not a crash or false `Ready`.

### Wake and intended sleep

- Distinct valid times persist across process death and reboot.
- Equal wake/sleep values are rejected or explicitly represented; never silently interpreted as 24 hours.
- Changing intended sleep does not change alarm state.
- Only one canonical preference owner remains after migration.

### Personal activities

- Edit Routine can change wake/sleep anchors without changing alarm configuration.
- A personal activity persists its stable ID, name, time, duration, enabled state and occurrence across process death and reboot.
- Every day resolves to all seven days; Weekly resolves to one selected day; Custom requires at least one selected day.
- Gym, Read a book and Extend Sadhana quick adds create editable activities; they are not special engine types.
- The generic bottom-right add control creates a new editable activity and the editor supports up to 12 activities.
- Blank names, invalid times, invalid durations and empty Custom selections cannot be saved.
- Editing or deleting a personal activity never arms, changes or dismisses an alarm and never creates, starts or stops app protection.
- Routine sorts activities correctly around the user's wake anchor, including events after midnight.

### Recurrence

- Every day, weekdays, weekends and arbitrary custom days compute the next boundary.
- Same-day windows start and end correctly.
- Monday 22:00-06:00 is active Monday night and ends Tuesday morning.
- The same overnight window is inactive Sunday night unless Sunday is selected.
- DST gap and overlap cases produce one deterministic boundary and do not duplicate an action.
- Time-zone and manual-clock changes recompute from local rules.
- `start == end` is invalid.
- Overlapping enabled windows are rejected; adjacent windows are accepted.
- Up to eight windows persist and independently enable/disable/delete.

### Delivery and process death

- Each boundary identifies the expected rule/action/instant.
- A delayed START delivered after its window ended is skipped and advanced.
- A duplicated receiver delivery is idempotent.
- A stale END cannot release a different active rule.
- Cold-process delivery works without `ShanivarSession.api` already populated.
- Reboot inside a valid window resumes protection; reboot after it ended does not.
- Failure to arm exact alarms produces `Needs device access` or `Needs attention`, never `Ready`.

### App selection

- Only eligible installed or explicitly curated packages can be selected.
- Essential/system exclusions cannot be selected through UI or corrupted storage.
- Uninstalled selected apps display unavailable and are not claimed protected.
- Selected package set persists and is shared across windows.
- Empty selection cannot arm protection.

### Status and recovery

- `Saved`, `Ready`, `Active`, `Needs attention` and `Off` map to distinct underlying conditions.
- Active requires engine confirmation.
- Stop protection works locally without network, sign-in, payment, timer or practice.
- Force-stop, revoked Usage access, revoked overlay, revoked exact alarms and notification denial produce truthful repair states.
- Alarm and focus recovery do not clear one another's configuration.

### Accessibility and UI

- All interactive controls have at least 48 dp targets and meaningful TalkBack labels/states.
- 200% font scale, smallest supported screen and landscape keep actions reachable by scrolling.
- Keyboard/switch access can create, edit and delete a window.
- Keyboard/switch access can create, edit and delete a personal activity and select occurrence days.
- Color is not the only carrier of status.
- Busy operations prevent duplicate submission without trapping navigation permanently.
- Light cards use dark foregrounds; black cards use light foregrounds.

## Required commands

From `android-app/`:

```powershell
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug -PshanivarApiUrl=https://hybridmonk-api.onrender.com/api
```

Do not run `pixel2Api34DebugAndroidTest` or another emulator/managed-device target. If a physical device is connected and authorized:

```powershell
./gradlew connectedDebugAndroidTest
```

Record raw task output and exact test count. A prior run is evidence for its own version only.

## Physical-device matrix

Run each case on at least one Android 14+ device and one supported older-device version where available. Record manufacturer restrictions and battery settings.

1. Fresh install -> onboarding -> Routine.
2. Existing-data migration from the current one-schedule build.
3. Edit Routine: change anchors; add Gym, a custom activity and custom days; relaunch and verify the same timeline.
4. Alarm fires from screen off; every dismissal and hard cap ends sound.
5. Same-day Focus START/END with app in foreground, background and process killed.
6. Overnight Focus START before midnight and END after midnight.
7. Reboot before start, during window and after end.
8. Device powered off across START; restart inside window and after end.
9. Permission revoked before a boundary.
10. Two non-overlapping windows on one day; add the second with the bottom-right `+`.
11. Custom week crossing midnight.
12. Time-zone change and manual time correction.
13. Blocked app launch, expected detection latency and overlay exit.
14. Essential local Stop while offline and signed out.
15. TalkBack and 200% font-size completion of setup, Edit Routine and recovery.

## Release blockers

Routine V3 cannot be called ready while any of these remains:

- delayed START can create protection beyond its actual window;
- a cold process silently skips a valid window;
- an END can release unrelated active protection;
- overnight recurrence is absent while the UI accepts it;
- local recovery needs server/account state;
- UI claims active/ready without engine evidence;
- alarm sound can persist after dismissal;
- physical-device alarm/focus/reboot tests remain unrun;
- content or controls become unreachable at large text/TalkBack.

Browser-wireframe verification is useful design evidence only. It cannot satisfy a native or physical-device gate.

