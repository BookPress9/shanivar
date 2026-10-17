# Security — Shanivar

Sources: `backend/src/index.js`, `backend/src/middleware/auth.js`, `.gitignore`, `ENFORCEMENT_STANCE_2026-09-12.md`, decisions 0002/0004, `SHANIVAR_TECHNICAL_MAP.md`. This is a baseline, not a security audit.

## 1. Authentication
Backend: email/password, bcrypt cost 12, JWT 7 days (`routes/auth.js:53,120,151`). Methods: email/password only. Passwordless/OTP deferred (`shanivar-launch-backlog.md`). Android Sadhana and web-app need no account.

## 2. Authorization
Roles in JWT: `role` claim; admin check `middleware/auth.js:43`. Own-row checks are done in routes. Server-side enforcement is required. UNRESOLVED: which routes are admin-only; no authorization matrix exists.

## 3. Secrets
- Never hard-code or commit secrets. `.gitignore` excludes `.env*` except `.env.example/.template` — added after `backend/.env` was once committed and pushed in a predecessor repo (`.gitignore` comment).
- Never ship `SUPABASE_SERVICE_ROLE_KEY` or `DATABASE_URL` in the APK (`DEVICE_DISTRIBUTION.md`).
- **UNRESOLVED (urgent, ops):** `SHANIVAR_TECHNICAL_MAP.md:281` records unrotated leaked credentials (GitHub token, Render key). Confirm rotation status with the founder. Do not write any credential into docs.
- The JWT_SECRET must be set; absence would let anyone mint tokens (`middleware/auth.js:7` comment).

## 4. Environment Variables
Private (backend): `DATABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_ANON_KEY`, `SUPABASE_URL`, `JWT_SECRET`, `GEMINI_API_KEY`, `SHOPIFY_WEBHOOK_SECRET`. Config: `PORT`, `NODE_ENV`, `FRONTEND_URL`, `GEMINI_MODEL`. Public (Android): API base URL via `-PshanivarApiUrl` (`android-app/app/build.gradle.kts:38-40`). Netlify: no env vars used by the static site.

## 5. API Security
- **Input validation:** server-side; `xss` sanitizer.
- **Rate limiting:** global 10,000/15 min (`index.js:64`); login/register 20/15 min; `trust proxy` = 1 so the limiter keys on the real client (`index.js:37`).
- **CORS:** allow-list incl. shanivar.com, thehybridmonk.com, localhost (`index.js:39-57`). UNRESOLVED: app.shanivar.com is not on the list; the web app makes no API calls, so none is needed today.
- **Body limit:** `express.json 10mb` (`index.js:61`). UNRESOLVED: is 10 MB intended?
- Auth and ownership: see §1-2.

## 6. Data Protection
- **Local-only (device):** Sadhana journal (<=100 rows, no free text, no feelings), Sankalp line (own prefs file, never uploaded, `allowBackup="false"` — `AndroidManifest.xml:79`; `SankalpStore.kt:6-15`), web-app localStorage ("Delete all my data on this device").
- **Server:** account, discipline and reflection data (`DATABASE.md` §10).
- **Website:** email + source from the waitlist stored by Netlify Forms; privacy notice is a DRAFT pending legal review (`website/README.md:3`).
- **Encryption/retention/deletion:** UNRESOLVED: retention periods; deletion path for server rows and Netlify submissions. See SOP-09.
- Practice-safety data minimum: ask only what changes a choice; no mental-health profiles (PRACTICE_SAFETY.md:32,44).

## 7. Logging
Never log passwords, tokens, keys, Sankalp text, reflection text. Backend currently logs errors to console (`index.js:110-113`). UNRESOLVED: no structured logging or crash reporting (`SHANIVAR_TECHNICAL_MAP.md:278`).

## 8. File Uploads
None accepted by the product. Photo-motion dismissal uses the camera on-device (`CAMERA` permission, manifest `:58`). UNRESOLVED: confirm photos never leave the device.

## Android permissions (declared, `AndroidManifest.xml`)
INTERNET, PACKAGE_USAGE_STATS, SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE (+SPECIAL_USE), SCHEDULE/USE_EXACT_ALARM, USE_FULL_SCREEN_INTENT, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS, CAMERA, ACTIVITY_RECOGNITION, VIBRATE, WAKE_LOCK. Play policy for exact alarm, full-screen intent and special-use services is an open risk (DEV-17, `routine-onboarding-marketing-plan.md:254`). No Device Admin, no Accessibility (0002).

## Enforcement safety (security-adjacent, release blockers)
Invariants 1-8 in `ENFORCEMENT_STANCE_2026-09-12.md:64-84`: synchronous silencing, never backend-gated, 15-minute watchdog, sensor failure never traps, finally/onDestroy teardown, bounded wake locks, idempotent dismissal, an always-available escape that is recorded.

## 9. Security Checklist (current honest state)
- [x] Secrets excluded from git by `.gitignore`
- [x] Rate limiting present
- [x] Passwords hashed
- [ ] Credential rotation confirmed (UNRESOLVED)
- [ ] Authorization matrix written
- [ ] Retention/deletion defined
- [ ] Dependency review/audit (no CI, DEV-15)
- [ ] Release signing and key custody (DEV-17)
- [ ] Privacy notice reviewed by legal
