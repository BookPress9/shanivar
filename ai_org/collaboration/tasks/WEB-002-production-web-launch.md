# WEB-002 · Production web launch

- **State:** IN_PROGRESS
- **Owner:** Codex
- **Started:** 2026-10-18

## Goal

Make the web application production-ready, connect Shanivar authentication, deploy it to `app.shanivar.com`, and add a login route from `shanivar.com`.

## Owned paths

- `web-app/**`
- `site/**` on a dedicated landing-site branch
- `docs/sop/SOP-03-web-app-deploy.md`
- this task and its report

## Acceptance checks

- First-run onboarding is explicit and resumable.
- Guest, email and Google auth paths are testable against the active Shanivar Supabase project.
- Local-only use remains available if auth or networking fails.
- Browser-supported notification permission is optional; Android-only exact alarms and installed-app blocking are labeled pending.
- Automated, real-browser and live Supabase tests pass.
- Separate Netlify production sites serve `shanivar.com` and `app.shanivar.com` over HTTPS.
- `shanivar.com` exposes a clear login/open-app link to `https://app.shanivar.com`.
- Production identifiers and rollback steps are recorded without committing credentials.
