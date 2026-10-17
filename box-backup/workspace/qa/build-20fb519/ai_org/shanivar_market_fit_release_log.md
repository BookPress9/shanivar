# Shanivar market-fit release learning log

Initialized 12 September 2026. Owner: [Market Fit Release Steward](agents/market_fit_release_steward.md).

## Current strategic thesis

Latest founder revision: [0007](../docs/decisions/0007-v1-alarms-and-focus.md) restores alarms and explicitly includes focus modes in v1. Retain the optional awareness loop and defer the remaining feature families. Marketing supplied [original copy and hypotheses](../marketing/shanivar/v1-product-direction-handoff.md); [Alarmy/one sec research](../research/shanivar-v1-alarm-focus-patterns.md) informs bounded patterns, not cloned screens or outcome claims. Owner: Claude for implementation, product/marketing for pilot learning. All-age intent is preserved with explicit readiness gates. Earlier alarm-deferral recommendations below are historical.

PM decision, 13 September: incorporate the [independent review](product_manager_mvp_review_2026-09-13.md) into the launch scope and Claude task. Add an optional pre-account pause without blocking claims, editable activity presets, helper handback and discoverable optional Shani explanation. Prioritize family acquisition learning after the two-path shakedown; preserve young self-use and separate denominators using the [pilot worksheet](shanivar_mvp_pilot_worksheet.md). Defer auth architecture, reminders and scheduling until observed barriers justify them. Whole-app coverage and voluntary initiation remain open fit questions. Owner: product manager; implementation: Claude task; evidence: founder record and source review only, no pilot results.

Current launch override, 13 September: both family introductions (worried adult children and independently willing parents) and younger self-use belong in the pilot. Lord Shani is an optional spiritual connection, not proof of adoption or authority over the user. The first build uses one shared immediate focus/notice/review loop, with metacognition expressed as planning and examining what helped. The [MVP scope](../android-app/INITIAL_MVP_SCOPE_2026-09-13.md), [Claude task](../android-app/CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md) and [backlog](../docs/strategy/shanivar-launch-backlog.md) control initial sequencing over the broader hypotheses below. Task preparation is not evidence that implementation is complete.

The founder's latest framing is not restoration to a former self. Shanivar should help someone get out of a mental loop into presence and, in the founder's spiritual language, live with cosmic flow rather than remain stuck in maya. Preserve that spiritual intent while testing a concrete sequence: **notice the loop, make present sensory contact, choose an optional grounded action, leave the app, and later reflect if useful.** Cosmic flow is an interpretive spiritual framing, not an externally measurable alignment score. This is a product hypothesis and positioning direction, not a demonstrated psychological result.

Candidate job: **When I catch myself escaping into my phone during a transition, help me notice what I need and choose one next action that fits who I am becoming.** The founder confirms **India** as the market focus and the existing audience: adult Gen Z, Millennials, parents and older adults. Within each, recruit willing participants who can identify an unwanted mental loop and an attempted workaround; a phone loop is the first instrumentable situation, not the whole spiritual purpose or a replacement audience. Situation-specific demand, language needs, willingness to use astrology and willingness to pay still require evidence.

## Founder answers and research recommendations

**Founder intent:** Shared consciousness is a spiritual conviction underlying Shanivar. Invite people who are willing and curious about their own routines and purpose rather than trying to persuade everyone. Users should discover through their own experience, not have the founder's worldview imposed on them. Presence includes comfort with boredom and an urge without immediately doomscrolling. Success can eventually mean little or no app use once a user's goals and desired focus are achieved. Astrology is intended to be accurate guidance, not an acquisition hook. Meditation before insight is a founder proposal.

**Steward recommendation, not yet validated:** Start with one recurring loop in a willing India-based cohort drawn from the existing adult audiences. Offer a brief optional sensory observation or meditation, then an open question and a chosen action. Keep metaphysical interpretation open to disagreement. Test the proposed ordering before making meditation a universal requirement. Separate verifiable astronomical calculation from interpretive accuracy and from practical usefulness; none establishes the other.

**Success definition to investigate:** The person can notice boredom or an urge, remain present if they choose, and act toward their own goal with progressively less prompting. App independence is a candidate outcome, not a promise or a reason to remove useful support prematurely.

## External market context supplied by the research lead

The root research synthesis identifies existing adjacent offers: one sec's intentional opening friction; Opal's offline actions and spiritual content in iOS block screens; The Pattern's charts and conversation; Waking Up's noticing and life-transition practices; Alarmy's step missions; and Rosebud's reflection memory. The subsequent India review found a much closer mechanism competitor: MantraLock blocks distracting apps until a Gita and mantra practice is completed. Level SuperMind overlaps across meditation, focus, astrology and AI personalization; the global bebored app overlaps across boredom practice, doomscrolling and AI reflection on loops. These findings undermine an unoccupied-category claim. The opportunity to test is a coherent, personally relevant loop-to-presence-to-action-to-independence experience across the existing audience, not possession of any one component. The completed [broader report](../research/shanivar-product-market-fit.md) and [India market-fit report](../research/shanivar-india-market-fit.md) provide source links, qualifications and a validation sequence. These are external market findings, not Shanivar outcome evidence.

