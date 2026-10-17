# Shanivar: Customer Success alignment review (first release)

From: Shanivar Customer Success · To: Shanivar PM · 17 Oct 2026, 22:45 CEST (Europe/Berlin)
Inputs (all read-only): CS delegation brief, Measurement plan (WIAC), Product Thesis (Notion); DEV-66, DEV-76, DEV-85, DEV-86, DEV-87 (Jira, all Open / awaiting founder); market research (17 Oct); repo `shanivar-v3` @ 20fb519.
Anchor: **Protect. Become Aware. Enter Flow.** North Star = **WIAC**. Success = users need *less* intervention to act intentionally.

## 1) What must change?
- **WIAC must not count reflexive taps.** The research shows block screens wear off within weeks because people tap through on autopilot. Today a "Return / Not now" tap counts toward WIAC (`intervention_choice` ≠ stop). Count pause and practice as WIAC. Report Return as a separate number so habituation can't inflate the North Star. *(Measurement plan §1, DEV-65; conflict with research)*
- **"Choose a short practice" on the block overlay goes to `BreathingHost`, not to the Stage 1 sitting.** That breaks the no-breath-pacing rule (DEV-89 blocker), and the research says the bridge should be a one-minute *sitting*. Point it at Stage 1 (DEV-36) once DEV-89 clears.
- **Make the CS tickets match the event contract.** DEV-76 expects `practice_completed` and `next_action_taken`. DEV-87 expects `lapsed_message_shown/returned`. Neither exists in the DEV-65 taxonomy (`practice_ended` + outcome). DEV-85 cites "A3, F2" instead of DEV keys. Fix this so the brief's rule "every recommendation links to a ticket" holds.
- **Use the thesis segments (A distraction, B routine, C meditation, D self-inquiry), not the research personas (P1/P2/P3).** P1→A, P2→C. P3 (family) has no segment yet and stays a hypothesis.
- **Drop the research North Star ("protected-and-present hours").** It is minutes-based, which the thesis rules out. Keep "protection active ÷ scheduled minutes" only as a reliability metric.

## 2) What already exists?
- **Protect:** Routine V3 with up to 8 recurring and overnight protection windows. Honest rule states, with `ACTIVE` shown only when the engine confirms it (DEV-3). Wake alarm with movement dismissal plus an accessible path. Polling + overlay blocking, and the overlay says the brief flash is expected.
- **Intervention:** the overlay offers Return to my day, a one-minute pause (`LocalPauseScreen`) and a short practice. Nothing is recorded (DEV-69).
- **Become aware:** Sadhana Stage 1, a 60 s sitting with an optional self-check (reached / drifts / what pulled you away). It is local only and never scored (0018). "Practice without the app" record; guidance tapers when the user chooses; Living Orbit return never punishes missed days.
- **CS groundwork:** the WIAC definition and dedupe rule, a draft event taxonomy, the segment activation table (thesis §3), and the research benchmarks and at-risk playbook. **No analytics exist**: `SessionReview.kt` is local only.

## 3) What's missing?
- **Measurement:** the event layer (DEV-65), recording the overlay choice (DEV-69), event verification (DEV-83) and segment/goal capture at onboarding (DEV-73/DEV-50). Until these land, WIAC = unmeasurable.
- **Protection health check / OEM guide (Xiaomi, Samsung, Oppo/Vivo):** the repo has no battery-optimisation check (PowerManager is used only in `AlarmRinger`). This fits DEV-70/DEV-71. It is the research's #1 cause of silent failure.
- **Weekly attention summary with no score:** no ticket exists; nearest is DEV-77 (Later). Not in scope until WIAC has a baseline.
- **Pricing principles** (no card for a trial, one-tap cancel, never paywall alarm/safety): no ticket, no Finance epic (Q4), only H8. Billing was the #1 complaint in 12/16 apps, so write the principles down before willingness-to-pay interviews.
- **CS deliverables:** the assumption register (DEV-66), the practice→need→action table (DEV-76), and the interview guide + churn taxonomy (DEV-86). DEV-86 has no "protection didn't work on my phone" or "pricing" category, so failures will hide under "technical failure / other".

