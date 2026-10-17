Status: founder-confirmed (Oct 17, 2026) — ready to build.

**Title:** Traceable test plan, AC-to-test matrix and defect register for all epics
**Epic:** Epic F (DEV-61)
**User problem:** Work is called done when it appears on screen, without independent verification.
**Hypothesis:** Not a hypothesis.
**Scope — in:** Matrix: ticket → AC → test case → result; defect register (severity, evidence, expected, actual); release-readiness report template.
**Scope — out:** Writing tests for unconfirmed tickets.
**Requirements:** Every confirmed ticket has linked test cases.
**Acceptance criteria:** Matrix covers epics A–G; template used for first RC.
**Dependencies:** All epics.
**Analytics:** None.
**Security/privacy:** Test evidence free of personal data. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**

* Normal: Trace one ticket fully.
* Edge: AC not testable — returned to owner.
* Failure: Test env down.
* Recovery: Retest after fix recorded.
  **Owner:** Shanivar Tester
  **Priority:** Highest
  **Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic F met. 
  **Evidence:** Strategy brief 2026-10-17 §9 Epic F; links to design, implementation, test results and decisions to be added here.

