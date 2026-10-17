# Routine V3 onboarding: marketing source pack and plans

Created 6 October 2026 by the Grok Bot collaborator (`app.shanivar`, `AGENTS.md:3`) for the Social Media Manager (`ai_org/agents/social_media_manager.md`).

**Status: planning only.** Marketing is paused by founder instruction (6 October 2026, given in the task; not yet recorded in a repo file, see decision D8 in the plan). Nothing in this folder has been published, scheduled, sent, paid for or switched on.

**Build truth, in one line:** Routine V3 and its onboarding are on `main` (commits `2914d4e`, `94b9c87`), but `ROUTINE_V3` is `false` (`android-app/app/build.gradle.kts:33`), real users still get the older onboarding (`ShanivarApp.kt:182-218`), the physical-device QA (DEV-7) has never run, and there is no Play release (DEV-17). No marketing may call a Routine feature available, live, Active or tested.

## Files

| File | What it is |
| --- | --- |
| `routine-onboarding-marketing-plan.md` | **Start here.** Build truth, the founder's positioning with guardrails, what to promote now vs later, audience, six messaging angles, phased rollout with build gates, waitlist funnel and UTMs, the first two weeks of Phase 0 (proposed), conflicts and which file wins, risks, open founder decisions |
| `social-presence-from-scratch.md` | Account reset for Instagram `@app.shanivar` (plus YouTube Shorts as a founder decision): handle, bio, link-in-bio UTM, Highlights, audit of existing posts, first-9 grid, pillars, cadence, community and disclosure rules |
| `zero-to-million-playbook.md` | Long-range growth: case studies with confidence markers, patterns to steal and avoid, reel trends and formats, kickoff paths, milestone roadmap M0 → 1M with gates and ST-008-safe metrics, 30-day checklist, banned claims by gate |
| `chatgpt-subscription-assessment.md` | Whether a ChatGPT subscription helps the marketing work, with recommendation, capability table and sources |
| `source/ui-copy-inventory.md` | Every user-facing Routine V3 onboarding/Routine string, verbatim with file:line, grouped by screen, with a ★ shortlist of marketing lines |
| `source/` (other files) | Verbatim snapshots at `6d26bb2` of the files the plans quote. **The originals win if they differ.** |
| `website/` | **Not part of this pack.** The pre-launch site at shanivar.com, built and committed separately by the website worker. Read only here |
| `../wish-list.md` | Living wish list of parked and deferred features, each with source and stance check. **Never marketed as features** |

## Source snapshots and their originals

| Snapshot in `source/` | Original |
| --- | --- |
| `RoutineOnboarding.kt`, `RoutineOnboardingFlow.kt`, `RoutineScreen.kt`, `RoutineProtectionScreen.kt`, `RoutineShell.kt`, `RoutinePlan.kt` | `android-app/app/src/main/java/com/shanivar/nativeapp/<same name>` |
| `CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md` | `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md` |
| `ROUTINE_V3_TEST_GUIDE_2026-09-30.md` | `android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md` |
| `ROUTINE_V3_DEVICE_QA_2026-10-05.md` | `android-app/ROUTINE_V3_DEVICE_QA_2026-10-05.md` |
| `0014-routine-sadhana-shell-and-routine-v3.md` | `docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md` |
| `shanivar-v3-wireframe-README.md` | `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md` |

## Rules for anyone using this pack

1. Product language comes only from `source/ui-copy-inventory.md` and must match the gate in the plan's §6.
2. Re-check build status (flag, DEV-7, DEV-17) on the day of any status post.
3. "Strict" means high standards, never lock-in. No shame and no productivity-bro framing (plan §2).
4. Nothing from `../wish-list.md` appears in marketing.
5. The funnel is bio → https://shanivar.com → waitlist → tester invite (M1/M2) → Google Play (M3), with UTM `utm_campaign=m<n>_<goal>`.
