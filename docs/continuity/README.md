# Daily work and resumption

Founder direction, 17 October 2026: keep daily changes, pending items and next actions in private GitHub
so another person/agent can resume. This authorizes task-branch commits/pushes and daily status publication.
Main merges, production, public release, spending and agent messages retain existing gates.

## Read first

1. [Current handoff](../HANDOFF_NEXT.md): branch, owners, tests and ordered actions.
2. [Daily checkpoint](https://github.com/BookPress9/shanivar/tree/codex/daily-status): branch SHAs and unpublished-work warnings.
3. [Agent-box archive](https://github.com/BookPress9/shanivar/blob/box-backup/SHANIVAR_STATUS.md): cloud artifacts and selective restore.
4. [Access matrix](ACCESS.md) and [integration report](../../ai_org/collaboration/reports/CONT-001-codex.md).

## Every working session

- Fetch; read governing decisions, assigned task and latest handoff. One writer per owned path.
- Isolate dirty checkouts in a new worktree. Never discard, overwrite or bulk-stage another worker's files.
- Before ending, update docs/HANDOFF_NEXT.md and a dated note here: task/owner/state, branch/tested SHA,
  changed paths, commands/results, NOT RUN checks, blockers/dependencies and exact next action.
  Distinguish implemented, tested, accepted, merged and released.
- Stage explicit owned paths, inspect git diff --cached, commit as Hybrid Monk <admin@thehybridmonk.com>,
  and push the task branch. Verify git ls-remote origin refs/heads/<branch> equals git rev-parse HEAD.
- Run `node scripts/continuity/daily-status.mjs --publish`.
  A metadata checkpoint does not substitute for pushing code or back up dirty files.
- Future agents should include this protocol in their assigned task's acceptance checks.

## Daily Windows job

The installer registers **Shanivar-Daily-GitHub-Status** at **23:30 Berlin time** using Windows'
W. Europe Standard Time DST rules. It runs as the current interactive user without elevation or saved password.
StartWhenAvailable handles missed starts. Computer on, user logged in, working Git credentials and network are required.

The job fetches origin and publishes only generated metadata to codex/daily-status through a temporary Git index
and commit tree. No checkout, stage-all, code backup, worker-branch push, main update or archive rewrite.
History is append-only; concurrent updates reject rather than force-push. Other computers are visible only through pushed refs.

```powershell
# Install once; refuses to replace an existing task
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/continuity/install-daily-task.ps1
# Run and inspect
Start-ScheduledTask -TaskName Shanivar-Daily-GitHub-Status
Get-ScheduledTaskInfo -TaskName Shanivar-Daily-GitHub-Status
$logDir = git rev-parse --git-path continuity
Get-Content (Join-Path $logDir runs.jsonl) -Tail 5
# Remove only this job if no longer wanted
Unregister-ScheduledTask -TaskName Shanivar-Daily-GitHub-Status -Confirm:$false
```

Nonzero LastTaskResult and ok:false in runs.jsonl report local failure. The remote checkpoint timestamp exposes staleness.
This Windows job sends no external failure message. The founder's separate agent-box backup/notification claim is unverified here.
Do not call the Windows job active until installation and a task run are verified in the report.

## Restore accurately

Clone/fetch all branches and tags. Resume the handoff branch, not main by habit.
Inspect each WIP ref and parent before restoring: do not assume a single patch on today's ticket tip.
An exact snapshot can be opened in a fresh detached worktree at its SHA. Preserve originals until reviewed.
The old box restore example references DEV-45/47 WIP refs absent from the fetched refs; those changes are now on normal branches.

DEV-54 remains ambiguous: d1d482c versus a2a9e23. The latter includes DEV-45 follow-ups; seven sync/test files differ.
Do not select by branch name alone. Restore only required campaign/docs/QA artifacts from box-backup/workspace;
never copy all secrets/home directories into a working branch. Recreate credentials through owning services.
APKs are rebuildable; excluded large videos and agent internal state require original storage.
