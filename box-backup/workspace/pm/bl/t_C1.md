Status: awaiting founder confirmation — do not build yet.

**Title:** Define routine completion semantics and make completion idempotent
**Epic:** Epic C (DEV-58)
**User problem:** Repeated taps, retries or sync could duplicate completions and inflate progress.
**Hypothesis:** Not a hypothesis — brief §4 requirement.
**Scope — in:** What counts as complete/missed/recovered; idempotency key per step-occurrence locally and in put_routine/sync (DEV-52, DEV-45).
**Scope — out:** Rewards/gamification (DEV-16 decision).
**Requirements:** Duplicate requests produce one completion; server validates; 409 on stale revision.
**Acceptance criteria:** Integration test: 5 identical requests → 1 row; offline replay → no duplicates.
**Dependencies:** DEV-52, DEV-45, DEV-38.
**Analytics:** routine_step_completed with dedup key.
**Security/privacy:** Per-user isolation via RLS. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Complete a step.
- Edge: Complete same step offline twice then sync.
- Failure: Server rejects — UI does not show success.
- Recovery: Retry after failure completes once.
**Owner:** Shanivar Backend
**Priority:** Highest
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic C met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic C; links to design, implementation, test results and decisions to be added here.
