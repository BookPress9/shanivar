Status: awaiting founder confirmation — do not build yet.

**Title:** Sensitive logging, retention, deletion and backup review
**Epic:** Epic E (DEV-60)
**User problem:** Credentials have leaked before (DEV-8); logs or analytics could expose personal data.
**Hypothesis:** Not a hypothesis.
**Scope — in:** Audit Android logs, Edge Function logs, crash reports (DEV-14), analytics (A3); retention periods; deletion path (DEV-46); backup/restore (DEV-41).
**Scope — out:** Legal policy text.
**Requirements:** No secrets/PII/protected-app names in logs or responses; documented retention and restore.
**Acceptance criteria:** Audit report; restore drill done on staging.
**Dependencies:** DEV-8, DEV-14, DEV-41, DEV-46.
**Analytics:** None.
**Security/privacy:** Core of this ticket. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Grep logs after a full session.
- Edge: Crash with user text in state.
- Failure: Backup restore fails — documented.
- Recovery: Restore drill succeeds.
**Owner:** Shanivar Backend
**Priority:** Highest
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic E met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic E; links to design, implementation, test results and decisions to be added here.
