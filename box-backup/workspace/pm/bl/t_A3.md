Status: awaiting founder confirmation — do not build yet.

**Title:** Define the event dictionary, analytics contract and WIAC (North Star) computation
**Epic:** Epic A (DEV-56)
**User problem:** There is no analytics infrastructure (SessionReview.kt is local-only), so no outcome in the brief can be measured.
**Hypothesis:** UNVALIDATED — WIAC (Weekly Intentional Actions Completed, deduplicated across features) reflects intentional behaviour; tested by pairing with self-reported intentionality (Epic G) before treating it as truth.
**Scope — in:** Event names, properties, timestamps (UTC + local tz), dedup key, consent rule; WIAC and activation/time-to-first-value/routine completion/protection-window effectiveness/recovery definitions; routine events (created, step completed, step missed, recovered).
**Scope — out:** Choosing a vendor; dashboards (G1); building pipelines.
**Requirements:** One schema doc; each event has owner, trigger, properties, PII class; no package names of protected apps, no free text, no self-report values as scores.
**Acceptance criteria:** Doc reviewed by FE, BE, Tester, CS; every metric in brief §6 traceable to events; privacy review signed.
**Dependencies:** A2; DEV-53/54 (orbit_events, observations).
**Analytics:** This ticket defines them.
**Security/privacy:** Data minimisation review; protected-app list never sent; self-reports excluded or stored only as user-owned records, never scores. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Each metric computed from a sample event log.
- Edge: Same action logged by two features — counted once.
- Failure: Missing timezone — event rejected/flagged.
- Recovery: Schema versioning: old events still parse.
**Owner:** Shanivar Backend
**Priority:** Highest
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic A met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic A; links to design, implementation, test results and decisions to be added here.
