Status: awaiting founder confirmation — do not build yet.

**Title:** Positioning doc and feature-claim verification checklist
**Epic:** Epic G (DEV-62)
**User problem:** Messaging could promise features or outcomes the app does not deliver.
**Hypothesis:** UNVALIDATED — segment-specific messaging around 'Protect. Become Aware. Enter Flow.' is understood; tested before larger campaigns.
**Scope — in:** Positioning, segment messages, claim checklist vs B1 constraints and shipped features.
**Scope — out:** Paid campaigns; competitor-gap claims.
**Requirements:** No medical/therapeutic/energy/performance claims; every claim maps to a shipped feature.
**Acceptance criteria:** Checklist applied to site, onboarding and store listing.
**Dependencies:** B1, A2.
**Analytics:** Measure activation of acquired users, not clicks.
**Security/privacy:** No tracking pixels without consent. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Check one page.
- Edge: Feature behind flag.
- Failure: Claim fails check — removed.
- Recovery: Re-check on each release.
**Owner:** Sana (Marketing)
**Priority:** Medium
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic G met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic G; links to design, implementation, test results and decisions to be added here.
