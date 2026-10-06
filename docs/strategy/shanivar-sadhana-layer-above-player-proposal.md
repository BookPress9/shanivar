# What comes after the Sadhana player: design proposal

- **Date:** 2026-10-06
- **Status:** **PROPOSED.** This is a design proposal, not a decision, and it does not authorize a build. Decision 0013 is still PROPOSED, and Phase 2 needs the founder (`docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md:18`).
- **Prepared by:** the `app.shanivar` Grok Bot, at the founder's request. Codex integrates (`AGENTS.md:8-9`). The founder accepts (`AGENTS.md:7`).
- **Scope:** the layer above the Steady player: sittings, the day, intention, progression, recipes and curation. No code, scripts or decisions change here.

**Labels used throughout:** **D** = already decided (an accepted decision, a stance entry, a recorded founder resolution, or built code) · **P** = proposed here · **F** = needs a founder decision.

**Read for this proposal** (`AGENTS.md:15-19`): `AGENTS.md`, `SHANIVAR_STANCE.md`, `ai_org/agents/{embodiment_practice_translator,founder_second_brain,sadhak_journey_researcher,sadhak_researcher,ux_behavioral_architecture_researcher,content_designer}.md`, decisions 0009-0014, `android-app/wireframes/steady/README.md` (the "Steady contract"), the V3 wireframe README and `app.js`, `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md`, `android-app/CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md`, `research/deep-foundation/{SADHANA_ROADMAP_WORKSHOP_V1,PRACTICE_SAFETY,PRACTICE_LIBRARY,SADHAK_JOURNEY}.md`, `shanivar-vbt/content/breathing-mvp-v1.json`, the `Breathing*.kt` files, `RoutinePlan.kt`, `RoutineTimeline.kt`, and `marketing/shanivar/wish-list.md`. `rg -i sankalp` finds the word only in agent charters (`founder_second_brain.md`, `instagram_keeper.md:50`). No product document or code uses it yet.

**Improve, don't restart.** The Sadhana roadmap workshop (`research/deep-foundation/SADHANA_ROADMAP_WORKSHOP_V1.md`) already proposes a capability spine, kinds of evidence, a grammar for challenges and a first MVP. This proposal builds on it and does not replace it.

---

## 1. What exists today

| Element | Fact | Source | |
|---|---|---|---|
| Player | One process-scoped session owner shared by Home, the block overlay and the post-alarm screen. It holds no reference to the alarm or Focus engines, so it cannot release either | `docs/decisions/0012-…md:13-22`; `BreathingSessionOwner.kt:23-38`; 0011:18 | D |
| Stage plan | Each practice already runs **arrival 10 s → instruction 20 s → practice → closing 10 s**. A sleep practice has no closing and ends silently. These are reading times, not breath times | `BreathingModels.kt:138-163`; `breathing-mvp-v1.json:17` | D |
| Catalogue | Eight practices, all unpaced (`noPrescribedBreathTiming`, `noRetention`). The validator **refuses** any `prescribedBreathPhases`. Durations must fall between 60 and 1800 s. Status: `implementation_seed_review_before_public_release` | `BreathingCatalog.kt:34-35`, `:106-110`; `BreathingModels.kt:12-15`; `breathing-mvp-v1.json:9-10` | D |
| Source model | Any practice that has a source is labelled "Modern adaptation · VBT verse …". Verse labels must match `^\d{1,3}(-[A-Z])?$` | `BreathingModels.kt:110-113`; `BreathingCatalog.kt:37-38` | D |
| Records | `BreathingRecord` stores session, practice, version, entry host, duration, active seconds, outcome, optional Helped/No change/Felt worse, and a nullable date. The journal is capped at 100 rows and holds **no free text or feelings** | `BreathingSessionState.kt:162-195`; `BreathingPreferences.kt:15-18`; VBT handoff:127 | D |
| Last practised | One quiet line per practice: "Last practised today / yesterday / 3 Oct". No counts, no runs of days, no totals (a74aa3f) | `BreathingSessionState.kt:210-228`; `BreathingScreen.kt:97-98`, `:238` | D |
| 90-second rule | A session counts only if it ended by timer or by the user **and** `activeSeconds >= 90` (pauses excluded). Interrupted sessions never count. Every row is still saved as a factual attempt (12fbecd) | `BreathingSessionState.kt:197-208`; `BreathingHistoryTest.kt:130-132` | D (founder, 6 Oct) |
| Routine | `RoutinePlanV1` holds wake and sleep times, up to 12 activities and protection windows. "Extend Sadhana" (20 min) is an editable quick-add. Activities never claim completion or unlock Sadhana | `RoutinePlan.kt:14-20`, `:78`; handoff:152-154 | D |
| Wireframe extras (not native) | Practice record ("Evidence. Not mythology."), "I practised without the app", an independent mode that turns reminders off, and reminders off by default | V3 `app.js:240`, `:242`, `:250`, `:402` | D (reference only) |

