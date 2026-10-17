# Product manager decision review: Shanivar initial launch

Date: 13 September 2026. Status: product recommendation for incorporation into the scoped Claude task; no application implementation, runtime verification, audience interviews or campaign experiments were performed in this review.

Founder revision after this review: [v1 decision](../docs/decisions/0007-v1-alarms-and-focus.md) restores alarms and focus modes. This review is retained as historical advice; its alarm deferral and one-target scope no longer control implementation. The [current scope](../android-app/INITIAL_MVP_SCOPE_2026-09-13.md) and [marketing handoff](../marketing/shanivar/v1-product-direction-handoff.md) control v1.

## Decision

Keep one application and one short practice loop. Prioritize learning from worried adult children introducing independently willing parents, while retaining a young-adult self-use comparison. The founder named both audiences; sequencing research toward the family proposition must not silently remove younger users from the product.

The current scope is appropriately small but still assumes that a generic focus timer will solve the parent's problem. The next iteration needs fewer setup barriers and clearer evidence about the parent's own motivation, rather than additional contemplative features. Optimize for time to a useful, voluntarily repeated experience, not install count or screen count alone.

Offer: **Make a little time for what matters. Choose an activity, pause a distracting app, and notice what helps you stay with it.**

For the helper: **Invite your parent to try a short break from scrolling. They choose the activity and stay in control.** For a willing parent: **A short practice for time you want to spend differently.** For a young adult: **Protect time for one task. Notice what pulls you away.** Optional Shani framing connects practice to the participant's chosen faith; no faith is required to operate the app.

## Founder commitments and hypotheses

| Founder commitment to preserve | Product hypothesis to test |
| --- | --- |
| Worried adult children are an entry audience; parents participate themselves | A child's invitation results in the parent's own interest rather than reluctant compliance |
| Lord Shani offers a meaningful connection | A respectful optional explanation increases parent comprehension or willingness; age does not establish belief |
| Younger people want focus and awareness of thinking | The same short loop supports a useful study/work activity without a separate engine |
| Presence includes noticing an urge and allowing boredom | A bounded pause adds useful experience beyond app blocking alone |
| Simple, direct tone inspired by action and ownership | Plain language improves comprehension without shame or pressure |
| Eventual reduced dependence is success | Participants can transfer the practice outside the app; nonuse alone cannot establish this |

## MVP decisions

| Decision | Feature or change | Reason and smallest test | Dependency / sequence |
| --- | --- | --- | --- |
| KEEP | One immediate session, one supported distracting app, one chosen activity | Small enough to demonstrate end to end. Observe one real session per participant | Existing focus engine; reconcile permission, auth and service state |
| KEEP | One home, remembered plan, clear start/end | Repetition should avoid repeating setup. Ask participant to start a second session without coaching | Save plan against correct identity; restore truthful state |
| KEEP | Optional bounded local pause and one skippable review | Preserves presence and awareness-of-thinking thesis without a feed | Local pause cannot unlock or claim protection |
| ADD | Offer the existing proposed local pause before account creation and Android permissions | Gives a truthful taste before setup effort. Observe understanding and whether they elect protected time afterward | Reuse one pause surface; do not add a second timer engine or simulate a blocked app |
| ADD | Three or four plain activity presets with editable choice | Parents need their own reason for a session; study/work alone biases the product toward younger users. Examples: reading, time with family, one task, quiet time | Copy and plan UI; no personalization model |
| ADD | Short helper guide with an explicit handback to the parent | An introducer needs to know what to say and when to stop doing setup. Ask parent to describe the plan in their own words | Static guide; no helper account, sharing permissions or remote pairing |
| CHANGE | Make optional Shani explanation discoverable in the helper explanation and optional presentation control | Settings-only copy would barely test the founder's acquisition theory. Compare plain and Shani invitations with willing adults | Original reviewed copy; changeable choice; no new mandatory onboarding page |
| CHANGE | Treat complete Hindi core as a gate for Hindi-speaking parent testing | English-only young-user success cannot establish parent fit. Invite only participants who can understand the delivered language | Review visible flow, permission explanations, errors, overlay, review and large text; no translation backend |
| ADD | Manual, separate measurement of invitation, parent willingness, setup, technical delivery and reported benefit | Child interest is not parent activation; confirmed blocking is not meaningful life change | Small research sheet and minimal existing event patterns; no analytics platform |
| CUT | Required role questionnaire, birth intake, general feature tour, mandatory reflection and extra destination tabs | Each consumes attention before the first useful experience | Safe launch routing and migration of active old services |
| CUT | Intelligence ranks, consciousness scores, guaranteed hyperfocus, divine authority or punishment | These do not make the MVP easier to use and exceed established outcomes | Static tone pass; no model training needed |
| DEFER | OTP/passwordless auth redesign or local-first protected-session architecture | Could address parent friction, but changing identity/engine architecture now delays evidence | Promote only if observed account friction prevents independent use; no invented credentials |
| DEFER | Automatic reminders, repeating schedules and multiple app targets | First verify that an intentional protected window fits the use case | Promote one capability when repeated user episodes show timing or coverage friction |
| DEFER | Movement alarms, astrology, AI scroller, practice library, gamification | Large scope relative to evidence needed for this offer; existing implementation is not a launch argument | Preserve safety/data contracts and promote only against a concrete unmet need |
| DEFER | In-app WhatsApp integration, referral systems, family dashboards and payments | Recruitment can be tested manually before these exist | Draft reusable invitation text and use a verified install destination during an authorized pilot; no automatic outreach |
| DEFER | School-age track | Maintain the existing adult pilot boundary while preserving founder ambition | Explicit age-specific experience and account/research scope before recruiting minors |

