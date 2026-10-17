# Shanivar V3 — Routine + Sadhana

Prepared 30 September 2026. Status: **founder-selected product reference**. The two-destination shell and Routine direction are approved for implementation. Sadhana progression remains provisional for the collaborative Phase 2 roadmap; this approval is not a public-release decision.

This supersedes the three-destination navigation and Routine treatment in [Rhythm V2](../../rhythm-v2/README.md). It does not supersede existing alarm safety, Focus recovery, Steady content-review gates, source governance or physical-device requirements.

Open `index.html` directly in a browser. The prototype is offline except for outbound source links and keeps temporary state in memory.

## What changed from V2

Founder feedback changes the product stance, not only the copy.

- **Dashboard is removed** from the primary product model. Shanivar now has two main places: **Routine** and **Sadhana**.
- The voice is direct, serious and action-oriented. It should feel like a coach that expects work, not generic wellness reassurance.
- Onboarding rhythm asks only:
  - `I usually wake around`
  - `I make room for sleep around`
- The prior schedule/shift selector is removed from onboarding.
- Routine explicitly treats personal growth as something worth scheduling and tracking, instead of leaving it to memory or mood.
- Routine now has an `Edit Routine` flow for wake/sleep anchors and personal activities. Activities support editable name, time, duration and Every day/Weekly/Custom occurrence, with quick starts for Gym, Read a book and Extend Sadhana.
- App protection now supports:
  - broader categorized app selection;
  - multiple protection windows in a 24-hour cycle;
  - a bottom-right `+` control to add windows;
  - Every day, Weekdays, Weekends and Custom week recurrence;
  - Monday–Sunday custom-day selection.
- Steady no longer asks the user to choose between room/contact/breath anchors. **Natural breathing is the foundation practice.**
- The Steady player uses a moving Saturn motif while explicitly stating that the motion does **not** pace the breath.
- Visual language deliberately alternates **light paper panels and black panels**. Light surfaces use dark text; dark surfaces use high-contrast light text. The previous dark-on-dark readability problem is removed.
- Sadhana progression is kept intentionally light so its soft gamification can be designed next rather than inheriting the previous system by default.

## Product model

| Destination | User question | Owns |
| --- | --- | --- |
| **Routine** | What am I actually making room for today? | Wake/sleep anchors, editable personal activities, alarm, focus protection windows, daily personal structure |
| **Sadhana** | What am I training until I can carry it without the app? | Foundation practice, learning path, practice record, independent mode, sources |

The first-run sequence is:

1. Welcome / stance
2. Wake + intended sleep anchors
3. Optional setup: wake alarm and app protection
4. Routine

## Voice contract

Shanivar should be:

- wild enough to wake the user up;
- real enough to avoid spiritual theatre;
- direct enough to create action;
- clear enough that the next step never needs interpretation;
- serious without becoming authoritarian or shame-based.

Preferred pattern:

`truth -> contradiction -> instruction -> action`

Example:

> You follow calendars for meetings, deadlines and other people. Then you expect your own growth to survive on memory and mood. That is backwards. Put it on the calendar. Start today.

Avoid generic lines that could belong to any wellness app, excessive reassurance, fake intimacy, shame, diagnosis or claims that Shanivar knows the user's inner truth.

## App protection contract

The browser wireframe simulates configuration only. A native implementation must distinguish:

- rule saved;
- required device access granted;
- protection ready;
- protection active;
- protection needs attention;
- protection off.

Multiple windows may cross midnight in a future native implementation. This wireframe stores start/end values without claiming actual enforcement.

Essential access and immediate local recovery remain mandatory.

## Steady foundation practice

Current foundation instruction:

1. User confirms they are stationary and in a suitable place.
2. Shanivar explains why natural breathing is used.
3. One-minute practice begins.
4. Saturn revolves as a visual attention object, **not a breathing pacer**.
5. Text guidance changes across the minute:
   - notice the breath already happening;
   - attention moved — return;
   - feel arrival and departure without control;
   - one more return.
6. The user can pause or end at any time.
7. Reflection is optional and may include discomfort.

## Sadhana progression

The current five chapter labels remain only as a temporary information architecture:

- Arrive
- Return
- Understand
- Make it yours
- Deepen

They are **not** ranks. The next design pass should define soft gamification, milestones, challenges, evidence and progression with the founder rather than treating this provisional structure as final.

## Accessibility and motion

- Primary mobile reference remains approximately 390 × 844 CSS pixels.
- Controls use large touch targets.
- Light surfaces use dark text and dark surfaces use light text.
- Reduced-motion preference stops Saturn animation.
- `End` remains available during practice.
- Native implementation still needs large-text, TalkBack/VoiceOver, landscape and physical-device review.

## Files

- `index.html` — browser shell
- `style.css` — visual system and responsive layout
- `app.js` — state, screens and interactions
- `RESEARCH.md` — research/adaptation boundaries
- `verify.mjs` — static checks plus optional Playwright browser checks when the repository dependency exists
- `QA.md` — verification summary for this package
- [Decision 0014](../../../../docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md) — accepted shell and Routine authority
- [Claude Routine build handoff](../../../CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md) — native implementation contract
- [Routine test guide](../../../ROUTINE_V3_TEST_GUIDE_2026-09-30.md) — automated and physical-device gates
- [Sadhana Phase 2 workshop](../../../../research/deep-foundation/SADHANA_ROADMAP_WORKSHOP_V1.md) — collaborative roadmap starting point
