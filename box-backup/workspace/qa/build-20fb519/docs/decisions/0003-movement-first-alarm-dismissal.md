# 0003 — Movement is the default alarm dismissal, with a mandatory accessible alternative

- **Date:** 2026-09-12
- **Status:** Accepted

## Context

An alarm that stops when you tap a button stops when you are still asleep. Sleep inertia defeats
intention: the hand reaches out and dismisses before the mind is involved. The dismissal mechanic
*is* the product — everything else in the alarm surface only exists to hold the user there until it
completes.

Four mechanics were built: `movement` (walk a step target, optionally to a named destination),
`qr` (scan a code placed across the room), `photo_motion` (photograph a scene), and `accessible`
(deliberate confirmation, no physical demand).

## Options

1. **QR as default** — the original default. Strongest proof of location.
2. **Movement as default.**
3. **No default; force a choice during onboarding.**

## Decision

Option 2. `movement` is the default for new users, with `movement_destination` and `step_target`
part of the alarm contract. QR and photo remain available.

`accessible` is **never** merely an option in a list — it is always reachable from the alarm screen
itself, at the moment of need.

## Consequences

- **QR requires setup the user must do the night before, awake and motivated** — printing or placing
  a code. The failure mode is silent: they skip it, and the alarm they cannot dismiss is the one
  they set for the morning they most needed it. Movement needs nothing but the phone.
- Movement depends on `TYPE_STEP_DETECTOR`, which some budget hardware does not expose, and on
  `ACTIVITY_RECOGNITION` being granted at runtime. Both can fail. **This is exactly why the
  accessible path is mandatory rather than a nicety** — the sensor's absence must never trap someone
  in a ringing alarm.
- It is defeatable by shaking the phone. Accepted: the goal is to make the lazy path harder than the
  intended one, not to be unfakeable. Someone willing to shake a phone 24 times is awake.
- Steps counted in bed still count. Also accepted — we do not have reliable indoor location, and
  inventing certainty we lack would produce false failures, which are far worse than false passes.
- Accessibility is not a special case here. Anyone who cannot walk on demand, or can't at 6am, gets
  a first-class path — not a degraded one.

## Revisit when

- Fire/dismissal telemetry (`alarm_fire_events`, `alarm_dismissal_events`) shows a meaningful share
  of users on hardware without a step detector, or a movement-dismissal failure rate that suggests
  people are being trapped rather than woken.
- The physical-device cohort reports that step counting is unreliable on a common OEM.
