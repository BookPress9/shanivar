Part of <mention-page url="https://app.notion.com/p/3fc10ae18c9f8158abd7e59528eee0a6">Shanivar Product Thesis — Protection, Awareness & Flow</mention-page>. Scope: **first release only**. Owner: Backend lead. Last updated: <mention-date start="2026-10-17"/>.
## Objective
Give the behavior-change loop a dependable, secure and measurable foundation on Supabase `shanivar-prototype` (ap-south-1). Supabase is the only backend; no frontend hosting.
## Top work items (first release)
1. Single-backend decision and staging project, plan and backups ([DEV-40](https://shanivar.atlassian.net/browse/DEV-40), [DEV-41](https://shanivar.atlassian.net/browse/DEV-41))
2. RLS hardening and a cross-user access test suite for every table, RPC and function ([DEV-42](https://shanivar.atlassian.net/browse/DEV-42), [DEV-79](https://shanivar.atlassian.net/browse/DEV-79))
3. Event dictionary, analytics contract and WIAC computation ([DEV-65](https://shanivar.atlassian.net/browse/DEV-65))
4. Idempotent routine completion and sync engine ([DEV-72](https://shanivar.atlassian.net/browse/DEV-72), [DEV-45](https://shanivar.atlassian.net/browse/DEV-45), [DEV-38](https://shanivar.atlassian.net/browse/DEV-38), [DEV-52](https://shanivar.atlassian.net/browse/DEV-52), [DEV-53](https://shanivar.atlassian.net/browse/DEV-53), [DEV-54](https://shanivar.atlassian.net/browse/DEV-54), [DEV-50](https://shanivar.atlassian.net/browse/DEV-50))
5. Auth: guest plus invite-gated email OTP; delete-account ([DEV-48](https://shanivar.atlassian.net/browse/DEV-48), [DEV-46](https://shanivar.atlassian.net/browse/DEV-46))
6. Migrations and functions through CI; logging, retention and backup review; runbook ([DEV-43](https://shanivar.atlassian.net/browse/DEV-43), [DEV-80](https://shanivar.atlassian.net/browse/DEV-80), [DEV-81](https://shanivar.atlassian.net/browse/DEV-81)); credential rotation with the founder ([DEV-8](https://shanivar.atlassian.net/browse/DEV-8))
## Acceptance criteria and definition of done
- No user can read or modify another user's rows (pgTAP plus integration tests).
- Repeated requests never duplicate completions or rewards (idempotency keys, unique constraints).
- Failures are observable (dead-letter, logs) and recoverable.
- No secrets or sensitive data appear in responses, analytics or logs; keys never live in git.
- Every migration is CI-applied and tested, and reversible where feasible; every endpoint documents success, validation and failure.
- **DoD:** review, tests, API docs, monitoring, a rollback note.
## Alignment review: answer by the next weekly status
1. What must change?
2. What already exists?
3. What is missing?
4. What are the dependencies and risks?
5. What can ship in the first release?
6. How will the result be verified?
Record any major interpretation of the thesis in the thesis Decision log before building.