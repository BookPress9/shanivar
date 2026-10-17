Status: founder-confirmed (Oct 17, 2026) — ready to build.

**Title:** Cross-user access test suite for every Supabase table, RPC and Edge Function
**Epic:** Epic E (DEV-60)
**User problem:** A user could read or modify another user's records.
**Hypothesis:** Not a hypothesis.
**Scope — in:** pgTAP/integration tests as user A against user B data for all sync tables, put_routine, submit_feedback, delete-account; anon access denied.
**Scope — out:** Pen test by a third party.
**Requirements:** Tests run in CI (DEV-15/DEV-43).
**Acceptance criteria:** Every endpoint has a passing isolation test; any failure is a release blocker.
**Dependencies:** DEV-42, DEV-43, DEV-15.
**Analytics:** None.
**Security/privacy:** Core of this ticket. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**

* Normal: User reads own rows.
* Edge: Guest → account migration.
* Failure: Forged user_id in payload rejected.
* Recovery: Policy fix + regression test.
  **Owner:** Shanivar Backend
  **Priority:** Highest
  **Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic E met. 
  **Evidence:** Strategy brief 2026-10-17 §9 Epic E; links to design, implementation, test results and decisions to be added here.

