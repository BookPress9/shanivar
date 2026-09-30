# 0005 — `BookPress9/shanivar` is the only repository; `hybridmonk-backend` is retired

- **Date:** 2026-09-12
- **Status:** Accepted — **steps 2-3 verified complete 2026-09-24** (Render is deploying the new
  focus-schedules route from a commit that only exists on `shanivar`, never pushed to the legacy
  repo — confirmed live via `GET /api/discipline/focus-schedules` returning 401, not 404). Nobody
  recorded *when* the dashboard repoint happened; this is downstream evidence, not a witnessed
  change. Step 1 (credential rotation) and step 4 (delete the legacy repo) remain open — see below.

## Context

Two GitHub repositories held versions of this product, with **unrelated git histories** (no merge
base). `hybridmonk-backend` was the deployment source for the live Render service;
`shanivar` was created later as a clean home for the product.

Measured at the deployed commit (`1f77161`) versus `shanivar` (`0cbaab3`):

| | `shanivar` | `hybridmonk-backend` |
|---|---|---|
| Tracked files | 299 | 3,496 — **3,312 of them `node_modules`** (94.7%) |
| Repo size | 1.3 MB | 25.6 MB |
| Secrets | none | live Render API key in plaintext in `render_check.js`; `.env.production` at the tip; `backend/.env`, `frontend/.env.production` in history |
| Extra contents | — | nginx, n8n, Docker, `deploy.zip`, AWS/Hostinger deploy scripts, the retired Next.js web frontend |

Both contain a complete, equivalent `backend/`.

## Decision

`shanivar` is the single source of truth. `hybridmonk-backend` is retired — not cleaned.

## Consequences

- **The leaked Render API key cannot be un-leaked.** It is in commit history; deleting the file
  changes nothing. It must be rotated regardless of what happens to the repository. The same is true
  of every `.env` ever committed there.
- Cleaning that repo in place would require a full history rewrite, which is more work than
  abandoning it and destroys nothing of value that `shanivar` doesn't already hold.
- The unrelated histories are why deploys had required a hand-built "bridge commit" via git
  plumbing. **Consolidating removes that step permanently** — this was the recurring cost, not the
  file count.
- The legacy history is preserved: in `hybridmonk-backend` itself (archived, not deleted) and in the
  local `legacy-main` branch tracking the `legacy-hybridmonk` remote.

## Verification that deleting the legacy repo loses nothing (2026-09-12)

A full path diff was run between the two trunks before authorising deletion:

- **`backend/src` is byte-identical** in both repos. Nothing deploy-critical is unique to the legacy.
- The only `backend/` files unique to the legacy are `puretest.js` and `test_out.txt` — scratch
  harnesses, already excluded by `.gitignore`.
- Everything else unique to the legacy belongs to **The Hybrid Monk**, a different product:
  the Next.js web frontend (56 files), `monk_social_automator`, `monk_social_webapp`, n8n, nginx,
  Docker/AWS/Hostinger deploy scripts, and `database/schema.sql` — whose tables (`journey_stages`,
  `badges`, `community_posts`, `user_chart`, `planet_positions`, `whatsapp_messages`, `ads`) belong
  to that product and have no counterpart in Shanivar's 10 tables.
- `memory/architecture.md`, `memory/api_map.md` and `memory/feature_backlog.md` describe that same
  earlier product — glassmorphism UI, a Gemini "Inner Observer", birth-chart personalisation,
  community circles. **A case-insensitive search across all legacy-only documentation returns zero
  occurrences of "shanivar".**
- Shanivar's own material — `android-app/` (160 files), `shanivar-vbt/` (47), `ai_org/` (11),
  `marketing/` (7), `docs/` — exists only in `shanivar` and was never in the legacy repo.

**Conclusion: the legacy repository may be deleted, subject to the ordering below.**

## Remaining steps — this decision is not yet fully executed

1. **Rotate the Render API key** (leaked in `render_check.js`), the GitHub PAT, and the Supabase
   keys. Do this first; it is independent of everything else.
2. **Repoint the Render service** to `BookPress9/shanivar`, branch `main`. Requires granting the
   Render GitHub App access to the new repository. **Root Directory stays `backend`** — the path is
   identical in both repos, which makes this a low-risk change.
   Service environment variables live on the service, not in the repo, and carry over untouched.
3. **Verify** `GET /api/health` returns 200 with `"database":"ok"` from a deploy sourced from
   `shanivar`.
4. **Only then delete** `hybridmonk-backend`. The verification above establishes that nothing
   Shanivar-related is lost. Founder's call, 2026-09-12: the earlier project is discontinued and its
   history is not worth preserving remotely. A local copy remains on the `legacy-main` branch.

**The ordering is not optional.** Render clones the source repository on every build. Deleting it
while the service still points there leaves production running but unable to redeploy, restart onto
a new instance, or roll back — and the failure surfaces at the worst moment, during the next deploy,
not at deletion time. Steps 2 and 3 must complete first.

Until step 3 passes, the live service still deploys from the old repository. Anyone shipping a
backend change before then must push to **both** remotes or the deploy will not include it.

## Revisit when

Nothing foreseeable. If a second repository is ever proposed (a separate web app, a marketing site),
that is a new decision — and the burden is on it to explain why it is not a directory here.
