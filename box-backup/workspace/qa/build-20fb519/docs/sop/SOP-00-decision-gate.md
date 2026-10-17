# SOP-00 Decision gate: documents first

**Status:** founder-instructed 11 October 2026. **Applies to:** every agent, every task.

## Purpose
Keep every decision, question and change tied to the baseline documents, so work does not drift from the product focus.

## Owner
Founder sets and changes the documents. Codex reviews. Each agent runs this gate itself before acting.

## Preconditions
Read `AGENTS.md` (Decision gate), `docs/README.md` (which doc answers which question) and `docs/blueprint/PRD.md`.

## Steps
1. **Name the task** in one line.
2. **Find the governing documents** using `docs/README.md`. Read them. Note `file:line` for the rule or decision that applies.
3. **Focus check.** Which PRD goal or non-goal does this serve? If none, stop and ask.
4. **Check for `UNRESOLVED:`** in the documents you read. If your work touches one, stop and ask the founder, one short question.
5. **New information needed?** Ask, record the answer in the correct document (use `ai_org/collaboration/founder_answers.md` first if unsure), commit the document change, then proceed.
6. **Conflicting documents?** List both passages, ask the founder, then fix the documents.
7. **Do the work** only inside what the documents allow.
8. **Report** with: documents read, passages relied on, documents changed, and anything left `UNRESOLVED:`.

## Approval gates
Founder approval is still required for stance changes, spending, publishing, deploying, pushing and any external action.

## Rollback
Revert the document commit and the dependent work together. Never leave work that depends on a reverted document.

## Evidence
`AGENTS.md` Decision gate section; the report in each task under `ai_org/collaboration/reports/`.

## Honest limit
This is a written agreement, not an automatic technical block. It works when agents read it and the founder spot-checks reports for the "documents read" line.
