# SOP-03 — Web app (app.shanivar.com) deploy

**Purpose:** Publish the static Sadhana web app when the founder decides. **Not deployed; no Netlify site exists** (`web-app/README.md`, "Deploy (later, not done)").
**Owner:** Founder decides; Web Developer/Codex executes.

## Preconditions
- Body-scan script reviewed by a qualified human, or the page keeps its internal-test notice and stays private (`web-app/README.md` open item 1; `PRACTICE_SAFETY.md:55`).
- Open items 2-6 in `web-app/README.md` triaged (seed texts needing source and safety review; 1-minute option; hum copy; accessibility check).
- `node --test test/` passes in `web-app/`.
- shanivar.com DNS working (SOP-02).

## Steps
1. Run `node --test test/` and open locally (`python -m http.server 8091`).
2. Founder approves creating a **separate** Netlify site: base dir `web-app`, publish `.`, no build; `netlify.toml` already sets security headers and CSP `connect-src 'none'`.
3. Deploy with the CLI to that site (`--dir web-app`), draft first, then production on approval.
4. Add DNS `CNAME app -> <new site>.netlify.app` and set `app.shanivar.com` as the domain; confirm HTTPS.
5. Verify: no network requests, localStorage works, Delete-all works, internal-test notice shown if still unreviewed.
6. Record the new site id in this SOP.

## Approval gates
Founder: creating the site, DNS change, removing the internal-test notice, announcing it.

## Rollback
Unpublish via Netlify deploy history or remove the CNAME. User data lives in each visitor's browser only; nothing to restore server-side.

## Evidence / links
`web-app/README.md`, `web-app/netlify.toml`, `web-app/logic.js:4` (rules mirror Android).
