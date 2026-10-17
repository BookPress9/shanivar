# 0010 — Recurring focus schedules, and a when-then commitment instead of a "deepest desire" prompt

- **Date:** 2026-09-19
- **Status:** Accepted

## Context

Focus/app-lock protection only ever started ad hoc: "protect this time, starting now, for N
minutes." There was no way to say "protect my work hours every weekday" or "keep this app off my
phone entirely" without manually starting a session and remembering to do it again tomorrow. The
founder asked for three scheduling presets - **all day**, **a few days a week**, **custom
times** - "based on customer desired routine," plus something that reminds the person of their
goal or "deepest desire" to keep them going.

That second half lands directly on ground [decision 0009](0009-behavioral-return-loop.md) had
already staked out one day earlier: the founder-approved V1 behavioral loop explicitly excludes
"an AI coach, content feed, social comparison, family surveillance, predictive targeting or
gamification," and specifies that plans "should be rendered as simple when-then commitments" with
"the chosen activity appear[ing] through the entire experience." An open-ended "what is your
deepest desire" prompt, answered back to the user later as a generated or dramatized reminder,
would be exactly the AI-coach pattern 0009 rules out - however well-intentioned the request behind
it. This decision exists to record that the request was fulfilled through 0009's own mechanism,
not around it.

## Decision

**Scheduling:** one unified shape - selected days, a start time, an end time - with three UI
presets over it:
- *All day*: days = all 7, start = 00:00, end = 23:59.
- *A few days a week*: user-selected days, start = 00:00, end = 23:59.
- *Custom times*: user-selected days and a user-chosen start/end.

`mode` is stored only so the client can redisplay which preset was picked; it is never validated
against or derived from the days/times server-side. v1 ships exactly one recurring schedule per
account, mirroring the existing one-alarm limit.

**The "reason to keep going":** a when-then commitment pair - `commitment_trigger` ("When ___")
and `commitment_action` ("I will ___") - typed by the user, stored verbatim, and shown back to
them unmodified wherever the chosen activity already appears (the schedule card, the block
screen). No text is generated, summarized, or reworded by the app. Both fields are optional.

## Reason

Implementation intentions ("when situation X arises, I will do Y") are literally the mechanism
0009 already cites as evidence-informed. Reusing the user's own words as the reminder satisfies
"remind them of their goal to keep them going" without inventing motivational copy, without a
model in the loop, and without drifting toward the coaching/engagement register the founder had
just ruled out for a different, adjacent reason. It also costs nothing extra to build: it's two
text fields on a table that already exists for a different purpose (the schedule), not a new
subsystem.

## Consequences

- Native scheduling needed a second engine (`FocusScheduler`/`FocusScheduleReceiver`), not a small
  extension of `AlarmScheduler`: a focus window has two boundaries (open, close) per active day,
  where an alarm has one. `FocusWindowRecurrence.nextBoundary` picks whichever is sooner by reusing
  `AlarmRecurrence.nextOccurrence` for each side - see that file's doc comment for why this
  composes correctly with no "am I currently inside the window?" special case.
- **Boot/clock-change catch-up is the opposite default from alarms, on purpose.**
  `BootRescheduleReceiver` (0008) deliberately does *not* retroactively fire a missed alarm - a
  single instant either happened or didn't. A focus window is a span: if the device was off
  through 9:00 and boots at 9:20 into a 9-5 window, staying unprotected until tomorrow is the wrong
  answer, so this path actively starts focus for whatever remains. Two structurally similar
  "what do we do about a missed recurrence" problems, two different correct answers - documented
  here so the asymmetry reads as a decision, not an inconsistency.
- `MAX_FOCUS_MINUTES` (backend) rose from 480 to 1440 so a single `/focus/start` call can express
  a full all-day window. This reuses the existing focus-session lifecycle entirely - the schedule
  is a new *persistence* surface, not a new session lifecycle.
- Inherits the same cold-process limitation `MovementDismissalActivity`'s dismissal recording
  already has: `FocusScheduleReceiver` and the boot-catch-up path both need `ShanivarSession.api`
  to already hold a session in-process. If the app was force-killed or the device rebooted with no
  process, a scheduled window cannot start or end until the app is opened again. Documented, not
  fixed here - the real fix (a persisted, refreshable session) is a pre-existing item, not one this
  feature introduces.
- No overnight-spanning custom window (e.g. 22:00-02:00) in v1 - `start_time` must be strictly
  before `end_time`. Flagged in the Codex handoff as a real, likely-requested gap.

## Revisit when

- Demand appears for more than one concurrent schedule (e.g., a stricter window on weekdays and a
  lighter one on weekends) - the single-schedule limit is a v1 scope choice, not a structural one;
  `FocusScheduler` would need to key its persisted rule by schedule id instead of assuming one.
- An overnight window is requested - `FocusWindowRecurrence` would need to treat `end_time <
  start_time` as "wraps past midnight" rather than rejecting it, which changes `isActiveAt`'s
  same-day assumption too.
- The commitment fields go unused in practice (pilot evidence, per 0009's own measurement stance) -
  that's a reason to simplify them away, not to make them smarter.
