Status: awaiting founder confirmation — do not build yet.

**Title:** Record the intervention choice on the block overlay (on-device)
**Epic:** Epic B (DEV-57)
**User problem:** We do not know whether interventions lead to intentional choices; BlockOverlayActivity offers Return / Practice / Pause but records nothing.
**Hypothesis:** UNVALIDATED — most interventions end in 'return to my day' or a practice rather than a pause; tested by measuring choice distribution.
**Scope — in:** Local record per intervention: time, choice, window id — no app package name in any synced or analytics payload.
**Scope — out:** Remote sync of the protected-app list; scoring.
**Requirements:** One record per shown overlay, idempotent on rerender; event per A3.
**Acceptance criteria:** Choice stored exactly once per overlay; analytics event fires once; no package name leaves device.
**Dependencies:** A3, B2.
**Analytics:** intervention_shown, intervention_choice{choice}.
**Security/privacy:** Package name stays local. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Tap each choice.
- Edge: Overlay re-shown quickly for same app.
- Failure: Storage write fails — overlay still works, error logged.
- Recovery: Process death mid-choice — no duplicate on restart.
**Owner:** Shanivar Frontend
**Priority:** High
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic B met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic B; links to design, implementation, test results and decisions to be added here.
