# DEV-82 — Defect register

As of 2026-10-17 22:45 (Europe/Berlin). Owner: Shanivar Tester. Evidence holds no personal data (test users are `a@test.local` / `b@test.local`).

**Severity:** Critical = harm/lock-out, security breach or safety-rule breach reachable by users · High = a core promise is wrong (protection, alarm, truthful status, data loss) · Medium = wrong behaviour with a workaround, or latent · Low = hygiene/tooling.
**QA status:** Open · Fixed – unit-verified (device pending) · Verified fixed (on branch) · Verified fixed (on main) · Pending retest.
"Verified in" = the build where QA last saw the fix pass. A Jira status of Completed does **not** mean device-verified.

| Key | Title | Severity | Jira status | QA status | Found in | Verified in build | Evidence / note |
|---|---|---|---|---|---|---|---|
| [DEV-1](https://shanivar.atlassian.net/browse/DEV-1) | Delayed START can begin a 24-hour focus session | Critical | Completed | Fixed – unit-verified, device pending (DEV-7) | source inspection 2026-09-29 | main@20fb519 (unit, 518/0 fail) | FocusWindowRecurrenceTest.dev1_* (3) |
| [DEV-2](https://shanivar.atlassian.net/browse/DEV-2) | Window END releases protection owned by another window | High | Completed | Fixed – unit-verified, device pending | source inspection | main@20fb519 (fix 2914d4e) | FocusWindowEndHandlerTest (15) |
| [DEV-3](https://shanivar.atlassian.net/browse/DEV-3) | Saved schedule shown as armed without engine confirmation | High | Completed | Fixed – unit-verified, device pending | source inspection | main@20fb519 (fix 2914d4e) | FocusScheduleStatusTest (16) |
| [DEV-4](https://shanivar.atlassian.net/browse/DEV-4) | Boundaries depend on in-process session; stop impossible offline | High | Completed | Fixed – unit-verified, device pending | source inspection | main@20fb519 | LocalProtectionTest; DisciplineEngineTest.releaseFocusStopsTheGuard… |
| [DEV-5](https://shanivar.atlassian.net/browse/DEV-5) | No overnight focus windows | High | Completed | Fixed – unit-verified, device pending; AC4 (explain TZ change) has no test | source inspection | main@20fb519 | FocusWindowRecurrenceTest overnight + DST |
| [DEV-6](https://shanivar.atlassian.net/browse/DEV-6) | All-day preset leaves a 1-minute gap; copy implies persistence | Medium | Completed | Fixed – unit-verified; copy AC not automated | source inspection | main@20fb519 (fix 2914d4e) | FocusWindowRecurrenceTest.fullCalendarDay* |
| [DEV-21](https://shanivar.atlassian.net/browse/DEV-21) | Reboot catch-up must resume local protection with no session | High | Completed | Passes in tests; real device pending under DEV-7 | dev | main@20fb519 (fix 88793bd) | BootRescheduleCatchUpTest (5) |
| [DEV-38](https://shanivar.atlassian.net/browse/DEV-38) | Sync outbox never queues own self-reports, edits/deletes, onboarding import, routine changes | Medium | Open | Verified fixed (on branch dev-38-outbox-gaps, not on main) | QA of DEV-35 B3 at b372079 | **20fb519** (failed once at 86b558a: legacy-import gap) | SyncOutboxGapsTest (11); /workspace/qa/dev38/rerun-* |
| [DEV-39](https://shanivar.atlassian.net/browse/DEV-39) | techniques.json has a timed box-breathing script and a nervous-system claim | High (safety rule; Jira says Medium) | Open | Verified fixed (on branch dev-39-techniques-safety) | QA safety check at b372079 | **8e122a8** (failed once at 3471087: "rounds" still present) | TechniqueContentSafetyTest (5) + mutation runs /workspace/qa/dev39-mut-*.log |
| QA-42-01 (not filed) | DEV-42: commit d9168eb cannot be reverted on its own (later caps/revoke migrations depend on it) | Low | — | Open — rollback hazard | QA of DEV-42 | dev42@ba38de6 | Feed into DEV-81 runbook: revert the DEV-42 set as a unit or ship a forward fix |
| QA-42-02 (not filed) | DEV-42: anon request returns a 401/permission hint that names the table | Low | — | Open — information disclosure | QA of DEV-42 | dev42@ba38de6 | 05_privileges / anon probe. Table names are not secret, but the hint confirms which tables exist |
| QA-42-03 (not filed) | DEV-42: text columns other than ids and `note` have no length cap | Medium | — | Open — hardening gap | QA of DEV-42 | dev42@ba38de6 | 04_size_caps covers plan/packages/drift_reasons/id/note only. Proposed fix: cap every free text column (follow-up ticket) |
| [DEV-8](https://shanivar.atlassian.net/browse/DEV-8) | Leaked GitHub PAT and Render key still valid (+ Atlassian token) | Critical | Open | Open — founder action; release blocker | audit 2026-09-24 | — | Not testable by QA until rotated |
| [DEV-17](https://shanivar.atlassian.net/browse/DEV-17) | No release signing; release API host NXDOMAIN | High | Open | Open — blocks any store/release build | audit 2026-09-24 | — | |
| [DEV-89](https://shanivar.atlassian.net/browse/DEV-89) | Breathing* screens and block-screen "breathe" path may pace breath (suspected) | High (if confirmed) | Open | Open — suspected, audit not run | DEV-39 follow-up | — | TechniqueContentSafetyTest does not scan these files |
| [DEV-36](https://shanivar.atlassian.net/browse/DEV-36) (gap) | Shipped Stage 1 check-in (47e668a) has a stepper, chips and a note; spec allows 2–3 one-tap options | Medium | Open | Open — spec mismatch, do not ship as-is | ticket analysis | main@20fb519 | SadhanaStage1.kt Stage1CheckCard |
| [DEV-19](https://shanivar.atlassian.net/browse/DEV-19) | git push hangs in non-interactive sessions | Low (tooling) | Open | Open | 2026-09-30 | — | QA never pushes |
| [DEV-45](https://shanivar.atlassian.net/browse/DEV-45) | Sync engine (Backend half) | — | Open | **Pending — in QA now** | — | dev45@2fce42d under test | No verdict yet |

**Counts:** 17 entries. Critical 2 (DEV-1 fixed in logic; DEV-8 open). High 8 (DEV-2/3/4/5/21 fixed in logic; DEV-39 verified on branch; DEV-17, DEV-89 open). Medium 4 (DEV-6 fixed; DEV-38 verified on branch; QA-42-03, DEV-36 open). Low 3 (QA-42-01, QA-42-02, DEV-19 open). Plus DEV-45, pending in QA.
**Open Critical/High with no fix:** DEV-8, DEV-17, DEV-89. Each blocks an RC unless the founder gives documented approval.
**Action for owners:** file QA-42-01..03 as Jira bugs, if the founder agrees. QA did not file them.
