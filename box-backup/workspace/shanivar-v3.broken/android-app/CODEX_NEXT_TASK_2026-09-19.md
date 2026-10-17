# Codex, next task — 2026-09-19

Founder request: the focus/app-lock tool needs recurring scheduling — **all day**, **a few days a
week**, or **custom times** — based on the person's own routine, plus something that reminds them
of their goal to keep them going. The engine and backend for this are done and tested; **the UI is
the gap, and it's yours.** This file is that handoff.

Read [decision 0010](../docs/decisions/0010-focus-schedule-when-then-commitment.md) first — it
records a real tension worth knowing about before you write copy: the founder's phrasing ("remind
them of their goal/deepest desire") sits right next to [decision 0009](../docs/decisions/0009-behavioral-return-loop.md),
approved one day earlier, which explicitly rules out an AI coach, generated motivational content,
and anything engagement-shaped for V1. 0010 resolves this by implementing the reminder as a
**when-then commitment** — the user's own words ("When ___, I will ___"), stored verbatim, shown
back to them unmodified. Never generate, rephrase, or dramatize this text. If a screen wants to
show *why* someone is protecting this time, use `intentionLabel` and the commitment fields exactly
as the user typed them — nothing else.

## What's already built (yours to call, not to reimplement)

**Backend** — `backend/src/routes/discipline.js`, full CRUD, self-healing migration, 9 new tests,
zero changes to anything focus-session already does:
- `POST /discipline/focus-schedules`, `GET /discipline/focus-schedules`,
  `PUT /discipline/focus-schedules/:id`, `DELETE /discipline/focus-schedules/:id`
- Fields: `mode` ('all_day'|'days'|'custom', display-only), `days_of_week` (0=Sunday..6=Saturday,
  same convention as alarms), `start_time`/`end_time` (HH:MM, `end_time` must be later than
  `start_time` — no overnight windows yet, see Known gaps), `block_package_list`,
  `intention_label`, `commitment_trigger`, `commitment_action`, `enabled`.
- "All day" is `days=[0,1,2,3,4,5,6]`, `start_time='00:00'`, `end_time='23:59'` — **the same shape
  as any other window**, not a separate code path. Your UI should present it as one of three preset
  buttons that pre-fill days/times, not three different forms.

**Kotlin client** — `RoutineApi.kt`: `listFocusSchedules()`, `createFocusSchedule(...)`,
`editFocusSchedule(...)`, `setFocusScheduleEnabled(id, enabled)`, `deleteFocusSchedule(id)`. Same
shape as the alarm methods you're already calling from `AlarmCard` — copy that pattern.

**Native scheduling engine** — new files, fully unit-tested (`FocusWindowRecurrenceTest`, 9 tests),
nothing here needs UI changes to work correctly, only UI to *create and edit* what it acts on:
- `FocusWindowRecurrence.kt` — pure functions, `nextBoundary(days, start, end, after)` →
  (instant, START or END) and `isActiveAt(...)`.
- `FocusScheduler.kt` — persists the one v1 schedule and arms the next native trigger. Mirrors
  `AlarmScheduler` on purpose.
- `FocusScheduleReceiver.kt` — fires at each boundary, calls the *existing*
  `DisciplineEngine.startFocus`/`releaseFocus` (the same calls your Start/End buttons make), then
  reschedules itself.
- `BootRescheduleReceiver.kt` — extended to also recompute the focus schedule on boot, **and**, if
  the device was off through a scheduled start, begins protection immediately for whatever remains
  of the window (deliberately the opposite of what it does for a missed alarm — see 0010's
  "Consequences" for why both are correct).

You do not need to touch AlarmManager, permissions plumbing, or the focus-session lifecycle for
any of this. You need a screen.

## What to build

**A schedule editor**, reachable from wherever the Focus card lives today. Suggested shape,
adapt to the current Home layout:

