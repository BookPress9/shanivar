# DEV-75 static review of the alarm code paths (main@20fb519)
Read-only, on the clean export `/workspace/qa/build-20fb519`. Paths are relative to `android-app/app/src/main/`.

## 1. Manifest
- L40 SCHEDULE_EXACT_ALARM, L46 USE_EXACT_ALARM, L51 USE_FULL_SCREEN_INTENT, L54 RECEIVE_BOOT_COMPLETED, L58 POST_NOTIFICATIONS, L60-61 CAMERA / ACTIVITY_RECOGNITION, L69-70 VIBRATE / WAKE_LOCK.
- L93 AlarmActivity exported=false singleInstance. L106 AlarmForegroundService specialUse. L112 AlarmFireReceiver exported=false.
- L115-122 boot receiver: BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, MY_PACKAGE_REPLACED. **No LOCKED_BOOT_COMPLETED, no `directBootAware`; prefs are credential-encrypted** → **DEV-95**.

## 2. Arming
- `AlarmScheduler.kt:90-99` `armNativeAlarm`: on API 31+ it returns false if `!canScheduleExactAlarms()`, otherwise `setAlarmClock(AlarmClockInfo, PendingIntent.getBroadcast(... IMMUTABLE))`.
- `ShanivarApp.kt:570-580` refuses to schedule without exact-alarm or notification permission. `:677` checks `canUseFullScreenIntent()` (API 34+).

## 3. Honest "armed" state → DEV-94
- `AlarmScheduler.kt:43-54` `scheduleRule` returns null **before** writing prefs, so a stale `next_trigger_at` survives a failed re-arm.
- `:76` `pendingTriggerAtEpochMillis()` = any stored value > 0 (past or not, permission or not).
- Consumers: `ShanivarApp.kt:305-306` (`alarmArmed`), `:562,615-622` ("Next: …", switch), `V1Home.kt:65`, `AlarmActivity` done screen.
- Re-arm callers that can fail silently: `BootRescheduleReceiver.kt:52-54`, `AlarmActivity.kt:69,176` (`runCatching { advanceToNextOccurrence() }`).
- No receiver for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.

## 4. Accessible dismissal → DEV-93
- `AlarmActivity.kt:193-205` `AlarmScreen`: text + one button, "Start moving".
- `MovementDismissalActivity.kt:55,61,64,132`: the accessible button shows only when `accessibleMovement` pref is true or `sensorUnavailable`.
- `docs/decisions/0003-movement-first-alarm-dismissal.md`: accessible "always reachable from the alarm screen itself". Website `index.html:115` promises the same.

## 5. Safety rails that pass
- `AlarmRinger.kt:43-60` hard cap 15 min on its own handler. Wake lock is released in `stop()`. Escalating volume (AlarmRingerTest).
- `AlarmActivity.kt` Back swallowed. Hard-cap path records "abandoned" and advances the recurrence after teardown.
- `AlarmFireReceiver.kt` IMPORTANCE_HIGH channel, CATEGORY_ALARM, full-screen intent + direct `startActivity` fallback.
- Privacy: `PhotoMotionDismissalActivity.kt:176-181` closes the image in memory. QR sends only the matched text.

## 6. Copy → DEV-96
- `AlarmFireReceiver.kt:35` "Shanivar's non-dismissible wake-up alarm." `:39-40` "Walk to your chosen place to dismiss." `AlarmForegroundService.kt:34` "Get up and move to finish dismissing this alarm."
