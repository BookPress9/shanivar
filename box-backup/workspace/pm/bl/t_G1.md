Status: awaiting founder confirmation — do not build yet.

**Title:** Activation and retention dashboard with goal segments
**Epic:** Epic G (DEV-62)
**User problem:** We cannot see who activates or returns, by primary goal.
**Hypothesis:** UNVALIDATED — segments (distraction, routine consistency, meditation, self-inquiry) behave differently.
**Scope — in:** Activation milestone per segment; W1/W4; WIAC; recovery; reliability.
**Scope — out:** Paid BI tools without approval.
**Requirements:** Dashboard reads only A3 events.
**Acceptance criteria:** Baseline numbers visible; limits stated.
**Dependencies:** A3, F2.
**Analytics:** Consumes A3.
**Security/privacy:** Aggregates only. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Load dashboard.
- Edge: Small segment — suppressed.
- Failure: Missing data.
- Recovery: Backfill after outage.
**Owner:** Customer Success
**Priority:** Medium
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic G met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic G; links to design, implementation, test results and decisions to be added here.