## 4) Dependencies and risks
- **Blocking chain:** DEV-65 → DEV-69 → DEV-83 before the closed test. DEV-7/DEV-71 device QA has never run. DEV-89 and DEV-34 content review are release blockers. DEV-8 credentials. Every CS ticket is "awaiting founder: do not build".
- **Small-n and window risk:** 12 testers × 14 days split across 4 segments gives about 3 per segment, so DEV-85 small-segment suppression will hide most cells. **W4 cannot be measured in a 14-day test**, and research D30 benchmarks (~4–5%) are context only (the plan says compare within users).
- **Interpretation risk:** fewer interventions per protected hour (H3) could mean awareness *or* silent OEM failure / disengagement. Always read it next to `protection_failure_detected` and interviews. Never present engagement as proof of awareness.
- **Privacy vs insight:** self-check answers and package names never leave the device, so CS can't see aggregate "phone urge" pulls (the research's Self-check → Routine link). That stays a local-only feature unless the founder decides otherwise.
- **Backend:** Supabase Free pauses after 1 week idle and has no backups (DEV-41). That could lose tester events mid-pilot.

## 5) What can ship in the first release?
- The events contract + overlay choice record + verification (DEV-65/69/83), with WIAC excluding Return taps (or reporting them separately).
- Overlay "short practice" → Stage 1 60 s sitting inside the lock, never unlocking (DEV-36/DEV-89).
- Failure surfacing + OEM repair steps on the `NEEDS_ATTENTION` card (DEV-70), validated on Xiaomi/Samsung in DEV-71.
- A minimal DEV-85 dashboard: activation per segment, time to first value, weekly WIAC, recovery, reliability. W4 is marked "not yet measurable".
- CS docs: DEV-66 register (seeded with H1–H8), the DEV-76 table, and the DEV-86 guide + taxonomy (add OEM/protection and pricing categories). DEV-49 in-app feedback.
- **Not in first release:** DEV-87 (Later; ship only the copy spec), the weekly summary, payments.

## 6) How will it be verified?
- **Events:** DEV-83 scripted runs. Each action in the DEV-84 E2E journey (goal → protect → intervene → act → reflect → recover) produces exactly one WIAC occasion; retries and rerenders don't double-count.
- **Reliability:** the DEV-7/DEV-71/DEV-75 device matrix including Xiaomi and Samsung: alarm on-time rate, protection active ÷ scheduled minutes, zero `ACTIVE` without engine confirmation.
- **Closed-test baseline (weeks 1–2):** WIAC median per user; % of interventions resolved with pause/practice vs Return; overlay → sitting take-up. If fewer than half of the people who reach the block screen ever try the sitting, the bridge hypothesis is weak. Also second-day practice return (H4) and recovery after a gap (H5).
- **Qualitative:** 5–8 interviews per cohort (active / inactive / returning) under DEV-86. Report behaviour and self-report separately, state the limits, and link each finding to a DEV ticket. First monthly report = DoD.

## Conflicts flagged (brief/tickets vs research)
1. Research North Star (minutes) vs WIAC (no minutes) → WIAC wins.
2. Research treats a "dismiss/Return" choice as effective, but also warns it wears off. WIAC currently counts it.
3. Research "one-minute sitting from block screen" vs the code path to the Breathing screen.
4. Research pull-mix / phone-urge metrics need self-check answers, which the plan never sends.
5. Research's day-30 benchmark vs a 14-day closed test and "no competitor comparison".
6. Research ticket IDs (SHAN-1…8) don't exist in Jira. Mapping: SHAN-1→DEV-65/69, SHAN-2→DEV-36/89, SHAN-3→DEV-70/71, SHAN-4/7 → no ticket.
7. Research suggests a gentle return note in week 1, but DEV-87 is "Later". DEV-74 covers in-app recovery only.

## Founder decisions needed
1. **WIAC rule:** count "Return / Not now" at the overlay, or count only pause/practice and report Return separately (habituation risk)?
2. **First-release scope:** reroute the overlay practice to the Stage 1 sitting (after DEV-89) and add the OEM health check to DEV-70; weekly summary and pricing principles stay out of the build.
3. **Measurement limits:** extend the closed test to ≥ 4 weeks for W4 (or accept W1-only), and confirm self-check answers and package names stay on the device (app category only).
