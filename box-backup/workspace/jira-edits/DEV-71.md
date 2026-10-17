Status: founder-confirmed (Oct 17, 2026) — ready to build.

**Title:** QA suite: protection permissions, sessions, reboot and recovery on real devices
**Epic:** Epic B (DEV-57)
**User problem:** Protection has never been verified on physical devices (DEV-7).
**Hypothesis:** Not a hypothesis.
**Scope — in:** Test cases for permission grant/deny/revoke, window start/end, overnight, reboot catch-up, essential stop, overlay choices, failure states; at least two OEMs / two Android versions.
**Scope — out:** Emulator-only sign-off.
**Requirements:** Each B-ticket AC has a reproducible test case.
**Acceptance criteria:** Suite executed on real devices with evidence; defects logged with severity.
**Dependencies:** DEV-7, B1–B4.
**Analytics:** Verify B3 events match actions.
**Security/privacy:** Test data contains no real personal app lists. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**

* Normal: Full window on device.
* Edge: Clock change during window.
* Failure: Permission denied at onboarding.
* Recovery: Reboot mid-window resumes.
  **Owner:** Shanivar Tester
  **Priority:** Highest
  **Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic B met. 
  **Evidence:** Strategy brief 2026-10-17 §9 Epic B; links to design, implementation, test results and decisions to be added here.

