# Complete available agent registry audit

Date: 2026-09-29. Owner: Staff Manager. Scope: repository role definitions and named historical assignments, not every agent ever used outside this workspace. Audit was conducted before this initiative's external research. A charter is an instruction document, not proof of a running process, account connection, recurring job, or verified capability.

## Search and evidence boundary

Inspected every Markdown registry/meeting/decision document immediately under `ai_org/`, all nine pre-existing `ai_org/agents/*.md` charters, and `ai_org/ontology/philosophy.md`. Searched repository Markdown for Staff Manager, agent registry, squad and numbered-agent definitions excluding dependency/build trees; inspected ownership in root `CLAUDE.md`, `android-app/ENFORCEMENT_STANCE_2026-09-12.md`, and references in `shanivar-vbt/app-shell/CODEX_HANDOFF.md`. No separate centralized runtime registry was established. No credentials, private account data or vendor files were needed.

The following tables enumerate named roles, with aliases consolidated only where supported. Capabilities describe mandates; historical test counts or delivery claims are not current verification. Inputs/outputs absent from terse squad entries are conservative operational interpretations, labelled by their relationship to the stated ownership.

## Standalone existing roles

Each evidence link points to the pre-existing charter; the five original roles also appear in [Phase 1 registry](../../ai_org/shanivaar_agents.md) and [bootstrap](../../ai_org/bootstrap.md).

| Role / alias | Mandate and capabilities | Inputs → outputs | Dependencies | Overlap with deep foundation |
|---|---|---|---|---|
| [Founder / Vision Keeper](../../ai_org/agents/founder.md) | Own direction, hypothesis and final decisions | Cohort/research findings → accepted direction, vetoes, next experiment | Human founder judgment; all reporting roles | Sole authority to accept proposed intellectual stance; agent must not impersonate acceptance |
| [Content & Practice Designer / Content Designer](../../ai_org/agents/content_designer.md) | Write short ordinary-life practices and 14-day cohort content | Approved concepts, voice, feedback → message/practice/check-in | Founder, source QA, User Researcher | Script collaborator; does not replace textual scholarship or safety review |
| [User Researcher](../../ai_org/agents/user_researcher.md) | Interview, observe behavior, distinguish intention from action | Consented concrete episodes/check-ins → observation, inference, one test, confidence | Founder visibility; actual participants | International seeker discovery; no invented audience evidence |
| [Delivery & Operations / Delivery Ops](../../ai_org/agents/delivery_ops.md) | Reliable opt-in delivery and response capture, deterministic safety routing | Approved messages, consent, actual delivery state → logs/escalations | Founder, delivery infrastructure | Later pilot execution; no new outbound authorization created here |
| [Learning Synthesizer](../../ai_org/agents/learning_synthesizer.md) | Turn cohort evidence into scorecard and one change | Completion and qualitative data → scorecard/one-variable recommendation | Delivery Ops, User Researcher | Experiment review; not general research strategy owner |
| [Web Developer](../../ai_org/agents/web_developer.md) | Responsive pages, accessibility, API adapters and browser checks | Product/campaign brief, API contract → working pages and verification | Codex UI lead, Claude backend, Social Media Manager | Later prototypes after scoped decision; research does not authorize redesign |
| [Social Media Manager](../../ai_org/agents/social_media_manager.md) | Editorial planning, claim checks, drafts and sourced performance review | Founder voice, real product status, analytics → campaign/review/writer brief | Creative Writer, Content Designer, Web Developer | NOTICE–QUESTION–PRACTICE tests; no fabricated testimonials or publication status |
| [Creative Writer](../../ai_org/agents/creative_writer.md) | Narrative, carousel progression, scripts and restrained voice craft | Bounded social brief, verified claims, approved learning → copy/art direction | Social Media Manager review; Content Designer | Compress depth without invented ancient attribution |
| [Market Fit Release Steward](../../ai_org/agents/market_fit_release_steward.md) | Keep audience/job/mechanism/evidence connected at release | Source state, cohort evidence → release learning log and test/revise/hold recommendation | User Researcher, Learning Synthesizer, product owners | Strategic countermetrics, independence vs abandonment; India-only audience assumption now broadened by founder |

## Existing coordination and implementation owners

