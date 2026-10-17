Status: founder-confirmed (Oct 17, 2026) — ready to build.

**Title:** Detect and surface protection failures (service killed, permission revoked)
**Epic:** Epic B (DEV-57)
**User problem:** If protection silently stops, users believe they are protected when they are not.
**Hypothesis:** Not a hypothesis — reliability requirement.
**Scope — in:** Heartbeat from FocusGuardService; detect usage-access/overlay permission loss (LocalProtectionController.canEnforce); show 'protection is off' state; crash/ANR to DEV-14 reporting.
**Scope — out:** Server-side monitoring of app usage.
**Requirements:** Never show 'protected' when not enforcing; failure visible within one poll interval.
**Acceptance criteria:** Killing the service shows off/unknown state; revoking permission shows the fix path; failure appears in crash reporting.
**Dependencies:** DEV-14, B1.
**Analytics:** protection_failure{reason}.
**Security/privacy:** No app names in logs. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**

* Normal: Normal window runs end to end.
* Edge: Doze / battery saver.
* Failure: Service force-stopped.
* Recovery: User re-grants permission and protection resumes.
  **Owner:** Shanivar Frontend
  **Priority:** Highest
  **Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic B met. 
  **Evidence:** Strategy brief 2026-10-17 §9 Epic B; links to design, implementation, test results and decisions to be added here.

