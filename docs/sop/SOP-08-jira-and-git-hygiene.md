# SOP-08 — Jira and git hygiene

**Purpose:** Right tracker, clean history, nothing leaves the machine unasked.
**Owner:** Codex (integration); founder authorises pushes and external actions.

## Preconditions
- **Shanivar Jira only:** https://shanivar.atlassian.net, project **DEV** (`routine-onboarding-marketing-plan.md:279`). **Never RoboMQ** Jira, projects or Confluence. Check the site URL before any Jira action; `.mcp.json` points at the Atlassian MCP generally, so the site must be named.
- Repo `origin` is `https://github.com/BookPress9/shanivar.git`; default branch `main`.

## Steps
1. Before editing: `git status`. Other agents' uncommitted changes may be present; never `git add -A`; add only your own paths.
2. Commit locally in small logical commits with an explicit author when asked: `Hybrid Monk <admin@thehybridmonk.com>`. Message states what and why. No secrets; no temporary flags (`ROUTINE_V3`).
3. **Push only when the founder says push**, naming the push. Never force-push or rewrite pushed history without confirmation. No deploy as a side effect of a commit.
4. Jira: read freely (DEV-7 device QA, DEV-9 no migration runner, DEV-15 no CI, DEV-17 release signing/Play). Creating, editing, commenting, transitioning tickets needs the founder's explicit ask. (DEV-9 and DEV-15 come from the founder brief; the repo mentions DEV-7 and DEV-17 and records the same gaps in `SHANIVAR_TECHNICAL_MAP.md:274,279`.)
5. Reference ticket IDs in commits when relevant.

## Approval gates
Founder: push, release, ticket creation/edit, branch protection, history rewrites.

## Rollback
Unpushed: `git revert` (preferred) or reset only with founder confirmation. Pushed: revert commit, never rewrite.

## Evidence / links
`decisions/0005-single-repo-shanivar.md`, `AGENTS.md`, `marketing/.../README.md:7`.
