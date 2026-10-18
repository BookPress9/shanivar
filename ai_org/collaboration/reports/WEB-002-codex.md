# WEB-002 production launch report

## Prepared on 18 October 2026

- Production client bundle builds from ignored `.env.production.local`; only the public Supabase URL and anon key enter `dist/config.js`.
- The supplied service-role credential is not stored, committed or deployed.
- First-run onboarding, Routine anchors, account choice, browser notification permission, offline shell and local continuation are implemented.
- Chrome mobile E2E covers onboarding, Routine editing, a Sadhana attempt, factual record creation, persistence and offline reopening.
- `shanivar.com` now serves the login link to `https://app.shanivar.com/`; the deployed and pushed landing commit is `89b4e471`.

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

- Production app site under the Shanivar domain-owner account: `https://shanivar-web-app.netlify.app`, Netlify site id `80e33eaa-6ade-4659-9eea-1fedc1e498ea`.
- Current app deploy: `6ad43af0d53d9e0af4efcca2`. The previous verified deployment remains available at `https://shanivar-app.netlify.app` during DNS cutover.
- Netlify SSO protection was disabled for this app site so production is publicly reachable.
- Authenticated Chrome E2E passes against production. Supported attempt and Orbit records leave the outbox; Routine stays queued because `put_routine` is missing.
- The production Auth callback is built for `https://app.shanivar.com`. Google remains visibly pending because the provider is disabled.
- Netlify accepted `app.shanivar.com` on the new app site. Hostinger DNS still returns NXDOMAIN; add `CNAME app -> shanivar-web-app.netlify.app`, then wait for Netlify TLS provisioning.
- The landing site deployed successfully as `6ad43b7d16e6fcfd810e1dfc`; `https://shanivar.com` returns HTTP 200 and contains the production app link.
- Supabase administration is still blocked: no management token or database password exists in the local environment, and the connected browser reaches GitHub's Supabase OAuth sign-in screen. The supplied service-role JWT is rejected by the project and cannot apply migrations or configure providers.

## Validation

- `npm test`: 12 passing.
- `npm run test:e2e`: passing in installed Chrome at 390×844.
- Production build: 12 static files, configured project reference present, no service-role claim present.
- Landing-page diff check: clean.
