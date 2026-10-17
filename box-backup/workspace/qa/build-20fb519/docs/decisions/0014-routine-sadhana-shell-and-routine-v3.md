# 0014 - Routine + Sadhana shell and Routine V3

- **Date:** 2026-09-30
- **Status:** **ACCEPTED - founder direction**
- **Implementation authorization:** Routine and the two-destination shell may be built. Sadhana progression remains a Phase 2 design decision.

## Decision

The founder-selected [Shanivar V3 wireframe](../../android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md) is the current product reference.

Shanivar has two primary destinations:

1. **Routine** - shape the day, configure the wake alarm and protect chosen time from selected apps.
2. **Sadhana** - practise until the skill increasingly survives without the app.

Dashboard is removed from the primary navigation. Returning launches open Routine unless an explicit deep link opens Sadhana or an alarm/focus recovery surface must take priority. Settings remains secondary rather than becoming another bottom destination.

Phase 1 implements the shell and Routine. Phase 2 defines Sadhana progression with the founder. Until Phase 2 is accepted, the current reviewed Steady experience remains the only implemented Sadhana content; provisional chapter labels and gamification are not a build mandate.

## Routine product contract

Routine owns:

- the person's stated usual wake and intended-sleep anchors;
- editable personal activities with name, time, duration and Every day, Weekly or Custom occurrence;
- an optional wake alarm using the existing alarm engine and safety invariants;
- a reusable, categorized selection of distracting apps;
- multiple protection windows across a week;
- Every day, Weekdays, Weekends and Custom week recurrence;
- overnight windows using a start-day convention;
- a readable daily timeline composed from actual saved state;
- truthful device capability and enforcement status;
- immediate local stop/recovery.

Personal activities are calendar entries and remain separate from device-backed alarms and protection rules. Quick-add labels are editable examples rather than fixed activity types. The final wireframe places the add-window control at the bottom right of App Protection and adds an Edit Routine flow for anchors and activities.

The routine may use *dinacharya* as attributed cultural context for structuring a day. The actual schedule follows the person's stated life. It does not calculate dosha, cortisol, circadian phase or an ideal biological timetable.

## Reliability and authority

Alarm and app protection are device actions. The local device state is authoritative for scheduling and recovery. A warm process, network connection or server response must not be required for a scheduled boundary or immediate stop. Account sync may mirror configuration later and must expose conflicts rather than silently replacing local protection.

A rule moves through explicit states: `DRAFT`, `SAVED_LOCAL`, `NEEDS_DEVICE_ACCESS`, `READY`, `ACTIVE`, `NEEDS_ATTENTION`, `OFF`. The interface may say `ACTIVE` only from engine-confirmed state. A saved form is not proof that protection is armed.

## Supersessions and preserved constraints

This decision supersedes:

- the three-tab `Dashboard / Routine / Sadhana` shell described in decision 0013;
- the navigation model in the Rhythm V2 wireframe;
- the current `Dashboard` default in the gated `ShanivarShell` scaffold.

It does **not** supersede:

- alarm escape and hard-cap invariants;
- immediate essential access and local Focus recovery;
- honest enforcement limitations;
- the ban on activity-based escalation into intensive practice;
- practice source, content-review, accessibility and physical-device gates;
- the principle that less reliance on Shanivar can be a successful outcome.

Decision 0013 remains the unresolved Sadhana discussion, with its navigation context replaced by this decision.

## Consequences

- Rename or replace the stale `THREE_TAB_NAV` flag; no current code or test should claim three destinations after the migration.
- `HomeState` and Dashboard UI are no longer launch/navigation requirements. Useful state-resolution logic may be adapted into the Routine timeline rather than deleted blindly.
- The current single-rule `FocusScheduler` cannot satisfy multiple windows and must be replaced or generalized before the V3 protection editor is called complete.
- Routine implementation uses feature boundaries and typed interfaces inside the current app module. A broad Gradle-module rewrite requires separate evidence; future optimization can target one feature without coupling its UI, persistence and device engines.
- The known delayed-boundary, cold-process, overnight and all-day-gap defects are Phase 1 blockers.
- The founder and team will design Sadhana progression separately. Routine work must not smuggle in ranks, XP, streak pressure or advanced-practice unlocks.

## Build and verification sources

- [Claude Routine V3 handoff](../../android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md)
- [Routine V3 test guide](../../android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md)
- [Clickable V3 wireframe](../../android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/index.html)
- [Enforcement stance](../../android-app/ENFORCEMENT_STANCE_2026-09-12.md)
