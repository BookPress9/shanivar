# 0016 - Initiation V1 wireframe adopted as design source of truth

- **Date:** 2026-10-12
- **Status:** Accepted (founder, voice, 12 October 2026; transcribed and interpreted in chat)
- **Design source of truth:** `android-app/wireframes/initiation-v1/index.html` (with `app.js`, `style.css`, `exports/`) and `android-app/wireframes/initiation-v1/HANDOFF.md`.

## Decision
1. **Accounts at launch:** yes, with the wireframe's sign-up options: Google, email code, and guest (local). Future only (ticket, not now): phone/SMS OTP, WhatsApp, mobile authenticator and app-based auth, and iPhone options once iOS exists.
2. **Cloud sync:** yes. Routine, protection windows, practice attempts and Orbit events sync per the HANDOFF API and data contracts.
3. **Living Orbit:** accepted as designed, including its glow after return days or a self-reported independent practice. Founder direction for the visual system: many circles throughout the app, with subtle "graffiti", connecting marks between them that people subconsciously join, like real dots.
4. **60-second practice:** kept as designed (foreground timer, stationary confirmation, pause, immediate End). It is a starting point; Sadhana will go much deeper later.
5. **Copy and look:** keep the wireframe as is; do not rewrite. Learn from it.

## Supersedes
- **0014**, where it makes the phone the only authority and the data local-only. The account and cloud sync model in HANDOFF.md now applies. The device stays the authority for device truth: alarms, enforcement and immediate local stop (HANDOFF "Android is responsible for device truth").
- **The privacy local-first stance** (`marketing/shanivar/routine-v3-onboarding/website/privacy.html`, "designed to be local-first", "No cloud backup"). The privacy notice must be updated before release, and that update needs founder approval (`docs/sop/SOP-02-website-change-and-deploy.md:21`).
- **The no-progress concern for the Orbit** (0009, 0013, PRD non-goals): the Orbit glow is accepted. Still no numeric score, ranks, leaderboards or streak decay, as the HANDOFF itself says.

## Still binding
- `research/deep-foundation/PRACTICE_SAFETY.md`: the planet or orbit must never pace breathing. Motion during practice stays ornamental, slow and unrelated to breath, and is static for reduced motion. Stationary confirmation and End stay.
- The protection honesty rules (voluntary, stoppable, never "unbreakable"; `android-app/ENFORCEMENT_STANCE_2026-09-12.md`), no health claims, and ST-010/ST-011 naming rules.

## Follow-ups
Jira: future sign-in methods; the circles-and-connecting-marks visual system. Backend auth needs new passwordless support (`backend/src/routes/auth.js`, per HANDOFF).