### The 60 s default conflicts with the 90 s rule (F)

- The visit draft defaults to `budgetSeconds = 60` (`BreathingSessionOwner.kt:18`). Seven of the eight practices default to 60 s (e.g. `breathing-mvp-v1.json:52`). Only `sleep_threshold` defaults to 180 s (`:438`). The duration chips offer 1, 3 or 5 min (`BreathingScreen.kt:160`).
- When the timer ends, active time is set to the full duration (`BreathingSessionState.kt:95-97`). A completed default session therefore records **60 s, which is below 90**, and never shows "Last practised".
- The block-overlay host offers only a 60-second pause (Steady contract `README.md:42`; VBT handoff:32). The V3 foundation practice is "one-minute" (V3 README:87). Those paths can never count.
- Effect: on the default path a person practises, finishes, and sees nothing recorded. That reads as a bug, not as honesty.

| Option | Change | Trade-off |
|---|---|---|
| A (**P, recommended**) | Keep 90 s. Make **3 min the default when Sadhana is opened from its tab**; all eight practices support 180 s. Call 1-minute host sessions "a pause". They stay in the journal and are not labelled as practice | Keeps the founder's 6 Oct threshold and makes the difference between a pause and a practice visible. Needs a one-line default change and new copy |
| B | Lower the rule to 60 s | Simple, but reverses the founder's 6 Oct decision |
| C | A timer-completed session counts at any length; an ended-by-user session counts from 90 s | Two rules to explain, and it blurs what "practised" means |

**Founder choice, 2026-10-06 (D):** option A: keep `MIN_PRACTISED_SECONDS = 90`, default to 3 min (180 s) when Sadhana is opened from its tab, and keep the alarm, pre-focus and block-screen entries as 1-minute pauses that do not count as "Last practised".

---

## 2. Sequencing: the sitting (P, built on D)

**Already decided:** practices stay unpaced (0011:16). One session owner (0012). No new scripts in Phase 1 (handoff:53). Activities are calendar entries only (handoff:154). Sleep practice ends silently (`BreathingCatalog.kt:139-146`). 0013 Option A names "a `sequences` content type with prerequisites" as its first concrete need (0013:94, `:162-163`).

**Proposed here: a sitting is an ordered list of one to three existing catalogue practices.** It uses 0013's `sequences` type and is called "sitting" in the interface.

| Step | Purpose | Phase 1-compatible content | Rule |
|---|---|---|---|
| Arrive (optional) | Stop the momentum | `external_grounding` or `natural_anchor` at 60 s | Always replaceable by the external anchor (`PRACTICE_SAFETY.md:16`) |
| Practice | The main practice | Any eligible catalogue entry at the chosen duration | Same eligibility filters as today (Steady contract `README.md:34`) |
| Close | Return to life | The existing closing stage and End screen. P10 "Choose the next small action" (`PRACTICE_LIBRARY.md:96-103`) only after review | A sleep practice may only be the **last** step and has no Close |

Validator rules (**P**, the same structural approach as `BreathingCatalog.kt:17-21`):
- Steps reference existing practice IDs only.
- No timing fields.
- Total time stays within the existing 1800 s maximum.
- Sleep comes last.
- No step requires a prerequisite that is inferred from activity.

