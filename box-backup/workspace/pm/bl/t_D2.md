Status: awaiting founder confirmation — do not build yet.

**Title:** Optional one-tap awareness prompt after a distraction or completed action
**Epic:** Epic D (DEV-59)
**User problem:** Users act without noticing the urge or mood behind it.
**Hypothesis:** UNVALIDATED — a short optional prompt increases self-reported awareness without adding friction; tested by skip rate and interviews.
**Scope — in:** Optional, skippable prompt after intervention or completion; uses existing check-in (DEV-36).
**Scope — out:** Mandatory prompts; mood scoring.
**Requirements:** Always skippable; never shown more than agreed frequency.
**Acceptance criteria:** Skip works in one tap; responses stored locally as self-report, never a score.
**Dependencies:** B3, DEV-36.
**Analytics:** awareness_prompt_shown/skipped/answered (no answer content).
**Security/privacy:** Answer content not sent to analytics. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Answer prompt.
- Edge: Prompt triggered twice quickly.
- Failure: Save fails.
- Recovery: Prompt state survives restart.
**Owner:** Shanivar Frontend
**Priority:** Low
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic D met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic D; links to design, implementation, test results and decisions to be added here.
