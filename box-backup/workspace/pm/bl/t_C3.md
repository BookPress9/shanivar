Status: awaiting founder confirmation — do not build yet.

**Title:** Missed-step recovery and non-punitive return
**Epic:** Epic C (DEV-58)
**User problem:** After missing steps, users face guilt or confusing state and abandon routines.
**Hypothesis:** UNVALIDATED — offering a smaller next step after a miss improves recovery after missed days; tested by recovery metric (A3).
**Scope — in:** Missed state, 'start smaller today' option, no streak shaming, no endless reminder loop.
**Scope — out:** Gamified penalties.
**Requirements:** A missed action never creates a loop or unrecoverable state.
**Acceptance criteria:** After 1, 3 and 7 missed days the app offers a clear way back; copy reviewed for guilt.
**Dependencies:** C1, A3.
**Analytics:** routine_step_missed, routine_recovered.
**Security/privacy:** None beyond C1. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Miss one step, return.
- Edge: Miss across timezone change.
- Failure: Reminder fails to deliver.
- Recovery: User returns after 30 days.
**Owner:** Shanivar Frontend
**Priority:** High
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic C met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic C; links to design, implementation, test results and decisions to be added here.
