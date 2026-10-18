# SOP-03 — Web app (app.shanivar.com) deploy

**Purpose:** Publish and operate the Shanivar web application. A production Netlify site now exists at `https://shanivar-app.netlify.app` (site id `2661df83-5332-4d2f-940b-c9755dbfa359`).
**Owner:** Founder decides; Web Developer/Codex executes.

## Preconditions
- Body-scan script reviewed by a qualified human, or the page keeps its internal-test notice and stays private (`web-app/README.md` open item 1; `PRACTICE_SAFETY.md:55`).
- Open items 2-6 in `web-app/README.md` triaged (seed texts needing source and safety review; 1-minute option; hum copy; accessibility check).
- `node --test test/` passes in `web-app/`.
- shanivar.com DNS working (SOP-02).

## Steps
1. Run `node --test test/` and open locally (`python -m http.server 8091`).
2. Use the separate Netlify site `shanivar-app`; build from `web-app` with `npm run build` and deploy `web-app/dist`.
3. Deploy with the CLI to that site (`--dir web-app`), draft first, then production on approval.
4. Add DNS `CNAME app -> shanivar-app.netlify.app` and set `app.shanivar.com` as the domain; confirm HTTPS. As of 18 October 2026, Netlify reports that hostname is reserved on landing-site id `b3437357-9883-451b-b902-20140f08139a`; use the account owning that site to move the hostname before changing DNS.
5. Verify guest/email authentication, authenticated record sync, localStorage, offline reopening, Delete-all and browser capability copy. Routine revision sync and tombstone deletion remain pending until their existing Supabase contracts are deployed.
6. Record each production deploy id in the WEB task report.

## Approval gates
Founder: creating the site, DNS change, removing the internal-test notice, announcing it.

## Rollback
Unpublish via Netlify deploy history or remove the CNAME. User data lives in each visitor's browser only; nothing to restore server-side.

## Evidence / links
`web-app/README.md`, `web-app/netlify.toml`, `web-app/logic.js:4` (rules mirror Android).