1. **Three preset buttons**: "All day", "A few days a week", "Custom times". Tapping one pre-fills
   days/start/end as described above and reveals only the controls that preset needs — "all day"
   needs no day picker at all if you want it maximally simple, though showing the (already-full)
   day chips greyed out is also fine.
2. **Day chips** (Su Mo Tu We Th Fr Sa) — reuse the exact chip pattern `AlarmCard` already has for
   `days_of_week`, same 0=Sunday convention. Hidden or disabled for "All day".
3. **Time pickers** for start/end — reuse the `TimePickerDialog` pattern already in `AlarmCard`.
   Hidden for "All day" and "A few days a week" (both fixed to 00:00–23:59).
4. **App picker** — reuse `KnownDistractingApps.ALL` exactly as `FocusCard` already does.
5. **The when-then pair** — two short text fields, plain labels, no icons implying AI:
   - "When..." → `commitment_trigger` (e.g. "When I reach for my phone")
   - "I will..." → `commitment_action` (e.g. "I will remember why I muted it")
   Both optional. Do not require them to save a schedule. Do not suggest example text that reads
   as generic motivational copy — if you want a placeholder, make it genuinely example-shaped
   ("e.g. When I feel bored") not prescriptive.
6. **Enable/disable, edit, delete** — same pattern as `AlarmCard`'s switch/Edit/Cancel row.
7. **Readiness** — scheduling requires `DisciplinePermissions.hasExactAlarmPermission()`, same gate
   the alarm already surfaces. No new permission is needed beyond what's already declared.
8. **Show the commitment where the chosen activity already appears** — per 0009's "the chosen
   activity should appear through the entire experience." Concretely: `BlockOverlayActivity`
   currently shows a generic "Your focus window is protected" line. When a schedule's commitment
   fields are non-blank, show them there instead — the person's own "When ___, I will ___",
   verbatim. Read them from wherever you're already caching schedule state (SharedPreferences is
   fine, matching how `MovementDismissalActivity` already reads alarm-side fields directly).

## Copy guidance (tone guide, `docs/strategy/shanivar-tone-and-values.md`)

Plain, direct, no dramatization. "What deserves your attention" fits; "What is your deepest desire"
does not — it's the exact register the tone guide's own replace-table warns against ("Unlock
hyperfocus and superior intelligence" → "Protect time for one task"). If you're ever unsure whether
a line of copy has drifted toward coaching, check it against that table before shipping it.

## Known gaps — real, not hidden

- **No overnight window.** `start_time` must be earlier than `end_time`; a 22:00–02:00 custom
  window is rejected by the backend (400) today. If this comes up in testing, it's a real product
  question (does "overnight" mean today's late window or split across two days?), not a quick fix.
- **One schedule at a time**, v1 scope, mirrors the one-alarm limit. Don't build multi-schedule UI
  yet — there's nowhere for it to persist to.
- **Cold-process gap**: if the app was force-killed or the phone rebooted with no cached session,
  a scheduled window can't start/end until the app is opened again (same limitation
  `MovementDismissalActivity`'s dismissal recording already has, not new). Nothing to fix in the UI
  for this — just don't be surprised by it in testing, and don't claim "always reliable" copy
  anywhere near this feature.

## Verification

```powershell
cd backend && npm test          # 305/305 passing as of this handoff
cd android-app
./gradlew.bat testDebugUnitTest lintDebug assembleDebug "-PshanivarApiUrl=https://hybridmonk-api.onrender.com/api"
```

92 Android unit tests pass as of this handoff, including the new `FocusWindowRecurrenceTest` and
`RoutineApiTest` coverage. Add tests for whatever UI state machine you build, same standard as
`AlarmCard`'s.

Announce before touching `FocusScheduler.kt`, `FocusScheduleReceiver.kt`,
`FocusWindowRecurrence.kt`, or `BootRescheduleReceiver.kt` — same shared-file convention as the
alarm surface (CLAUDE.md §5). Everything else here is new-file or additive; nothing should conflict
with your in-flight V1Home/FocusCard/AlarmCard work.
