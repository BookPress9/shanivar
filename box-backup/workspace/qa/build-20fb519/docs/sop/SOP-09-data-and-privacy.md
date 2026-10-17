# SOP-09 — Data and privacy

**Purpose:** Collect the minimum, keep practice data on the person's device, delete on request.
**Owner:** Founder (privacy decisions, legal review).

## Preconditions
Data inventory:
| Data | Where | Basis |
|---|---|---|
| Sadhana journal (<=100 rows; practice id, duration, active seconds, outcome, optional Helped/No change/Felt worse; no free text) | device SharedPreferences | `BreathingPreferences.kt:15-18,71`; `BreathingSessionState.kt:192-195` |
| Sankalp line (<=60 chars) | device, own prefs file, never uploaded | `SankalpStore.kt:6-15`; `allowBackup="false"` `AndroidManifest.xml:79` |
| Web app sessions and Sankalp | browser localStorage only | `web-app/README.md` |
| Routine plan, alarm, protection rules | device (and account-linked discipline data on the backend where used) | `RoutinePlanRepository.kt`; `DATABASE.md` |
| Waitlist email + `source` UTM | Netlify Forms | `index.html:261-265` |
| Account, discipline, reflection data | Supabase Postgres | `DATABASE.md` |
Privacy notice is a **DRAFT pending legal review** (`website/README.md:3`; visible notice by the form).

## Steps
1. New data field? Ask: does it change a current choice? If not, do not collect (`PRACTICE_SAFETY.md:32`). No analytics SDK, third-party scripts or cookies without founder approval.
2. Never log or upload practice text, Sankalp, feelings or package lists beyond what a documented API needs.
3. **Waitlist emails:** Netlify sends form notifications and stores submissions (UNRESOLVED: which address receives notifications, and retention of stored submissions). Use emails only to invite to early testing (form hint, `index.html:269`). No bulk mail without founder approval and a consent line.
4. **Delete on request:** (a) waitlist: delete the submission in Netlify Forms and any export, confirm to the person; (b) app: tell the person to clear app data or uninstall for local data (web: footer "Delete all my data on this device"); (c) server account: UNRESOLVED: no documented account-deletion endpoint or procedure; escalate to the founder.
5. Reply to a privacy request only with the founder's approval for the message; log request date and action taken (without the content of the person's data).
6. Legal review of the privacy notice before the site collects emails beyond testing, before any public store listing, and before school-age claims (`shanivar-launch-backlog.md` readiness gate).

## Approval gates
Founder: notice wording, retention periods, any new collection, any export, replying to a data request.

## Rollback
Collection changes: revert the page/form and delete data collected under the old wording only with founder approval.

## Evidence / links
`docs/blueprint/SECURITY.md` §6, `VBT_BREATHING_RELEASE_EVIDENCE_2026-09-25.md:24`, `website/privacy.html`.
