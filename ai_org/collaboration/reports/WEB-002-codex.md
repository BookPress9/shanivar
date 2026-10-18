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

- Public production site: `https://shanivar-app.netlify.app`, Netlify site id `2661df83-5332-4d2f-940b-c9755dbfa359`.
- Current deploy: `6ad438a33131079d5e7e6540`.
- Netlify SSO protection was disabled for this app site so production is publicly reachable.
- Authenticated Chrome E2E passes against production. Supported attempt and Orbit records leave the outbox; Routine stays queued because `put_routine` is missing.
- The production Auth callback currently uses the reachable Netlify URL. Google is visibly pending because the provider is disabled.
- `app.shanivar.com` remains blocked: it has no DNS record and Netlify says the hostname is already reserved on existing site `b3437357-9883-451b-b902-20140f08139a`, which the authorized Book Press account cannot access.
- The landing login change was pushed to `main` at `531d40d9`, but the live landing site has not auto-deployed that commit.

## Validation

- `npm test`: 12 passing.
- `npm run test:e2e`: passing in installed Chrome at 390×844.
- Production build: 12 static files, configured project reference present, no service-role claim present.
- Landing-page diff check: clean.
