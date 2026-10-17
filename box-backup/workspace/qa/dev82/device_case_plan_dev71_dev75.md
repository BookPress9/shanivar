# DEV-71 / DEV-75 — device case plan (emulator first, then real devices)

Owner: Shanivar Tester · 2026-10-17 (Europe/Berlin) · Status of every emulator case: **emulator-pending**. PM will say when the emulator is booted. Every real-device row stays **NOT RUN** until it runs on a physical phone.
**Rule:** an emulator pass is integration evidence only. Under the DoD it never replaces the Pixel and OEM rows (DEV-7 AC3, QA brief). Record emulator results as "emulator: PASS/FAIL" next to the case and leave the real-device verdict open.

## Setup (once the emulator is booted)
- Use the PM-provided emulator through `adb` (`adb devices` must list it). **Do not run** the Gradle managed-device task `pixel2Api34DebugAndroidTest`: it is forbidden on the dev machine. `./gradlew :app:connectedDebugAndroidTest` may target the attached emulator.
- Build: `./gradlew :app:assembleDebug` from the RC commit, then `adb install -r app/build/outputs/apk/debug/app-debug.apk` (package `com.shanivar.nativeapp.dev`). Turn ROUTINE_V3 on for debug only, as the device QA checklist describes. No source change.
- Record per case: emulator image (API level, ABI), build hash, exact commands, raw `adb logcat` excerpt, screenshot (`adb exec-out screencap -p`), and PASS/FAIL/NOT RUN. Evidence goes in `/workspace/qa/dev71/` and `/workspace/qa/dev75/`, with no personal data.
- Sync is off, or local stack only. Never the cloud project. Never reset the shared local stack.
- Restore the emulator clock and time zone after R3/R4/A6 (`adb shell settings put global auto_time 1`).

## DEV-71 — protection (permissions, sessions, reboot, recovery)
| ID | Case | Steps (emulator) | Expected | Emulator | Real device |
|---|---|---|---|---|---|
| P1 | Usage access revoked mid-window | Start a window → `adb shell appops set <pkg> GET_USAGE_STATS ignore` → reopen Routine | NEEDS_DEVICE_ACCESS with repair step; never Active | emulator-pending | NOT RUN |
| P2 | Overlay revoked | `appops set <pkg> SYSTEM_ALERT_WINDOW deny` during a window | Honest state, no false Active, stop still works | emulator-pending | NOT RUN |
| P3 | Exact alarm denied | `appops set <pkg> SCHEDULE_EXACT_ALARM deny` → save window | Not Ready; repair path named | emulator-pending | NOT RUN |
| S1 | Window START/END on time | Window now+2 → now+5 min, protected app = a test app | Overlay at START (brief flash is acceptable and disclosed), release at END | emulator-pending | NOT RUN |
| S2 | Two windows, one ends inside the other | Two windows, adjacent and nested (overlap is rejected at save) | Earlier END does not release the later window (DEV-2) | emulator-pending | NOT RUN |
| S3 | Overnight + full day | Set clock to 21:58 with a 22:00→06:00 window; set clock past midnight; full calendar day | Active across midnight, no 1-minute gap (DEV-5/6) | emulator-pending | NOT RUN |
| S4 | Stop offline; no snooze | Airplane mode (`cmd connectivity airplane-mode enable`), signed out → Stop | Protection ends immediately; block screen offers no snooze/"5 more min" (DEV-4/10) | emulator-pending | NOT RUN |
| R1 | Reboot inside / after a window | `adb reboot` inside the window; then again after END | Resumes inside; does not start after; next boundary re-armed (DEV-21) | emulator-pending | NOT RUN |
| R2 | Process death / force-stop | `adb shell am kill <pkg>`; separately `am force-stop <pkg>` | Kill: protection persists and resumes. Force-stop: the documented bypass, shown honestly (DEV-70 gap) | emulator-pending | NOT RUN |
| R3 | Doze, late START | `adb shell dumpsys deviceidle force-idle` across START; `unforce` after END | No 24 h session from a stale START (DEV-1) | emulator-pending | NOT RUN |
| R4 | TZ change + DST fall-back | Zone Europe/Berlin, clock 2026-10-25 02:30 (repeated hour); zone change to Asia/Kolkata | One boundary per rule; new next-boundary explained (DEV-5 AC4 gap) | emulator-pending | NOT RUN |
| R5 | App update | `adb install -r` same build during an active window | Windows and alarms re-armed (B2) | emulator-pending | NOT RUN |
| D1 | OEM battery saver / app killer | Xiaomi/Realme/Vivo/Samsung with saver ON, 2 h window | FocusGuardService survives, or failure surfaced | — | NOT RUN |
| D2 | Overnight on battery, low-battery mode | Real phone overnight | Protection holds until END | — | NOT RUN |
| D3 | Repeat on stock Pixel | Full table above | As above | — | NOT RUN |

## DEV-75 — alarms and notifications
| ID | Case | Steps (emulator) | Expected | Emulator | Real device |
|---|---|---|---|---|---|
| A1 | Fires from lock screen | Alarm now+2 min, lock (`input keyevent 26`) | Full-screen alarm UI on API 34+; sound | emulator-pending | NOT RUN |
| A2 | Doze / swipe-away | force-idle, swipe app from recents | Fires on time (setAlarmClock) | emulator-pending | NOT RUN |
| A3 | Reboot / update / recurrence | `adb reboot`; `install -r`; let a repeating alarm fire | Re-armed; next occurrence recomputed (0008) | emulator-pending | NOT RUN |
| A4 | Exact alarm denied | `appops set <pkg> SCHEDULE_EXACT_ALARM deny` | "Permission required" shown, no silent fail | emulator-pending | NOT RUN |
| A5 | Notifications denied | `pm revoke <pkg> android.permission.POST_NOTIFICATIONS` | State shown honestly | emulator-pending | NOT RUN |
| A6 | DST / TZ | Alarm 02:30 on 2026-10-25 Europe/Berlin; zone change | Exactly one ring at local wall-clock time | emulator-pending | NOT RUN |
| A7 | Dismiss paths | Accessible path; QR/photo with emulator virtual camera | Accessible path always reachable (0003) | emulator-pending | NOT RUN |
| A8 | Alarm during practice | Start 60 s Sadhana, alarm fires | Practice interrupted and recorded truthfully | emulator-pending | NOT RUN |
| A9 | Hard cap | Leave ringing | Ends at the cap; no endless ring | emulator-pending | NOT RUN |
| D1 | Movement dismissal | Real step detector / ACTIVITY_RECOGNITION | Dismiss by steps; fallback reachable | — | NOT RUN |
| D2 | Wakes a person | Volume, vibration, speaker, DND | Audible and noticeable | — | NOT RUN |
| D3 | OEM killer | OEM phone, saver ON, overnight | Alarm not suppressed | — | NOT RUN |
| D4 | Powered off across alarm time | Power off, power on after | Behaviour documented, no ghost alarm | — | NOT RUN |

Counts: 21 emulator-pending cases (DEV-71: 12, DEV-75: 9). Real-device rows NOT RUN: DEV-71 4 (AC1 + D1–D3), DEV-75 6 (AC1, AC2, D1–D4).