The pre-account pause is an optional entry into the same local exercise already in scope, not an additional onboarding requirement. A person ready to start protected time should go straight to their plan. Label the preview plainly: **This pause does not block other apps.** Do not count it as focus activation.

## Most important risk in the current offering

A parent may scroll reactively throughout the day and never think to start a protected session. Conversely, a young adult may already have a bounded study task. One implementation can test both situations, but their success rates must remain separate. A failed parent test cannot be erased by pooling it with stronger young-adult results.

Whole-app blocking may also remove an activity the person wants alongside the unwanted feed. Ask which exact activity they want less of. The inspected gateway receives package names; it does not establish feed-only blocking. Never pitch blocking just Shorts, Reels or a feed while keeping the rest of that app available. Keep essential communication out of defaults and explain the selected app's actual scope before confirmation.

Code review found `OnboardingFlow.kt` still exposes LANGUAGE/ACCOUNT/BIRTH/CAPABILITIES/ARRIVAL and email/password account UI. `DisciplineEngine.kt` checks usage access and overlay permission and uses backend focus start/release. These are source observations, not measured usability. The implementation should use existing contracts first and record whether account, network or Android setup is the real abandonment point.

## Smallest learning sequence

1. **Internal build check:** one complete protected session on physical hardware, honest failure/release behavior, no hidden active deferred service, and review of both delivered languages. A pitch walkthrough is not a substitute for this gate.
2. **Six-participant shakedown:** keep the existing three willing parents introduced by adult children and three independent young adults. Count helpers separately. Observe first setup, then ask for a second attempt without coaching. Record all facilitator intervention; help is legitimate but must not be mistaken for independent usability.
3. **Family acquisition check:** after blockers are corrected, give a small set of adult introducers two truthful invitation drafts, plain and optional Shani framing, and ask them to choose or explain what fits. With permission, test invitations with parents themselves. Count invitations actually delivered, parents independently interested, eligible devices, installs, first protected sessions and returns. Tiny samples give reasons and failure modes, not a statistically established winner.
4. **Short voluntary follow-up:** ask whether the participant repeated, what they did with the protected time, whether they needed help, and whether the app introduced new frustration. Ask the parent directly when possible; the helper cannot validate the parent's experience on their behalf.
5. **One next release decision:** resolve the largest observed barrier before adding breadth. If language or auth prevents first use, fix that; if sessions work but nobody remembers to start, test a lightweight reminder before a scheduler; if participants only want feed-level control the engine cannot provide, revise the offer before expanding acquisition.

Suggested initial management gate, not an efficacy benchmark: do not expand beyond the shakedown while any participant is left with an unmanaged active service or misleading status. Seek at least two participants in each path who can independently repeat the core operation and describe whether it served their chosen activity. If that does not happen, collect the reason and change the experience rather than treating more installs as progress.

## Evidence to retain for each path

- Invitation delivered and participant's own reason for trying; language and optional framing chosen.
- Eligibility/device and setup completion, time, help required, account/network/permission failure category.
- Engine-confirmed session start, completion or deliberate release, distinct from pause-only use.
- Optional report of the chosen activity, helpful/unhelpful experience and willingness to repeat.
- Observed or voluntarily reported independent repeat; lack of response recorded as unknown.

No private activity report goes to helpers. No improvement in intelligence, metacognition, wellbeing or spiritual state is inferred from button presses. Future independence needs a direct voluntary account of useful transfer beyond Shanivar.

## Product manager recommendation to Claude

Implement the existing streamlined loop with the small additions above. Deliver readable English/Hindi for the corresponding pilot, a static helper guide, optional Shani framing visible enough to evaluate, a pre-account local pause entry, and truthful setup/status/release behavior. These reduce friction and test the founder's distinct family proposition while retaining the young-adult path.

Do not expand the engine or create a separate family application. Ship a verifiable demonstration of one chosen activity and learn whether parents voluntarily repeat it. Record what would justify the next feature before building it.

Reviewed sources: [launch scope](../android-app/INITIAL_MVP_SCOPE_2026-09-13.md), [Claude task](../android-app/CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md), [founder direction](../docs/strategy/shanivar-founder-direction.md), [audience direction](../marketing/shanivar/audience-direction.md), [backlog](../docs/strategy/shanivar-launch-backlog.md), [root working agreement](../CLAUDE.md), [delivery tracker](product_owner_first_apk.md), and the two source files named above. This review makes product judgments from the founder record and repository; it introduces no new market-size or competitor claims.
