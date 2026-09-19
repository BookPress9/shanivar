# Native integration contract: Discipline Engine (app-lock + alarm) → Codex

Initial-launch override, 13 September: [current scope](INITIAL_MVP_SCOPE_2026-09-13.md) requires alarms plus focus modes using existing contracts. Alarm setup and repeat-day recurrence are required; automatic focus schedules remain deferred. Preserve existing safety and contract invariants, including managing already-active services on upgrade. The [Claude task](CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md) controls work sequencing over the earlier MVP checklist below.

Status, updated 2026-09-12: backend live-verified against Supabase; Compose integration and the native engine pass JVM/Robolectric and Android 14 managed-emulator checks. Physical-device checks remain open; see the honest gap list below. Full research/rationale: `shanivar-vbt/discipline-engine/platform-research.md`. Squad record: `ai_org/discipline_engine_squad.md`. Product-owner-level tracker: `ai_org/product_owner_first_apk.md`.

**Founder update, 2026-09-12:** the QR-first dismissal UI and the `qr`/`photo_motion` product choice below are superseded by [the movement-first alarm direction](FOUNDER_MOVEMENT_ALARM_DIRECTION_2026-09-12.md). The new-user Compose flow now saves the movement contract, schedules the alarm locally, confirms it to the API, and opens `MovementDismissalActivity`; the earlier QR/photo code remains as a legacy rollback path.

## Read first: the enforcement stance, 2026-09-12
How hard these features push is now a written founder-level decision:
[ENFORCEMENT_STANCE_2026-09-12.md](ENFORCEMENT_STANCE_2026-09-12.md). It governs everything below.
Short version: maximise real friction, never claim the impossible, and never let enforcement become
a malfunction the user cannot escape. It contains the alarm escalation spec, the eight safety
invariants that guarantee an alarm always stops when it should, the app-lock enforcement ladder with
its honest limits, and the redirect-to-practice design for the block surface.

Landed against it already: escalating alarm volume on the alarm stream, an independent 15-minute
watchdog that silences the alarm even if the dismissal logic is broken, relaunch-on-Home for the
alarm surface, and synchronous silencing that never waits on the network.

## Elevated to MVP priority — 2026-09-11; connected 2026-09-12
The founder named Alarm + social-media app-lock as the MVP. `ShanivarApp.kt` now calls `DisciplineEngine.status/startFocus/releaseFocus`; Rhythm creates an API alarm, calls `AlarmScheduler.schedule()`, and confirms the computed occurrence. UI state only says protected or scheduled after the engine/API succeeds.

Also fixed since the last handoff, unrelated to this feature but blocking it on a real device either way: `android-app/gradle.properties`'s `shanivarApiUrl` was hardcoded to the emulator-only `10.0.2.2`; it now points at the dev machine's LAN IP, verified end to end. See `ai_org/product_owner_first_apk.md` for that account, and `ai_org/family_testing_handoff_meeting_2026-09-11.md` for why local Wi-Fi testing is the current plan.

**Landed, 2026-09-12**: the default block-list is now Instagram + YouTube + TikTok + Facebook + X + Snapchat (package IDs below), still fully overridable per-user, and `GET /api/discipline/lock-targets` is live — returns `{ targets: [{ package: "com.instagram.android", label: "Instagram" }, ...] }` for exactly those six, authenticated the same as every other route in this file. Also fixed while hardening this for MVP: `POST /focus/start` had a real concurrency race (proven live — two simultaneous starts both returned 201, giving one user two active sessions at once); now enforced by a DB-level unique constraint, the same pattern already used for the auth-registration race. Everything in this doc is stable and safe to build against now — nothing pending.

Package IDs, if the block-list picker needs them outside the catalog call for any reason: `com.instagram.android`, `com.google.android.youtube`, `com.zhiliaoapp.musically` (TikTok — kept its pre-2018-merger "musical.ly" id, not something with "tiktok" in it), `com.facebook.katana`, `com.twitter.android`, `com.snapchat.android`.

