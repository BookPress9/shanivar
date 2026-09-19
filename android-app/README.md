# Shanivar Android

Current implementation task: [streamlined two-entry MVP](CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md), **executed 2026-09-18** - see "Streamlined build" below for what changed, how to build it, and what still needs a physical device. Its [scope](INITIAL_MVP_SCOPE_2026-09-13.md) controls the first pitch/usability build. The rest of this file (and LOCAL_TESTING.md) describes the broader/earlier app and its emulator-based workflow; treat those as historical for anything the streamlined section below overrides. Claude leads this scoped task across the existing UI/backend ownership boundary.

## Streamlined build (2026-09-18)

**What changed.** The four-tab shell (Today/Focus/Practice/Rhythm) is replaced by one Home screen
with two independent cards, "Start focus" and "Set alarm" - see `HomeScreen`/`FocusCard`/
`AlarmCard` in `ShanivarApp.kt`. Onboarding drops birth intake and the generic capability tour
(`OnboardingFlow.kt`): LANGUAGE → WELCOME (self-use or a parent/family-member helper guide) → PLAN
(one of four editable presets) → ACCOUNT → ARRIVAL, with a pre-account one-minute local pause
reachable from WELCOME with no sign-in and no permissions (`LocalPauseScreen.kt`). The block screen
now offers that same pause as its primary action and "Return to my day" (device home, not back into
Shanivar) as secondary, replacing the AI reflection scroller as this build's block-screen redirect
(`BlockOverlayActivity.kt`). A focus session's manual end offers one skippable quick-answer review,
stored locally (`SessionReview.kt`) - this is fixed-choice, not the AI scroller, and records nothing
server-side.

**The alarm is now genuinely recurring.** `AlarmScheduler` previously persisted a single trigger
instant; nothing re-armed it after it fired, so "repeat on Mon/Wed/Fri" silently stopped repeating
after the first ring. It now persists the full rule (time + selected days) and recomputes the next
occurrence via `AlarmRecurrence.nextOccurrence` after every dismissal, every hard-cap timeout, and
every boot (`BootRescheduleReceiver`, which also fixes a missed-while-off alarm silently vanishing
forever instead of resuming its recurrence going forward). The Alarm card exposes day-of-week
chips, edit, enable/disable, cancel and a sound preview (`AlarmSoundPreview.kt`), backed by full
alarm CRUD in `RoutineApi.kt` (`listAlarms`/`editAlarm`/`setAlarmEnabled`/`deleteAlarm`, alongside
the existing `createAlarm`/`confirmAlarm`) - **all against the existing backend routes, unchanged**;
this required zero backend changes; see the doc comment on `RoutineApi.editAlarm` for one HTTP-verb
gotcha worth knowing (PUT, not PATCH - PATCH isn't in the JDK `HttpURLConnection` method whitelist
this client is built on, and would fail at request time, not compile time).

`POST_NOTIFICATIONS` is now requested at alarm setup, with the previously-silent failure mode made
explicit: without it, `AlarmFireReceiver`'s full-screen intent is a no-op and `AlarmActivity` never
launches on Android 13+, so the Alarm card blocks scheduling and says so rather than claiming
"Alarm confirmed" over a permission gap. `MovementDismissalActivity` now shows the accessible
dismissal automatically whenever no step sensor is available or `ACTIVITY_RECOGNITION` is denied,
not only when a user had pre-selected it - previously that device silently froze at 0/target steps
with no visible way out, which is exactly the trap non-negotiable #2 exists to prevent.

**Hidden switch.** `BuildConfig.STREAMLINED_LAUNCH` (`app/build.gradle.kts`, default `true`) is the
one explicit toggle between the new Home surface and the old four-tab `LegacyTabShell`, kept intact
rather than deleted. Flip it to `false` to fall back; nothing about the alarm/focus engines, backend
contract, or persisted data differs between the two paths - only which composable presents them.

**State migration.** All existing local data (SharedPreferences `rhythm`, `reflections`, the
encrypted reflection store) is read as before; new keys (`intentionLabel`, `alarmDays`, `alarmId`,
`alarmEnabled`) default safely on a device that never had them. On sign-in, `ShanivarApp` discovers
any existing server-side alarm via `RoutineApi.listAlarms()` and adopts it locally (id, days,
enabled) rather than trusting a possibly-stale local cache - this also means a different account
signing in on the same device can never inherit a previous account's cached alarm id. No active
focus/alarm is ever orphaned by this change: `FocusGuardService`/`AlarmScheduler`/`AlarmManager`
entries are independent of which UI shell is compiled in.

**Known limitations, stated rather than hidden.** The Focus card's "app to pause" picker is a fixed
allowlist of six known apps (`KnownDistractingApps`), not a full installed-app picker - see its doc
comment for why. The skippable session review is attached only to a manual "End this session now";
it does not yet distinguish a session reaching its full duration from an early end, both get the
same neutral copy - see CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md's package 5/7 for the fuller spec
this simplifies. Hindi strings exist throughout the new onboarding/Home copy but have not had a
native-speaker comprehension review - do not treat this as the "complete Hindi" gate the task and
INITIAL_MVP_SCOPE_2026-09-13.md require before Hindi-speaking parent recruitment.

**Build and verify:**

```powershell
cd android-app
./gradlew.bat testDebugUnitTest lintDebug assembleDebug "-PshanivarApiUrl=https://hybridmonk-api.onrender.com/api"
```

