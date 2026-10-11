# Shanivar pre-launch website (draft, local, not deployed)

Status on 6 October 2026: **founder approved go-live.** Hosted on Netlify (free tier), with the waitlist collected by Netlify Forms. A visible notice beside the form says the privacy notice is a draft pending legal review. No third-party script, font or CDN is used.

Domain: **https://shanivar.com** is the canonical domain. The founder owns it, registered through Hostinger (RDAP: registrar Hostinger, registered 2025-10-04, currently on parking nameservers). It is used in canonical, Open Graph, `robots.txt` and `sitemap.xml`.

## Preview

```bash
cd marketing/shanivar/routine-v3-onboarding/website
python3 -m http.server 8000
# open http://localhost:8000/
```

Plain HTML and CSS, one small vanilla script (`site.js`), no build step. `404.html` uses root-relative links so it works when a host serves it at any missing path. Under `python3 -m http.server` a missing path shows Python's own 404 page instead, which is expected.

## Pages and files

| File | Purpose |
| --- | --- |
| `index.html` | Hero, problem, how Routine works, the timeline, Sadhana and Steady, the stance, what Shanivar isn't, the early-access waitlist (with repeated CTAs) and the FAQ |
| `about.html` | Why Shanivar exists, The Hybrid Monk, how we work, a dated status table. No founder bio (TODO) |
| `privacy.html` | **DRAFT** privacy placeholder, clearly marked, pending legal review |
| `404.html` | Not-found page ("Attention moved. Return."), `noindex` |
| `style.css` | Visual system adapted from the V3 wireframe |
| `site.js` | Copies UTM tags into the hidden `source` field and onto internal links. The form submits natively to Netlify Forms |
| `thanks.html` | Shown after a waitlist sign-up (noindex) |
| `favicon.svg` | Saturn motif |
| `og-image.png` | 1200×630 Open Graph image, generated locally with PIL (no remote image) |
| `robots.txt`, `sitemap.xml` | For https://shanivar.com |
| `preview-mobile.png`, `preview-desktop.png` | Full-page screenshots of `index.html` at 390 px (2× scale) and 1280 px, from headless Chrome |

## Visual system

Taken from `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/{README.md,style.css,index.html}`: black panels (`#0b0d0b`, `#0f120f`), cream paper (`#f2efe6`), sage (`#d8e5b7`), muted gold (`#c9aa73`, or `#7a5c2b` on paper), editorial serif headings, light/dark foreground pairs (dark text on paper, light text on black, never dark-on-dark), gold focus outline, and the Saturn motif labelled as "not a breathing pacer". Fonts are system stacks only: a Palatino/Georgia-style serif for headings and `system-ui` for body text. `prefers-reduced-motion` stops the Saturn orbit and smooth scrolling.

Accessibility: semantic landmarks, skip link, labelled inputs with hints, visible `:focus-visible`, AA-contrast colour pairs, at least 44 px targets, and fluid type with no horizontal overflow at 320 px or 640 px wide (640 px matches a 1280 px screen at 200 % zoom).

## Positioning note

**Founder's words (6 October 2026, relayed by the dispatching agent):** Shanivar is a very strict, high-velocity application for high-velocity people who want to move and track things, and who feel their life has been going off lately. Frame it as a calling: a moment to change your life.

**How I applied it.** The hero and the waitlist copy are sharper and more urgent, following the V3 voice contract in the wireframe README ("wild enough to wake the user up… direct enough to create action… serious without becoming authoritarian or shame-based") and its pattern of truth, then contradiction, then instruction, then action:
- truth: "Work gets calendars. Deadlines get reminders. Other people get your structure." (onboarding UI)
- contradiction: "Your own life gets memory and mood."
- instruction: "Treat this as the moment you stop leaving it to chance."
- action: "Join the early-access list."

**How I translated "strict".** I wrote it as **high standards and a coach that expects work** ("Strict, the way a good coach is strict. High standards. A clear next action. An expectation that you do the work.") and stated outright that "Strict does not mean caged."