How it runs (**P**):
- The sitting is a queue *above* the one session owner, which runs one practice at a time. Steps never overlap.
- The default between steps is an explicit "Continue". Skip and End stay one tap away.
- End stops the whole sitting.
- Each step writes its own `BreathingRecord`, with an optional `sittingId` (a new field).

How sittings fit into the day (mostly existing moments):

| Moment | Existing entry | Sitting role |
|---|---|---|
| After the alarm | `AFTER_ALARM` "Take a quiet minute" (VBT handoff:137) | A pause only. It never touches dismissal |
| Before focus | `BEFORE_FOCUS` (Steady contract `README.md:44`) | A pause only. Focus still needs its own Start |
| Block overlay | `BLOCK_OVERLAY`, 1-minute limit | A pause only. No unlock (ST-006, `SHANIVAR_STANCE.md:201`) |
| Planned sitting | A Routine activity such as "Extend Sadhana" (`RoutinePlan.kt:78`) | **P:** an activity may link to a sitting, and the timeline shows "Open sitting". This fills the "one optional Sadhana entry" the handoff asks for (handoff:278), which `TimelineKind` does not have yet (`RoutineTimeline.kt:12`) |
| Wind-down or sleep | `BreathingSchedule` windows, `sleep_threshold` | Suggestion ranking only. A clock never routes anyone into sleep practice (`BreathingCatalog.kt:165-169`) |

A missed sitting changes nothing: no reminder, no mark, no copy (workshop:82). Reminders stay a separate opt-in wish (wish-list B3, `:31`).

---

## 3. Sankalp (intention)

**Already decided:**
- 0010 turned a "deepest desire" prompt into a **user-typed when-then commitment, stored word for word and shown back unchanged, with nothing generated** (`0010-…md:19-23`, `:37-40`).
- An intention field of up to 60 characters already exists for focus (`FocusScheduleCard.kt:246`).
- The breathing journal must hold no free text (`BreathingPreferences.kt:15-18`).
- The practitioner can disagree, change or skip (ST-003, `SHANIVAR_STANCE.md:103`).

**Proposed here:**

| Aspect | Design |
|---|---|
| Form | One optional line in the person's own words, up to 60 characters, for **today** or **this sitting**. Offer the when-then pair from 0010 as an option, because that form has the evidence behind it |
| Where it appears | Word for word on the Ready screen, and again on the End screen next to "your next action". Nowhere else. No notifications |
| Ownership | Edit, clear or skip at any time. Never rewritten, summarized, suggested or completed by the app (0010:37-40) |
| No scoring | No "Did you keep it?" question, no fulfilment rate, no history of kept or broken intentions. An optional private note is allowed, never aggregated |
| Storage | A **separate local store**, not the breathing journal, with its own "Clear". Never synced. Not visible to A7 unless the person explicitly shares it (§6) |
| Naming | Default label "Intention". "Sankalp" is an optional label with a short source note, like the optional Shani wording (Steady contract `README.md:15`) and ST-001's tradition labelling |

**Needs a founder decision:** per day or per sitting. Plain intention or the when-then form. The Sanskrit label. Note that the founder's second-brain agent is already called *Sankalpa* (`founder_second_brain.md:3`). Using the same word in the product invites confusion.

---

## 4. Progression without streaks

**Already decided:**
- No streaks, ranks or scores (0009:15).
- No gamification in V1 (0009:17).
- No attainment levels, and nothing unlocked by streaks (ST-005, `SHANIVAR_STANCE.md:167-169`).
- "No score, streak, confetti" (Steady contract `README.md:39`).
- On 6 Oct the founder resolved streaks **against** them (wish-list A6/F1, `:22`, `:70`).
- Depth opens through "informed choice plus stated prerequisites, never accumulated activity" (0013:75-76; 0014:58).
- Breath comfort is never inferred from minutes (`BreathingPreferences.kt:27-28`).
- Measure practice minutes, never session minutes (0013:48).

**Proposed here: five layers, smallest first.**