82 JVM/Robolectric tests pass (0 failures), including new coverage: `AlarmRecurrenceTest` (the
recurrence math - today-vs-next-week, multi-day nearest-match, week wraparound, exact-instant, empty/
invalid days), `RoutineApiTest`'s alarm-CRUD HTTP-verb/path coverage, and `NativeUiTest`/
`OnboardingFlowTest` rewritten against the new Home/onboarding copy. Lint is clean. The debug APK
builds against the live production backend
(`android-app/app/build/outputs/apk/debug/app-debug.apk`), which answered `GET /api/health` with
`{"status":"ok","database":"ok"}` at the time of this build - so testers need only internet, not a
shared Wi-Fi network or a locally running backend.

**Not verified: anything requiring physical hardware.** No emulator or managed-device task ran on
this machine (forbidden - see CLAUDE.md §1.6). Nothing here has confirmed: the alarm actually
sounds and escalates on a real device, `POST_NOTIFICATIONS`/exact-alarm prompts render and persist
correctly, the alarm fires while locked/backgrounded/in Doze, movement/step counting behaves on
real hardware, the block overlay's detection latency is tolerable in practice, TalkBack/large-font
behavior, or any of this survives an aggressive-OEM battery policy (Xiaomi/Oppo/Vivo/Samsung). The
eight-check physical-device protocol in ENFORCEMENT_STANCE_2026-09-12.md remains the gate before
any of this reaches a real tester.

**Demo script** (once on a physical device):
1. **Alarm-only** - Home → Set alarm → pick a time ~1 minute out, select today's day, Save &
   schedule. Grant notification/exact-alarm permissions if prompted. Lock the phone and wait for it
   to actually ring, escalate, and be dismissed by the movement or accessible path. Label this a
   test alarm, honestly - one short-interval ring is not overnight reliability evidence.
2. **Focus-only** - Home → Start focus → pick a preset and an app from the list → Start protected
   focus → open the selected app to see the block overlay's door (pause / return to my day) →
   return to Shanivar → End this session now → answer or skip the quick review.
3. **Combined** - both running at once, confirming the alarm card's controls stay reachable while
   focus is active and vice versa (non-negotiable: neither tool requires the other).
4. **Parent/helper framing** - restart onboarding (clear app data), choose the helper path, confirm
   the guide explains whole-app blocking and hands the phone back, and that the optional Shani
   explanation is reachable without being a mandatory step.

Native frontend owned by Codex, built against the Kotlin/Jetpack Compose decision in ../shanivar-vbt/app-shell/CODEX_HANDOFF.md. The old Capacitor scaffold is reference-only.

## Build
Requires JDK 17, Android SDK 35, Gradle 8.9 (wrapper included). Configure ANDROID_HOME or an untracked local.properties with sdk.dir.
From the repository root on Windows, run `./android-app/gradlew.bat -p android-app assembleDebug testDebugUnitTest lintDebug`. On macOS/Linux, use `bash android-app/gradlew` with the same arguments.
Debug API: http://10.0.2.2:5000/api (Android emulator host). Override debug builds with -PshanivarApiUrl=http://10.0.2.2:5101/api for the isolated local frontend fixture. Release names https://api.thehybridmonk.com/api; publish that DNS/API endpoint before a release build is distributed. Cleartext traffic is allowed only in the debug manifest.

## Content
Run node scripts/compile-content.cjs from this directory to regenerate the 12 technique assets from the Content team's Markdown. It requires verse and named translation fields. The script is packaging, not an independent citation verifier. See ../shanivar-vbt/qa/citation-verification-report.md for content QA.

## Integration status (verified 2026-09-12)
- The Express API uses the Supabase Postgres connection for account, profile, rhythm, focus, alarm, reflection and Today data. A disposable-account integration smoke test covers the real hosted database.
- Compose onboarding calls registration/profile endpoints and preserves English/Hindi plus optional birth inputs. Rhythm loads and saves account preferences. Focus calls live status/start/release routes. Alarm creation is scheduled locally and confirmed to the API only after Android accepts it.
- The movement-first dismissal surface uses the hardware step detector with a separated-motion fallback and records the outcome on a best-effort basis. QR/photo code remains only for legacy records and rollback.
- Authentication is currently in a ViewModel and survives activity recreation. App process death requires sign-in again; persistent secure session storage is outstanding. Signing out invalidates pending personal-note responses.
- Practice-player notes and completion remain local. Paid access and WhatsApp delivery still need their product/backend release contracts.
- Daily note fallback is labelled General reflection. The current backend does not establish chart/transit accuracy.
- Practice assets use technique slugs, while the legacy completion endpoint expects database practice IDs. Do not send slugs to that endpoint or award local XP/streak as if it were a server confirmation.
- Android 14 managed-emulator rendering and interaction checks are available. Real-device sensor, lock-screen, OEM battery-policy and reboot evidence remains a release gate. See [LOCAL_TESTING.md](LOCAL_TESTING.md) and [DEVICE_DISTRIBUTION.md](DEVICE_DISTRIBUTION.md).

Full direction and release evidence requirements: PRODUCT_DIRECTION.md.

## Messages from Shanivar

The Compose reflection scroller is implemented against Claude's /api/reflections contract. See [REFLECTION_SCROLLER_TESTING.md](REFLECTION_SCROLLER_TESTING.md) for the local mock flow and [REFLECTION_SCROLLER_PLAN.md](REFLECTION_SCROLLER_PLAN.md) for implementation scope. Release evidence and remaining device gates are tracked separately in REFLECTION_SCROLLER_QA.md.
