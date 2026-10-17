Status: awaiting founder confirmation — do not build yet.

**Title:** Set baselines, experiment design and an assumption register
**Epic:** Epic A (DEV-56)
**User problem:** Without baselines and labelled assumptions, the team cannot tell whether a change helped.
**Hypothesis:** UNVALIDATED — each strategic assumption (e.g. protection → action → awareness) can be tested with small experiments.
**Scope — in:** Assumption register (assumption, evidence, test, owner, status); baseline metrics plan for activation, W1/W4; experiment template.
**Scope — out:** Running experiments; paid acquisition.
**Requirements:** Register lists every UNVALIDATED hypothesis in DEV tickets.
**Acceptance criteria:** Register exists and is linked from Notion; each hypothesis has a test method.
**Dependencies:** A3.
**Analytics:** Uses A3 events.
**Security/privacy:** No individual-level data in the register. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Add one assumption end-to-end.
- Edge: Assumption with no feasible test — marked.
- Failure: Baseline not computable — documented limit.
- Recovery: Register reviewed monthly.
**Owner:** Customer Success
**Priority:** Medium
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic A met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic A; links to design, implementation, test results and decisions to be added here.
