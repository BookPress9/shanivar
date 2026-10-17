# QA alignment review — "Protect. Become Aware. Enter Flow."
Shanivar Tester, 2026-10-17 (Europe/Berlin). Sources: the QA delegation brief and the product thesis (Notion), Jira DEV (89 issues), and the repo worktrees.

**1) What must change?**
- "Done" now means ACs + integration + regression + analytics pass, plus a real-device run for alarms and protection. DEV-1–6 and DEV-21 are "Completed" in Jira but are only unit-verified. Treat them as "device pending (DEV-7)", not done.
- Every ticket needs testable ACs. 21 tickets have no AC section and the 7 epics have none. QA derived ACs and returns them to the owners to confirm.
- Fixes that pass on branches (DEV-38 @20fb519, DEV-39 @8e122a8, DEV-42 @ba38de6) don't count until they're merged and regression passes on main. Every verdict names its build.
- Safety tests must cover every breathing surface, not only techniques.json. That means the Breathing* screens and the block-screen "breathe" path (DEV-89).

**2) What already exists?**
- Main @20fb519: 518 JVM/Robolectric tests, 0 failures (1 skipped: the e2e that needs a stack). They cover scheduler correctness, honest status, offline local stop, reboot catch-up, the outbox and the Sadhana content gates. 76 ACs are automated-pass.
- pgTAP 01–05 (RLS isolation, privileges, caps, timestamps), passing at dev42@ba38de6. TechniqueContentSafetyTest with mutation runs (DEV-39). Sync engine tests and pgTAP 06 (DEV-45, in QA now).
- Device protocol docs (8-check stance, Routine V3 guide, device QA checklist). Retest history with evidence in /workspace/qa/.

**3) What's missing?**
- No real-device run yet: all 37 real-device rows are NOT RUN, and there's no OEM phone (DEV-7/71/75). 21 DEV-71/75 cases are planned for the emulator first (emulator-pending); they never replace the phone rows.
- No analytics at all: the intervention choice isn't recorded (DEV-69) and there is no event contract (DEV-65). So WIAC can't be verified (DEV-83).
- No CI gate (DEV-15/43), no staging (DEV-41), so no E2E (DEV-55). No crash reporting (DEV-14). No cross-user tests for RPCs or Edge Functions (DEV-79).
- No E2E protect → act → reflect suite (DEV-84). Completion idempotency (DEV-72) and missed-day recovery (DEV-74) aren't built.

**4) What dependencies or risks are there?**
- Founder: rotate credentials (DEV-8), staging and plan (DEV-41), DEV-40, physical Pixel + OEM phones, and resuming device testing (paused 5 Oct).
- Top risk: OEM battery killers plus polling-based blocking (H7). The DST change on 25 Oct 2026 (Berlin) is a real-world test window next week.
- Shared local Supabase and 6 worktrees can interfere with each other. With no CI, regressions can slip in. d9168eb can't be reverted on its own (rollback risk).
- Open Critical/High: DEV-8, DEV-17, DEV-89 (suspected). Each blocks an RC unless the founder approves it.

**5) What can ship in the first release?**
- Routine V3: day plan, protection windows with honest status, local stop and the wake alarm. Only after the device protocol passes on 2 devices (Pixel + 1 OEM).
- Guest sync (DEV-38/42/45/47), once it's merged to main, regression is green and the DEV-79 isolation suite is complete.
- The 60 s Sadhana Stage 1. Only with review manifests (DEV-34), DEV-89 cleared and the check-in simplified to 2–3 taps (DEV-36).
- Not yet: WIAC or effectiveness claims, auth providers, or any "works on every phone" claim.

**6) How will it be verified?**
- The DEV-82 AC-to-test matrix (315 ACs) plus a release-readiness report per RC. Regression = `testDebugUnitTest` + `supabase test db` (local) + the device protocol. Anything skipped is NOT RUN.
- Device protocol on a Pixel and an OEM phone: reboot, Doze, battery saver, force-stop, revoked access, airplane mode, DST/time-zone change, process death, and the alarm actually waking someone.
- Analytics: compare scripted journeys with the emitted events (DEV-83). Security: cross-user suite (DEV-79). Copy and safety audits (DEV-88, DEV-89).
- Exit gate: no open Critical/High without documented founder approval.