| Role / alias | Mandate and capabilities | Inputs → outputs | Dependencies / mission overlap | Evidence |
|---|---|---|---|---|
| Staff Manager | Split bounded tasks, reconcile dependencies, independently verify reports, escalate genuine founder decisions | Squad reports/contracts → reconciled handoff, QA gates and synthesis | Exists across squads; now assigned coordinator of this research initiative, not newly invented | [Backend squad](../../ai_org/backend_squad.md), [VBT track](../../ai_org/shanivar_vbt_track.md), [Reflection squad](../../ai_org/reflection_scroller_squad.md), [Discipline squad](../../ai_org/discipline_engine_squad.md) |
| Product Owner | Deliver installable end-to-end prototype rather than isolated backend completeness | Current source/test/device evidence → punch list and delivery tracker | Claude/Codex/QA; later implementation acceptance, not scholarly authority | [Product Owner tracker](../../ai_org/product_owner_first_apk.md) |
| Product Manager / PM | Scope and sequence audience learning | Founder goals, friction, current offer → product recommendations/pilot worksheet | Founder and research; latest founder directions override historical alarm deferrals | [PM review](../../ai_org/product_manager_mvp_review_2026-09-13.md), [pilot worksheet](../../ai_org/shanivar_mvp_pilot_worksheet.md) |
| Project Manager / Project Management perspective | Release sequencing and enforcement stance | Release findings → readiness recommendations | Possibly same operational function as PM, but identity is not proven; retain explicit alias ambiguity | [Tester brainstorm](../../ai_org/release_brainstorm_20_testers_2026-09-12.md), [enforcement stance](../../android-app/ENFORCEMENT_STANCE_2026-09-12.md) |
| Codex / UI–UX lead / frontend owner | Native Compose surfaces and web direction under current task authority | Contracts/wireframes → UI and validation | Claude native/backend seams; not a continuously running named employee | [Web/social team](../../ai_org/shanivar_web_social_team.md), [working agreement](../../CLAUDE.md) |
| Claude / backend and native-service owner | Backend, persistence, native engines and integration contracts; some later tasks authorize end-to-end work | Scoped handoff → implementation/evidence | Codex surfaces, QA, founder scope | Same working agreement and squad files |

## Every named squad specialist

These are distinct assignments even where the title overlaps another squad. Reusing a specialist capability must preserve the source context.

| Squad / role | Mandate and capabilities | Inputs → outputs | Dependencies and overlap |
|---|---|---|---|
| [VBT: Content / Research Agent](../../ai_org/shanivar_vbt_track.md) | Select Level-1 VBT material, script and cite | Translation passages → 12-technique catalogue/scripts | QA and assessment; narrow ancestor of Ancient Knowledge and Embodiment roles |
| VBT: Personalization / Assessment Agent | Intake/schema/scoring/routing | Finished technique inventory and intake → routing algorithm | Content and App/UI; personalization proposals need current agency/comfort rules |
| VBT: Discipline Engine Agent | Research Android enforcement constraints before implementation | Platform docs/product rules → feasibility and module architecture | App/UI/native owners; technical feasibility reviewer for Focus |
| VBT: App/UI Agent | Shell architecture, onboarding/player functional contract | Techniques/routing → Codex handoff and creative direction | Codex builds actual UI; not full research UX ownership |
| VBT: QA / Scriptural Integrity Agent | Verify named verse/edition and block unverifiable content | Scripts/citations → technique pass/fail | Content and Staff Manager; clear overlap with Provenance role, narrower historical scope |
| [Backend: Dev 1 Foundation & Auth](../../ai_org/backend_squad.md) | Auth/security/config/database foundations | Existing routes/findings → fixes | QA/Staff Manager; later privacy and identity implementation |
| Backend: Dev 2 Core Practice Loop | Practices, journey, gamification, habits/journal | Domain contracts → route/engine fixes | QA; historical gamification does not authorize spiritual ranks |
| Backend: Dev 3 Astrology / AI / Insights | External generation, chart/insight/analytics errors | Provider contracts and computed data → integration fixes | QA; separate calculations, interpretations and generated reflections |
| Backend: Dev 4 Community / Commerce / Integrations | Community, content, billing, commerce, WhatsApp | Domain contracts/schema → fixes | QA; deferred capabilities stay deferred |
| Backend: QA Agent | Jest/mock harness and consolidated bug list | Implementations + actual environment → test evidence/unverified list | Four dev agents; mocked success cannot prove live or device behavior |
| Backend: dedicated astronomical-calculation specialist (unnamed) | Research and integrate real calculated chart data | Astronomical library/reference positions → integration and reference verification | Dev 3/Staff Manager; historical bounded assignment, no standalone charter |
| [Reflection: Agent 1 Creative / Audience Psychology](../../ai_org/reflection_scroller_squad.md) | Engagement psychology translated into respectful card writing | Audience stance → examples and response-option guidelines | Generation/UI owners; overlapping creative/psychology remit is not broad clinical research |
| Reflection: Agent 2 Backend Data / API | Migrations, sessions/cards/responses, idempotency/isolation | API spec → routes/schema | Generation service and native client |
| Reflection: Agent 3 Backend Generation Service | Context snapshots, computed aspects, schema validation, fallbacks | Data contract and voice guidelines → generation service | Data/API and creative; no inference of psychological truth from charts |
| [Discipline: Agent A Backend API](../../ai_org/discipline_engine_squad.md) | Preference/session/verification persistence | Discipline contract → routes/state records | Native engine; session record is not proof of lived benefit |
| Discipline: Agent B Native Kotlin Engine | Enforcement, alarm scheduling and dismissal implementation | Platform research/API → native module | Backend API, Codex and physical QA; old QR directions superseded by movement direction |

