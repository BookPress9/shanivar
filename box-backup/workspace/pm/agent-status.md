# Shanivar agent status (hand-maintained)

Last updated: 2026-10-17 22:49 (Europe/Berlin, CEST). Edit this file when assignments change; `/workspace/bin/snapshot-to-github.sh` copies it into SHANIVAR_STATUS.md.

## Who is working on what right now

| Agent | Current work |
|---|---|
| Shanivar Frontend | DEV-89 (breathing safety, worktree `/workspace/shanivar-v3-dev89`) plus DEV-47 follow-ups (`/workspace/shanivar-v3-dev47`) |
| Shanivar Backend | DEV-54 (sadhana record schema, `/workspace/shanivar-v3-dev54`) plus the DEV-45 patch (`/workspace/shanivar-v3-dev45`): retry limit / park on persistent 404, no "Synced" while parked, flushNow / reset / sessionLost |
| Shanivar Tester | DEV-82, then the alignment review, then re-QA of DEV-45 and DEV-47 (at b502776) |
| Shanivar Customer Success | Market research (`/workspace/shanivar-cs`) |
| Sana (marketing) | Rewriting the reels per decision F-07: Shani/Saturn framing is OK, no classical texts or teachers (`/workspace/campaign`) |
| Shanivar PM | Emulator setup CANCELLED (17 Oct, 22:54); emulator files are not backed up |

## Check at end (in-progress items)

- [ ] DEV-89: Frontend changes in dev89 are uncommitted (BlockOverlayActivity, BreathingSessionState, BreathingSurfaceSafetyTest); commit and push
- [ ] DEV-47 follow-ups: uncommitted changes in dev47 on top of b502776; commit, push, hand to Tester
- [ ] DEV-45 patch (retry limit / park on 404 / no "Synced" while parked / flushNow, reset, sessionLost): uncommitted in dev45 incl. new SyncFollowupsTest; commit, push, re-QA
- [ ] DEV-54: Backend schema work; confirm committed and pushed
- [ ] DEV-82: Tester result
- [ ] Alignment review (`/workspace/shanivar-cs/alignment-review.md`, `/workspace/handoffs/frontend-thesis-alignment-2026-10-17.md`): outcome recorded
- [ ] Re-QA DEV-45 and DEV-47 (b502776): pass/fail recorded in Jira
- [ ] Sana: reels 01-12 rewritten per F-07 in `/workspace/campaign/md` and Notion; nothing published
- [ ] CS market research: final version in `/workspace/shanivar-cs`
- [x] PM emulator setup: cancelled
- [ ] `scratch-dev47x45` (detached, staged DEV-47 x DEV-45 merge experiment): decide keep or drop
- [ ] `/workspace/shanivar-app` (old clone on main 33d0ce4, uncommitted routine-plan work): decide keep or drop
- [ ] PR #1: still open; do not merge without the founder
