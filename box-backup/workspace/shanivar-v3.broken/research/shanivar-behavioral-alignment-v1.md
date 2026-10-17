# Shanivar V1 behavioral alignment

Date: 18 September 2026  
Status: product-planning recommendation for the locked Alarm + Focus MVP. This document does not expand the approved feature families or claim that Shanivar has produced behavioral outcomes.

## Product stance

**Shanivar helps a person turn a conscious decision into a repeatable action, notice the moment they drift, and return without shame.**

Discipline here means keeping a commitment chosen in a calmer moment. It does not mean obedience to the app, punishment, uninterrupted perfection or a score of someone's character. Presence means creating enough space between an urge and an action for choice to become possible.

The product loop is:

> **Choose → Prepare → Protect → Notice → Review → Adjust → Leave**

The reason to return is not to consume more Shanivar. A person returns to:

1. begin a focus session for something that matters;
2. prepare or adjust the next wake-up action;
3. inspect a small amount of evidence about what helped;
4. make the next attempt easier or more realistic.

The app should become less necessary as the behavior becomes easier outside it. Useful repeat use and growing independence can both be successful outcomes.

## Evidence foundations

| Psychological mechanism | What the evidence supports | Shanivar interpretation | Boundary |
| --- | --- | --- | --- |
| Implementation intentions | Specific if-then plans connect a foreseeable cue to a chosen response. A meta-analysis of 94 independent tests reported a medium-to-large aggregate effect on goal attainment, though effects vary by domain and later applied reviews often find smaller effects. [Gollwitzer & Sheeran](https://doi.org/10.1016/S0065-2601%2806%2938002-1), [MCII meta-analysis](https://pmc.ncbi.nlm.nih.gov/articles/PMC8149892/) | Turn an intention into: “When it is 7:00, I will stand and go toward the window,” or “When I begin studying, Instagram and YouTube stay paused for 25 minutes.” | Do not imply that making a plan guarantees action. |
| Autonomy-supportive motivation | Self-determination theory predicts better initiation and maintenance when behavior reflects personal choice and values rather than external pressure. Reviews of digital behavior-change interventions recommend supporting autonomy, competence and meaningful personal reasons. [Digital intervention review](https://pmc.ncbi.nlm.nih.gov/articles/PMC5999061/), [SDT/meaning pilot](https://pmc.ncbi.nlm.nih.gov/articles/PMC11234062/) | Users choose the activity, apps, duration, alarm action and wording. Helpers hand control back. The app describes consequences plainly and allows deliberate management inside Shanivar. | “Strict” must not become coercive, shaming or deceptive. |
| Friction at the decision point | A field experiment of the one sec intervention combined a short delay, deliberation prompt and option to abandon opening a target app; target-app use declined during the study. The authors also separated intervention components experimentally. [PNAS study](https://pmc.ncbi.nlm.nih.gov/articles/PMC9974409/) | The block surface interrupts automatic opening and recalls the user's chosen activity. The pause helps the person observe the urge, then returns them to device home. | Shanivar uses package-level blocking and a different Android mechanism. Do not import one sec's effect size as a Shanivar claim. |
| Urge tolerance | In a randomized study of 123 student smokers, brief urge-surfing instructions did not significantly reduce reported urges, but the mindfulness group smoked fewer cigarettes during seven-day follow-up. [Study abstract](https://pubmed.ncbi.nlm.nih.gov/20025372/) | The pause should say “notice the urge” rather than promise to remove it. Success is choosing a response while an urge may still be present. | Evidence from smoking cannot establish an effect on doomscrolling; test the mechanism in Shanivar's context. |
| Progress monitoring | A meta-analysis of experimental studies concluded that interventions increasing progress monitoring can support goal attainment. [Harkin et al.](https://pubmed.ncbi.nlm.nih.gov/26479070/) | Show factual, compact evidence: planned sessions, sessions begun, sessions completed or deliberately ended, and the user's optional report of whether the chosen activity happened. | Engine events are delivery evidence, not proof of attention, wellbeing or spiritual progress. |
| Repetition in a stable context | Real-world habit research found that automaticity developed through repeated behavior in a consistent context, with substantial variation between people; missing one opportunity did not materially derail the modeled formation process. [Lally et al.](https://onlinelibrary.wiley.com/doi/abs/10.1002/ejsp.674) | Encourage a repeatable cue and realistic action. Preserve the last focus plan and alarm rule so the useful behavior is easy to repeat. | Do not advertise a universal “21-day” or “66-day” transformation timeline. |
| Just-in-time support | JITAI research recommends intervening when a person is vulnerable or receptive and explicitly includes “provide nothing” to reduce burden and intrusion. [Design principles](https://pmc.ncbi.nlm.nih.gov/articles/PMC5364076/), [behavioral framework](https://pmc.ncbi.nlm.nih.gov/articles/PMC4732268/) | Intervene at chosen moments: alarm time, focus start, attempted opening of a protected app, and session end. Stay quiet otherwise. | V1 does not need predictive AI or passive inference to apply this principle. |
| Returning after lapses | Research on goal lapses suggests self-compassion can support adaptive reengagement and perceived control after setbacks. [Dietary lapse study](https://pubmed.ncbi.nlm.nih.gov/37619622/), [goal-lapse psychoeducation](https://journals.sagepub.com/doi/10.1177/07342829231189010) | Use “What got in the way?” and “What will you change next time?” Never “You failed” or “You broke discipline.” A lapse becomes information for the next plan. | This is not therapy and does not replace care for compulsive behavior or mental-health conditions. |
| Streak effects | Logged streaks can increase persistence, but broken streaks can reduce subsequent motivation and make preserving the log a goal of its own. [Journal of Consumer Research](https://academic.oup.com/jcr/article/49/6/1095/6623414) | Prefer flexible consistency evidence over an unbroken daily streak: “3 of 4 planned sessions,” “returned after a miss,” or a simple recent history. | Never reward opening Shanivar merely to preserve a streak. |

Blocking itself has plausible value, but scope matters. A 2025 randomized trial found improvements after blocking mobile internet for two weeks, but Shanivar blocks selected apps for short user-chosen windows, so those findings cannot be treated as evidence for Shanivar's outcomes. [PNAS Nexus trial](https://pmc.ncbi.nlm.nih.gov/articles/PMC11834938/)

## Alignment to the locked V1

### 1. Welcome: identify a personally meaningful reason

Keep the approved short welcome. Add no personality quiz or philosophy lesson. The helper path asks the participant directly what they want time for.

Product question:

> “What do you want this time to make possible?”

Editable examples: read, study, create, pray, walk, rest, spend time with family, or another activity.

This chosen reason becomes the language on Home, Focus, the block surface and the optional review. It should not become a profile label or psychological diagnosis.

### 2. Home: two tools, one commitment model

Keep **Start focus** and **Set alarm** independent.

- Focus card: “Protect time for [chosen activity].”
- Alarm card: “Begin [chosen action] at [time].”
- Local pause: “Make one conscious choice now.”

The home should show only the next truthful action or current state. It should not display a feed, content carousel, consciousness score or generic motivational quote.

### 3. Focus plan: add an if-then sentence without another screen

Use the existing fields to generate a one-line commitment on the review/start surface:

> “When I start [activity], [selected apps] stay paused for [duration].”

Allow editing the activity, duration and selected apps. The action remains **Start protected focus**. The implementation intention is made concrete without teaching terminology.

When a selected app is attempted, the block surface recalls:

> “You chose this time for [activity].”

Primary action: **Return to my day**.  
Secondary action: **Take a short pause**.

The pause prompts observation rather than urge elimination:

> “Notice where the urge shows up. You do not have to make it disappear.”

### 4. Alarm plan: link the cue to a real first action

Use the existing alarm time, repeat days, destination cue, movement target and non-walking alternative to render:

> “When the alarm rings at [time], I will [chosen action].”

The action must be safe and user-editable. Examples: stand by the window, sit upright and drink water, place both feet on the floor, begin a non-walking confirmation. Movement cannot be required for every body or environment.

Alarm dismissal is evidence that the local interaction completed. It does not prove that the person woke fully, completed a morning routine or changed a habit.

### 5. Review: turn experience into the next plan

Replace the current “what helped?” answer list with two optional, fast questions:

1. **Did this help you make time for [activity]?** Yes / A little / No / Skip.
2. If A little or No: **What should change next time?** Shorter time / Different apps / Different time / Different activity / Nothing yet.

After an early release:

> “You ended this session early. What got in the way?”

Possible answers: needed the app, plan was too long, wrong moment, technical problem, another reason, skip.

Do not force reflection and do not keep the user inside the app after answering. Apply the selected adjustment to the next editable plan only after confirmation.

### 6. Return surface: evidence, not a streak

Within the locked MVP, add one compact **Your recent choices** card using already available local/backend events:

- “3 focus sessions begun”
- “2 completed; 1 ended deliberately”
- “2 of 3 helped your chosen activity” when voluntarily answered
- “Next alarm: Tuesday, 07:00”

Show a rolling seven-day or last-five-attempt view. Do not show a daily streak, rank, XP, “discipline score,” comparison with other users or a red failure state. The useful call to action is:

> **Repeat last plan** or **Adjust next plan**

This is the principal reason to return beyond operating the tools: see what happened and choose the next experiment. It remains small enough for V1 and does not create another feed.

## Return cadence

| Moment | Why Shanivar appears | User benefit | Product behavior |
| --- | --- | --- | --- |
| Chosen alarm time | Begin a previously selected action | Converts intention into a concrete start | Ring, show the chosen action, dismiss safely, leave |
| User starts focus | Protect a meaningful activity | Reduces exposure to selected distractions | Confirm plan, start real protection, show truthful state |
| Protected app attempted | Interrupt automatic behavior | Creates a choice point | Recall intention, return home or observe briefly |
| Focus ends or is released | Learn from the attempt | Improves the next plan | Optional two-question review, then exit |
| User-chosen weekly check | Recalibrate | See patterns without daily pressure | Compact factual summary and one adjustment |

No default “We miss you,” streak-loss warning or guilt notification. A reminder must refer to a plan the user chose, state the action, and be independently controllable. The system should sometimes provide nothing.

## V1 priorities

### Required before expanding the MVP

1. Preserve the approved Alarm and Focus functionality and safety contracts.
2. Carry the chosen activity consistently from plan to active state, block surface and review.
3. Render the focus and alarm plans as concrete when-then commitments.
4. Distinguish normal completion, early release, technical failure and unknown outcome.
5. Replace generic review choices with usefulness and next-adjustment questions.
6. Add a compact recent-choices card and repeat/adjust action, without streaks.
7. Keep reminders opt-in and tied to the user's own plan.
8. Measure voluntary repeat, assistance and reduced dependence without treating app opens as success.

### Remain outside V1

- AI coaching, predictive timing and inferred emotional states
- social competition, leaderboards, XP and discipline/consciousness scores
- mandatory daily streaks and streak-loss notifications
- family surveillance or private reports to helpers
- astrology, content feed and broad practice library
- automatic changes to a user's plan based on inferred behavior

## Learning plan

Test mechanisms separately. A six-person shakedown cannot estimate effectiveness, but it can reveal comprehension and failure modes.

### Comprehension and usability

- Can a participant explain their focus plan as “when X, then Y”?
- Can they distinguish whole-app blocking from feed-only blocking?
- Can they independently change the next plan after a poor fit?
- Does an alarm-only user avoid focus permissions, and vice versa?
- Does “recent choices” communicate evidence without feeling like judgment?

### Behavioral hypotheses

| Hypothesis | Minimum comparison | Primary signal | Stop or revise when |
| --- | --- | --- | --- |
| Personally meaningful activity copy improves voluntary repeat | Generic “focus” versus participant's chosen activity | Second independently started session | Copy is misunderstood or creates no stated relevance |
| If-then summary improves plan recall | Existing fields with/without generated commitment sentence | Participant can state cue and action without coaching | Adds friction without better recall |
| Optional urge observation improves return to intention | Return-home only versus return-home plus optional pause | Chosen response after a blocked-app attempt and optional usefulness | Pause becomes ritualized app time or frustrates users |
| Adjustment review improves the next attempt | Usefulness question alone versus usefulness + one adjustment | Confirmed change followed by another attempt | People skip because review is burdensome |
| Flexible recent evidence supports consistency | No summary versus last-five-attempt summary | Repeat/adjust action, plus interview response | Users optimize the log, feel judged or confuse events with outcomes |

Keep parent-introduction and young self-use results separate. Record assistance, language and framing. For school-age participants, conduct the separately required consent, privacy and comprehension work before recruitment.

## Measures that fit the mission

Primary product measures:

- time from opening to a confirmed alarm or focus action;
- percent of valid starts that reach completion, deliberate release or known failure;
- independently repeated use for the same personally chosen reason;
- optional usefulness response;
- plan adjustment after a poor fit;
- user-confirmed transfer outside the app;
- progressively less assistance where that is the person's goal.

Guardrail measures:

- permission or account abandonment;
- false active/scheduled states;
- unmanaged alarms or focus services;
- repeated bypass attempts;
- shame, pressure or family coercion reported in interviews;
- app opens with no chosen action;
- review or summary becoming a reason to remain in the app.

Do not optimize daily active users, session length, content consumption or notification opens as top-line success. They conflict with the founder's definition of eventual independence.

## Claims policy

Appropriate:

- “Turn a chosen intention into protected time.”
- “Add a pause before an automatic choice.”
- “Notice what helped and adjust the next attempt.”

Not established:

- “Repairs dopamine.”
- “Cures phone addiction.”
- “Builds metacognition” as a measured outcome.
- “Creates discipline in 21/66 days.”
- “Makes you fully present,” “raises intelligence,” or produces spiritual realization.

The research provides design rationale and hypotheses. Shanivar needs its own controlled product evidence before making outcome claims.
