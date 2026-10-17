# DEV-71 static review of the protection code paths (main@20fb519)
Read-only review on a clean `git archive` export (`/workspace/qa/build-20fb519`). No source changed. Paths are relative to `android-app/app/src/main/`.

## 1. Manifest (`AndroidManifest.xml`)
- L11 `PACKAGE_USAGE_STATS` (Settings grant), L19 `SYSTEM_ALERT_WINDOW`, L24/L32 `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`, L54 `RECEIVE_BOOT_COMPLETED`, L58 `POST_NOTIFICATIONS`.
- L101-105 `FocusGuardService` exported=false, `foregroundServiceType="specialUse"`, subtype "Detects and blocks a user-selected distracting app during a focus session the user started". Play review must accept it (DEV-67).
- L88 `BlockOverlayActivity` exported=false, singleInstance, excludeFromRecents.
- L115-122 `BootRescheduleReceiver` exported=true, but every action it accepts is a protected system broadcast, and the code re-filters with `RECONCILE_ACTIONS`. OK.
- L72-79 `<queries>` limits visibility to the 6 catalogue apps. This matches `ProtectionAppCatalog` (no full app inventory read). Privacy: good.
- No AccessibilityService and no DeviceAdmin (decision 0002). OK.

## 2. Reboot / clock catch-up: `BootRescheduleReceiver.kt`
- L74-79 `BootFocusCatchUp.apply` uses `FocusWindowRecurrence.activeWindowEnd` + `LocalProtectionController` with no session. L141 always re-arms (`runCatching`).
- L84 server mirror only `if (outcome.started && api != null)`. Matches DEV-21. Unit coverage: BootRescheduleCatchUpTest (5), BootReconcileActionsTest (1), all pass.

## 3. Permission loss
- `LocalProtection.kt:68-83` `start()` refuses when `!canEnforce()` (usage && overlay).
- `DisciplineEngine.kt:57-60` NEEDS_PERMISSION before any ACTIVE. L66-72 re-asserts the guard before claiming ACTIVE.
- `FocusScheduleStatus.kt:73-101` and `RoutineProtection.kt:251-262`: missing access → NEEDS_DEVICE_ACCESS ahead of ACTIVE. Covered by FocusScheduleStatusTest (16) and RoutineProtectionTest (31).
- **Observation (DEV-70 scope):** `FocusGuardService.kt:62-76` poll loop never re-checks permissions. `queryEvents` returns nothing without Usage access, and `startActivity` fails silently without overlay. The ongoing notification (L118 "Focus protection is on") stays up. There is no heartbeat and no `protection_failure` event. This is exactly DEV-70, so no duplicate bug was filed.

## 4. Stop and no-snooze
- `DisciplineEngine.releaseFocus()` L115-127: `guard.stop()` first, server release best effort.
- `LocalProtectionController.stopNow()` has no API type. `RoutineProtectionScreen.kt:185` "Stop protection now".
- `BlockOverlayActivity.kt:105-115`: Return to my day / Choose a short practice / Take a one-minute pause. No snooze. Back goes home (L40).
- "Choose a short practice" → `BreathingHost(BLOCK_OVERLAY)` (L49): already **DEV-89**.

## 5. Clock change
- `FocusGuardService` stores absolute `endsAt` (epoch ms). Moving the clock forward past END stops it (L66). Moving it back extends blocking in wall-clock terms. `TIME_SET` runs `BootFocusCatchUp`, which can start protection but never shortens an active one. Not filed: Stop always works and the product impact is unclear. Phone case R6 records the actual behaviour.