## Evidence ledger

| Evidence | What it supports | What it does not establish |
| --- | --- | --- |
| [Product direction](../android-app/PRODUCT_DIRECTION.md), read 12 September | Intended attention, routine, chart, reflection and practice experience; physical-device acceptance requirements | PMF, reliable behavior across phones, or user outcomes |
| [Current app source](../android-app/app/src/main/java/com/shanivar/nativeapp/ShanivarApp.kt) | Focus calls `gateway.startFocus`; movement alarm calls native scheduling; local practice explicitly does not update account streak | Hardware reliability, real-world attention reclaimed, or practice consistency |
| [Reflection UI source](../android-app/app/src/main/java/com/shanivar/nativeapp/ReflectionScreen.kt) | AI/privacy disclosure, answer-gated continuation, response count/limit, explicit session exit, completion and continuation controls | Felt relevance, insight quality, reduced scrolling, or durable agency |
| [Chart implementation](../backend/src/utils/astrology.js), [reflection generation](../backend/src/utils/reflectionGeneration.js) | An ephemeris-based calculation implementation and provenance fields exist | Independent numerical verification in this pass or validity of astrological psychological interpretation |
| [Audience direction](../marketing/shanivar/audience-direction.md) | Founder intent to serve multiple adult life stages and allow disagreement with interpretation | Demand from all those audiences or one shared acquisition strategy |
| [Earlier tester brainstorm](release_brainstorm_20_testers_2026-09-12.md) | Useful risk hypotheses and proposed staged cohort/withdrawal design | Current availability: some implementation claims are already superseded by current source |

No cohort outcome dataset, attributable retention analysis, paid conversion evidence or completed physical-device outcome study was reviewed for this initialization. Do not read this as proof none exists elsewhere. Existing records of technical tests or account counts are not product-market-fit evidence. No app changes or tests were performed for this documentation task.

## Initial feature opportunity and risk log

All behavioral mechanisms below are hypotheses. The order is a learning sequence, not a committed roadmap.

| ID / priority | Opportunity and current status | User job / proposed mechanism | Smallest discriminating test | Primary signal and countermetric | Evidence needed before expansion |
| --- | --- | --- | --- | --- | --- |
| MF-01 / first | Transition-specific pause; focus/routine primitives exist, a complete noticing-to-action loop is not established | Interrupt one unwanted app-open moment and make a chosen action available while intention is salient | In one chosen recurring window, compare existing friction with the same friction plus one optional question/action; change only the prompt | Voluntary chosen action reported soon after the moment, denominated by eligible moments; track bypass, prompt irritation and technical delivery separately | Repeated usefulness beyond the first few days and credible examples of changed action |
| MF-02 / first | Real-world exit from reflection; finite session UI exists but also offers continuation | Turn insight into a small action instead of replacing one feed with another | Change only the completed-session primary action to a clear optional off-app action | Reported action after reflection; track total session time, repeated same-day sessions and unwanted pressure | Users can describe value outside the app; shorter app time can count as success |
| MF-03 / next | Teacher that accepts correction; response capture exists, personalization quality unverified | Let users develop judgment rather than adopt a model's account of their identity | Change only the explicit invitation to disagree with an observation | Whether disagreements lead to relevant next guidance; track preachiness and unsupported certainty in reviewed outputs | Evidence of useful correction, not merely more responses |
| MF-04 / comparator | Optional astrology as meaning/relevance layer; calculation source exists | Familiar symbolic language may invite reflection for some users and alienate others | For consenting users who accept either framing, compare matched chart-framed and chart-free prompts with the action held constant | Voluntary action and perceived relevance by predeclared astrology preference; track trust loss and deterministic interpretation | Incremental benefit beyond generic specificity; no claims that charts discover true identity |
| MF-05 / reliability gate | Movement alarm; current source wires scheduling and movement configuration | Make a user-chosen physical transition easier at waking | First verify eligible alarm delivery/dismissal on physical phones; only then compare a single post-dismissal action cue | Successful alarm delivery as technical denominator, then chosen post-wake action; track bypass and returning to the unwanted app | Hardware/OEM evidence and post-dismissal behavior; steps are not attention reclaimed |
| MF-06 / later | Change-aware reflection; broad life-stage editorial intent exists, a tested adaptive flow is not established | Reconsider routines when a job, home or role changes without being told to return to an old self | Interview one transition segment about recent concrete episodes before building; then test one context-specific prompt | Repeated episodes, costly workarounds and voluntary reuse; track overpersonalization and burdensome questioning | One segment with repeated need and a viable acquisition path |
| MF-07 / later | Shared presence across generations; editorial concept exists | Offer an invitation to a shared moment without making a parent a person to fix | Test one consent-based invitation as a concept, no monitoring or family dashboard | Both participants independently report welcome/usefulness; track obligation, guilt and privacy discomfort | Evidence from both participants; younger adult enthusiasm alone is insufficient |
| MF-08 / later | Flexible progress after a missed day; local practice/streak mismatch is visible | Make restarting easy without turning spirituality into performance | After reliable completion accounting, change only lapse-recovery copy/interaction | Voluntary restart after a lapse; track guilt and mechanically completed sessions | Reliable event definitions and non-shaming repeat use before gamification expansion |