| Layer | What the person sees | Source it extends |
|---|---|---|
| 1. Dated history | A list, newest first: date · practice · practised minutes · how it ended · feedback. Short attempts are shown as "pause" or "attempt", not hidden | Journal (`BreathingSessionState.kt:173-184`); 0009:15 "factual attempts"; wish-list B2 (`:30`) |
| 2. Range | Practices tried, and practices not tried yet, offered out of curiosity. **No "5 of 8" fraction** | The catalogue; workshop:55 (depth is a branch available anytime) |
| 3. Depth by choice | Authored sequences with plain prerequisites ("asks you to be comfortable with breath awareness"). Self-confirmed, never earned | 0013 Option A (`:80-94`); `SADHAK_JOURNEY.md:10` |
| 4. Own reflection | The existing Helped/No change/Felt worse. Optional private notes later, under separate consent | `BreathingRecord.feedback`; PRACTICE_SAFETY:44 |
| 5. Carry | "I practised without the app" as a self-report, independent mode, and fewer prompts counted as progress. Silence is never scored as success | V3 `app.js:242`, `:402`; workshop:84, `:88`; ST-008 (`SHANIVAR_STANCE.md:263`); backlog:14 |

**Banned (D unless marked P):**
- Streaks, run-of-days, streak freezes or repair. "Don't break the chain" calendars and **heatmaps** (P: a heatmap is a streak in visual form).
- XP, points, levels, badges, ranks, titles, leaderboards, social comparison (0009:15-17, ST-005, F1).
- Readiness, consciousness or "success" scores. Breath or physiological scores (ST-004 `:135`; PRACTICE_SAFETY:18).
- Unlocks driven by activity or time. Rewards for longer, more intense or more frequent practice (0013:75-76, 0014:58, workshop:90-98).
- Loss-aversion or "you missed…" copy. Push re-engagement (wish-list D5 `:52`).
- **P:** catalogue-completion percentages. Session minutes as a metric.

**Founder decisions:** whether practice counts or total minutes appear at all (workshop:160). 0013:87 allows them, while the B2 code chose none (`BreathingSessionState.kt:210-213`). Also, what a person sees after a missed week (workshop:161).

---

## 5. Ujjayi as a future "recipe"

**Already decided:** idea only (`embodiment_practice_translator.md:23-27`; wish-list A2 `:18`). `AGENTS.md:20-21` records the mistake of framing Ujjayi as new and proposing a timed recipe.

**Proposed meaning of "recipe":** an **authored sitting (§2) with one reviewed technique step.** For example: Arrive (`natural_anchor`, with an external-anchor alternative) → Ujjayi step → return to natural breath → Close. A recipe is not a breath-timing script. The validator ban (`BreathingCatalog.kt:106-110`) stays, and the Ujjayi step would be unpaced.

**What must change first, in order:**

| # | Prerequisite | Source |
|---|---|---|
| 1 | Phase 1 ends. New scripts are banned in Phase 1 | handoff:53 |
| 2 | A decision **amending 0011**: "unpaced comfortable awareness" (0011:16) does not cover gently shaped breathing. Add a new technique class with its own population, instruction and evaluation | 0011:16, `:28` |
| 3 | A new **PRACTICE_SAFETY row** between "natural breath observation" and "intensive manipulation". Nasal breathing stays optional, normal breathing is always allowed, and an external anchor stays available | `PRACTICE_SAFETY.md:16-17`, `:34` |
| 4 | **Source labelling:** Ujjayi is not VBT. The model hard-codes "VBT verse" (`BreathingModels.kt:110-113`; `BreathingCatalog.kt:37-38`), so it needs a source model that is not tied to VBT, plus a provenance ledger entry. None exists yet; the nearest is a project-description-only Haṭha note at `SOURCE_LEDGER_ANCIENT.md:101`. If a source pairs Ujjayi with retention, the omission must be disclosed | ST-001; PRACTICE_SAFETY:40 |
| 5 | A `PRACTICE_LIBRARY.md` entry and all release gates (source, qualified practice review, accessibility, user study, founder version and date) | `PRACTICE_LIBRARY.md:109`; PRACTICE_SAFETY:40-43 |
| 6 | Offered only on the Sadhana tab, behind self-confirmed breath comfort. Never from the overlay or alarm hosts, never a default suggestion, never unlocked by activity | `BreathingPreferences.kt:27-28`; 0013:75-76 |