**Why.** `android-app/ENFORCEMENT_STANCE_2026-09-12.md` ("never promise the impossible", "write as if anyone could", no Device Admin, no fee to unlock early), decision 0014 (immediate local stop and recovery, essential access) and the shipped UI copy ("Protection is voluntary and stoppable — never unbreakable") all rule out lock-in language. The site therefore has no "can't escape", "unbreakable", "we won't let you" or "no excuses" shame lines. The tone guide adds: "A missed session is information." "Notice before you react" stays as the depth beneath the speed (stance section).

**"Track things".** I deliberately did **not** claim tracking. The Routine V3 handoff keeps reminders and activity-completion tracking behind a separate future contract, and Sadhana progression and history are still undesigned (0013, 0014). The site says "One timeline, built from what you saved" instead.

## Funnel

The early-access waitlist is the entry point of the marketing funnel.

```text
social posts / link in bio  ->  shanivar.com (this site)  ->  waitlist form
   ->  M1/M2 tester invite (early Android test builds)  ->  M3 Play Store release
```

- The waitlist CTA appears in the header ("Join the list", on every page), the hero, two CTA bands (after Routine and after the stance), the early-access section and the About page.
- The site does not show milestone codes. It says only that people on the list are invited first when test builds are ready.
- Companion plans being written in this folder by another worker (referenced by name; they may not exist yet): `../social-presence-from-scratch.md`, `../zero-to-million-playbook.md`, `../routine-onboarding-marketing-plan.md`.

### UTM link patterns

`site.js` reads `utm_source`, `utm_medium` and `utm_campaign` (each capped at 64 characters), writes them into the hidden `source` field as `source / medium / campaign` (default `direct`), and carries them onto internal links. Nothing leaves the browser.

| Use | Link |
| --- | --- |
| Instagram bio | `https://shanivar.com/?utm_source=instagram&utm_medium=bio&utm_campaign=m0_waitlist` |
| Instagram post or Reel CTA | `https://shanivar.com/?utm_source=instagram&utm_medium=post&utm_campaign=m0_waitlist#early-access` |
| Instagram story link | `https://shanivar.com/?utm_source=instagram&utm_medium=story&utm_campaign=m0_waitlist#early-access` |
| X profile | `https://shanivar.com/?utm_source=x&utm_medium=bio&utm_campaign=m0_waitlist` |
| WhatsApp share | `https://shanivar.com/?utm_source=whatsapp&utm_medium=share&utm_campaign=m0_waitlist#early-access` |
| Direct or QR | `https://shanivar.com/?utm_source=qr&utm_medium=offline&utm_campaign=m0_waitlist` |

Pattern: lowercase `utm_source` = platform, `utm_medium` = placement, `utm_campaign` = `m<milestone>_<goal>`.

### Form backend: Netlify Forms (chosen 6 Oct 2026)

The founder approved go-live. The form uses `data-netlify="true"`, a `bot-field` honeypot and real `name` attributes, and redirects to `thanks.html`. The original options are kept below for the record.

A static host serves files only, so it **cannot collect sign-ups by itself**. The form is static markup with a visible "TODO: form not connected" flag. Its inputs have `data-field` attributes but no `name` attributes, so nothing can be submitted. Options for the founder to choose (nothing has been set up or signed up for):

1. **Netlify Forms (free tier):** host the site on Netlify, add `name`, `data-netlify="true"` and a honeypot field to the form, and rename `data-field` to `name`. Submissions appear in the Netlify dashboard. Check the current free-tier submission limits before choosing.
2. **Google Form:** link to or embed a Google Form, or post to its form-response endpoint. No hosting change, but it loads a Google page or script, so update the privacy notice and the "no third parties" claim.

Either way: consent text, data retention, deletion route and the privacy notice need legal review **before** the form goes live.

## Copy-source table

Every headline and product claim, with the repo file it rests on. "UI" means a verbatim string in `android-app/app/src/main/java/com/shanivar/nativeapp/`.

