# Shanivar Sadhana, web version (internal test)

Small static web version of the Android Sadhana tab: "Your first sitting" (six-step body-scan sitting) and the 8 unpaced Steady practices. Plain HTML/CSS/vanilla JS. No framework, no build step, no third-party scripts, no network requests, no cookies. Everything is saved in this browser's localStorage only.

Content is reused verbatim from `shanivar-vbt/content/breathing-mvp-v1.json` (catalogVersion vbt-breathe-2026-09-25.1) and `android-app/.../InitiationSitting.kt`. Rules follow `BreathingSessionState.kt` (90 s rule), `SankalpStore.kt` (60 chars), `PRACTICE_SAFETY.md` and `SHANIVAR_STANCE.md`.

## Files
`index.html`, `style.css`, `app.js` (UI + session player), `logic.js` (pure logic), `content.js` (copy and practices), `netlify.toml`, `test/logic.test.js`.

## Run
    cd web-app
    python -m http.server 8091      # then open http://localhost:8091
Tests (no dependencies): `node --test test/`

## Deploy (later, not done)
Separate Netlify site with base dir `web-app`, publish `.`, no build command (`netlify.toml` sets the headers: nosniff, no-referrer, CSP `default-src 'self'`). Then, once shanivar.com DNS is fixed, add a CNAME `app` -> that Netlify site and set `app.shanivar.com` as its domain. No site has been created and nothing has been deployed or pushed.

## Behaviour
- Sessions: Arrive / Practice / Close, countdown timer, Pause, Skip and End any time. No breath animation or cue. Page hidden = auto-pause; closing the page mid-session saves an "interrupted" row.
- A session counts as practised only if active time (pauses excluded) is >= 90 s and it was not interrupted. Shorter sessions (including the 1-minute option) are saved but do not count.
- "Last practised today / yesterday / 3 Oct" per practice; no counts, streaks or scores.
- Sankalp line: max 60 characters, localStorage only, shown on the End screens, Clear button.
- "Delete all my data on this device" in the footer (two-step).

## Not in it
No login, no app protection/blocking, no alarm, no notifications, no sync, no analytics, no sound or tone (the Android tone for "Follow a fading sound" is not included; the page says so), no recorded or generated voice. The optional per-step audio hook (`sitting_guide_<step>.mp3`) was left out on purpose: probing for files would cause 404 requests.

## Open review items
1. The body-scan script is a draft from an unattributed Instagram post and is not reviewed by a qualified human (PRACTICE_SAFETY.md). Do not release publicly before review. The in-page internal-test notice must stay until then.
2. Steady practice texts and VBT source labels are "implementation seed"; need Sanskrit/source and safety review.
3. Decide: ask the founder whether a 1-minute option should stay, given it can never count as practised.
4. Hum, then listen: confirm the web copy is acceptable without any audio consent step.
5. localStorage can be cleared by the browser; there is no backup by design.
6. Real-device accessibility check (screen reader, keyboard) not yet done.