## New, 2026-09-12: report every alarm that fires — `POST /api/telemetry/alarm-fired`

`AlarmFireReceiver` must call this the moment an alarm reaches the user, before or alongside
showing `AlarmActivity`. Fire-and-forget: never gate the ringing UI on it, exactly like dismissal
recording.

```
POST /api/telemetry/alarm-fired
{
  "alarm_schedule_id": "<uuid, optional>",
  "scheduled_for": "<ISO timestamp the alarm was set for, optional>",
  "fired_at": "<ISO timestamp, optional — server defaults to now>",
  "delivery_path": "full_screen_intent" | "notification_only" | "activity_direct" | "unknown",
  "device_manufacturer": "Xiaomi",   // android.os.Build.MANUFACTURER
  "device_model": "Redmi Note 12",   // android.os.Build.MODEL
  "android_sdk": 34,                 // android.os.Build.VERSION.SDK_INT
  "app_version": "0.1.0",
  "client_event_id": "<uuid you generate per alarm occurrence>"
}
```

**Why this matters more than it looks.** `alarm_dismissal_events` only records what happened after
an alarm woke someone. An alarm the OS killed, deferred, or never delivered writes nothing at all —
so "Xiaomi silently dropped it" and "this person stopped using the app" are the same shape in the
data. With a physical-device cohort whose whole purpose is finding where alarms die, that gap makes
the test unmeasurable. Fire events close it.

**`client_event_id` is not optional in practice.** Generate one UUID per alarm occurrence and reuse
it on retry. A device that fires offline will report late; without a stable id that retry is counted
as a second alarm, which inflates apparent reliability — the precise opposite of what this measures.
Retries return `200` with the original row rather than `201`.

`delivery_path` is what actually got the user's attention, and it is the diagnostic that separates
"full-screen intent was honoured" from "we only managed a notification" on OEMs that quietly
downgrade it. Send `unknown` rather than guessing.

Verified live against the production database on 2026-09-12: recording with device metadata,
retry idempotency, cross-user rejection (404), validation rejection (400), and admin-gating of the
aggregate (403). Backend suite 296/296.

## Build spec: the two screens that matter most