| Site copy (page · section) | Source |
| --- | --- |
| "Discipline is freedom." (index · hero H1, title, OG; founder-given 2026-10-11) | UI `RoutineOnboardingFlow.kt` welcome: `DISCIPLINE IS FREEDOM` |
| "Build the day you intend to live." (index · Routine copy, meta description) | UI `RoutineOnboardingFlow.kt` welcome |
| "Work gets calendars. Deadlines get reminders. Other people get your structure." (hero) | UI `RoutineOnboardingFlow.kt` welcome |
| "Your own life gets memory and mood… the moment you stop leaving it to chance." | Wireframe README voice example ("memory and mood") + UI heading "Stop leaving your life to chance." + founder positioning (calling) |
| "Routine shapes the day. Sadhana trains attention until the practice can leave the phone…" | Decision 0014 (two destinations) + UI welcome "Train attention until the practice can leave the phone and enter your life." |
| "Stop leaving your life to chance." (problem H2) | UI `RoutineOnboardingFlow.kt` welcome heading |
| "You follow calendars… memory and mood. That is backwards. Put it on the calendar." | `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md`, voice contract example |
| Reaching for the phone at boredom, effort or discomfort; "You opened it for one thing. What was it?" | `docs/strategy/shanivar-founder-direction.md` (founder problem); `marketing/shanivar/audience-direction.md` (content pillar hook) |
| "Shanivar is not here to keep you busy. It is here to make what matters harder to ignore." | UI welcome footer line |
| "Strict, the way a good coach is strict…" / "A missed day is information." | Founder positioning (README note above); wireframe README ("coach that expects work"); `docs/strategy/shanivar-tone-and-values.md` ("A missed session is information") |
| "Wake. Focus. Protect time. Put your personal work on the calendar." (Routine H2) | UI welcome card |
| "Give the day two fixed points." / "Not an ideal morning. Your actual life." / "I usually wake around" / "I make room for sleep around" | UI `RoutineOnboardingFlow.kt` (Your day); handoff fidelity checklist |
| Activities: name, time, duration, Every day/Weekly/Custom; Gym, Read a book, Extend Sadhana are editable; never arm alarm or protection | `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md` (personal-activity editor); UI `RoutineScreen.kt` ("This does not arm an alarm or start protection.") |
| "Set it once. Then get on with life." / "Two tools. Both editable…" | UI `RoutineOnboardingFlow.kt` (Guardrails) |
| Wake alarm: "Start when you said you would."; stand or move, accessible non-physical exit, hard cap ends ringing | UI (`Dismissal stays humane`); `ENFORCEMENT_STANCE_2026-09-12.md` invariants 3–4 and the 13 Sept clarification |
| App protection: "Choose what loses access to you."; multiple windows, overnight, Every day/Weekdays/Weekends/Custom | UI; decision 0014 Routine contract; `RoutineProtectionScreen.kt` ("Overnight: starts on the chosen day…") |
| "Voluntary and stoppable. Never unbreakable." / "You already know which apps take more than they give. Select them once." | UI `RoutineOnboardingFlow.kt` (app protection) |
| "Essential access stays available." / "Calls and system apps are never selected." | UI `RoutineOnboardingFlow.kt`, `RoutineProtectionScreen.kt` |
| "Stop protection anytime: no waiting, no payment, no forced practice, no server." | UI `RoutineOnboardingFlow.kt` (device access) |
| Hard to undo from bed, not impossible; bypass via Settings, browser, second device | `ENFORCEMENT_STANCE_2026-09-12.md` Boundary 1 and app-lock known limits |
| No device-administrator tricks to block uninstall | `ENFORCEMENT_STANCE_2026-09-12.md`; `docs/decisions/0002-no-device-admin-enforcement.md` |
| "Saved is not Active"; states Saved, Needs device access, Ready, Active, Needs attention, Off | Decision 0014 (rule states); UI ("Saved is not Active.", "Needs attention", "Active") |
| "Shanivar will say it needs device access. It will not pretend you are protected." | UI device-access screen |
| Timeline built from saved state; status only on device-backed events; no ideal biological timetable | Handoff (timeline, personal-activity editor); decision 0014 (no dosha/cortisol/circadian calculation) |
| Timeline sample times | Illustrative only, labelled "Illustration with sample times. Not a screenshot of the app." |
| "Do the work. Need fewer prompts." (Sadhana H2) | UI welcome card |
| "The practice is working when you can carry it without the player." | Wireframe `app.js` (Sadhana chapter copy) |
| Steady: "Natural breath. Nothing else. Do not make it deeper…"; one minute; confirm stationary; no pacing or holding; pause or end anytime; optional reflection that may include discomfort | Wireframe README (Steady foundation practice) + `app.js`; decision 0011 (unpaced, no retention); 0013 (validator refuses breath timing). Labelled "as designed" |
| Steady is the only Sadhana content in internal builds; needs review before public release | Decision 0014 (Phase 1); `android-app/VBT_BREATHING_RELEASE_EVIDENCE_2026-09-25.md` (content review NOT RUN) |
| "Progression waits."; no streaks, points or ranks; time in the app never unlocks intense practice | UI welcome ("Progression waits"); decision 0013; decision 0014 (no ranks, XP or streak pressure; ban on activity-based escalation) |
| "Less reliance on Shanivar can be a good outcome." | Decision 0014 (preserved constraints); stance ST-008 (PROPOSED, used only as an aim) |
| "Notice the mind. Don't control it." (stance H2) | Dispatch brief's phrasing, **not a verbatim repo line**. Supported by Steady ("without control", "do not make it deeper") and founder direction ("notice boredom or an urge without immediately reaching…") |
| "Notice what is already happening inside, before you react." | Founder direction (presence begins with noticing); wireframe Steady cue "notice the breath already happening" |
| Patient teacher: notices, asks a question, may suggest one small practice, gives space | `research/deep-foundation/FOUNDER_BRIEF_2026-09-29.md` §3 |
| Disagree, skip or stop; stopping is valid | `SHANIVAR_STANCE.md` ST-003/ST-004 (PROPOSED; used as product behaviour, not doctrine); tone guide |
| "Leave Shanivar more present than when you opened it." | Founder brief §2 |
| Not a detox, not wellness or therapy, not a meditation library, not a horoscope, not social, not a trap | Tone guide (no clinical or one-minute claims); ST-007 (no astrology in the current launch surface); handoff "Do not build" (no social or family reporting); enforcement stance (no fee to unlock early) |
| Android only, 8.0 or newer | `android-app/app/build.gradle.kts` `minSdk = 26`; `SHANIVAR_TECHNICAL_MAP.md` ("No iOS") |
| Early testing adults 18+ | `marketing/shanivar/audience-direction.md` (first cohort adults 18+) |
| Phone makers behave differently | `ROUTINE_V3_DEVICE_QA_2026-10-05.md` §8.3 (OEM battery restrictions); native contract (Samsung, Xiaomi) |
| Offline: alarm, windows and stop designed to run without network or server | Decision 0014 (reliability and authority); labelled "still being verified" |
| Pricing not decided; never a fee to end protection early | No pricing in repo; `ENFORCEMENT_STANCE_2026-09-12.md` ("Charge to unlock early … ruled out") |
| About: "Built by someone who needed it first" plus the founder problem | `docs/strategy/shanivar-founder-direction.md` (founder problem, success and departure) |
| About: status table | `build.gradle.kts` (`ROUTINE_V3` false), `ROUTINE_V3_DEVICE_QA_2026-10-05.md` (blank, not run), technical map (no release signing), decision 0013 |
| Privacy: Routine plan stored locally | `RoutinePlanRepository.kt` (local SharedPreferences JSON); handoff domain model |
| Privacy: no cloud backup | `AndroidManifest.xml` `allowBackup="false"`; technical map §1.11 |
| Privacy: Steady makes no network calls; feeling label not stored | `VBT_BREATHING_RELEASE_EVIDENCE_2026-09-25.md` (local privacy gate) |
| Privacy: no analytics or push in the app | Technical map §1.13 (no FCM) and §1.16 (analytics absent in app) |
| Privacy: device-access grants and their purposes | UI device-access screen ("What each grant is for") and alarm screen (notifications) |
| Privacy: account features can store preferences, alarms and schedules on a server | `RoutineApi.kt` (`/discipline/preferences`, `/alarms`, `/focus-schedules`); technical map §1.3 |

