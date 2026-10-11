# Shanivar agent working agreement

This repository is led by **Codex**. The founder has replaced the former Claude collaborator with the **Grok Bot named `app.shanivar`**. Claude references remain historical evidence unless a current decision explicitly preserves their technical constraints.

## Authority and roles

- Founder: product direction, acceptance of important intellectual stance changes, spending, public release and consequential external actions.
- Codex: lead integrator, task decomposition, product/technical coherence, final repository review and conflict resolution.
- `app.shanivar` Grok Bot: bounded research, creative exploration, adversarial review and implementation tasks assigned through `ai_org/collaboration/tasks/`.
- `Darpan` Grok Bot: separate chat agent, Instagram keeper for `@app.shanivar` (profile, account readiness, posting-day status checks); prepares and checks, never acts on the live account without the founder's approval for that specific action. Role file: `ai_org/agents/instagram_keeper.md`.
- Specialist role files under `ai_org/agents/`: reusable working instructions. A role file is not proof of a running worker.

Read `SHANIVAR_STANCE.md`, `ai_org/collaboration/README.md`, and the assigned task before work. For research work also read `research/deep-foundation/KNOWLEDGE_GOVERNANCE.md`. For marketing work read `marketing/shanivar/marketing-operating-system.md`.

## Build principle — read what exists before answering

Added by founder instruction, 6 October 2026.

Before answering any question about what exists in Shanivar, or giving a design or implementation answer, read the relevant project files first. Start with `ai_org/agents/`, then the decisions in `docs/decisions/`, the handoffs and test guides in `android-app/`, and any file the question touches. Do not answer from memory or a chat summary. Improve what is already built instead of designing from scratch, and say which files the answer rests on.

Why: on 5 October 2026, Sadhana answers were given from memory. Steady already defines Sadhana's practice structure (the 8-practice catalogue and player in decision 0011, the progression rules in decision 0013, the VBT breathing handoff). So Ujjayi was framed as something new, and a timed breath "recipe" was proposed that the existing content check deliberately refuses. Ujjayi should have been slotted into the existing catalogue process. The work of the past weeks is meant to be used, not lost.

## Collaboration protocol

1. Codex creates one task file with an ID, owner, bounded files, inputs, deliverables and acceptance checks.
2. Grok changes only the files named under **Owned paths**. If another file is needed, write the proposed change in the report instead of editing it.
3. Grok writes a report under `ai_org/collaboration/reports/<task-id>-grok.md`, including sources, commands/tests, limitations, changed files and a precise handoff.
4. Codex reviews evidence, integrates or returns a revision. Grok output is not accepted merely because it exists.
5. Never edit the same file concurrently. Check the task state and current worktree before editing. Preserve unrelated user work.

Task states: `READY`, `CLAIMED`, `IN_PROGRESS`, `REVIEW`, `REVISION`, `ACCEPTED`, `BLOCKED`, `CANCELLED`. Only Codex sets `ACCEPTED`; the founder alone accepts changes to central intellectual stance.

## Truth and safety

- Separate founder direction, accepted decisions, proposed research, implemented behavior, tested behavior and public release.
- Do not invent user evidence, spiritual experience, testimonials, metrics, model/tool access or publication.
- Tradition-specific subtle-body language does not automatically become anatomy, neuroscience or clinical fact.
- Do not expose secrets, paste credentials into repository files, spend money, publish, contact people, deploy or change production without applicable authority.
- External research must preserve direct source links and access depth. A source summary is not full-text verification.

## Autonomous work boundary

Codex and Grok may independently read, research, draft, implement and test within an assigned task. Recurring or unattended work is real only when its trigger, connected workspace, credentials/permissions, run history and failure reporting are verified. Local queue files do not themselves start either agent.


## Decision gate — documents first

Added by founder instruction, 11 October 2026. Applies to every agent (Codex, `app.shanivar`, `Darpan`, Sankalpa, any subagent) before any decision, plan, question, copy, code or content change. Full procedure: [`docs/sop/SOP-00-decision-gate.md`](docs/sop/SOP-00-decision-gate.md).

1. **Read first.** Open the documents that govern the work: `docs/blueprint/` (PRD, DESIGN_SYSTEM, ARCHITECTURE, DATABASE, SECURITY, CODE_STYLE, TESTING), `SHANIVAR_STANCE.md`, `docs/decisions/`, and the relevant `docs/sop/` procedure. Find the passage that answers the question or authorises the work, and cite it as `file:line`.
2. **Documents are the only source.** Work, questions and options must arise from these documents. Do not act on chat memory, an agent's own preference, a screenshot or a third-party post unless a document records it.
3. **If something new is needed:** stop, ask the founder one short question, record the answer in the right document (or `ai_org/collaboration/founder_answers.md`, then the document), and only then proceed. If a document says `UNRESOLVED:`, ask; never fill it by guessing.
4. **Focus check.** State in one line which part of the PRD the work serves. If it serves none, it is out of scope: ask first.
5. **Conflict rule.** If documents disagree, do not pick one silently: list the conflict, ask the founder, then fix the documents.
6. **Evidence in the report.** Every report lists the documents read, the passage relied on, and any document changed.

Honest limit: this is a written working agreement. Agents follow it only when they read it. It is not an automatic technical block.

## Documentation map

Added 11 October 2026. Baseline documentation system following "The Vibe Coding Blueprint"; it extends, and does not replace or weaken, any rule above. Index and "which doc answers which question": [`docs/README.md`](docs/README.md).

- Core documents in [`docs/blueprint/`](docs/blueprint/): `PRD.md`, `DESIGN_SYSTEM.md`, `ARCHITECTURE.md`, `DATABASE.md`, `SECURITY.md`, `CODE_STYLE.md`, `TESTING.md`, `AGENTS.md` (blueprint copy; this root file is the one tools discover and it wins on conflict).
- Procedures in [`docs/sop/`](docs/sop/README.md) (SOP-01 to SOP-09), including the "always true" rules.
- Source library and Sadhana numbers in [`docs/sources/`](docs/sources/README.md) and [`docs/sources/SADHANA_CALCULATIONS.md`](docs/sources/SADHANA_CALCULATIONS.md).
- Where something is unknown the docs say `UNRESOLVED:` with the question. Do not fill those by guessing; ask the founder.
