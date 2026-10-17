# AI Coding Agent Instructions — Shanivar (blueprint copy)

**Placement:** the discovered file is the repo root `AGENTS.md`; it points here. This file is the blueprint's AGENTS section and must not weaken the root rules. If they conflict, the root `AGENTS.md` wins and the conflict is reported.

## 1. Project Context
Project: Shanivar by The Hybrid Monk. Purpose: Routine + Sadhana Android app with a wake alarm and voluntary app protection (`PRD.md`). Primary users: see `PRD.md` §4. Roles and authority: root `AGENTS.md:5-11`. The founder alone accepts stance changes (`SHANIVAR_STANCE.md:5`).

## 2. Source of Truth
Before significant changes read, in order: `SHANIVAR_STANCE.md`, `docs/decisions/` (0014 first), then the relevant blueprint files: `PRD.md`, `DESIGN_SYSTEM.md`, `ARCHITECTURE.md`, `DATABASE.md`, `SECURITY.md`, `CODE_STYLE.md`, `TESTING.md`; plus `research/deep-foundation/PRACTICE_SAFETY.md` for any practice, `android-app/ENFORCEMENT_STANCE_2026-09-12.md` for alarm/lock, and the SOP for the task (`docs/sop/README.md`). Do not contradict them without first identifying the conflict.

## 3. General Rules
Read the existing files first; improve what exists; cite `file:line`; never invent; write `UNRESOLVED: <question>` instead of guessing. No secrets in files. No new dependencies or unrelated edits. Separate founder direction, accepted decisions, proposals, implemented, tested and public (root `AGENTS.md`, "Truth and safety").

## 4. Hard "never" list
No Device Admin / AccessibilityService (0002). No "impossible to bypass" language. No streaks, scores, ranks, XP, levels. No health, clinical or nervous-system claims. No unreviewed practice shipped publicly. No committing temporary flags (`ROUTINE_V3`). No push, deploy, post, send or spend without the founder's explicit word for that action. No RoboMQ Jira, ever (SOP-08).

## 5. Planning
Before a non-trivial task: read docs and code; list affected files, flags and tests; check whether a decision already closes the option (`docs/decisions/README.md:13-16`); write a short plan; stop on conflicts and ask one question.

## 6. Implementation
Focused changes, existing patterns, preserve behaviour, flag-gated where decision 0014 requires.

## 7. Validation
Run what applies (`TESTING.md` §7). Report unrun checks as NOT RUN. Review `git status` for unintended files. Other agents' uncommitted work in the tree is not yours: stage only your own paths.

## 8. Documentation
Update docs in the same change when behaviour, architecture, schema, security, numbers or copy change. Sadhana numbers: `docs/sources/SADHANA_CALCULATIONS.md`.

## 9. Conflict Handling
Do not guess. Name the two files and lines that disagree and ask. Known conflicts: `docs/README.md` "Known conflicts".

## 10. Completion Criteria
Requirements met; checks run and reported honestly; no unintended files; secrets absent; docs updated; commit authored as `Hybrid Monk <admin@thehybridmonk.com>` locally and not pushed unless the founder says push.
