Status: founder-confirmed (Oct 17, 2026) — ready to build.

**Title:** First meaningful action flow: new user completes one action without configuring the whole app
**Epic:** Epic C (DEV-58)
**User problem:** First-time users are overwhelmed by setting up protection, routine and Sadhana at once.
**Hypothesis:** UNVALIDATED — choosing one goal and doing one small action in session one increases activation; tested via activation rate and time-to-first-value (A3).
**Scope — in:** Initiation path (DEV-24/26) ends in one chosen action today; skip/defer for other setup; permission asks just-in-time.
**Scope — out:** Full onboarding redesign; account creation required up front.
**Requirements:** Loading, empty, error, permission-denied and offline states; accessible controls.
**Acceptance criteria:** New user reaches first completed action in a usability test without help; failed save never shows success.
**Dependencies:** DEV-24, DEV-26, C1, A3.
**Analytics:** first_action_completed, time_to_first_value.
**Security/privacy:** Guest mode; no data collected beyond the action. Safety rules: No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise.
**Test cases:**

* Normal: Fresh install to first action.
* Edge: User skips every optional step.
* Failure: Offline at first launch.
* Recovery: App killed mid-onboarding resumes draft (DEV-24).
  **Owner:** Shanivar Frontend
  **Priority:** Highest
  **Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic C met. 
  **Evidence:** Strategy brief 2026-10-17 §9 Epic C; links to design, implementation, test results and decisions to be added here.

