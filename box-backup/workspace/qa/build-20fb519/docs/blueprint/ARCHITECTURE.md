# Architecture — Shanivar

Sources: `SHANIVAR_TECHNICAL_MAP.md`, `docs/decisions/0001-0014`, `android-app/app/build.gradle.kts`, `backend/src/index.js`. Where this file and the code differ, the code wins and this file gets fixed.

## 1. System Overview
Five surfaces, one repo (decision 0005, `BookPress9/shanivar`):
1. **Android app** `android-app/` (Kotlin, Jetpack Compose, package `com.shanivar.nativeapp`). Owns alarm, app protection, Routine, Sadhana. Sadhana data is local only.
2. **Backend** `backend/` (Node >=18, Express, Postgres via Supabase). Auth, discipline preferences/alarms/focus, telemetry, reflections, plus many legacy routers (`index.js:73-91`).
3. **Website** `marketing/shanivar/routine-v3-onboarding/website/` — static HTML/CSS, Netlify, Netlify Forms.
4. **Web app** `web-app/` — static vanilla JS Sadhana, localStorage, no network (`web-app/netlify.toml` CSP `connect-src 'none'`). Not deployed.
5. **Marketing/Instagram** `@app.shanivar` — content only; paused.
Also `shanivar-vbt/` (practice content, e.g. `shanivar-vbt/content/breathing-mvp-v1.json`) and `research/` (knowledge governance).

## 2. Architecture Pattern
- **Pattern:** Native client with a thin API (modular monolith backend; routes per domain in `backend/src/routes/`).
- **Reason:** The API still does auth, ownership checks, idempotency and AI orchestration (`android-app/DEVICE_DISTRIBUTION.md` "API hosting decision"). Sadhana/Steady deliberately builds with no backend or LLM dependency (decision 0011:16).

## 3. Technology Stack
- **Frontend (Android):** Kotlin, Compose BOM 2024.12.01, Material3, CameraX 1.4.1, ML Kit barcode 17.3.0, coroutines 1.9.0 (`android-app/app/build.gradle.kts:56-68`). Java 17. compileSdk 35, minSdk 26.
- **Frontend (web):** plain HTML/CSS/JS, no build step.
- **Backend:** Express 4.21, pg 8, jsonwebtoken, bcryptjs (cost 12, `routes/auth.js:53`), express-rate-limit, xss, circular-natal-horoscope-js (`backend/package.json`).
- **Database:** PostgreSQL on Supabase, always through the Supavisor pooler (0004). See `DATABASE.md`.
- **Authentication:** email/password with JWT, 7-day expiry (`routes/auth.js:120`).
- **Storage:** Android SharedPreferences; web localStorage. No object storage (`SHANIVAR_TECHNICAL_MAP.md:284`).
- **Hosting:** API on Render (`index.js:29-36` comment; `DEVICE_DISTRIBUTION.md`: `https://hybridmonk-api.onrender.com/api`). Website on Netlify (SOP-02). Web app: planned Netlify (SOP-03).
- **Payments:** none live; `routes/billing.js` exists. UNRESOLVED: is billing used by anything?
- **Email:** none for users. Netlify Forms emails notify the founder on sign-ups (SOP-09). UNRESOLVED: who sends tester invites (`routine-onboarding-marketing-plan.md:266` D5).
- **Analytics:** none; no crash reporting (`SHANIVAR_TECHNICAL_MAP.md:278`). Alarm-fire telemetry is a table, not analytics (`routes/telemetry.js`).
- **Other:** Gemini via `GEMINI_API_KEY` (`routes/ai.js`; reflection generation). UNRESOLVED: is any AI path active in the shipped app? 0009 bans an AI coach in V1.

## 4. System Components
### Android
- Alarm engine: `AlarmScheduler`, `AlarmFireReceiver`, `AlarmForegroundService`, `AlarmRinger`, dismissal activities (movement/QR/photo).
- App protection: `FocusGuardService`, `ForegroundAppBlocker`, `BlockOverlayActivity`, `FocusScheduler` (overlay + usage polling; 0002).
- Routine V3: `RoutineShell`, `RoutineScreen`, `RoutinePlan*`, `RoutineProtection*`, `RoutineTimeline` (flag-gated).
- Sadhana: `BreathingSessionOwner` (process-scoped object, 0012), `BreathingScreen`, `BreathingCatalog` + `assets/breathing-v1.json`, `InitiationSitting`, `SankalpStore`, `BreathingPreferences` (local journal).
- Responsibilities: enforce safety invariants locally; never gate silencing on network (`ENFORCEMENT_STANCE_2026-09-12.md:68-70`).
### Backend
- Authenticate, validate, own-row checks, idempotency; run inline schema DDL at boot (0001).
### Database
- Source of truth for account-linked discipline data only. Sadhana records are not stored server-side.