## Claims deliberately left out (build state)

Verified on 6 October 2026: `ROUTINE_V3` is `false` in `android-app/app/build.gradle.kts`; the physical-phone QA sheet `ROUTINE_V3_DEVICE_QA_2026-10-05.md` (DEV-7) has no results; there is no release `signingConfig` (technical map), so no Play release is possible; Steady's content review for public release is NOT RUN.

Therefore the site does **not** include:
- "Download now", "Get it on Google Play", a Play badge or any store link
- user counts, ratings, testimonials, reviews or press quotes
- any claim that Routine, protection or the alarm is live, Active, tested or reliable on real phones (everything is "in development" or "as designed")
- "unbreakable", "impossible to bypass", "can't escape", "we won't let you", "no excuses" or any lock-in framing
- tracking or progress claims (activity completion, history, streaks), because they are not in Phase 1
- Sadhana chapters, levels or progression (provisional in 0013 and 0014), the 8-practice catalogue by name, or any breathing technique beyond Steady
- astrology, charts, AI guidance, the "Messages from Shanivar" scroller, reflection features, family or helper features: older intended features, not current launch scope
- health, sleep, anxiety, dopamine, circadian or productivity outcomes
- pricing, free or paid tiers, a launch date, iOS
- a founder name, photo or bio, or The Hybrid Monk clothing-brand details from the older root README

