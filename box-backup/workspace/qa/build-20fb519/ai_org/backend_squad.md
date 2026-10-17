# Shanivar Backend Dev + QA Squad

Staff-manager-run squad for the existing Express/Postgres/Supabase backend at `backend/`. Distinct from both the WhatsApp lean-loop track (`shanivaar_agents.md`) and the VBT native-app track (`shanivar_vbt_track.md`) — this squad works on the already-built `backend/` API that the existing Next.js frontend (and Codex's ongoing frontend work) consumes.

## Founder decision on record (2026-09-09)
The Supabase project this backend pointed to (`zpukheowjaxvocbplfmp`) no longer resolves via DNS on either its DB host or REST API host — confirmed not a sandbox network issue (other hosts resolve fine). Founder decision: **do not stand up a real database right now.** Dev agents fix bugs and improve code quality via static review + unit tests; QA validates only against the existing in-memory `backend/src/mock_server.js` and Jest unit tests. Real-DB-backed behavior stays explicitly unverified until the founder resolves Supabase (new project or restored credentials) and says so.

## Staff Manager
Splits backend work by product domain (not by file, to keep agent count sane against ~2200 LOC across 14 route files), sequences dev vs. QA, reconciles overlapping findings, and is the sole point that escalates back to the founder when an agent surfaces something only the founder can decide (e.g. a design/product ambiguity, a security tradeoff, another missing external credential).

## Dev Agent 1: Foundation & Auth
**Owns**: `src/routes/auth.js`, `src/routes/users.js`, `src/middleware/auth.js`, `src/db.js`, `src/lib/supabase.js`, `src/index.js`.
**Focus**: JWT handling, password hashing, auth middleware correctness, CORS/rate-limit config in `index.js`, SQL parameterization in `db.js` usage.

## Dev Agent 2: Core Practice Loop
**Owns**: `src/routes/practices.js`, `src/routes/journey.js`, `src/routes/gamification.js`, `src/utils/GamificationEngine.js`, `src/routes/habits.js`, `src/routes/journal.js`.
**Focus**: the gamified consciousness-app core loop (XP, stages, daily practices, journaling, habit tracking).

## Dev Agent 3: Astrology / AI / Insights
**Owns**: `src/routes/chart.js`, `src/routes/insights.js`, `src/routes/ai.js`, `src/routes/analytics.js`, `src/utils/AnalyticsEngine.js`.
**Focus**: Gemini API integration correctness and error paths, insight/analytics generation logic. Heaviest external-dependency surface in the codebase.

## Dev Agent 4: Community / Commerce / Integrations
**Owns**: `src/routes/community.js`, `src/routes/content.js`, `src/routes/ecommerce.js`, `src/routes/billing.js`, `src/routes/whatsapp.js`.
**Focus**: the Hybrid Monk e-commerce bridge, community features, WhatsApp integration points (note: unrelated to the separate WhatsApp lean-loop product in `ai_org/shanivaar_agents.md` — this is Shanivar's own webhook/notification surface).

## QA Agent
**Owns**: `backend/tests/`, `backend/src/mock_server.js` as the validation harness, test infrastructure.
**Known issue to fix first**: `chart.test.js` and `insights.test.js` `require()` route modules directly without loading `dotenv`, so `process.env.GEMINI_API_KEY` is undefined at import time in tests even though `.env` has it set — this produces false failures, not real bugs. Fix at the test-infrastructure level (e.g. a Jest `setupFiles` entry loading dotenv), not by patching individual test files.
**Focus**: run the full suite, distinguish real bugs from DB-unreachability-caused failures, extend coverage via the mock server where a real DB would otherwise be needed, and produce one consolidated bug list back to the Staff Manager — explicitly separating "real bug, fixed/needs fixing" from "cannot verify without live DB, flagged for later."

## Founder decisions on the 7 post-squad questions (2026-09-09)
1. **Journal XP cap** — implemented: only the first journal entry per calendar day awards XP (extra entries still save, just no additional XP). Matches practices/habits pattern.
2. **Content premium/stage gating** — founder said "make the best decision yourself." Implemented server-derived gating (`resolveAccess` in `content.js`): stage and membership are looked up from the authenticated user's real DB row, never trusted from a client query param. Locked items stay visible in listings (teaser, no body/media) rather than disappearing, for discoverability/upgrade-path UX.
3. **Sanitization** — founder: backend should sanitize. Implemented `backend/src/utils/sanitize.js` (using `xss`, not `sanitize-html` — the latter's `htmlparser2` dependency is ESM-only and breaks under Jest) applied to journal entries and community posts/replies.
4. **JWT_SECRET fallback** — founder: make the best call. Now hard-fails at boot in production if unset; falls back to a dev-only value with a console warning outside production.
5. **Tighter rate limit on auth** — founder: yes. Added a dedicated 20-req/15-min limiter on `/api/auth/login` and `/api/auth/register`, on top of the existing blanket API limiter.
6. **Case-sensitive email / signup race** — founder: keep UX intuitive, make the best call. Emails are now normalized to lowercase on register and login (so `Foo@x.com` and `foo@x.com` are the same account). The registration race is handled by catching a unique-constraint violation (Postgres code `23505`) on insert and returning 409 instead of a raw 500.
7. **Dead `ASTROLOGY_SERVICE_URL` / no real transit-data source** — founder: point it at a microservice that exists, research it, then plug it into the backend. A dedicated agent was spun up for this (separate from the 4 domain dev agents) to research a real astronomical-calculation solution (self-hosted library preferred over a hosted API requiring new credentials) and wire it into `chart.js`/`insights.js`.

All 7 items are now resolved. Item 7 landed as: `circular-natal-horoscope-js` (self-hosted, Unlicense, no API key, no network dependency) wraps a Moshier analytical ephemeris, verified against known reference charts (Einstein's natal chart, published planetary positions for Jan 1 2000). `chart.js` no longer calls Gemini at all for chart data (real calculation replaces the old LLM-invented positions); `insights.js` now feeds Gemini real computed transit data instead of a vague instruction. No founder action needed — no signup, no API key, no recurring cost. Final state: 195/195 tests passing, 18 suites.

## Live-database re-verification pass (2026-09-09)
Founder resumed the paused Supabase project (real DB, ~102 pre-existing users from prior testing). The backend server was started for real against it, and two real bugs surfaced immediately that the fully-mocked test suite structurally could not catch:
1. `chart.js` had `const pool = require('../db')` (grabs the whole `{query, pool}` module object) then called `pool.connect()` for its transaction — crashed with `pool.connect is not a function`. Same bug class Dev Agent 4 already fixed in `ecommerce.js` during the mocked pass. Fixed to `const { pool } = require('../db')`.
2. `GEMINI_MODEL=gemini-2.0-flash` in `.env` is retired — Gemini's API returned a 404 telling us to use `gemini-3.6-flash`. Updated `.env`. Fixes both `ai.js` and `insights.js` (chart.js no longer calls Gemini at all).

Both fixed and verified via live smoke test: register → login → chart generate → chart read-back → ai chat → insights generate, all real 200/201s against the live DB and live Gemini.

All 4 domain dev agents ran a second pass against the live DB (ports 5011-5014, tagged test accounts). Results, final state 195/195 tests / 18 suites:

- **Dev 1 (Foundation/Auth)**: found and fixed a real timezone bug — Postgres `DATE` columns were parsed into JS `Date` objects at local-server-midnight then serialized to UTC, silently shifting the calendar day backward on any host with a positive UTC offset. Fixed centrally in `db.js` (`types.setTypeParser(1082, ...)`), covers every `DATE` column app-wide.
- **Dev 2 (Core Practice Loop)**: found `practice_completions` had no DB-level unique constraint (app-level guard only) — added a real unique index. Found two badge `condition_type`s (`active_habits`, `journal_entries`) were permanently unreachable since `GamificationEngine` never queried for them — fixed.
- **Dev 3 (Astrology/AI/Insights)**: found Gemini's internal "thinking tokens" were silently consuming the entire `maxOutputTokens` budget in `insights.js`, truncating real insight generation with no error surfaced — fixed by raising the budget (2000). Found the same risk in `ai.js` and it was proactively fixed too. Found `ai.js`'s failure-path fallback was tagged identically to a real AI reply, giving users no signal the model wasn't actually reached — fixed to match the existing "offline mode" convention. **Flagged a cross-team, founder-level finding**: `auth.js`'s registration still seeds a hardcoded dummy chart (Leo/Scorpio/Aries, lat/lon 0,0) rather than using the real astrology engine — confirmed live that 100 of 101 real `user_chart` rows have zero real planet/house data. Registration doesn't currently collect birth location, so this can't be silently switched to real calculation without a product decision (collect lat/lon at signup, or drop the pre-seed and require an explicit `/chart/generate` call). **Not yet resolved — needs founder input.**
- **Dev 4 (Community/Commerce/Integrations)**: found the `ecommerce.js` webhook referenced a column and a table (`tailoring_pipeline_status`, `user_measurements`) that don't exist in the live schema — the earlier `pool.connect` fix let the transaction start, but it always rolled back with "column does not exist." Fixed with a self-healing migration block matching `db.js`'s existing pattern. Some of its planned live end-to-end assertions (billing round-trip, content admin-gating, whatsapp/community round-trip) weren't completed before a session restart interrupted it — open for a follow-up pass if full coverage is wanted.

Also found and fixed directly by the Staff Manager: `chart.js`'s `pool.connect` destructuring bug, and the deprecated `GEMINI_MODEL` (`gemini-2.0-flash` → `gemini-3.6-flash`).

Also updated for real-backend integration: `android-app/gradle.properties`'s `shanivarApiUrl` switched from the isolated mock fixture (`10.0.2.2:5101`) to the real backend (`10.0.2.2:5000`). `frontend/.env.development`'s `NEXT_PUBLIC_API_URL` already pointed at `http://localhost:5000/api` — no change needed there.

## Reporting protocol
Every dev/QA agent reports back to the Staff Manager: what it found, what it fixed, and anything it could not resolve or that requires a founder decision. The Staff Manager does not let an agent guess on founder-level ambiguity — it escalates instead.
