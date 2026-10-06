# Shanivar wish list (living document)

> **Marketing must never present a wish-list item as a feature.** Nothing here is built, scheduled or promised. No post, caption, bio, website line, store listing, ad or reply may describe these items as existing, coming soon or planned for a date. If a draft mentions one, it fails review (`marketing/shanivar/marketing-operating-system.md`; claim gates in `marketing/shanivar/routine-v3-onboarding/routine-onboarding-marketing-plan.md` §6).

Started 6 October 2026 by the Grok Bot collaborator at the founder's request. Every item comes from something the repo already **parks or defers**, or from something the new marketing plans **want but the build lacks**, and each names its source. Status for every item: **wish, not committed**. Adding an item here is not a product decision. Product decisions live in `docs/decisions/` and Jira (project DEV).

**Stance check key** (from `SHANIVAR_STANCE.md` and the Routine V3 handoff): **Awareness** (does it help the person notice?), **Agency** (does the person choose and stay in control?), **Noise** (does it add prompts or attention load?), **Timing** (is now the right phase?). ✓ passes · ~ conditional · ✗ fails.

**Phases:** P1 = current Routine V3 Phase 1 (`docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md:18`); P2 = Sadhana Phase 2 / after tester build; Later = after public release; Hold = needs explicit new evidence or a decision.

---

## A. Sadhana and practice

