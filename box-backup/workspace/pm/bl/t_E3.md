Status: awaiting founder confirmation — do not build yet.

**Title:** Release, rollback and incident-response runbook
**Epic:** Epic E (DEV-60)
**User problem:** Without a runbook, a bad release or migration cannot be reversed quickly.
**Hypothesis:** Not a hypothesis.
**Scope — in:** Android release steps (DEV-17 signing), staged rollout, Supabase migration rollback (DEV-9/43), incident severity and contacts.
**Scope — out:** Automation beyond DEV-43.
**Requirements:** Runbook in repo docs.
**Acceptance criteria:** Rollback rehearsed on staging once; linked to G6.
**Dependencies:** DEV-17, DEV-43, DEV-41.
**Analytics:** None.
**Security/privacy:** Secrets referenced, never written in runbook. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Dry-run release.
- Edge: Migration not reversible — documented forward-fix.
- Failure: Rollback fails.
- Recovery: Incident closed with postmortem.
**Owner:** Shanivar Backend
**Priority:** High
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic E met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic E; links to design, implementation, test results and decisions to be added here.
