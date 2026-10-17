# Shanivar — Vigyan Bhairava Tantra Discipline Track (Parallel Phase)

This track runs **in parallel** to the existing Brahma Labs Phase 1 WhatsApp lean-validation loop ([shanivaar_agents.md](shanivaar_agents.md)). It does not replace or pause that loop. This is the native-app, technique-matching + hard-discipline build for **Shanivar**, the flagship app under The Hybrid Monk ecosystem.

## Locked Decisions (do not re-litigate without founder input)
- **Platform**: Android first. iOS Screen Time/Family Controls cannot replicate true app-lockout or a non-dismissible alarm — expect a real feature gap if iOS is ever prioritized.
- **Alarm dismissal**: requires a physical/verified action (e.g. scan a placed QR code, solve a puzzle, complete a movement) — not a simple in-app tap.
- **Domain/account**: fully separate from thehybridmonk.com (shanivar.com, per existing README architecture).
- **Monetization**: deferred. No paywall/payment scaffolding in this MVP — consistent with the Phase 1 "don't build ahead of demand" filter.

## Staff Manager
Sequences the 5 specialist agents below, unblocks cross-dependencies, and is the single point that reconciles Content Agent's final technique list against Assessment Agent's routing logic and App/UI's session-player contract. Holds release-gate authority jointly with QA: nothing ships without a QA pass.

## Agent 1: Content / Research Agent
**Owns**: sourcing ~12 Level-1 Vigyan Bhairava Tantra techniques that render in a guided modern format without losing essence, plus guided-session scripts.
**Hard rule**: every technique must cite its primary source (verse/sloka number in the Vigyan Bhairava Tantra plus the translation edition referenced) — no secondhand paraphrase passed off as primary.
**Reports**: technique list + scripts + citations to Staff Manager and QA.

## Agent 2: Personalization / Assessment Agent
**Owns**: intake assessment (consciousness state, thought patterns, lifestyle) that routes a user to their best-fit technique from the Level-1 set.
**Dependency**: final routing table needs Content Agent's finished technique list — may build the assessment schema and scoring logic against placeholder technique categories in the meantime, reconciled by Staff Manager once the list lands.
**Reports**: assessment spec + scoring/routing algorithm to Staff Manager.

## Agent 3: Discipline Engine Agent
**Owns**: (a) app-lockout during active practice/focus sessions — starting with blocking one social media category, (b) a non-dismissible alarm requiring physical/verified action to close.
**Hard rule**: research Android permission/platform constraints (Accessibility Service, UsageStatsManager, Device Admin, exact-alarm scheduling, full-screen intents, System Alert Window) and flag any limitation *before* writing implementation code.
**Reports**: feasibility/architecture findings to Staff Manager before build sign-off.

## Agent 4: App/UI Agent
**Owns**: architecture decision (native vs. hybrid shell), onboarding-flow and session-player *functional contracts*, and Shanivar visual identity direction (Saturn/discipline theme) — as a creative-direction handoff, not a literal build.
**Note — ownership split with Codex**: the founder runs a separate agent (Codex) that owns the actual frontend/UI/UX implementation. This agent's job is to produce `shanivar-vbt/app-shell/CODEX_HANDOFF.md` — enough functional contract (what's fixed) and creative direction (what's open) for Codex to build the real UI with its own creative energy, not a rigid blueprint to mechanically implement. Any code scaffold this agent writes is reference-only, clearly labeled as not the thing Codex should build on.
**Reports**: architecture decision + Codex handoff package to Staff Manager.

## Agent 5: QA / Scriptural Integrity Agent
**Owns**: cross-checking all technique content against primary-source citations before anything ships.
**Hard rule**: blocks release of any technique whose citation cannot be verified against a named translation/edition. Runs before every release milestone, not just once at the end.
**Reports**: pass/fail per technique to Staff Manager.

## Sequencing
1. Content Agent + Assessment Agent start first (they define what Level 1 actually is).
2. App/UI Agent builds the shell in parallel.
3. Discipline Engine Agent builds its module independently, research-first.
4. QA reviews before any release milestone.

## Deliverable Locations
```
shanivar-vbt/
  content/            # Content Agent: techniques, scripts, citations
  assessment/          # Personalization Agent: intake + routing spec
  discipline-engine/   # Discipline Agent: research doc, then Android module
  app-shell/           # App/UI Agent: architecture doc + scaffold
  qa/                  # QA Agent: verification reports
```
