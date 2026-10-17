Status: awaiting founder confirmation — do not build yet.

**Title:** Map existing Shanivar features against the protect → act → reflect journey
**Epic:** Epic A (DEV-56)
**User problem:** Nobody can say which existing screens (Focus/Protection, Routine, Initiation, Sadhana, Orbit) serve which journey step, so gaps and duplicates are invisible.
**Hypothesis:** UNVALIDATED — most journey steps already exist but are not connected; tested by this map plus Epic F E2E walk-through.
**Scope — in:** Inventory of current screens/flows in android-app (FocusGuardService, BlockOverlayActivity, RoutineProtectionScreen, InitiationOrbitScreens, LivingOrbit, Sadhana S1–S7) mapped to journey steps; user-flow diagram.
**Scope — out:** Redesign; new features.
**Requirements:** Table: feature → journey step → status (exists/partial/missing) → ticket.
**Acceptance criteria:** Every journey step has at least one row; every missing step links a ticket; diagram attached.
**Dependencies:** DEV-11, DEV-26, DEV-31/32.
**Analytics:** None.
**Security/privacy:** None. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Walk the map on a device build.
- Edge: Feature serving two steps.
- Failure: Screen in code but unreachable in nav — marked.
- Recovery: Map updated when a ticket closes.
**Owner:** Shanivar Frontend
**Priority:** High
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic A met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic A; links to design, implementation, test results and decisions to be added here.
