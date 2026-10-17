# Product Requirements Document — Shanivar

Status: baseline, written 2026-10-11 from existing repo files. Where a fact is not in the repo it says `UNRESOLVED:`. Source-of-truth order: `SHANIVAR_STANCE.md` (ST-009 is the only ACCEPTED entry, `SHANIVAR_STANCE.md:277`) > `docs/decisions/` > strategy docs > marketing. Only the founder accepts stance changes (`SHANIVAR_STANCE.md:5`).

## 1. Product Overview
- **Product name:** Shanivar, by The Hybrid Monk (`ai_org/agents/instagram_keeper.md:58`).
- **Tagline:** "Discipline is freedom." Founder-given 2026-10-11; not yet an accepted stance entry (`marketing/shanivar/brand/tagline.md:1-5`). Record only; do not cite as doctrine.
- **One-line description:** An Android app with two places: **Routine** (shape the day, set a wake alarm, protect chosen time from selected apps) and **Sadhana** (practise until the skill survives without the app) — `docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md:13-18`. Core ideology: live as a Hybrid Monk (connected with everything, attached to nothing) and walk the Sadhak path (the right stance to face suffering and grow through it) — `SHANIVAR_STANCE.md` ST-010, decision 0015.
- **Product type:** Mobile app, with a pre-launch marketing website and an internal-test static web version of Sadhana.
- **Primary platform:** Android, minSdk 26, targetSdk 35 (`android-app/app/build.gradle.kts:11-12`). No iOS in the repo.
- **Surfaces:** (1) `android-app/`; (2) `backend/` Node API; (3) website `marketing/shanivar/routine-v3-onboarding/website/` (shanivar.com); (4) `web-app/` (planned app.shanivar.com, not deployed, `web-app/README.md`); (5) Instagram `@app.shanivar` (marketing, paused).

## 2. Problem
- **Problem:** The automatic reach for stimulation when boredom, effort or discomfort appears; mental loops; drifting from routine (`docs/strategy/shanivar-founder-direction.md:19-23`).
- **Why it matters:** Founder's own need and conviction. A spiritual/philosophical thesis, not a clinical claim (`shanivar-founder-direction.md:13-15`).
- **How users solve it today:** UNRESOLVED: no user research on current alternatives is recorded as evidence. Competitor references only: Alarmy and one sec (`docs/decisions/0007-v1-alarms-and-focus.md`), `research/deep-foundation/COMPETITOR_PRODUCT_RESEARCH.md`.

## 3. Goal
- **Primary goal:** Help people move from automatic loops toward embodied observation and conscious choice (`SHANIVAR_STANCE.md:11`), delivered as a Routine plus a Sadhana practice space.
- **Secondary goals:** a wake alarm that is hard to dismiss from bed but never traps the user (0003, `android-app/ENFORCEMENT_STANCE_2026-09-12.md:10`); app protection windows that are voluntary and stoppable (0002, ST-006); success may mean needing the app less (ST-008, `shanivar-founder-direction.md:48-50`).

## 4. Target Users
- **Adult Gen Z** (students, starting work), **millennials** balancing responsibility and identity, **younger adults** wanting a more present relationship with parents, **parents/older adults** for themselves; market focus India (`shanivar-founder-direction.md:27-38`). All ages intended, adult pilot first (0007).
- **Needs:** one clear next action; control stays with the person; optional depth (ST-002, ST-005).
- **Pain points:** doomscrolling, lost routine, mental loops (`shanivar-founder-direction.md:21`).
- UNRESOLVED: international scope is in research (`SHANIVAR_STANCE.md:11`) but market focus is recorded as India. Which is the launch market?

## 5. User Stories
- As a person, I want to state when I usually wake and make room for sleep, so my day has two fixed points (`docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md:24`; wireframe README `:15-17`).
- As a person, I want a wake alarm I must move to dismiss, with an always-reachable accessible way out, so I am awake without being trapped (0003).
- As a person, I want to choose apps that lose access to me in set windows, and stop protection immediately if I need to (0014 Routine contract; `ENFORCEMENT_STANCE_2026-09-12.md:38-45`).
- As a person, I want short unpaced practices I can end at any time, and a record that states facts only (0011, 0013, `BreathingSessionState.kt:198-228`).
- As a person, I want my Sankalp line kept on my phone, word for word (`SankalpStore.kt:6-15`).
- As a visitor, I want to join an early-access list (website `index.html:261`).

