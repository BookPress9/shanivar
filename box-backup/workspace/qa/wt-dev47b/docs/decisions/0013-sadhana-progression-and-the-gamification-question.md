# 0013 — Sadhana progression, and what "gamified" may and may not mean

- **Date:** 2026-09-29
- **Status:** **PROPOSED — Phase 2 founder collaboration required.** Nothing in the Sadhana tab is built beyond the existing Steady practices until this is resolved. [Decision 0014](0014-routine-sadhana-shell-and-routine-v3.md) supersedes the three-tab context: the accepted shell is Routine / Sadhana with no Dashboard. The safety and gamification questions in this record remain open.

## Context

Historical founder direction, 29 September 2026: three bottom-nav destinations — **Dashboard**,
**Routine**, **Sadhana**. On 30 September, the founder-selected V3 wireframe removed Dashboard and authorized Routine as Phase 1; see decision 0014. The original request remains relevant to the Sadhana question: Routine sets the day's agenda from a Shanivar
suggestion based on **dinacharya**, and contains the app lock as a one-time configuration plus the
alarm setup. Sadhana is *"purely measurable from the basics to advanced"*, *"a gamified version of the
sadhana"*, covering *"very intense practices around manovigyana, around tantra"*, tracked, and intended
to *"let the users take the time out of their day to visit Shanivar and spend their time on this app."*

Two of those three destinations are straightforward and are scaffolded already. The Sadhana description
runs into three things this repo has already accepted, and one of them is a safety boundary rather than
a preference.

## The three conflicts, with citations

### 1. "Gamified" is explicitly banned for V1

- [Decision 0009](0009-behavioral-return-loop.md), founder-approved 2026-09-19: *"Add no AI coach,
  content feed, social comparison, family surveillance, predictive targeting or **gamification** to
  V1."* And: *"A compact recent-choices card should show factual attempts and outcomes **without
  streaks, ranks or scores**."*
- `CLAUDE.md`: *"Astrology, the reflection scroller, the practice library, gamification, WhatsApp,
  family dashboards and paid-plan UI are **later capabilities**."*
- [`SHANIVAR_STANCE.md`](../../SHANIVAR_STANCE.md) ST-005, *Depth is chosen, not earned through
  status*: *"No attainment levels, spiritual leaderboards or source access unlocked by streaks."*
- The Steady wireframe contract (`android-app/wireframes/steady/README.md`), End screen: *"No score,
  streak, confetti or claim of successful meditation."*

The founder may overturn 0009 — that is the founder's call. But 0009 is a written, approved decision,
so it should be overturned **on the record**, not superseded by inference.

### 2. "Spend their time on this app" inverts the stated north star

- Founder brief §2: *"We are **not** trying to make people spend the maximum possible time inside
  Shanivar... Shanivar helps the person leave Shanivar more present than when they entered it."*
- ST-008, *Useful departure is an outcome worth investigating*.

**This conflict is largely resolvable by measuring the right thing**, and I think the founder's intent
and the north star are actually compatible. A twenty-minute seated practice *is* twenty minutes spent
in the app, and there is nothing wrong with it — it is practice time, not browsing time. What the
mission rejects is time spent *in the interface*: scrolling, browsing, deciding, being retained.

So: **measure practice minutes, never session minutes.** A person who opens Shanivar, practises for
twenty minutes and leaves is a complete success. A person who spends twenty minutes in Shanivar without
practising is a product failure, even though a standard engagement dashboard would score them
identically. That distinction is cheap to implement and worth fixing in the metric definitions before
any dashboard exists.

### 3. "Very intense practices" is a safety boundary, not a preference

This is the one I would push back on hardest, and the reason is the *combination* rather than any single
part.

- ST-004: *"Advanced/intensive methods remain research-only without exact-version review and an
  appropriate setting."*
- [Decision 0011](0011-vbt-breathing-third-pillar.md) extends the practice-library deferral **only for
  the eight specified starter practices**, and deliberately excludes traditional retention practices.
- `CLAUDE_TECHNICAL_INTEGRATION_V1.md` §17 and the test guide define risk classes up to
  `QUALIFIED_GUIDANCE` and `DO_NOT_AUTO_DELIVER`.
- The shipped catalogue validator **refuses to load content carrying prescribed breath timing at all** —
  the data model has no field able to express a retention instruction.

The hazard is not "advanced practice exists". It is **progression-driven escalation**: if continued use
is what advances someone from basics to advanced, then the mechanism deciding who attempts intensive
breath work is *engagement*, not readiness. Intensive pranayama, retention and kundalini-oriented
practice have real contraindications, and an unsupervised app that escalates on activity is the exact
delivery mechanism ST-004 exists to prevent. Stance ST-005 already names this: *"Time spent in-app never
establishes readiness for intense practice."*

This one I would keep regardless of how gamification resolves: **whatever unlocks depth must be
informed choice plus stated prerequisites, never accumulated activity.**

