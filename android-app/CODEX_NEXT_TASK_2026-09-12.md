# Codex, next task — 2026-09-12

The MVP is connected and the backend is deployed. `https://hybridmonk-api.onrender.com` is live
running the movement contract, and the app now points at it — testers need only internet, no shared
Wi-Fi. Verified in production: movement-first defaults, movement cue save, alarm creation carrying
the contract, movement dismissal, accessible dismissal, fire telemetry, focus lifecycle. 7/7.

Good work on the wiring. Three things remain before this can go to physical testers, in priority
order. The first one means the alarm currently cannot appear at all on a modern phone.

Everything here is governed by [ENFORCEMENT_STANCE_2026-09-12.md](ENFORCEMENT_STANCE_2026-09-12.md).

---

## 1. Request POST_NOTIFICATIONS at runtime — hard blocker

`POST_NOTIFICATIONS` is declared in the manifest and **never requested anywhere in Kotlin**
(verified by grep across `app/src/main`). With `targetSdk 35`, Android 13+ denies it by default.

The consequence is worse than a missing notification: `AlarmFireReceiver` delivers the alarm through
`manager.notify()` with a full-screen intent. Denied notifications make that call a no-op, so
**`AlarmActivity` never launches and the alarm never appears** — silently, with no error, on every
Android 13+ device. Every alarm fix landed this week is downstream of this one permission.

- Request it with `ActivityResultContracts.RequestPermission()` during onboarding, before the first
  alarm is ever scheduled, with a plain-language reason shown first.
- Re-check on resume; a user can revoke it later.
- If it is denied, the Rhythm screen must not display an alarm as scheduled. Per the truth-state rule
  the wireframes already set out, say plainly that the alarm cannot ring until notifications are
  allowed, and offer the settings deep link. Never show "Alarm confirmed" over a permission that
  would prevent it firing.
- Also harden the `context.startActivity()` fallback at `AlarmFireReceiver.kt:53`: it is an
  unguarded background activity launch, restricted since Android 10 and tightened in 14. Wrap it,
  and treat failure as a recorded outcome rather than an exception.

## 2. Report every alarm that fires — `POST /api/telemetry/alarm-fired`

Live in production and documented in
[CLAUDE_DISCIPLINE_ENGINE_NATIVE_CONTRACT.md](CLAUDE_DISCIPLINE_ENGINE_NATIVE_CONTRACT.md). Nothing
calls it yet.

Call it from `AlarmFireReceiver` the moment an alarm reaches the user. Fire-and-forget — never gate
the ringing UI on it, exactly like dismissal recording.

Send `device_manufacturer`, `device_model`, `android_sdk` from `android.os.Build`, the
`delivery_path` that actually worked (`full_screen_intent` vs `notification_only` — send `unknown`
rather than guessing), and a `client_event_id` UUID generated once per alarm occurrence and reused
on retry.

Why it matters: `alarm_dismissal_events` only records alarms that succeeded. An alarm the OS killed
writes nothing, so "Xiaomi silently dropped it" and "this person stopped using the app" are the same
shape in the data. The physical-device cohort exists precisely to find where alarms die, and without
this it cannot. The `client_event_id` is what stops an offline device's late retry from being counted
as a second successful alarm and inflating the number.

## 3. Turn the block screen into a door, not a wall

`BlockOverlayActivity` currently makes "Back to Shanivar" the primary `Button` and "Reflect with
Shanivar" a secondary `OutlinedButton`. Invert that.

Someone hitting the lock is already holding the phone and already wants something. Spending that
moment on a refusal gets nothing back; the founder's direction is to convert it.

- **Primary action: a short practice.** Three to five minutes — the thing they actually opened the
  phone hoping to feel. Route into the existing practice player.
- **Secondary: Reflect with Shanivar.** Already wired via `ReflectionNavigation.fromBlockedSurface()`.
- **Blog: not yet.** Content doesn't exist. A dead link at the moment of peak frustration is worse
  than one fewer option. Leave the seam, ship the door when there's a room behind it.
- Rotate what's offered so the same screen at the same moment doesn't become wallpaper.
- Keep the honest line about detection latency — a brief flash of the blocked app is expected.
- No snooze, no "just five minutes", no bypass affordance. Ending a session early stays a deliberate
  act inside Shanivar.

Copy holds the Hybrid Monk register: direct, warm, never smug, never scolding. They asked us to hold
this line for them; the screen should sound like it remembers that.

---

## Not asking you to build
The accessible/non-walking alternative — you already did it (`accessibleMovement`,
`MovementDismissalActivity.onAccessible`). Good; the stance requires exactly that, so nobody is ever
trapped by a sensor their phone doesn't have.

## Note on the alarm files
I changed `AlarmActivity`, `AlarmRinger`, `AlarmForegroundService`, `ShanivarApi` and the manifest
for the audio, escalation, back/home blocking and the watchdog. Your movement rewrite and my changes
merged cleanly, but we are both in these files — shout before large refactors of the alarm surface
and I'll do the same.

## Still unowned, and it blocks the cohort
Nothing has run on physical Android hardware. Not once. The eight-check protocol is in the stance
document; every item needs a real phone, and neither of us has one.
