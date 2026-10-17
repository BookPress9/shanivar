# Shanivar documentation index

Baseline documentation system (11 October 2026), following "The Vibe Coding Blueprint". Written from the repo as it is; unknowns are marked `UNRESOLVED:`. Agents start at the root [`AGENTS.md`](../AGENTS.md) and [`SHANIVAR_STANCE.md`](../SHANIVAR_STANCE.md).

## Which doc answers which question
| Question | Read |
|---|---|
| What are we building, for whom, what is out of scope? | [blueprint/PRD.md](blueprint/PRD.md) |
| What does it look and sound like? Tagline, voice, colours | [blueprint/DESIGN_SYSTEM.md](blueprint/DESIGN_SYSTEM.md) |
| How is the system put together? Stack, flags, failure cases | [blueprint/ARCHITECTURE.md](blueprint/ARCHITECTURE.md) |
| What data exists server-side? Migrations? | [blueprint/DATABASE.md](blueprint/DATABASE.md) |
| What must be protected? Secrets, permissions, privacy | [blueprint/SECURITY.md](blueprint/SECURITY.md) |
| How is code written here? | [blueprint/CODE_STYLE.md](blueprint/CODE_STYLE.md) |
| How do we know it works? Commands, gaps | [blueprint/TESTING.md](blueprint/TESTING.md) |
| How should an agent behave? | [blueprint/AGENTS.md](blueprint/AGENTS.md) and root `AGENTS.md` |
| How do I build and ship a test APK? | [sop/SOP-01](sop/SOP-01-app-build-and-test-apk-release.md) |
| How do I change or deploy the website? | [sop/SOP-02](sop/SOP-02-website-change-and-deploy.md) |
| How will app.shanivar.com be deployed? | [sop/SOP-03](sop/SOP-03-web-app-deploy.md) |
| May this practice or script ship? | [sop/SOP-04](sop/SOP-04-practice-content-change-and-safety-review.md), `research/deep-foundation/PRACTICE_SAFETY.md` |
| May we say this publicly? | [sop/SOP-05](sop/SOP-05-marketing-and-claims-review.md) |
| How do we run Instagram with Darpan? | [sop/SOP-06](sop/SOP-06-instagram-operations-with-darpan.md) |
| Who may change the stance or a decision? | [sop/SOP-07](sop/SOP-07-decisions-and-stance-changes.md) |
| Which Jira, when may I push? | [sop/SOP-08](sop/SOP-08-jira-and-git-hygiene.md) |
| What do we collect, how do we delete? | [sop/SOP-09](sop/SOP-09-data-and-privacy.md) |
| The rules that are always true | [sop/README.md](sop/README.md) |
| Which books does the founder draw on, and how far can we trust them? | [sources/README.md](sources/README.md) |
| What numbers and rules are in Sadhana, and why? | [sources/SADHANA_CALCULATIONS.md](sources/SADHANA_CALCULATIONS.md) |
| Why was something decided / rejected? | [decisions/README.md](decisions/README.md) |
| What is the founder's intent and sequencing? | [strategy/](strategy/): `shanivar-founder-direction.md`, `shanivar-tone-and-values.md`, `shanivar-launch-backlog.md`, `shanivar-sadhana-layer-above-player-proposal.md`, `shanivar-sankalp-and-sittings-initiation-proposal.md` |
| What is the current intellectual stance? | [../SHANIVAR_STANCE.md](../SHANIVAR_STANCE.md) (ST-009 accepted; others PROPOSED) |
| Alarm and lock safety rules | [../android-app/ENFORCEMENT_STANCE_2026-09-12.md](../android-app/ENFORCEMENT_STANCE_2026-09-12.md), decisions 0002, 0003 |
| Routine V3 tests and device QA | [../android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md](../android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md), `ROUTINE_V3_DEVICE_QA_2026-10-05.md` |
| V3 product reference (wireframe) | [../android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md](../android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md) |
| Technical debt and system map | [../SHANIVAR_TECHNICAL_MAP.md](../SHANIVAR_TECHNICAL_MAP.md) |
| Roles of agents | `../ai_org/agents/` |
| Marketing plan, Instagram drafts, website | `../marketing/shanivar/routine-v3-onboarding/` |

## Decisions (0001-0014)
Index with status: [decisions/README.md](decisions/README.md#index). Accepted product form: [0014](decisions/0014-routine-sadhana-shell-and-routine-v3.md). Still PROPOSED: [0013](decisions/0013-sadhana-progression-and-the-gamification-question.md).

## Known conflicts and stale statements (found while writing)
1. **API base URL:** `android-app/app/build.gradle.kts:16` defaults to `https://api.thehybridmonk.com/api`; `SHANIVAR_TECHNICAL_MAP.md:280` says that host was NXDOMAIN; `DEVICE_DISTRIBUTION.md` builds against `https://hybridmonk-api.onrender.com/api`.
2. **Domain state:** `website/README.md:5` and the marketing plan say shanivar.com is on parking nameservers; the founder-supplied DNS (A @ 75.2.60.5, CNAME www) points at Netlify. Repo text is stale.
3. **Default practice length:** `shanivar-sadhana-layer-above-player-proposal.md:30-33` says 60 s; code is 180 s (`BreathingSessionOwner.kt:16`). Also the wireframe README `:87` describes a one-minute Steady practice, which can never count under the 90 s rule.
4. **Table count:** decision 0001 says 10 tables; `SHANIVAR_TECHNICAL_MAP.md:274` counts 44 inline DDL statements; the base schema file `database/schema.sql` referenced by `backend/scripts/setup-supabase.js:95` is not in the repo.
5. **Repo naming:** 0005 says `hybridmonk-backend` is retired; `backend/package.json` is still named `hybridmonk-backend`.
6. **Enforcement invariant 8** (escape requires standing up) vs decision 0007 (non-walking dismissal must not require standing); already flagged in `ENFORCEMENT_STANCE_2026-09-12.md:51`.
7. **Marketing pause** is described as "not yet recorded in a repo file" (`routine-onboarding-marketing-plan.md:3`); SOP-05 and Darpan require a record before unpausing.
8. **Decision 0013** is PROPOSED yet the Sadhana tab already ships the sitting and last-practised label under 0013's wording; 0014 says progression is Phase 2.
9. **Decisions index** line for 0011 renders a `?` instead of a dash (`decisions/README.md:55`), cosmetic.
10. **Jira DEV-9 and DEV-15** are in the founder brief but not named in any repo file; the gaps themselves are recorded in `SHANIVAR_TECHNICAL_MAP.md:274,279`.

## Keeping this alive
Update the relevant doc in the same change as the behaviour. Do not create duplicate rules; link instead. Stale or giant files are worse than short true ones.