## 5. Architecture Diagram
```
Android app --HTTPS--> Express API (Render) --pooler--> Postgres (Supabase)
   |  local: SharedPreferences (Sadhana journal, Sankalp, Routine plan)
Browser --> shanivar.com (Netlify static) --form POST--> Netlify Forms
Browser --> web-app (localStorage only; not deployed)
```
UNRESOLVED: which Android calls hit the API in the current build (see `DisciplineApi.kt`, `RoutineApi.kt`; not audited here).

## 6. Data Flow (alarm dismissal, as designed)
Alarm fires (exact alarm) → full-screen intent → user completes movement/accessible path → ringer silenced synchronously → dismissal recorded fire-and-forget to backend. Network failure never keeps the alarm ringing (`ENFORCEMENT_STANCE_2026-09-12.md:64-70`).

## 7. Project Structure
`android-app/app/src/main/java/com/shanivar/nativeapp/` (flat package); `backend/src/{routes,middleware,utils,lib}`, `backend/tests/`; `docs/{decisions,strategy,blueprint,sop,sources}`; `research/deep-foundation/` (stance sources); `ai_org/agents/` (roles); `marketing/shanivar/`.

## 8. Dependency Rules
- Enforcement never uses Device Admin or Accessibility Service (0002, permanent).
- Sadhana data stays local; it must not reach the breathing journal's neighbours: Sankalp has its own prefs file and is never uploaded (`SankalpStore.kt:6-15`).
- A practice can never dismiss an alarm or release a focus lock (0011:18; ST-006).
- Alarm silence never depends on the backend.
- No secrets in the APK (`DEVICE_DISTRIBUTION.md` "API hosting decision").

## 9. Key Technical Decisions
| Decision | File | Reason in one line |
|---|---|---|
| Inline schema DDL at boot | 0001 | no runner yet; revisit at 2nd instance or ~15 tables |
| No Device Admin/Accessibility | 0002 | Play policy and stalkerware-like behaviour |
| Movement-first dismissal + accessible always | 0003 | sleep inertia; never trap |
| Supavisor pooler only | 0004 | connection limits |
| Single repo | 0005 | one source of truth |
| Recurrence recomputed after each fire | 0008 | no pre-generated occurrences |
| Process-scoped breathing owner | 0012 | survive rotation, shared by all hosts |
| Routine + Sadhana shell | 0014 | accepted product form |

## 10. Performance
UNRESOLVED: no performance budgets. Known: Render free tier cold start 22.6 s observed (`DEVICE_DISTRIBUTION.md`); rate limit 10,000 req/15 min per IP (`index.js:64-68`); auth endpoints 20 per 15 min (`index.js:30-35` comment, `routes/auth.js`).

## 11. Scalability
Single backend instance assumed. Second instance triggers the migration-runner work (0001). `SHANIVAR_TECHNICAL_MAP.md:283` lists SharedPreferences-as-database as debt.

## 12. Failure Scenarios
- **DB down:** API errors; alarms and protection keep working from local state. UNRESOLVED: verify and test.
- **API timeout:** dismissal and telemetry are fire-and-forget; silencing is local.
- **Third party (Gemini, Netlify):** not on any safety path.
- **Auth fails:** UNRESOLVED: offline behaviour of Routine when token expires.
- **Sensor missing:** accessible dismissal shows immediately (invariant 4).
- **Process death mid-practice:** session is saved as interrupted and never counts as practised (`BreathingSessionState.kt:102`, `:204`).
- **Known debt:** no CI (DEV-15), no migration runner (DEV-9), no signing/Play prep (DEV-17), no hardware verification (DEV-7) — `SHANIVAR_TECHNICAL_MAP.md:274-280`.
