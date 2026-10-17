Status: awaiting founder confirmation — do not build yet.

**Title:** Verify analytics events match actual user actions
**Epic:** Epic F (DEV-61)
**User problem:** Analytics may be duplicated by rerenders or miss actions, corrupting WIAC.
**Hypothesis:** Not a hypothesis.
**Scope — in:** Scripted sessions compared with emitted events per A3.
**Scope — out:** Dashboard design.
**Requirements:** No duplicates, no missing events, no forbidden properties.
**Acceptance criteria:** Report per event: expected vs actual.
**Dependencies:** A3, B3, C1.
**Analytics:** Core of ticket.
**Security/privacy:** Check that no sensitive properties appear. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Scripted journey.
- Edge: Rotation/rerender.
- Failure: Offline then sync.
- Recovery: Events flushed once after reconnect.
**Owner:** Shanivar Tester
**Priority:** High
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic F met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic F; links to design, implementation, test results and decisions to be added here.
