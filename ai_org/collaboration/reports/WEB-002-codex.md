# WEB-002 production launch report

## Prepared on 18 October 2026

- Production client bundle builds from ignored `.env.production.local`; only the public Supabase URL and anon key enter `dist/config.js`.
- The supplied service-role credential is not stored, committed or deployed.
- First-run onboarding, Routine anchors, account choice, browser notification permission, offline shell and local continuation are implemented.
- Chrome mobile E2E covers onboarding, Routine editing, a Sadhana attempt, factual record creation, persistence and offline reopening.
- `shanivar.com` login copy is prepared on `codex/site-login-2026-10-18`.

## Live Supabase findings

- Project API resolves at `katleeaentaugfszpmus.supabase.co`.
- Anonymous guest signup succeeds.
- Email auth is enabled and confirmation is required.
- Google auth is disabled.
- Authenticated reads succeed for `routine_plans`, `protection_windows`, `practice_attempts`, `self_reports` and `orbit_events`.
- `tombstones` returns 404 and `put_routine` is not established, so deletion sync and Routine cloud sync remain pending backend deployment.
- The client now parks a missing Routine RPC and continues syncing supported records.
- A temporary anonymous test user was created. Cleanup through the supplied service-role key failed because Supabase rejected that key as invalid; remove that test user from the Auth dashboard during production administration.

## Deployment state

- Netlify login ticket `759b682748b21f88093fcfaab8bcdab0` is pending authorization.
- `app.shanivar.com` has no DNS record.
- No production deploy or DNS change has occurred yet.

## Validation

- `npm test`: 10 passing.
- `npm run test:e2e`: passing in installed Chrome at 390×844.
- Production build: 12 static files, configured project reference present, no service-role claim present.
- Landing-page diff check: clean.
