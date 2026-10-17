# WEB-001 Codex report

## Result

Replaced the local Sadhana-only prototype with a responsive, installable Shanivar web application. It now includes Today, Routine, Sadhana, records, Living Orbit, account entry and local-first Supabase sync. No backend, migration or native file changed.

## Supabase contract

The web client matches the current Android DEV-45 contract: anonymous auth, email OTP, Google OAuth, authenticated RLS requests, `client_updated_at`, `(user_id,id)` upserts, deletion tombstones, and `put_routine` revision/idempotency calls. The outbox compacts repeated entity writes and survives offline use.

Live E2E remains unverified. The only Supabase project found in local Shanivar configuration is `zpukheowjaxvocbplfmp`, already documented in `docs/SUPABASE_E2E.md` as deleted or changed and currently without a resolvable endpoint. No secret was copied into Git. Deployment must supply a working project URL and public anon key in `web-app/config.js`.

## Product behavior

- Routine supports daily anchors, up to 12 activities, up to 8 protection windows, recurrence days and overlap validation.
- Browsers store and sync desired protection windows, while plainly stating that installed-app blocking and exact device alarms require Android.
- Sadhana uses the five released definitions from the current native catalog. Timers accrue foreground active time and support pause, anchor change, end and interrupted outcome.
- Records keep attempts, active minutes and independent practice separate. Capabilities are factual evidence states without scores or streaks.
- The web shell is responsive and installable, and caches locally for offline reopening.

## Verification

- `node --check web-app/app.js web-app/logic.js web-app/sync.js web-app/sw.js`
- `node --test web-app/test/*.test.js` — 9 passing
- `git diff --check` — clean apart from Windows line-ending notices
- Static server responded successfully on port 8091.
- Visual browser automation was unavailable in this environment, so final real-browser keyboard, mobile viewport and install-prompt QA remains a handoff item.

## Limitations and handoff

1. Supply the active Shanivar Supabase URL and public anon key at deployment time.
2. Register the deployed web URL in Supabase Auth redirect URLs and verify guest, email and Google flows.
3. Confirm the existing backend has `put_routine`; otherwise Routine remains safely queued as designed.
4. Run Chrome/Firefox/Safari real-browser QA, including iOS standalone mode and screen reader checks.
5. Public release still depends on the repository's content/safety review gates.

## Changed files

- `web-app/**`
- `ai_org/collaboration/tasks/WEB-001-web-app-parity.md`
- `ai_org/collaboration/reports/WEB-001-codex.md`
