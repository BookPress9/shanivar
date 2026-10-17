Status: awaiting founder confirmation — do not build yet.

**Title:** Verify alarm and notification behaviour on supported real devices
**Epic:** Epic C (DEV-58)
**User problem:** A failed alarm leaves no trace and breaks the routine (AlarmScheduler/AlarmFireReceiver/AlarmForegroundService, setAlarmClock, exact-alarm permission).
**Hypothesis:** Not a hypothesis.
**Scope — in:** Device matrix (min 2 OEMs, Android 12–15): exact alarm permission granted/denied, Doze, reboot (BootRescheduleReceiver), full-screen intent, notification permission denied, DND, movement-first dismissal.
**Scope — out:** Emulator-only sign-off.
**Requirements:** Every alarm AC has a test; results with evidence.
**Acceptance criteria:** All cases pass or have documented defects; no unresolved critical.
**Dependencies:** DEV-14, DEV-11.
**Analytics:** Compare alarm_observations (DEV-53) with actual.
**Security/privacy:** Camera frames used for dismissal not stored/sent. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**
- Normal: Daily alarm fires.
- Edge: DST change.
- Failure: Exact-alarm permission revoked.
- Recovery: Reboot reschedules.
**Owner:** Shanivar Tester
**Priority:** Highest
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic C met. 
**Evidence:** Strategy brief 2026-10-17 §9 Epic C; links to design, implementation, test results and decisions to be added here.
