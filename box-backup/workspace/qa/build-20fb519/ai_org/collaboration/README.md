# Codex ↔ app.shanivar collaboration

Effective 4 October 2026. Codex leads the project; Grok Bot `app.shanivar` is the persistent collaborator replacing Claude for new work. This folder is the auditable interchange between the two systems.

## Workflow

```text
Founder direction
      ↓
Codex: frame task + protected boundaries
      ↓
tasks/<ID>.md  →  app.shanivar claims and works
      ↓
reports/<ID>-grok.md + owned artifacts
      ↓
Codex: verify, integrate, revise or accept
      ↓
current product / research / marketing handoff
```

Codex assigns one outcome, not an open-ended persona prompt. Grok reads the repository authorities named in the task, records what it actually accessed, and returns reviewable artifacts. The agents challenge each other's claims through evidence; they do not average conflicting answers.

## Transport state

`app.shanivar` runs on Grok Bot's persistent cloud computer. The Windows desktop application is installed and signed in, but the inspected installation exposes no local CLI, MCP endpoint, listening port or xAI API credential for Codex to submit messages programmatically. Its registered `grokbot:` URL protocol is not documented as a prompt-submission interface, so it is not used for automation.

The supported bridge is a shared Git repository or a file handoff into Grok Bot. Connect `app.shanivar` to `https://github.com/BookPress9/shanivar` and give it the bootstrap message in `GROK_BOOTSTRAP.md`. Until the current local changes are committed and pushed by an authorized repository workflow, the bot will only see the remote repository's last pushed state. Do not claim the first assignment has started until its report or Grok conversation confirms it.

After the bot can read the repository, ask it to save the queue-check workflow as a routine only after one supervised task completes correctly. A useful routine checks `ai_org/collaboration/tasks/` for tasks assigned to `app.shanivar`, ignores terminal states, claims exactly one task, completes only owned paths, and writes the report. Its run history and failure state must remain visible.

## Collision rules

- One writer per owned path.
- Grok never edits `SHANIVAR_STANCE.md`, decision status, central ledgers or Codex-owned integration files unless its task explicitly grants that path.
- Codex does not alter a Grok-owned path while its task is `CLAIMED` or `IN_PROGRESS`, except to stop unsafe work.
- Both agents may read the entire repository and report conflicts.
- Commit references are preferred for handoffs. If work remains uncommitted, list exact paths and hashes.

## Current evidence

- Official Grok Bot documentation says bots work on persistent cloud computers, retain context, coordinate, and can save recurring routines.
- Official Grok tooling documentation says Grok reads `AGENTS.md` and Claude-compatible instruction files.
- Official OpenAI documentation supports durable agents, multi-agent orchestration and MCP connections, but this active Codex session has no callable Grok connector.
- Therefore the repository queue is configured, while automatic cross-product message delivery remains pending the one-time Grok repository connection.

