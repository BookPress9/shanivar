Status: awaiting founder confirmation — do not build yet.

**Title:** Audit protection capabilities and Android platform constraints
**Epic:** Epic B (DEV-57)
**User problem:** Users may be promised protection the platform cannot guarantee.
**Hypothesis:** Not a hypothesis — fact-finding.
**Scope — in:** Document actual mechanism: UsageStatsManager polling in FocusGuardService + SYSTEM_ALERT_WINDOW BlockOverlayActivity (no AccessibilityService), specialUse foreground service, exact alarms, boot reschedule; known brief app flash; OEM battery killers; Android 14/15 FGS rules; permission revocation.
**Scope — out:** Switching to AccessibilityService (needs founder decision).
**Requirements:** Constraint table: capability, API, permission, failure mode, user-visible copy.
**Acceptance criteria:** Table reviewed by Tester; every limit has user copy and a test in B5; feeds Marketing claim checklist (G4).
**Dependencies:** DEV-7, DEV-10, DEV-21.
**Analytics:** None.
**Security/privacy:** Usage access data used only on device; nothing about foreground apps leaves the device. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Read code paths and verify on one device.
- Edge: OEM with aggressive battery saver.
- Failure: Usage access revoked mid-session.
- Recovery: Doc updated after DEV-7 results.
**Owner:** Shanivar Frontend
**Priority:** Highest
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic B met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic B; links to design, implementation, test results and decisions to be added here.
