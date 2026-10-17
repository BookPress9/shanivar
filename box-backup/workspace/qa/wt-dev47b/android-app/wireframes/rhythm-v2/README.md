# Shanivar V2 - Routine, Dashboard, Sadhana

> **Superseded product shell (30 September 2026):** the founder-selected [Shanivar V3 wireframe](../shanivar-v3-wireframe/shanivar-v3/README.md) removes Dashboard and makes Routine + Sadhana the primary navigation. Preserve this package as design history; do not implement its three-tab shell. Its research and safety boundaries remain useful unless V3 explicitly replaces them.

Prepared 30 September 2026. Status: **clickable product wireframe and implementation handoff**, not an approved native redesign or release. Open [index.html](index.html) directly in a browser. The prototype is offline except for outbound source links and keeps its temporary state in memory.

## Product model

The three bottom destinations each answer one question:

| Destination | User question | Owns |
| --- | --- | --- |
| **Routine** | How will I shape today? | User-chosen wake/sleep anchors, alarm setup, reusable app selection and focus window, day agenda |
| **Dashboard** | What matters next? | One current action, truthful alarm/focus status, recent practice and configuration gaps |
| **Sadhana** | How is my practice becoming my own? | Five optional learning chapters, practice player, source library, audit, challenges and independent mode |

Onboarding is three steps: promise, personal rhythm, optional setup. Alarm and app protection work independently. Permission requests occur only after a person chooses the relevant capability. Returning users land on Dashboard.

## Sadhana progression

The five chapters are **Arrive, Return, Understand, Make it yours, and Deepen**. They are chosen learning themes, not spiritual ranks. A practitioner may open any chapter, return to a basic anchor, or browse sources immediately.

Progress records observable or self-reported events:

- a timed practice was attempted or ended early;
- the participant optionally described it as useful, unchanged or uncomfortable;
- the participant reported practising without the app;
- a chapter or source was opened.

Elapsed time is never presented as attention quality. Shanivar does not infer awareness, readiness, diagnosis, body-clock state or metaphysical attainment. The most meaningful milestone is a comfortable practice performed without a prompt. Independent mode turns practice reminders off while leaving guidance available.

Gamification supports competence and agency through chapters, transparent milestones, hands-on challenges, editable history and recovery after gaps. There are no punitive streak resets, leaderboards, spiritual levels, loot currencies, shame notifications or rewards for intensity. Challenge completion does not unlock breath retention, fasting, sleep restriction, cold/heat exposure, supplements, kundalini, trauma excavation or altered-state practices.

## Routine contract

Routine is inspired by the idea of *dinacharya* while using the participant's chosen times. It must not claim to calculate dosha, cortisol, circadian phase or the biologically correct schedule. Shift work is a first-class choice. Suggested anchors must be editable and removable.

App-lock configuration is reusable but does not become active because a browser checkbox was selected. Native implementation must verify Android capability and show `Needs device access`, `Ready`, `Active`, `Needs attention`, or `Off` truthfully. Preserve essential apps and local recovery without waiting, quota, payment or practice. Fix and physically verify the scheduling concerns recorded in the [Deep Foundation report](../../../research/deep-foundation/SHANIVAR_DEEP_FOUNDATION_REPORT_V1.md#16-clock-and-focus-research) before promising scheduled protection.

Alarm implementation preserves the existing safety invariants, including an accessible non-physical dismissal and hard cap. The wireframe makes no sound and enforces nothing.

## Mental-health boundary

The supplied JKP Hinglish PDF uses a helpful `concept -> example -> apply` teaching rhythm. The wireframe adapts that pedagogy and selected questions. It does not adopt the PDF as a diagnostic authority. It never interprets intrusive thoughts, childhood experience, addiction, narcissism, demoralization or medication needs from product behavior.

Reflection is private by default and may be skipped. Do not ask a person to disclose trauma or diagnose another person. Persistent or severe distress belongs with appropriate qualified support, not another challenge. See [RESEARCH.md](RESEARCH.md) and the foundation [practice-safety policy](../../../research/deep-foundation/PRACTICE_SAFETY.md).

## Key screen behavior

| Screen | Required native behavior |
| --- | --- |
| Welcome / Rhythm / Setup | No account gate; times remain editable; alarm and protection optional and independent |
| Dashboard | One next action; live capability status; no wellness or readiness score |
| Routine | User-chosen agenda; setup controls; no silent activation or medical/circadian claims |
| Alarm | Reuse existing verified alarm engine and accessibility escape; wireframe copy is illustrative |
| App protection | Same-day window in this concept; explicit app selection; preserve essential access |
| Device access | Open system settings only after intent; recheck actual status on return |
| Sadhana | All chapters visible; current chapter is a choice, not a locked level |
| Chapter | `Understand -> example -> try`; one practical challenge and source access |
| Ready / Practice | External anchor equal to natural breath; stationary-setting gate; static marker; End always visible |
| Reflection | Optional negative/neutral reporting; discomfort stops progression prompts |
| Practice record | Separate app-observed elapsed time from self-report; edit/delete/export controls |
| Independent mode | Turn off practice reminders; allow a private practice with no proof requirement |
| Protocol library | Purpose, action, measurement and exact adaptation/source boundary |
| Essential access | Immediate local stop; no quota, delay, payment or practice requirement |

## Accessibility and privacy

Use the existing Compose theme primitives. Reference layout is 390 x 844 CSS pixels; native screens must scroll, wrap at 200% font scale and work with system bars and landscape. Use at least 48 dp native touch targets. Keep End reachable and labelled. Do not announce countdown changes each second. The attention marker is decorative and static. Respect reduced-motion preferences.

The audit stores the minimum useful event. Free-text journaling is not required. Exact retention, export, deletion, encryption and sync behavior need a privacy decision before implementation. The prototype has no account, analytics, persistence or backend calls.

## Implementation sequence

1. Validate the information architecture and three-tab naming with representative users, including shift workers and people who decline inward attention.
2. Repair and physically verify alarm/focus scheduling and local recovery before connecting Routine controls.
3. Implement the shell and truthful status model with fixture content; keep current tested safety code.
4. Add the existing reviewed Steady practices behind Sadhana. Do not treat the new protocol cards as release-approved content.
5. Define a versioned practice-event schema and privacy controls before persistence or sync.
6. Run content/provenance, mental-health, accessibility, large-text, screen-reader and physical-device reviews.
7. Test whether chapter cues produce more self-initiated practice and less reliance on reminders. A retention increase alone is insufficient.

## Review artifacts

- [Interactive prototype](index.html)
- [Screen collection](exports/index.html)
- [Source and adaptation register](RESEARCH.md)
- [Browser verification](QA.md)
- [Foundation report](../../../research/deep-foundation/SHANIVAR_DEEP_FOUNDATION_REPORT_V1.md)
- [Manual stance](../../../SHANIVAR_STANCE.md)

No app source, navigation or existing database contract is changed by this wireframe.
