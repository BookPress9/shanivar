# 0008 — Alarm recurrence is recomputed after every fire, not pre-generated

- **Date:** 2026-09-18
- **Status:** Accepted

## Context

`AlarmScheduler` (Android) previously persisted exactly one `AlarmManager` trigger instant. Nothing
ever recomputed a next one after it fired, so a "repeat on Mon/Wed/Fri" alarm silently stopped
repeating after its first ring - the MVP streamline task's mandatory alarm work package exists
largely because of this gap.

`AlarmManager` can only ever hold one pending trigger per `PendingIntent`. Making a `days_of_week`
selection actually recur requires *something* to decide, after each occurrence, what the next one
is.

## Options

1. **Recompute on demand.** Persist the rule (time + selected days); after every dismissal, every
   hard-cap timeout, and every boot, call a pure function (`AlarmRecurrence.nextOccurrence`) with
   "now" and re-arm exactly one new trigger.
2. **Pre-generate a window of future occurrences** (e.g., the next 90 days) and schedule them as a
   batch, similar to how some calendar/recurrence engines work.
3. **Move recurrence server-side** - have the backend compute the next occurrence and push it to
   the device (via a data message or poll), rather than the device deciding for itself.

## Decision

Option 1.

## Consequences

- **Correctness depends on every exit path actually calling it.** `AlarmActivity.dismissAlarm()`
  and its hard-cap callback both call `scheduler.advanceToNextOccurrence()`; a future dismissal
  path that forgets this call would silently stop the alarm's recurrence again, exactly like the
  original bug. There is no structural guarantee against this - a test alone (`AlarmRecurrenceTest`
  covers the pure math, not that every call site invokes it) does not catch a *missing* call.
- **Clock/timezone changes are handled for free.** Because recomputation always starts from
  "now" in the device's current zone, a boot after a timezone change or manual clock adjustment
  produces a correct next occurrence without special-casing - `BootRescheduleReceiver` needed no
  DST/timezone-specific logic.
- **A missed occurrence (device off through the trigger time) does not retroactively fire.**
  Recomputing forward from "now" finds the next *future* valid occurrence; it deliberately does not
  fire a stale alarm hours late. Whether a missed wake-up alarm should ever fire immediately on
  boot is a product decision this repo has explicitly declined to make by default (see
  `BootRescheduleReceiver`'s doc comment) - option 1 makes the "don't fire stale" choice the
  default, silently. If the founder wants the other answer later, that changes this function, not
  the storage model.
- Option 2 (pre-generation) was rejected: it multiplies `AlarmManager` bookkeeping and Doze exposure
  for no benefit at v1's "one alarm" scope, and produces its own class of bugs (a generated window
  running out, or drifting from a rule edited mid-window).
- Option 3 (server-computed) was rejected: it makes every recurrence advance dependent on network
  reachability, which is exactly backwards for a feature whose entire value proposition is working
  while asleep with a phone that may have no signal. The local device is the only party that can
  guarantee "the alarm rings" without a network round trip.

## Revisit when

- v1's one-alarm-per-user limit is lifted (multiple alarms) - the same recompute-on-demand pattern
  should extend per-alarm-id rather than needing a new model, but confirm that assumption before
  building it.
- A demand emerges for "catch up" behavior (fire a missed alarm on next boot) - that is a new
  product decision, not a natural extension of this one.