## Open TODOs

- **Domain:** owned (Hostinger); DNS is still on parking, so point it to a host only when the founder approves deploy.
- **Form backend:** founder chooses Netlify Forms (free tier) or a Google Form; then wire up `name` attributes, add a honeypot, and update the privacy notice and consent copy. Until then the form is preview-only (`TODO(form-backend)` in `index.html` and `site.js`).
- **Privacy legal review:** `privacy.html` is a DRAFT placeholder. Needs a lawyer's review, a full Android permission list from the release build (including camera and activity recognition, used by the alarm), account data handling, hosting logs, UTM storage and a contact address.
- **Play link:** gated on the M3 public Play release, which needs release signing, Play Console declarations (exact alarm, full-screen intent, special-use foreground service) and passing device QA. Add no store badge before then.
- **Founder content:** founder name, photo and bio for About, only as supplied and approved.
- **Hosting:** choose a host (it also decides the form option) and configure `404.html` as the error page.
- **Git tracking:** `.gitignore` line 40 (`marketing/shanivar/*`) ignores this folder, so it was committed with `git add -f`. Codex may want an explicit exception, for example `!marketing/shanivar/routine-v3-onboarding/` (proposal only; `.gitignore` is outside this task's owned path).

## Deploy gates

1. **No deploy until the founder approves** this exact revision, the host and the form backend.
2. **Nothing is described as available** until `ROUTINE_V3` is on in a release build **and** DEV-7 physical-device QA passes. Store links wait for the M3 Play release.
3. The privacy notice must pass legal review before any sign-up is collected.
4. Any copy change that adds a product claim needs a new row in the copy-source table above.

## Verification (6 October 2026)

- Served with `python3 -m http.server`. A small link checker (kept outside the repo) fetched all 10 files (200) and every internal `href`/`src` and `#fragment` target on the four pages: **0 failures, no external URLs** apart from the `https://shanivar.com` canonical/OG/sitemap URLs.
- Headless Chrome (CDP): no horizontal overflow at 320 px (all pages) or 640 px (index, about 200 % zoom at 1280 px); UTM test `?utm_source=instagram&utm_medium=bio&utm_campaign=m0_waitlist` filled `source` with `instagram / bio / m0_waitlist` and carried the tags onto internal links.
- Screenshots: `preview-mobile.png` (390 px CSS, 2×) and `preview-desktop.png` (1280 px), full page, colour-quantized to keep them small.
