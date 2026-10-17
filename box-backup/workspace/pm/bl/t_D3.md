Status: awaiting founder confirmation — do not build yet.

**Title:** Connect relevant Mano Vijnana content to practical exercises
**Epic:** Epic D (DEV-59)
**User problem:** Study content feels abstract and disconnected from daily action.
**Hypothesis:** UNVALIDATED — linking a concept to one exercise increases practice return rate.
**Scope — in:** Links from Deep study (DEV-33) to an existing exercise.
**Scope — out:** New teachings; teacher attribution in UI.
**Requirements:** Each link reviewed for claims and teacher-internal rule.
**Acceptance criteria:** Links work; content review manifest (DEV-34) approved.
**Dependencies:** DEV-33, DEV-34, DEV-51.
**Analytics:** study_to_practice_opened.
**Security/privacy:** None. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Open link.
- Edge: Content missing.
- Failure: Offline.
- Recovery: Content restored on reconnect.
**Owner:** Shanivar Frontend
**Priority:** Low
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic D met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic D; links to design, implementation, test results and decisions to be added here.
