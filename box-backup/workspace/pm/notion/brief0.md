Part of <mention-page url="https://app.notion.com/p/3fc10ae18c9f8158abd7e59528eee0a6">Shanivar Product Thesis — Protection, Awareness & Flow</mention-page>. Scope: **first release only**. Owner: Frontend lead. Last updated: <mention-date start="2026-10-17"/>.
## Objective
Make the move from protection to intentional action intuitive, reliable and low-friction on native Android.
## Top work items (first release)
1. First-action flow: a new user completes one meaningful action without configuring everything ([DEV-73](https://shanivar.atlassian.net/browse/DEV-73), [DEV-24](https://shanivar.atlassian.net/browse/DEV-24), [DEV-26](https://shanivar.atlassian.net/browse/DEV-26))
2. Block overlay: show why the intervention appeared and record the choice on-device ([DEV-69](https://shanivar.atlassian.net/browse/DEV-69), [DEV-68](https://shanivar.atlassian.net/browse/DEV-68), [DEV-10](https://shanivar.atlassian.net/browse/DEV-10))
3. Missed-step recovery and non-punitive return ([DEV-74](https://shanivar.atlassian.net/browse/DEV-74))
4. Protection failure surfacing: service killed, permission revoked ([DEV-70](https://shanivar.atlassian.net/browse/DEV-70), [DEV-67](https://shanivar.atlassian.net/browse/DEV-67))
5. Stage 1 practice with audio built into the practice; Sadhana screens S1–S7 ([DEV-36](https://shanivar.atlassian.net/browse/DEV-36), [DEV-37](https://shanivar.atlassian.net/browse/DEV-37), [DEV-27](https://shanivar.atlassian.net/browse/DEV-27)–[DEV-33](https://shanivar.atlassian.net/browse/DEV-33)); fix the breathing content ([DEV-39](https://shanivar.atlassian.net/browse/DEV-39), [DEV-89](https://shanivar.atlassian.net/browse/DEV-89))
6. Sync wiring, guest sign-in and build flavours ([DEV-47](https://shanivar.atlassian.net/browse/DEV-47), [DEV-44](https://shanivar.atlassian.net/browse/DEV-44)); loading, empty, error, permission-denied and offline states; instrument the DEV-65 events
## Acceptance criteria and definition of done
- A first-time user finishes one meaningful action with no full setup.
- The user can tell why the intervention appeared and what each choice does.
- A missed action never creates a loop or an unrecoverable state; a failed request never shows success.
- Each event fires once per action, with no rerender duplicates (verified by DEV-83).
- No breath pacing, no claims, no teacher names; TalkBack and 200% font pass.
- **DoD:** code review, unit and UI tests, docs, telemetry, the QA suite passes on real devices.
## Alignment review: answer by the next weekly status
1. What must change?
2. What already exists?
3. What is missing?
4. What are the dependencies and risks?
5. What can ship in the first release?
6. How will the result be verified?
Record any major interpretation of the thesis in the thesis Decision log before building.