## Proposed first release learning brief

**Audience/moment:** Willing adults in India, within the existing Gen Z, Millennial, parent and older-adult audience, who can identify one recurring unwanted scroll episode around waking or another daily transition. Recruit by the behavior, curiosity and prior workaround; device mix matters separately for technical reliability. Use the six-person shakedown then broader cohort only as a practical exploratory sequence, not as a statistical efficacy sample. Begin with one loop per participant; do not simultaneously launch a general life-transformation program.

**Hypothesis:** Adding one short, optional noticing question and a user-chosen next action to an already working interruption increases intentional follow-through at that moment.

**One variable:** Presence of the noticing/action prompt. Keep restriction rules, timing and chart treatment constant. If delivery fails, resolve reliability before interpreting the behavioral experiment.

**Primary outcome:** Fraction of eligible transition moments followed by a voluntarily reported chosen action within a predefined observation window. Record missing responses separately; do not silently count respondents as the whole denominator or claim self-report is observation. Distinguish technical eligibility, exposure, answer, claimed action and repeat use.

**Countermetrics:** Prompt irritation, bypass/disable behavior, increased time inside Shanivar, perceived judgment and disclosure discomfort. Specifically check scroller dependency: repeatedly opening Shanivar to avoid boredom, feeling unable to choose without its guidance, or substituting its messages for the unwanted feed. Treat these as hypotheses to ask about, not diagnoses inferred from session counts. Collect a few concrete follow-up accounts of what happened next, including the most recent failure.

**Decision rule:** Set the observation period and minimum useful effect after baseline feasibility is known and before intervention results are viewed. Continue only if repeat benefit is credible and countermetrics acceptable. Tiny uncontrolled cohorts support design decisions, not causal effect sizes. If reflection completes but the unwanted behavior resumes unchanged, revise the action bridge before adding content volume.

**Retention interpretation:** Track returns at the relevant recurring moment, not just daily opens. Continued support may remain useful, but indefinite engagement is not the founder's ultimate aim. Do not promise permanent transformation without evidence.

## Independence follow-up after the first-loop pilot

This is a subsequent recommended learning stage, not another variable in the first experiment. Once useful action and basic reliability are established, invite participants to reduce prompts in agreed steps while keeping support available. Compare their own chosen-goal follow-through and experiences of boredom/urges before and during reduced prompting. Use a pre-agreed follow-up window and record how many participants were eligible, opted in, reduced prompts and responded.

Classify departure only with evidence:

- **User-confirmed independence:** The participant reports meeting the chosen goal and sustaining the relevant behavior with less or no app support at follow-up. Label this self-reported and provisional; app inactivity alone does not qualify.
- **Useful ongoing support:** The participant chooses continued help and reports value; distinguish this from the independence outcome without treating it as failure.
- **Unsuccessful departure:** The participant reports low value, unwanted dependency, friction or technical problems.
- **Unknown departure:** No reliable follow-up or reason; retain as unknown rather than successful graduation or confirmed dissatisfaction.

Candidate primary outcome for this later stage: the fraction of opted-in participants reporting sustained chosen-goal behavior with reduced prompts at the pre-agreed follow-up. Publish nonresponse and the all-eligible denominator alongside it. Ask whether the app helped them make their own judgment rather than merely comply. Qualitative accounts can reveal improvement or substitution that app telemetry cannot determine.

## Next evidence to attach

- Completed [external market and behavioral-research synthesis](../research/shanivar-product-market-fit.md) is attached; future updates should record which new evidence changes a release decision.
- Current physical-device verification record and telemetry definitions from release owners.
- Baseline interviews with actual failed workarounds and a cohort record including negative cases.
- After demonstrated use, a concrete continuation/payment choice; stated enthusiasm alone is not demand.

## Update history

| Date | Change | Evidence level | Release implication |
| --- | --- | --- | --- |
| 2026-09-12 | Initialized from founder thesis and current repository inspection | Intent and implemented source only | Test one noticing-to-action loop before broad feature expansion; await market synthesis and user evidence |
| 2026-09-12 | Incorporated founder answers: India focus, willing participation, shared-consciousness conviction, own experience, boredom tolerance, eventual app independence, astrology accuracy intent and meditation-before-insight proposal | Founder intent; experimental recommendations remain unvalidated | Keep one first-loop pilot; evaluate reduced prompts subsequently; monitor scroller dependency and classify departure through follow-up |
| 2026-09-12 | Incorporated completed market research and clarified presence/maya purpose | External research plus strategic hypotheses; no Shanivar outcome validation | Test incremental value beyond existing interruption tools; separate interesting reflection from repeated useful action |
| 2026-09-13 | Added India competitor audit and complete founder-answer record | Official competitor pages/store listings plus founder direction; exact-market absence and Shanivar outcomes remain unproven | Do not claim first or competition-free; test the loop-to-action-to-independence sequence against simpler and spiritual-blocking substitutes |