---

## 6. Where A7 Hybrid Monk curation plugs in (Later)

**Already decided:** A7 is a wish, not committed. It allows suggestions only and no unlocks driven by activity (0013:76, 0014:58). It must be honestly disclosed, with no fabricated guru (`SHANIVAR_STANCE.md:13`). It needs analytics (DEV-14), consent and a privacy change (wish-list `:23`). "No AI coach" applies to V1 (0009:17).

**Proposed here: one plug-in point, the existing Suggestion step.** It keeps the Steady contract: one primary card, up to two alternatives, and the inputs behind the suggestion explained (Steady contract `README.md:33`).

| Rule | Design |
|---|---|
| What can be suggested | A reviewed catalogue practice, sitting or sequence only. Never new content, never an unlock, never a higher intensity |
| Disclosure | Every suggestion shows "Suggested by [named human coach]" or "AI-assisted, reviewed by [name]", with the reason in plain words |
| Agency | Accept, decline or undo with one tap. Declining costs nothing and changes nothing else |
| Noise | Shown only when the person opens Sadhana. No push, no badge, no inbox |
| Data | Only consented, minimal journal fields. Sankalp text and notes are excluded unless explicitly shared. Visible to the person, deletable, and revocable |
| Sequence | A6 aggregate overview first, then A7 |

**Prerequisites:** DEV-14 analytics; a consent flow; a privacy-page change (today it says "Steady keeps practice data local… No analytics", `privacy.html:71-72`); a decision amending 0009:17 for after V1; a named reviewer.

---

## 7. ST-009 principles mapped to the design (P; ST-009 itself states no product implication, `SHANIVAR_STANCE.md:302`)

| Principle (`SHANIVAR_STANCE.md:291-296`) | Design expression | Must never become |
|---|---|---|
| Discipline | Sittings the person puts on their own Routine calendar, kept as voluntary boundaries (ST-006) | Streaks, penalties, practice required to unlock or dismiss |
| Keep moving forward | Dated history shows movement. A next challenge the person chooses (workshop:81). After a gap, just the last date | Progress bars, levels, "don't lose your progress" |
| Learning | Source and adaptation views. "What this asks of you" on every sequence | Quizzes that gate depth |
| Curiosity | "Not tried yet". Source depth open at any time (workshop:55) | Collection or completion percentages |
| Be more present, be more in your body | The practices themselves. The body is optional and the external anchor is equal (ST-004) | Mandatory breath focus, body or physiological scores |
| Walk towards your purpose | Sankalp in the person's own words. Sitting → next action (P10) | Purpose written by the app, AI reminders (0010:19-23) |

---

## 8. Three-way test (interesting / behaviourally effective / commercially viable) and the four lenses

The lenses come from `founder_second_brain.md:21-22`. A = awareness, Ag = agency, N = noise, T = timing.

| Element | Interesting concept | Behaviourally effective | Commercially viable | A · Ag · N · T |
|---|---|---|---|---|
| Fix 60/90 | Low | High: an honest record is a precondition for everything else | Neutral | ✓ · ✓ · ✓ · ✓ now |
| Dated history | Low | Plausible for self-monitoring (0009 reason, `:21`). Unproven here | Expected as standard | ✓ · ✓ · ✓ · ~ (0013) |
| Sittings + Routine link | Medium | Stable context plus a planned time. Unproven | Medium: practice worth setting time aside for (0013:122-127) | ✓ · ✓ · ~ · ~ |
| Sankalp | High | The when-then form has evidence. A bare intention is weaker | Low directly, high for brand fit | ✓ · ✓ · ✓ · ~ |
| Range / curiosity | Medium | Unknown. Risk of collecting for its own sake | Low | ✓ · ✓ · ~ · ~ |
| Carry / independent | High | This is the actual goal (0014:14). Hard to measure | Awkward: success means less use (ST-008) | ✓ · ✓ · ✓ · ~ |
| Ujjayi recipe | High (founder interest) | Unknown, with real comfort risk | Medium: a named practice differentiates. Review is costly | ~ · ✓ · ✓ · ✗ |
| A7 curation | High | Possibly ("feeling guided"). Risk of dependence | Highest potential, heaviest prerequisites. No paid claim made here | ~ · ~ · ~ · ✗ |

