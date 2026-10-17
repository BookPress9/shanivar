# SOP-02 — Website change and deploy (shanivar.com)

**Purpose:** Change and publish the pre-launch static site safely.
**Owner:** Web Developer role (`ai_org/agents/web_developer.md`) coordinated by Codex; founder approves every deploy.

## Preconditions
- Source: `marketing/shanivar/routine-v3-onboarding/website/` (plain HTML/CSS, `site.js`, no build step; `website/README.md`).
- Netlify site id `b3437357-9883-451b-b902-20140f08139a` (founder-supplied; not recorded in repo).
- DNS for shanivar.com (founder-supplied state 2026-10-11): `A @ 75.2.60.5`, `CNAME www -> shanivar.netlify.app`; HTTPS on. Registrar Hostinger (`website/README.md:5`). Repo docs of 6 Oct still say "parking nameservers" — superseded, re-verify before relying (UNRESOLVED).
- Copy follows `docs/sop/SOP-05` (claims) and `ui-copy-inventory.md`. Tagline "Discipline is freedom." is applied by a separate job (`marketing/shanivar/brand/tagline.md`).

## Steps
1. Edit files locally; preview: `python3 -m http.server 8000` in the website folder.
2. Check: no claim of availability/download/launch date; "Android app in development"; no health or streak language; privacy notice still marked DRAFT; no third-party scripts (`website/README.md:3`).
3. Forms: the form is `name="early-access"`, `data-netlify="true"`, honeypot `bot-field`, hidden `source` filled from UTMs (`index.html:261-265`). Do not rename fields without updating `site.js`.
4. Commit locally (SOP-08).
5. With founder approval: deploy with the Netlify CLI against the known site: `netlify deploy --dir <website folder> --site b3437357-9883-451b-b902-20140f08139a` (draft preview URL first), check it, then `--prod` only on the founder's word. Auth stays in the CLI login; never paste tokens into files.
6. Post-deploy: load https://shanivar.com and https://www.shanivar.com over HTTPS; submit one test sign-up and see it in Netlify Forms with its `source`; delete the test submission.

## Approval gates
Founder: every production deploy, DNS change, privacy-notice change, any new data collected, any change to what the form promises ("Used only to invite you to early testing").

## Rollback
In Netlify, restore the previous production deploy (Deploys > publish deploy). DNS rollback only with founder: restore previous records (record the old values first). UNRESOLVED: previous DNS values (parking) are not recorded.

## Evidence / links
`website/README.md`, `routine-onboarding-marketing-plan.md:266` (D5), `social-presence-from-scratch.md:83,179` (bio link rule), `.netlify/` link folder is untracked local state (do not commit).