## 6. Core Features
### Feature 1 — Routine (flag-gated, not live)
- **Purpose:** Day structure: wake/sleep anchors, editable personal activities (up to 12), optional wake alarm, multiple protection windows, readable timeline (0014; `android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md`).
- **Status:** Written on `main`; `ROUTINE_V3 = false` (`android-app/app/build.gradle.kts:33`). Real users get the older single Home. Physical-device QA (DEV-7) not run (`marketing/shanivar/routine-v3-onboarding/README.md:7`).
- **Acceptance criteria:** the gates in `ROUTINE_V3_TEST_GUIDE_2026-09-30.md` and `ENFORCEMENT_STANCE_2026-09-12.md` invariants 1-8.
- **Edge cases:** overnight windows (start-day convention), DST gaps, equal wake/sleep times, sensor missing (accessible dismissal) — test guide `:30-45`.
### Feature 2 — Wake alarm
- Movement default, QR, photo-motion, accessible (0003). 15-minute hard ring cap, silencing never gated on backend (`ENFORCEMENT_STANCE_2026-09-12.md:64-84`).
### Feature 3 — App protection
- Overlay + usage polling; no Device Admin/Accessibility (0002). Honest ceiling: a determined person can bypass (`ENFORCEMENT_STANCE_2026-09-12.md:19-29`). A brief flash of the blocked app is a known cost (0002).
### Feature 4 — Sadhana (Steady practices + first sitting)
- **Ideology:** Sadhana is the Sadhak path (ST-010, 0015): uncomfortable by nature; if you cannot cope, accept it and stop. A contemplative aim, never a treatment claim.
- 8 unpaced practices (0011), body-scan "first sitting" with Sankalp (internal test only, **not reviewed**, `research/deep-foundation/PRACTICE_SAFETY.md:55`). Numbers: `docs/sources/SADHANA_CALCULATIONS.md`. Progression is PROPOSED only (0013).
### Feature 5 — Waitlist website
- Netlify Forms early-access list; draft privacy notice pending legal review (`website/README.md:3`).

## 7. Non-Goals
No Device Admin or AccessibilityService (0002, permanent); no streaks, ranks, levels, XP, leaderboards, consciousness scoring (0009, ST-005, `docs/strategy/shanivar-launch-backlog.md` "Hold" rows); no medical/therapeutic claims (PRACTICE_SAFETY.md:49-51); no breath retention or intensive breathwork (PRACTICE_SAFETY.md); no AI coach or social comparison in V1 (0009); no iOS; astrology off the launch surface (ST-007); no family surveillance (backlog "Hold").

## 8. MVP Scope
- **Must have (per 0007/0014):** one editable alarm with repeat days and non-walking dismissal; protection windows; Routine shell; reviewed Steady practices.
- **Nice to have:** Hindi core copy (backlog "Initial" row); Sadhana history view (`shanivar-sadhana-layer-above-player-proposal.md:227`, PROPOSED).
- **Future:** Sadhana progression (0013 PROPOSED); multiple alarms, QR missions, astrology, AI reflection, payments (backlog "Later" rows).
- UNRESOLVED: is the first public release Routine V3 or the older Home? Flag is off and Play listing (DEV-17) is open.

## 9. Success Metrics
- Stated principle: measure confirmed usefulness, optional transfer and unwanted dependence separately; never score silence as success (ST-008, `SHANIVAR_STANCE.md:263`). Engagement-maximising metrics are rejected (0009).
- UNRESOLVED: no numeric targets, no analytics or crash reporting exist (`SHANIVAR_TECHNICAL_MAP.md:278`). What is the first measurable pilot outcome?

## 10. Constraints
- **Technical:** no release signing (DEV-17, `SHANIVAR_TECHNICAL_MAP.md:280`); no migration runner (DEV-9, `:274`); no CI (DEV-15, `:279`); debug builds only (`android-app/app/build.gradle.kts:35-42`); Android emulator not authorized, hardware only (`ROUTINE_V3_TEST_GUIDE_2026-09-30.md:3`).
- **Business:** founder approves spending, public release, external actions (`AGENTS.md:7`). Marketing paused (founder, 2026-10-06; `routine-onboarding-marketing-plan.md:3`).
- **Design:** `DESIGN_SYSTEM.md`; app design source of truth is `android-app/wireframes/initiation-v1/` (`index.html`, `HANDOFF.md`) per decision 0016 (accounts at launch, cloud sync, Living Orbit, 60-second practice; supersedes 0014 local-only). **Timeline:** UNRESOLVED: no dated plan; the marketing plan forbids publishing a launch date (`routine-onboarding-marketing-plan.md:253`).

## 11. Assumptions
Founder convictions (one consciousness, maya) are not product claims (`shanivar-founder-direction.md:13-17`). Religious resonance with parents is a hypothesis (`:27`). Meditation-before-insight improving receptivity is untested (`:44`).

## 12. Open Questions
See the top-10 list in the delivery report; also UNRESOLVED markers above and in each blueprint file.

## 13. Definition of Done
- [ ] Requirements implemented and match accepted decisions
- [ ] Critical flows pass (`TESTING.md`)
- [ ] `testDebugUnitTest`, `lintDebug`, `assembleDebug` pass; hardware checks run or recorded as NOT RUN
- [ ] Security (`SECURITY.md`) and practice-safety gates satisfied
- [ ] Docs updated in the same change

## Sadhana focus lock (13 Oct 2026, decision 0017)

During an active Sadhana practice, other apps are blocked and notification pop-ups silenced by default, using existing usage-access/overlay protection and DND access if granted. Calls, emergency and End are always available; End releases the lock instantly. Missing permissions are shown honestly. Users can turn the default off in Prepare.