---

## 9. Open questions, phased order, decisions needed

**Open questions for Shubham:**
1. 60 vs 90: keep 90 s and make 3 min the Sadhana default, with 1-minute sessions called "pauses" (option A)?
2. Accept 0013 Option A so that history, sittings and sequences can be built?
3. Sankalp per day or per sitting, plain or when-then, and the Sanskrit label or "Intention"?
4. Should practice counts or total minutes be visible at all (workshop:160)?
5. Which spine names: Arrive/Return/Understand/Make it yours/Deepen (V3) or Arrive/Return/See/Choose/Carry (workshop)?
6. Does "soft gamification" now mean only voluntary challenges and self-reports (see conflict 2)?
7. A7: a named human coach, or AI reviewed by a named human?

**Phased order (smallest useful step first):**

| Step | What | Gate |
|---|---|---|
| 0 | Resolve 60/90 (a constant or default plus copy) | Q1 |
| 1 | Dated history view built from the existing journal, plus an "I practised without the app" record | 0013 Option A |
| 2 | Sittings from the existing 8 practices. Routine activity → "Open sitting". Sadhana timeline entry | Phase 2 + sittings decision |
| 3 | Local Sankalp line | Q3 + privacy-page note |
| 4 | Authored sequences with prerequisites. First Return/Choose challenges (workshop:141-153) | Phase 2 path decision |
| 5 | Ujjayi recipe | §5 prerequisites |
| 6 | A6, then A7 | DEV-14, consent, privacy, 0009 amendment |

**New or amended decisions needed:**
- 0013 accepted (or superseded) with Option A.
- The 60/90 resolution recorded.
- A `sequences`/sittings content type with its validator rules.
- Sankalp storage and naming.
- 0011 amended for a technique class and sources other than VBT.
- A PRACTICE_SAFETY row for Ujjayi.
- 0009:17 amended for A7 after V1.
- A privacy-policy change.
- Founder wording for any ST-009 product implication.

**Conflicts found between files:**
1. The 60 s defaults (`BreathingSessionOwner.kt:18`, `breathing-mvp-v1.json:52`, Steady contract `README.md:42`, V3 README:87) against the 90 s rule (`BreathingSessionState.kt:198`).
2. "Soft gamification… milestones, challenges" (V3 README:30, `:107`; workshop:25) against the 6 Oct resolution against streaks (wish-list `:22`, `:70`) and 0009:17.
3. Chapter names: V3 README:101-105 against workshop:49-55. Workshop:55 says "Deep study is a branch, not a sixth rank", while V3 makes "Deepen" the fifth chapter.
4. V3 README:27 drops the anchor choice ("Natural breathing is the foundation"). That conflicts with ST-004 (`:135`), PRACTICE_SAFETY:16 and Steady contract `README.md:32`. Native code keeps the choice (`BreathingScreen.kt:175`).
5. Counts: 0013:87 and V3 `app.js:240` show counts, but the B2 code shows none (`BreathingSessionState.kt:210-213`).
6. The Routine handoff asks for "one optional Sadhana entry" (handoff:278), but `TimelineKind` has none (`RoutineTimeline.kt:12`).
7. 0010's when-then commitment is not part of `RoutinePlanV1`/`ProtectionWindowV1` (`RoutinePlan.kt:14-45`). The overlay still reads the legacy cache (`BlockOverlayActivity.kt:37`). Unverified whether V3 keeps it.
8. The source model is tied to VBT (`BreathingModels.kt:110-113`), which blocks any non-VBT recipe.
9. A7's "coach persona" (wish-list `:23`) against "no AI coach" (0009:17) and no fabricated guru (`SHANIVAR_STANCE.md:13`).
10. "Sankalpa" names both an agent (`founder_second_brain.md:3`) and the proposed product feature.