The family-testing meeting uses **Backend & Content squad**, **Reflection Scroller squad**, **Discipline Engine squad**, **Codex**, and **QA** as reporting groups, not five additional unique standalone agents. The tester brainstorm names **Backend Architecture** and **QA / Release Risk** perspectives alongside **Project Management** and **User Research**; these are historical review assignments with findings/readiness outputs and dependencies on implementation evidence, not separate installed workers. “Research lead” in the release learning log is an attribution without a recoverable standalone charter; do not invent its identity or merge it with an existing role.

## Gaps and decisions

The nine existing standalone charters contain no dedicated role for any of the seven requested research specialties. Existing Content Research and Scriptural Integrity partially overlap but lack the requested cross-tradition, bibliographic and manuscript-disagreement remit. Existing User Researcher observes people; it does not replace cognitive-science synthesis. Existing Creative/Audience Psychology and App/UI do not cover the full UX research program. No Sadhak Journey owner exists.

Created dedicated, reusable charters for [Ancient Knowledge](../../ai_org/agents/ancient_knowledge_researcher.md), [Textual Provenance](../../ai_org/agents/textual_provenance_researcher.md), [Manovigyana / Human Psychology](../../ai_org/agents/manovigyana_psychology_researcher.md), [Embodiment / Practice Translation](../../ai_org/agents/embodiment_practice_translator.md), [UX Psychology / Behavioral Architecture](../../ai_org/agents/ux_behavioral_architecture_researcher.md), [High-Scale Product Patterns](../../ai_org/agents/product_pattern_researcher.md), and [Sadhak Journey](../../ai_org/agents/sadhak_journey_researcher.md). Formalized existing [Staff Manager](../../ai_org/agents/staff_manager.md) ownership. This creates instructions, not seven persistent processes; bounded invocation and review are described in [research organization](RESEARCH_ORGANIZATION.md).

## Conflicts requiring explicit treatment

1. The legacy [ontology](../../ai_org/ontology/philosophy.md) calls itself ultimate authority and prescribes six consciousness levels plus neurological analogies. The latest founder brief rejects status hierarchies and false scientific equivalence. Preserve it as historical material; do not import its ladder, heavy box-breathing default or required neuroscience analogies into the new stance.
2. Bootstrap's five-role limit is a historical cohort constraint. The founder explicitly authorizes this expanded research organization; no additional approval is required to create the seven charters.
3. India-only market focus is broadened for research by the international-seeker direction. Existing adult/family learning remains relevant; neither geography nor generation proves demand.
4. Historical content/publication approvals do not imply clinical safety, complete textual review, or current shipping status. Steady release evidence and current source outrank older “pending implementation” marketing text. This audit does not itself certify release.
5. A researcher cannot accept its own intellectual proposal for the founder, contact people, publish, deploy, or redesign the app merely because a role charter exists. Research drafts remain proposed; the explicit current brief authorizes research and local artifacts.

Audit result: registry inventory complete within the stated repository boundary. Existing roles preserved. Seven specialist gaps identified and assigned. External research may proceed with provenance and review boundaries intact.
