Status: awaiting founder confirmation — do not build yet.

**Title:** Map each initial practice to a user need, expected action and clarity check
**Epic:** Epic D (DEV-59)
**User problem:** Practices may not connect to what the user does next.
**Hypothesis:** UNVALIDATED — a practice tied to a specific next action is completed and followed by that action more often; tested by D-epic analytics and interviews.
**Scope — in:** Table for DEV-36 Stage 1 and S1–S7 practices: need, expected action, safety check (no breath pacing, no claims, teachers internal), clarity/autonomy review with 5 users.
**Scope — out:** New content.
**Requirements:** Every shipped practice has a row and passed safety review.
**Acceptance criteria:** Table complete; content issues ticketed (e.g. DEV-39).
**Dependencies:** DEV-36, DEV-39, DEV-51.
**Analytics:** practice_completed, next_action_taken.
**Security/privacy:** Reflections are user-owned, never scores. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Review one practice.
- Edge: Practice fitting no need — flagged.
- Failure: Claim found in copy — blocked.
- Recovery: Content updated and re-reviewed.
**Owner:** Customer Success
**Priority:** Medium
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic D met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic D; links to design, implementation, test results and decisions to be added here.
