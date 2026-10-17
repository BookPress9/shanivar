# Continue Shanivar — 17 October 2026

Owner: Codex. Task: CONT-001. **Resume branch: `codex/continuity-2026-10-17`**.

```powershell
git fetch origin --prune
git switch codex/continuity-2026-10-17
git pull --ff-only origin codex/continuity-2026-10-17
git status --short --branch
```

For a dirty checkout, preserve it and create a separate worktree from
`origin/codex/continuity-2026-10-17`. Read AGENTS.md, SHANIVAR_STANCE.md and the task first.
The original Windows checkout remains untouched, including its untracked wireframes, research and marketing.

## Current checkpoint

- Private GitHub fetched. Original local main was 83 commits behind remote main `b372079`.
- Reviewed box-backup `89b935b`, saved assignments, sync contracts and QA alignment evidence.
- Review branch integrates DEV-45 `d699ee0` (includes DEV-38 `20fb519` and DEV-42 `7e9c524` history),
  DEV-39 `8e122a8`, and DEV-47 `9806c99`. Main and source agent branches remain unchanged.
- Resolved shared contract/outbox conflicts using DEV-45's extended contract and DEV-47's lifecycle wiring.
  Replaced reflection with typed factory/scheduler/session/suspend-reset calls, preserved the enqueue hook,
  and added an AppSync-to-real-engine reset regression test.
- Added daily GitHub metadata checkpoints and a session handoff protocol. Exact fresh test results and
  schedule verification are in [CONT-001 report](../ai_org/collaboration/reports/CONT-001-codex.md).
- No production migration, deployment, release or new stance acceptance.

## Next actions, in order

| Priority | Owner | Action / required evidence |
|---|---|---|
| 1 | Codex / Tester | Use CONT-001 report for integrated JVM/lint/APK outcomes. Complete any failed/NOT RUN checks; record tested SHA. Run pgTAP and live sync E2E only on a local Supabase stack. |
| 2 | Backend / Codex | Resolve DEV-54 `d1d482c` versus `a2a9e23` (rebased on DEV-45). Both saved; confirm intended history before integration, then verify payload/schema agreement. Reflection builder exists there but enqueue is not wired. |
| 3 | Frontend / Codex | Review DEV-89 WIP `93102ab` and current ownership, then integrate and run every breathing-surface safety test. DEV-39 catalogue fixes alone do not clear DEV-89. |
| 4 | Codex / Tester | Verify DEV-45/47 together with local Supabase, including cross-user isolation (DEV-79), reset, token loss, offline/reconnect and routine parking. DEV-52 conflict UI / put_routine remain dependencies. |
| 5 | Analytics owner | Review DEV-65 WIP `dd2579a`. Normal DEV-65 branch points at DEV-54 `a2a9e23`; its name does not establish analytics completion. Validate DEV-83 before WIAC/effectiveness claims. |
| 6 | Founder + Tester | Resume Pixel/OEM alarm, reboot, Doze, revoked-access, process-death, timezone/DST, TalkBack and large-font checks. Berlin DST changes 25 October 2026. Emulator setup was cancelled; none run here. |
| 7 | Founder / operations | Restore Shanivar Jira access; verify credential rotation (DEV-8), staging (DEV-41), signing/Play (DEV-17), review manifests (DEV-34). Review cloud migration-history repair plan before any apply. |
| 8 | Sana / CS | Resume campaign/CS artifacts from box-backup/workspace after checking ownership and F-07 restrictions. Nothing published by this checkpoint. |

PR #1 remains pending founder merge approval. Retain detached scratch and old-clone WIP until reviewed.
The box status has stale "uncommitted" checklist entries: compare actual refs and evidence.
The QA alignment report calls `20fb519` "main", but fetched main is `b372079`; treat its 518-test claim as
historical branch evidence, not proof of this integration or current main.

[Access matrix](continuity/ACCESS.md) · [Daily protocol / restore](continuity/README.md).
The reported 30-minute agent-box backup is not independently verified from this Windows session.

---

## Archived phone handoff — 13 October 2026

The text below is historical and contains outdated/internally conflicting pending lists and test counts.
Use the current checkpoint and task report above for resumption.

> 13 Oct: Sadhana vagal layer/self-check/voice copy shipped (18437eb). B1–B8 done (ad984d0, bbd2042, ba56e76, 2828ccd, 5048af5, 1e4879c, a024596, f6ec0b7; DEV-35). Full suite 496/0 failures. Phone-test debug APK (draft release): https://github.com/BookPress9/shanivar/releases/tag/untagged-620bd070cac9d2285653. Note: release builds launch no practice until founder review manifests are recorded. Full status: docs/FINALIZATION_CHECKLIST.md. Remaining: founder / services / device items only.


