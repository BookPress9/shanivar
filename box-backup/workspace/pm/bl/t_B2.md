Status: awaiting founder confirmation — do not build yet.

**Title:** Specify protection-window behaviour and user controls
**Epic:** Epic B (DEV-57)
**User problem:** Users do not always understand why an intervention appeared or what their choices are.
**Hypothesis:** UNVALIDATED — a clear reason plus 2–3 explicit choices reduces frustration and bypassing; tested with usability sessions and B3 choice data.
**Scope — in:** Spec for start/end, overnight windows, essential-access stop (DEV-10), no-snooze rule, copy explaining why the screen appeared, choices (return to my day / short practice / one-minute pause), permission-denied and unavailable states.
**Scope — out:** New blocking modes; web blocking.
**Requirements:** Spec + wireframe; copy free of claims.
**Acceptance criteria:** Spec covers all states; Tester converts to test cases; founder approves copy.
**Dependencies:** B1, DEV-10, DEV-12.
**Analytics:** Defines states for B3.
**Security/privacy:** Spec keeps the protected-app list local. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Walk each state on wireframe.
- Edge: Window crossing midnight / DST.
- Failure: Permission missing at window start.
- Recovery: Window resumes correctly after reboot (DEV-21).
**Owner:** Shanivar Frontend
**Priority:** High
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic B met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic B; links to design, implementation, test results and decisions to be added here.