## Options

### A. Progression without status — *recommended*

Practice history is visible and factual. Sequences are explicit and self-chosen. Depth is unlocked by
informed consent plus stated prerequisites.

| | |
|---|---|
| Measurable? | **Yes** — practice count per practice, total practice minutes, sequence position, self-reported usefulness |
| Basics → advanced? | **Yes** — authored sequences with stated prerequisites and what each asks of the person |
| Gamified? | No streaks, no XP, no levels, no badges, no leaderboard, no loss aversion |
| Safety | Advanced practice needs explicit acknowledgement; activity never unlocks it |
| Conflicts with 0009 | **None.** 0009 already permits "factual attempts and outcomes" |
| Conflicts with ST-004/ST-005 | None |
| Founder decision needed | None — buildable immediately |
| Complexity | Low–medium. Needs a date on `BreathingRecord`, a `sequences` content type, Room when history grows |

### B. Gamification with a safety firewall

Add streaks, points and levels for **LOW_RISK practices only**. Advanced practice sits entirely outside
the game layer and is reachable only by explicit informed choice.

| | |
|---|---|
| Measurable / motivating? | Yes, in the conventional sense |
| Conflicts | **Overturns 0009 and ST-005.** Requires a superseding decision |
| Safety | Acceptable *if* the firewall holds — but the firewall is the thing that erodes first under pressure to make progression feel continuous |
| Risk | A streak creates loss aversion, which is an engagement mechanic pointed at a person trying to reduce compulsive phone use. The product would be using the mechanism it treats as the problem |
| Founder decision needed | **Yes**, explicitly superseding 0009 |
| Complexity | Medium–high (game state, anti-cheat, reset semantics, accessibility of pressure-free use) |

### C. Full gamification of the whole sadhana path

Progression, including into intensive practice, driven by accumulated activity.

| | |
|---|---|
| Conflicts | Overturns 0009, ST-004 **and** ST-005 |
| Safety | **Not recommended under any framing.** Makes engagement the gate on practices with real contraindications |
| Complexity | High, plus a liability surface |

## Recommendation

**Option A, and it delivers more of the founder's stated intent than it appears to.**

The direction asks for sadhana that is *measurable*, moves *from basics to advanced*, is *tracked*, and
is worth *giving real time to*. Option A gives all four: visible practice history, authored sequences
with real prerequisites, per-practice tracking, and practice sessions substantial enough to be worth
setting aside time for. None of that requires a streak.

What Option A gives up is the specific pull of status mechanics — points, levels, the fear of breaking a
run. That pull is real, and for this product it points the wrong way: Shanivar exists to help someone
notice a compulsive loop and step out of it, and a streak is a compulsive loop with the product's name
on it. The most persuasive argument for Option A is not 0009; it is that a person who practises because
they will lose their streak is not practising for the reason the app claims to serve.

If the founder wants B anyway, the decision is legitimate and I will build it — but it needs to
supersede 0009 in writing, and the firewall around intensive practice should be a hard rule with a test,
not a guideline.

**Independent of A/B/C, I recommend keeping:** activity never unlocks intensive practice; practice
minutes rather than session minutes as the success metric; and the catalogue validator's refusal to
represent prescribed breath timing.

## Also unresolved: dinacharya is a fourth tradition

Routine is to suggest a day based on **dinacharya**, which is an **Āyurvedic** framework — distinct from
the Śaiva/Tantric VBT material, from Pātañjala Yoga and from the psychology bridge. Stance ST-001
requires that traditions are not flattened, so dinacharya needs its own source ledger entry, its own
provenance and its own claim limits before it drives a suggestion. That is GPT's research to produce; I
should not invent it.

Technically it is feasible and mostly already in place: dinacharya is time-anchored (often to sunrise and
sunset), `BreathingSchedule` already does wake/sleep window arithmetic, and the backend already has
`tz-lookup` and astronomical calculation available for solar times. The engineering is small. The
**provenance and the claim discipline are the actual work** — "Shanivar suggests your day based on
dinacharya" is a claim about a tradition, and it must not become health advice.

## Consequences

- The earlier three-tab shell, `HomeState` resolver and Routine grouping were built and tested as a scaffold (recorded 16 new tests; 176 Android tests total, 0 failures, lint clean). They now require migration to the accepted two-destination shell; those historical results are not evidence for V3.
- The Sadhana tab currently renders the existing Steady practices and nothing more. It deliberately
  contains no progression UI pending this decision.
- If Option A is accepted, the first concrete needs are a date field on `BreathingRecord`, a `sequences`
  content type with prerequisites, and Room once history outgrows SharedPreferences.
- If Option B or C is accepted, this file should be superseded by a decision that explicitly names which
  parts of 0009 and ST-005 no longer apply.

## Revisit when

The founder decides A, B or C; or a pilot shows that practice history alone does not sustain voluntary
return, which would be genuine evidence for revisiting rather than an assumption.
