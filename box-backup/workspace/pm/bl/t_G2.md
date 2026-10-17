Status: awaiting founder confirmation — do not build yet.

**Title:** Feedback and churn-reason taxonomy, interview guide and monthly product review
**Epic:** Epic G (DEV-62)
**User problem:** We do not know why users struggle, leave or return.
**Hypothesis:** UNVALIDATED — most abandonment comes from friction or technical failure; tested by interviews and in-app feedback (DEV-49).
**Scope — in:** Taxonomy (technical failure, friction, low relevance, unrealistic routines, unclear value, other); interview guide (active/inactive/returning); monthly cross-functional review.
**Scope — out:** Incentivised research spend.
**Requirements:** Feedback tagged with taxonomy; findings linked to tickets.
**Acceptance criteria:** First 5 interviews done; first monthly report published.
**Dependencies:** DEV-49.
**Analytics:** feedback_submitted (category only).
**Security/privacy:** Consent for interviews; no health data. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Tag a feedback item.
- Edge: Item fits no category.
- Failure: Feedback table unreachable.
- Recovery: Re-tag after taxonomy change.
**Owner:** Customer Success
**Priority:** Medium
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic G met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic G; links to design, implementation, test results and decisions to be added here.
