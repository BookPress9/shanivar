# 0005 — `BookPress9/shanivar` is the only repository; `hybridmonk-backend` is retired

- **Date:** 2026-09-12
- **Status:** Accepted — **execution incomplete**, see *Remaining steps*

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

## Remaining steps — this decision is not yet fully executed

1. **Rotate the Render API key** (leaked in `render_check.js`), the GitHub PAT, and the Supabase
   keys. Do this first; it is independent of everything else.
2. **Repoint the Render service** to `BookPress9/shanivar`, branch `main`. Requires granting the
   Render GitHub App access to the new repository. **Root Directory stays `backend`** — the path is
   identical in both repos, which makes this a low-risk change.
   Service environment variables live on the service, not in the repo, and carry over untouched.
3. **Verify** `GET /api/health` returns 200 with `"database":"ok"` from a deploy sourced from
   `shanivar`.
4. **Only then archive** `hybridmonk-backend` (read-only). Do not delete it — it is the sole
   remote record of the earlier project phase.

Until step 3 passes, the live service still deploys from the old repository. Anyone shipping a
backend change before then must push to **both** remotes or the deploy will not include it.

## Revisit when

Nothing foreseeable. If a second repository is ever proposed (a separate web app, a marketing site),
that is a new decision — and the burden is on it to explain why it is not a directory here.
