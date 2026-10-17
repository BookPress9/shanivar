Status: awaiting founder confirmation — do not build yet.

**Title:** End-to-end protect → act → reflect journey and regression suite
**Epic:** Epic F (DEV-61)
**User problem:** The connected journey has never been tested as a whole.
**Hypothesis:** Not a hypothesis.
**Scope — in:** E2E script from first launch through intervention, action and optional reflection; regression run before every RC.
**Scope — out:** Load testing.
**Requirements:** Suite runnable on device and documented.
**Acceptance criteria:** Pass on two devices before release recommendation.
**Dependencies:** C2, B3, D2, DEV-55.
**Analytics:** Verify journey events.
**Security/privacy:** Test accounts only. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Full journey.
- Edge: User skips reflection.
- Failure: API failure mid-journey.
- Recovery: Resume after app kill.
**Owner:** Shanivar Tester
**Priority:** High
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic F met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic F; links to design, implementation, test results and decisions to be added here.