## Current state
- **Android app** (`android-app/`, Kotlin and Compose). Debug builds turn on two flags (release is unchanged):
  - `INITIATION_V1`: the initiation onboarding flow (guest works end to end; Google and email show "not available yet").
  - `SADHANA_V1`: the Sadhana experience. Living Spine with 33 stages and 7 waypoints, Path (Arrive → Return → See → Choose → Carry), practice detail, prepare, a still-planet player, return, record, practice without the app, deep study, and a NASA Saturn loading surface.
- **Sadhana visual v1 approved by the founder (13 Oct; tag `sadhana-visual-v1`):** 33-stage Living Spine with root pulse, Saturn loading surface. Still pending: vagal layer, self-check, voice copy, practice reviews, device QA.
- Living Spine tip pulse kept as non-rhythmic root branches (founder, 13 Oct; 0017).
- 482 unit tests pass (`testDebugUnitTest`). Review screenshots are on the agent box at `/workspace/sadhana-shots/` (not in the repo).
- Research-only practices can never launch. Release builds refuse practices without a content review manifest.

## Decisions (`docs/decisions/`)
- **0015** Sadhak and Hybrid Monk core ideology (ST-010). Naming rule amended 13 Oct: the main app, website and marketing name no teacher or tradition; only in-app deep study may name traditions, texts and passages; chakras use plain labels outside deep study; private influences (ST-011) stay internal.
- **0016** initiation-v1 wireframe adopted (accounts and cloud sync at launch, Orbit glow, 60 s practice).
- **0017** sadhana-v1 wireframe adopted, with build notes, honesty edits and the 13 Oct decisions: kaleidoscope zoom rejected; NASA PIA06193 Saturn with a steady ring spin; 33-stage spine with founder thresholds 1/5/11/17/24/29/33.

## Open founder questions
1. Vagal firefly layer, self-check and new voice copy: not built; waiting on the founder plus an SOP-05 claims review.
2. (Tip pulse resolved: root form, non-rhythmic.) Spine orientation: app.js lights stages from the top while thresholds count from the base. The build counts from the base; confirm.
3. Progression meaning and visual language sign-off (HANDOFF gate 8).
4. Practices longer than 60 s need a practice review; who verifies chakra source passages?
5. The "Choose" capability needs a Routine-boundary link (not wired yet).
6. NASA image in a commercial app: credit is shown and no endorsement is implied; confirm you're comfortable with that.

## Jira (Shanivar site, project DEV)
| Key | Summary | Status |
|---|---|---|
| DEV-22 | Future sign-in methods | Open (backlog) |
| DEV-23 | Circles visual system | Open (backlog) |
| DEV-24 | Initiation HANDOFF steps 1-5 domain logic (DST parked) | Work in progress |
| DEV-25 | Accounts + cloud sync: real providers, backend /v1, hosting (HIGH) | Open, needs founder |
| DEV-26 | Initiation Compose screens | Work in progress |
| DEV-27 | Sadhana S1 practice domain + research-only lock | Work in progress |
| DEV-28 | S2 attempt timing + events | Work in progress |
| DEV-29 | S3 record, reflection, capability evidence | Work in progress |
| DEV-30 | S4 Living Spine data | Work in progress |
| DEV-31 | S5 screens A (home, spine, path, loader) | Work in progress |
| DEV-32 | S6 screens B (detail, prepare, player, return) | Work in progress |
| DEV-33 | S7 screens C (record, without app, study) | Work in progress |
| DEV-34 | S8 content reviews, /v1 sync, device and accessibility QA | Open, needs founder |

## Next steps
1. Founder review of the debug APK (initiation, then the Sadhana tab after the first minute).
2. Answer the open questions above, then build the vagal layer, self-check and copy if approved.
3. DEV-25: Google OAuth client, email provider, passwordless backend, /v1 routes, hosting and database, privacy notice update.
4. DEV-34: practice and source reviews, physical-device QA (process death, lock, calls), TalkBack, 200% font.
5. HANDOFF step 6 accessibility review for initiation.

## Build
```bash
cd android-app
export ANDROID_HOME=/path/to/Android/Sdk JAVA_HOME=/path/to/jdk17
bash gradlew testDebugUnitTest assembleDebug      # APK: app/build/outputs/apk/debug/app-debug.apk
bash gradlew testDebugUnitTest -PsadhanaShots=/tmp/shots   # optional review screenshots
```
Do not use `--offline`. Commit as "Hybrid Monk <admin@thehybridmonk.com>". Run the decision gate first (AGENTS.md, SOP-00).

## Where things live
- Wireframes: `android-app/wireframes/initiation-v1/` and `android-app/wireframes/sadhana-v1/` (`HANDOFF.md`, `BUILD_PLAN.md`, `exports/`).
- Sadhana code: `android-app/app/src/main/java/com/shanivar/nativeapp/Sadhana*.kt`. Initiation code: `Initiation*.kt`.
- Asset licences: `android-app/app/ASSET_NOTES.md`.
- Stance and safety: `SHANIVAR_STANCE.md`, `research/deep-foundation/PRACTICE_SAFETY.md`, `docs/sop/`.