| # | Wish | Why | Source | Phase | Stance check | Status |
|---|---|---|---|---|---|---|
| A1 | **Sadhana Phase 2 progression**, option A "progression without status": visible practice history, authored sequences, no points or levels | The founder wants practice that is worth giving real time to; Phase 1 says "Progression waits" | Jira DEV-16 (Pending); `docs/decisions/0013-sadhana-progression-and-the-gamification-question.md:80`, `:122-132`; `docs/decisions/0014-…md:18`; `android-app/app/src/main/java/com/shanivar/nativeapp/RoutineOnboardingFlow.kt:283`; handoff:51 (Do not build in Phase 1) | P2 | Awareness ✓ · Agency ✓ · Noise ~ (no pressure mechanics) · Timing ✗ until DEV-16 is decided | wish, not committed |
| A2 | **Ujjayi as the first named pranayama**, slotted into the existing practice catalogue via the practice-translator process (no new timed "recipe") | Founder interest on 5 Oct; repo says it should go through the existing process | `AGENTS.md:20`; `ai_org/agents/embodiment_practice_translator.md`; `docs/decisions/0011-vbt-breathing-third-pillar.md` | P2 | Awareness ✓ · Agency ✓ · Noise ✓ · Timing ~ (source and content review first; "new breathing/practice scripts" are banned in Phase 1, handoff:53) | wish, not committed |
| A3 | **New breathing/practice scripts** beyond the reviewed Steady catalogue | More variety | `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:53` (Do not build in Phase 1); `docs/strategy/shanivar-launch-backlog.md:20` (larger practice library, Later) | Later | Awareness ✓ · Agency ✓ · Noise ~ · Timing ✗ (source and suitability review) | wish, not committed |
| A4 | **Advanced-practice unlocking** | Depth for experienced users | Handoff:54 (Do not build in Phase 1); 0014:58 bans activity-based escalation into intensive practice | Hold | Awareness ~ · Agency ✗ if unlocks gate content · Noise ~ · Timing ✗ | wish, not committed |
| A5 | **Dinacharya-informed day suggestion** (attributed cultural context only; the schedule still follows the person's stated life) | Cultural depth the founder values | `docs/decisions/0014-…md:37`; a source ledger is needed (DEV-16 notes) | Later | Awareness ✓ · Agency ✓ if optional · Noise ~ · Timing ✗ (no circadian/cortisol claims, handoff:55) | wish, not committed |
| A6 | **Admin overview of user Sadhana depth, plus light gamification on practice streaks** (one combined later-build item). Admin side: an aggregate view of how deep people go in Sadhana (practices tried, practice minutes, sequences reached). User side: a slightly gamified streak to help keep the practice going | Founder request on the 6 Oct 2026 call: see how deep users go, and a hint to make streak tracking slightly gamified to keep the streak going | Founder call, 6 Oct 2026. Conflicts to resolve first: streaks are excluded by decision 0009 (`docs/decisions/0009-behavioral-return-loop.md:15`), ST-005 (`SHANIVAR_STANCE.md:167-169`), the Steady contract ("no score, streak", `wireframes/steady/README.md:39`) and F1 below; the V3 wireframe's "soft gamification" (`…/shanivar-v3/README.md:30`, `:107`) points the other way. 0013 measures practice minutes, never session minutes (`docs/decisions/0013-…md:48`). Admin view needs analytics that do not exist (DEV-14) and a privacy-policy change (no private activity sharing, `privacy.html`) | Later | Awareness ~ · Agency ~ only if streaks are opt-in with no loss pressure, and admin data is aggregate and consented · Noise ✗ risk (streak prompts) · Timing ✗ until DEV-16 and a decision amending 0009 | wish, not committed (needs a founder decision on streaks vs 0009/ST-005) |

## B. Tracking (implied by the founder's "move and track things")

| # | Wish | Why | Source | Phase | Stance check | Status |
|---|---|---|---|---|---|---|
| B1 | **Completion tracking for personal activities** (mark done or not done, opt-in) | The founder's positioning, "people who want to move and track things"; the wireframe talks of growth "worth scheduling and tracking" | Gap: `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:154` ("Keep any later reminder or completion-tracking capability behind a separate contract and explicit user choice"); `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md` | P2 | Awareness ✓ · Agency ✓ only if opt-in and factual · Noise ~ · Timing ~ (needs its own contract) | wish, not committed |
| B2 | **Dated factual practice/activity history** ("what I did, when"), with no streaks or scores | Lets high-velocity users see movement without status mechanics | Decision 0009 (`docs/decisions/0009-behavioral-return-loop.md:15`, "factual attempts and outcomes without streaks, ranks or scores"); DEV-16 option A | P2 | Awareness ✓ · Agency ✓ · Noise ✓ · Timing ~ | wish, not committed |
| B3 | **Reminders for personal activities** (opt-in) | Users "value sessions but forget to initiate" | Handoff:154; `docs/strategy/shanivar-launch-backlog.md:11` (lightweight reminder experiment, Next, conditional) | P2 | Awareness ✓ · Agency ✓ if opt-in · Noise ✗ risk (test first) · Timing ~ | wish, not committed |
| B4 | **Optional weekly recalibration** in the Routine experience | Adjust the plan from real life, not ideal life | `docs/decisions/0009-behavioral-return-loop.md:15`. Built status in Routine V3 not verified | P2 | Awareness ✓ · Agency ✓ · Noise ~ (weekly at most) · Timing ~ | wish, not committed |
| B5 | **Opt-in, privacy-respecting usefulness feedback in app** ("Did this help you make room for what you planned?") | The plans' Phase 2+ metrics need it; no analytics exist today | `marketing/shanivar/v1-product-direction-handoff.md:62`; `marketing/shanivar/routine-v3-onboarding/website/privacy.html` ("No analytics… in the app today", read only); ST-008 | P2 | Awareness ✓ · Agency ✓ · Noise ~ · Timing ~ | wish, not committed |
| B6 | **Crash reporting** (readiness, not a user feature) | Measure reliability before public release | Jira DEV-14 (Open) | P2 | Awareness n/a · Agency ✓ with disclosure · Noise ✓ · Timing ✓ before release | wish, not committed |

## C. Alarm and protection

| # | Wish | Why | Source | Phase | Stance check | Status |
|---|---|---|---|---|---|---|
| C1 | **Multiple alarms, camera/QR missions, sleep tracking, wake-up rechecks** | Demand beyond one reliable alarm | `docs/strategy/shanivar-launch-backlog.md:17` (Later) | Later | Awareness ~ · Agency ✓ · Noise ✗ risk · Timing ✗ | wish, not committed |
| C2 | **Blog "third door" on the block screen** (a real article as an alternative) | Converts the frustration moment into something useful | `android-app/ENFORCEMENT_STANCE_2026-09-12.md:126`, `:147` (parked until content exists) | Later | Awareness ✓ · Agency ✓ · Noise ✓ · Timing ✗ (no content yet) | wish, not committed |
| C3 | **Account-required device scheduling** | Cross-device sync | Handoff:57 (Do not build in Phase 1); local device state is authoritative (`docs/decisions/0014-…md:41`) | Hold | Agency ✗ if it blocks local recovery · Timing ✗ | wish, not committed |

## D. Social, family and growth surfaces

| # | Wish | Why | Source | Phase | Stance check | Status |
|---|---|---|---|---|---|---|
| D1 | **Shareable routine card/template** (Notion-style; user chooses to share) | Organic distribution pattern in `zero-to-million-playbook.md` §B | Plans want it; build lacks it. "Social features" are banned in Phase 1 (handoff:56) | Later | Awareness ✓ · Agency ✓ if user-initiated · Noise ~ · Timing ✗ | wish, not committed |
| D2 | **WhatsApp and remote invitation links** | India distribution | `docs/strategy/shanivar-launch-backlog.md:21` (Later; "no unrequested outreach") | Later | Agency ✓ with consent · Noise ~ · Timing ✗ | wish, not committed |
| D3 | **Helper / parent handback onboarding path** | Second audience entry path (adult children introducing parents) | `marketing/shanivar/audience-direction.md`; absent from the V3 onboarding (`RoutineOnboardingFlow.kt`) | P2 | Awareness ✓ · Agency ✓ only if the parent chooses · Timing ~ | wish, not committed |
| D4 | **Family accounts / reporting** | Requested by helpers | `docs/strategy/shanivar-launch-backlog.md:24` (Hold); handoff:56 | Hold | Agency ✗ by default (no private activity sharing) · Timing ✗ | wish, not committed |
| D5 | **Push campaigns** | Re-engagement | Handoff:56 (Do not build in Phase 1) | Hold | Noise ✗ (conflicts with ST-008) | wish, not committed; recorded so it is not added silently |
| D6 | **Web "decide once" interactive micro-experience at shanivar.com** (Calm DoNothingFor2Minutes pattern) | Idea-first waitlist growth with no product claim | `zero-to-million-playbook.md` §B (Calm); website is owned by the website worker (`marketing/shanivar/routine-v3-onboarding/website/`) | P1 (marketing) | Awareness ✓ · Agency ✓ · Noise ✓ · Timing ✓ once the site is deployed | wish, not committed |

## E. Platform, language and business

| # | Wish | Why | Source | Phase | Stance check | Status |
|---|---|---|---|---|---|---|
| E1 | **Hindi and other Indian languages** | India-first audience; parent path | Jira DEV-13 (strings not extracted, blocks Hindi); `docs/strategy/shanivar-launch-backlog.md:15` | P2 | Awareness ✓ · Agency ✓ · Timing ~ (fluent review needed) | wish, not committed |
| E2 | **iOS app** | Reach | No iOS code in the repo; Android only (`android-app/DEVICE_DISTRIBUTION.md`) | Later | Timing ✗ | wish, not committed |
| E3 | **Payments / paid plan UI** | Sustainability | Handoff:56; `docs/strategy/shanivar-launch-backlog.md:22` (Later; never a fee to unlock, `android-app/ENFORCEMENT_STANCE_2026-09-12.md:139-140`) | Later | Agency ✗ if tied to unlocking · Timing ✗ | wish, not committed |
| E4 | **Astrology and birth inputs** | Founder-selected optional framing | `docs/strategy/shanivar-launch-backlog.md:18` (Later); ST-007 keeps astrology off the launch surface (`SHANIVAR_STANCE.md`) | Later | Agency ✓ if voluntary · Timing ✗ | wish, not committed |
| E5 | **AI reflection / scroller / recommendations, dosha inference** | Personalisation | Backlog:19 (Later); handoff:55 (Do not build in Phase 1) | Hold | Noise ✗ risk · Timing ✗ (no dosha/cortisol claims) | wish, not committed |
| E6 | **School-age availability** | All-ages direction | `docs/strategy/shanivar-launch-backlog.md:23` (readiness gate) | Later | Agency ✓ only with consent design · Timing ✗ (18+ first cohort) | wish, not committed |

## F. Listed only so they are not reintroduced silently

| # | Wish | Source | Stance check | Status |
|---|---|---|---|---|
| F1 | XP, ranks, scores, badges, streak-loss pressure, leaderboards; consciousness stages | Handoff:52; backlog:25 (Hold); 0009:15; ST-005 | **Fails**: Agency ✗ · Noise ✗. Kept here only as a visible "no", pending DEV-16 | wish, not committed (stance-failing) |
| F2 | A new navigation framework solely for two destinations | Handoff:58 | Timing ✗ | wish, not committed |
| F3 | Apparel integration and in-app content campaigns | Backlog:26 (Hold); Hybrid Monk clothing kept separate (`ai_org/agents/social_media_manager.md:47`) | Noise ✗ · Timing ✗ | wish, not committed |

---

## How to use this list

- **Product:** promote an item only through a decision record (`docs/decisions/`) or a Jira issue with a stance check. Then remove it here, or mark it "moved to DEV-xx".
- **Marketing:** may use this list to see what *not* to say. It may run research polls on problems ("What would you want to see about your week?") without naming or promising an item.
- **Anyone adding an item:** include the wish, why, source file, phase, stance check and status "wish, not committed".