No wireframes exist for this app (confirmed — there's no design doc or mockup anywhere in the repo), so this section is deliberately more prescriptive than a typical handoff: treat it as the wireframe. Match the existing visual language already established in `ShanivarApp.kt` (the `Void`/`Bone`/`Gold`/`Muted`/`Surface` palette, `SectionCard`, `PrimaryAction`, the direct-warm-spare copy voice) rather than introducing a new style for just these two screens.

### 1. Focus tab (replaces the current placeholder `FocusScreen`)
- On entry, call `DisciplineEngine.status()`. Branch on `ProtectionState`:
  - `NEEDS_PERMISSION` → don't show a focus-duration picker at all; show what's missing (usage access / overlay, whichever `DisciplinePermissions` reports as false) and a single button into the permission-grant flow (section 3 below). Don't let someone start a session that will silently fail.
  - `OFF` → the existing "your focus window" card, but the button now actually calls `startFocus(minutes, packageNames)` against a real `packageNames` set the user picked (see block-list picker below), not decorative copy.
  - `ACTIVE` → replace the picker with "Protected until `HH:MM`" (from `focusEndsAtEpochMillis`, already computed correctly client-side — never show the backend's raw `expires_at`) and the list of `protectedPackages` by friendly name, plus a "release early" action calling `releaseFocus()`.
  - Surface `startFocus`'s two real failure modes distinctly, per `DisciplineError`: `EntitlementRequired` (402) → "upgrade needed" messaging, no pricing UI yet (open founder decision, don't invent a price); `Network` → a retry affordance, not a silent failure.
- **Block-list picker**: a simple multi-select checklist sourced from `GET /discipline/lock-targets` (falls back to just Instagram if that call fails — don't block the whole screen on it), defaulting to whatever the user already saved in `discipline_preferences.block_package_list`.
- **Duration**: offer quick-pick chips (e.g. 15 / 30 / 60 / 120 min) over the backend's real 5–480 minute range, rather than a raw numeric field — this is a UI-only convenience, the API already takes any integer in range.

### 2. Rhythm screen's alarm section (replaces the current SharedPreferences-only save)
- List existing alarms from `GET /discipline/alarms` (a user can have more than one — this is already a full multi-alarm CRUD server-side, don't build a single-alarm UI against it).
- Create/edit an alarm: time, days-of-week (a real day-of-week selector, 0=Sunday matching the API), sound, and dismissal method (`qr` vs `photo_motion` — the existing `FilterChip` pattern in the current placeholder code already gestures at this, keep that pattern). On save, call `POST /discipline/alarms` (or `PUT .../:id`), then actually call `AlarmScheduler.schedule(triggerAt, alarmId)` — today nothing does this outside the boot-reschedule path, which is the core gap. After scheduling, call `POST /discipline/alarms/:id/confirm-scheduled` with the real computed trigger time, per the existing contract (the server never assumes scheduling happened until told).
- The actual alarm-firing → dismissal flow is **already fully built and needs no new Compose work**: `AlarmFireReceiver` → `AlarmForegroundService`/full-screen intent → `AlarmActivity` → launches either `QrDismissalActivity` or `PhotoMotionDismissalActivity` depending on the saved preference, both of which already record the outcome server-side. Don't duplicate this — the only gap is that nothing schedules the first alarm.

### 3. Permission-grant onboarding (needed for both screens above to do anything real)
A short, linear sequence — not four unrelated settings buttons scattered across the app:
1. Usage access (`Settings.ACTION_USAGE_ACCESS_SETTINGS` deep link) — explain in one plain sentence why before sending them there, since it's a settings toggle, not a dialog.
2. Overlay permission (`ACTION_MANAGE_OVERLAY_PERMISSION`), same pattern.
3. Exact alarm (`SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`).
4. Camera — a normal `ActivityResultContracts.RequestPermission()` dialog; the dismissal activities currently assume this is already granted and have no request UI of their own, so it must happen before either is launched.
Re-check permission state on every return to the app (`NATIVE_HANDOFF.md`'s existing rule) — someone can revoke any of these mid-session. OEM battery-optimization whitelisting (Samsung/Xiaomi/etc.) isn't enforceable in code; give it a plain-language setup step, not a permission check.

### Copy voice, for the new strings needed above
Match what's already shipped (`ShanivarApp.kt`'s `FocusScreen`/`RhythmScreen`): direct, warm, no jargon, present tense, honest about limits. E.g. today's real placeholder line "App lock is not connected in this build" should become something like "Protected until 6:40 PM · Instagram, TikTok" when active, or "Choose what to protect yourself from, and for how long" as the picker's heading — not "Configure app blocking settings."

## Definition of done for this handoff
Mirrors `ai_org/product_owner_first_apk.md`'s v0 bar: a real focus session actually blocks a chosen app with a real overlay, a real alarm actually fires and requires a real QR/photo+motion dismissal, and permission grants are guided rather than assumed. Pricing/plan UI is explicitly out of scope until the founder defines one (a 402 just means "show an upgrade prompt," not "invent a price").

## What's already implemented and ready to call

`DisciplineEngine` (in `DailyRhythm.kt`'s `DisciplineGateway` interface — `status()`, `startFocus(minutes, packageNames)`, `releaseFocus()`) is a real implementation now, not a proposal. It:
- Refuses to claim `ACTIVE`/`OFF` without both `PACKAGE_USAGE_STATS` and overlay permission actually granted — reports `NEEDS_PERMISSION` otherwise, checked locally before ever touching the network.
- Calls the live `/api/discipline/*` backend routes for focus-session state, so `status()` reflects the server's view, not just a local flag.
- On `releaseFocus()` failure, does **not** stop the local guard — the UI must keep the session screen open and let the user retry, per the existing `NATIVE_HANDOFF.md` rule.

Two alarm dismissal methods are implemented end-to-end: `QrDismissalActivity` (CameraX + ML Kit barcode scanning) and `PhotoMotionDismissalActivity` (CameraX capture + accelerometer motion-threshold check). Both record their outcome via the backend (fire-and-forget, never gating local dismissal on that call succeeding).

## What Codex needs to build around this

1. **Onboarding/permission-grant flow** — this is the single biggest UX lift, because none of these are normal runtime-dialog permissions:
   - `PACKAGE_USAGE_STATS`: granted via `Settings.ACTION_USAGE_ACCESS_SETTINGS` deep link, not a dialog. The user has to find Shanivar in a system list and toggle it — needs clear, illustrated in-app instructions before sending them there.
   - Overlay (`SYSTEM_ALERT_WINDOW`): `Settings.canDrawOverlays()` check + `ACTION_MANAGE_OVERLAY_PERMISSION` deep link, same "leaves the app, comes back" pattern.
   - Exact alarm (`SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`) and camera: more standard grant flows, but camera has no in-app request UI yet (the dismissal activities assume it's already granted — Codex needs to add the actual `ActivityResultContracts.RequestPermission()` call before launching either dismissal activity).
   - OEM battery-optimization whitelisting (Samsung/Xiaomi/etc.): not enforceable in code at all, needs an explicit setup step with OEM-specific guidance copy, following the pattern alarm apps like Alarmy use. `NATIVE_HANDOFF.md`'s existing rule applies: "Returning from Android settings must re-read permission status."
2. **Settings screens**: dismissal-method selection (`qr` vs `photo_motion`), block-list configuration (v1 default is Instagram-only, `DisciplineTargets.DEFAULT_BLOCKED_PACKAGES` is extensible), alarm time/days/sound — all backed by the live `/api/discipline/preferences` and `/api/discipline/alarms` routes.
3. **QR registration UX**: v1 uses a fixed, app-generated QR value (see `QrRegistration` in `QrDismissal.kt`) — Codex needs a screen showing/printing this code for the user to place somewhere away from bed. Not a per-user-customizable arbitrary QR yet.
4. **Focus/Today screen wiring**: `startFocus`/`releaseFocus`/`status` calls, remaining-time display (uses `started_at + requested_duration_minutes`, deliberately not the backend's `expires_at` column — that includes a 30-minute staleness-cleanup grace period the user never asked for and would be misleading to show as "time remaining").
5. **Paid entitlement UI**: the backend gates focus-session start on `users.membership != 'free'` (a real check, wired to the same column `billing.js` already manages) but there is no real pricing/plan defined yet — a 402 response means "show an upgrade prompt," but what plan/price to show is an open founder decision, not something to invent in the UI.

## The one rule that overrides everything else

Nothing about the alarm or lock is truly non-dismissible — the research doc is explicit about this. A user can always force-stop the app, revoke a permission, or uninstall. Never let onboarding, marketing copy, or in-app language claim otherwise; "maximally inconvenient to bypass" is the accurate framing, not "impossible."

## Explicitly unverified — do not claim these work until tested on a real device

Detection latency and reliability of the `UsageStatsManager` polling loop; whether the block overlay can actually launch from the background on real OEM skins (Samsung/Xiaomi are known aggressive background-killers); real `AlarmManager`/full-screen-intent firing and lock-screen behavior; the actual permission-grant flows end-to-end; CameraX/ML Kit accuracy on real hardware; the motion-threshold constants (2.5 m/s² sustained for 1500ms) — these are a documented heuristic guess, not measured against real gait data, and will likely need retuning; behavior after an actual device reboot; the Play Console core-functionality (alarms) and special-use foreground-service declarations, which are administrative steps still outstanding and which gate whether the OS actually honors `USE_FULL_SCREEN_INTENT`/exact-alarm scheduling in production.

One more real limitation: dismissal-event recording depends on an in-memory session bridge since the app doesn't persist the auth token to disk — if the process was killed since the user's last login (a realistic overnight-alarm scenario), the record call silently no-ops. This never blocks the user's actual dismissal, but it does mean some dismissal analytics will have gaps until token persistence is added.
