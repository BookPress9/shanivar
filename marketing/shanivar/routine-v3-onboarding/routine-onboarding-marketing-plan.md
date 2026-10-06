# Routine V3 onboarding — marketing plan (handoff-ready)

Prepared 6 October 2026 by the Grok Bot collaborator (`app.shanivar`) for the Social Media Manager (`ai_org/agents/social_media_manager.md`). **Plan only. Marketing execution is paused by founder instruction (6 October 2026, given in this task; not yet recorded in a repo file).** Nothing here has been published, scheduled, sent or paid for, and no recurring routine has been turned on. This plan builds on the existing marketing operating system (`marketing/shanivar/marketing-operating-system.md`) and does not replace it.

Companion files: `source/ui-copy-inventory.md` (the only allowed source of product language), `social-presence-from-scratch.md` (account setup), `zero-to-million-playbook.md` (long-range growth), `chatgpt-subscription-assessment.md` (tools), `../wish-list.md` (things not built and not promised).

---

## 1. Build truth on 6 October 2026 (verified in files)

| Fact | Evidence |
| --- | --- |
| Routine V3 and its onboarding are on `main` | Commits `2914d4e` (Routine V3) and `94b9c87` (V3 onboarding). `git log` at HEAD `6d26bb2` |
| The build flag is **off** | `android-app/app/build.gradle.kts:33`: `buildConfigField("boolean", "ROUTINE_V3", "false")` |
| Real users get the **older** onboarding | `android-app/app/src/main/java/com/shanivar/nativeapp/ShanivarApp.kt:182-218` (V3 flow only when `shouldUseRoutineV3Onboarding` is true, otherwise `OnboardingFlow(...)`); gating rule at `RoutineOnboarding.kt:182-183` |
| Physical-phone QA (DEV-7) has **never run** | `android-app/ROUTINE_V3_DEVICE_QA_2026-10-05.md`: device table and every Pass/Fail cell are empty; Jira DEV-7 status "Pending", title "Run the 8-check physical-device protocol — it has never executed once" (read 6 Oct 2026) |
| Routine V3 "cannot be called ready" while physical-device tests are unrun | `android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md:121-133` |
| V3 becomes the default **only after** the device matrix | `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:334-340` (R4) |
| "Active" may only be shown from engine-confirmed state | `docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md:43` |
| Related focus bugs still **Open** in Jira | DEV-2 (multi-window END), DEV-3 (saved reported as armed), DEV-6 (all-day preset), read 6 Oct 2026. Commit `2914d4e` addresses them in code ("multi-window end, honest Active, all-day"), but device verification (QA sheet sections 3, 6, 7) is pending |
| Sadhana progression is **undecided** | Jira DEV-16 "Pending"; `docs/decisions/0013-sadhana-progression-and-the-gamification-question.md` (PROPOSED); `docs/decisions/0014-…md:18` |
| **No public release, no Play listing** | `android-app/DEVICE_DISTRIBUTION.md`: debug APK served from a LAN HTTP server; "there is no hosted distribution channel set up yet"; Firebase App Distribution only *recommended*. Jira DEV-17 "Open": no release `signingConfig`, release API host `api.thehybridmonk.com` NXDOMAIN, Play Console declarations (`USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, `specialUse` services) unresolved |
| Version | `android-app/app/build.gradle.kts:13-14`: `versionCode = 1`, `versionName = "0.1.0"` |
| Existing public account | `@app.shanivar` on Instagram (`marketing/shanivar/execution-2026-09-30/operations.md:36-40`; `marketing/shanivar/month-02/README.md:5`). One founder-reported post, BASE-01 (`marketing/shanivar/marketing-operating-system.md:60`). No later publication is established |
| Website domain | **shanivar.com is owned by the founder** (registered through Hostinger; founder-confirmed 6 Oct 2026). A separate worker builds the site in `marketing/shanivar/routine-v3-onboarding/website/` (read only here). Its waitlist form now posts to **Netlify Forms**, with the founder's go-live approval on 6 Oct 2026 (`website/README.md:3`, `:83-85`; `website/index.html:260-279`; commit `257855f`). It collects email, optional Android phone model, an 18+ confirmation and a hidden UTM `source` field. **Not live yet:** https://shanivar.com did not respond at 06:21 CEST on 6 Oct 2026, and the README still lists parking nameservers (`website/README.md:5`, `:169`) |

**Plain conclusion:** Routine V3 is written but **not in anyone's hands**. Nothing may say a Routine feature is available, live, Active, working or tested until the flag is on **and** DEV-7 passes. Section 6 sets out the full gate ladder.

---

## 2. Founder positioning (his words) and its guardrails

**Founder positioning, 6 October 2026, as given:** Shanivar is *a very strict, high-velocity application for high-velocity people who want to move and track things, and who feel their life has been going off lately.* It is framed as *a calling, a moment to change your life.*

How this plan uses it:

- **Audience:** sharpens the target to **high-velocity people who feel off-track**: people who normally move fast and keep score, and who sense the last weeks or months have drifted (section 4).
- **Hooks:** the "calling" becomes an invitation to **take the day back now**, phrased as a choice and never as a verdict. Example: "If the last few months have felt off, this is your moment to take the day back." It must not say "you've wasted your life."

**Guardrails (non-negotiable, from the files):**

1. **"Strict" means high standards and a coach that expects work.** The V3 wireframe voice contract asks for "direct enough to create action… serious without becoming authoritarian or shame-based" and "a coach that expects work, not generic wellness reassurance" (`android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md:14`, `:46-64`; copy at `source/shanivar-v3-wireframe-README.md`).
2. **"Strict" never means lock-in.** The product says "Protection is voluntary and stoppable — never unbreakable" (`RoutineOnboardingFlow.kt:468`) and "Stop protection anytime — no waiting, no payment, no forced practice, no server" (`RoutineOnboardingFlow.kt:538`). Decision 0014 keeps "immediate essential access and local Focus recovery" and "honest enforcement limitations" (`docs/decisions/0014-…md:56-57`). The enforcement stance bans "impossible" in marketing and requires us to "write as if anyone could" bypass it (`android-app/ENFORCEMENT_STANCE_2026-09-12.md:26-29`, `:137-140`).
3. **No shame and no productivity-bro framing.** The V3 voice contract excludes "shame, diagnosis" (`…/shanivar-v3/README.md:64`). The tone guide says a missed session "is information. Do not accuse, moralize" (`docs/strategy/shanivar-tone-and-values.md`, Writing rules). The handoff says copy "may not shame the person or infer why they failed" (`android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:283`).
4. **"Track things" is not built.** Personal activities are calendar entries: "Saving one does not… claim completion. Keep any later reminder or completion-tracking capability behind a separate contract and explicit user choice" (`android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:154`). Marketing may say "Put your personal work on the calendar." (`RoutineOnboardingFlow.kt:279`) and may not say "track your progress", "streaks" or "habit tracking". Tracking is parked in `../wish-list.md`.
5. **"Change your life" is an invitation, not an outcome.** ST-008 says "No claim of lasting transformation from short observation windows" (`SHANIVAR_STANCE.md`, ST-008). The moment of decision is the user's. Shanivar does not promise the result.

**Open founder decision (not silently resolved):** "very strict, high-velocity" pulls against the recorded **patient-teacher** instruction (`SHANIVAR_STANCE.md:11`: "preserve the patient-teacher voice; leave authority with the practitioner"), against the voice direction's "notice an ordinary moment, offer an optional way to observe, and leave the choice with the person" (`marketing/shanivar/voice-direction.md`, 2 Oct section), and against the two-entry audience that includes parents (`marketing/shanivar/audience-direction.md`). The V3 wireframe already moved the app's own voice towards "direct, serious and action-oriented" (`…/shanivar-v3/README.md:14`). This plan uses V3's direct voice for Routine content and keeps the patient, observational voice for Sadhana/Steady and editorial posts. **The founder should confirm or replace that split** (section 9, D1).

---

## 3. What to promote now versus later

| Content type | Example | Allowed now (Phase 0)? |
| --- | --- | --- |
| **The problem** (life drifting, structure going to everyone except you, apps taking more than they give) | "Work gets calendars. Deadlines get reminders. Other people get your structure." | **Yes**, no product claim needed |
| **The idea / stance** (decide once, protect chosen time, stoppable boundaries, need fewer prompts) | "Choose what loses access to you." | **Yes**, framed as what Shanivar is being built to do |
| **Build-in-public status** | "Routine V3 is written. It has not been tested on a real phone yet. That happens next." | **Yes**, and it must be exactly true on the day of posting |
| **Design previews** | V3 wireframe or debug-build screens with a persistent "DESIGN IN DEVELOPMENT — NOT RELEASED" label | **Yes, with label.** No demos of protection or alarms "working" (`ai_org/agents/ugc_video_producer.md`: do not generate product UI as evidence) |
| **Feature available / works / tested** | "Set multiple protection windows tonight." "Blocks apps overnight." | **No.** Gated (section 6) |
| **Download / install / Play Store** | "Download Shanivar" | **No.** Gated to Phase 3 |
| **User outcomes / testimonials** | "I got 2 hours back" | **No, ever, unless** it is a real, consented, dated self-report and labelled as one (`marketing/shanivar/marketing-operating-system.md:34`) |
| **Sadhana progression / levels / streaks** | "Level up your Sadhana" | **No.** Undecided (DEV-16); a streak/XP framing also conflicts with ST-005 and decision 0009 |

---

## 4. Target audience (reconciled)

**Primary for Routine V3 pre-launch:** India-based, English-comfortable adults about 18–34, matching the 18+ first cohort (`ai_org/agents/market_fit_release_steward.md`, "First cohort remains 18+"; India focus in the same file). They are **high-velocity people who feel off-track**: students, early-career knowledge workers, founders and creators who usually move fast and keep score, and who feel the last stretch has slipped to feeds and other people's schedules.

How this reconciles with the existing files:

- `marketing/shanivar/audience-research.md` named "adults 22–34 in India who work or create on screens" as the first testable hypothesis, with the objection "Another routine I will abandon". That is the same group, now sharpened by the founder's "off-track" lens. The "Not an ideal morning. Your actual life." angle answers that objection directly.
- `marketing/shanivar/audience-direction.md` sets two entry paths: (1) adult children introducing parents, (2) younger self-directed users. **Path 2 leads Routine V3 marketing.** Path 1 is **held, not dropped**: the V3 onboarding has no helper/handback step (verified in `RoutineOnboardingFlow.kt`; the flow is Welcome, Your day, Guardrails, Alarm, App protection, Device access), and Hindi UI is blocked by DEV-13 (strings not extracted). Parent-path content resumes when those exist. This is a sequencing choice for the founder to confirm (D3).
- Secondary: millennials 31–44 balancing work and care ("a short return to attention without arranging a perfect morning", `audience-research.md`, segment 2). "Your actual life" speaks to them too.
- Excluded: minors (`audience-research.md`: "Do not target minors"); diagnosis-based or vulnerability-based targeting (same file).
- Language: English first (Hindi app copy blocked by DEV-13). Hindi editorial posts can continue only with fluent review (`marketing/shanivar/month-02/operations.md`).

---

## 5. Messaging angles (drawn only from `source/ui-copy-inventory.md`)

Each angle quotes the UI verbatim. In Phase 0, hooks describe **the problem or the intention**, never a working feature.

### A1 — "Discipline for real life" / "Stop leaving your life to chance."

- **Source lines:** "DISCIPLINE FOR REAL LIFE" (`RoutineOnboardingFlow.kt:274`); "Stop leaving your life\nto chance." (`:275`); "Work gets calendars. Deadlines get reminders. Other people get your structure. Shanivar starts giving some of it back to you." (`:276`).
- **Insight:** high-velocity people already run on structure, but it is all lent out. The calling is to keep some for yourself.
- **Hooks:**
  1. "Your manager's meeting has a calendar invite. Your own work has 'whenever'."
  2. "If the last few months have felt off, it may not be motivation. It may be that everyone else got your structure."
  3. "Discipline for real life, not for the highlight reel."
- **Must NOT claim:** that Shanivar schedules or runs your day now; productivity gains; "discipline guaranteed".

### A2 — "Not an ideal morning. Your actual life."

- **Source lines:** "Not an ideal morning. Your actual life." (`RoutineOnboardingFlow.kt:305`; heading form `RoutineScreen.kt:98`); "…built around reality — not someone else's ideal morning." (`RoutineOnboardingFlow.kt:327`); "Give the day\ntwo fixed points." (`:304`).
- **Insight:** the 5am / "that girl" routine genre makes ordinary days feel like failure (2025 backlash; see `zero-to-million-playbook.md` §C). Start from when you actually wake.
- **Hooks:**
  1. "No 5am. No ice bath. Two fixed points: when you actually wake, and when you make room for sleep."
  2. "Your routine shouldn't start with someone else's alarm."
  3. "Not an ideal morning. Your actual life."
- **Must NOT claim:** circadian, cortisol, dosha or "ideal biological timetable" (`docs/decisions/0014-…md:37`); that the app arms an alarm from wake/sleep (it doesn't: "Your wake alarm remains a separate, optional choice", `RoutineOnboardingFlow.kt:322`).

### A3 — "Choose what loses access to you."

- **Source lines:** "Choose what loses access to you." (`RoutineOnboardingFlow.kt:364`; `RoutineProtectionScreen.kt:127`); "Choose what\ngets cut off." (`RoutineOnboardingFlow.kt:467`); "You already know which apps take more than they give." (`:468`); "Morning. Work. Night. Add the boundaries your 24-hour cycle needs." (`RoutineProtectionScreen.kt:236`).
- **Insight:** agency, not abstinence. You already know which apps cost you. The point is deciding *when* they get a key to your attention.
- **Hooks:**
  1. "Not less screen time. Fewer apps with a key to your evening."
  2. "You already know which apps take more than they give."
  3. "Morning. Work. Night. Which one do you want back first?"
- **Must NOT claim:** "blocks Instagram everywhere" (the browser and second devices are not blocked; `android-app/ENFORCEMENT_STANCE_2026-09-12.md:108-113`); "impossible to bypass"; that multiple windows work on your phone (pending DEV-7 §2, §6); detox framing.

### A4 — "Voluntary and stoppable — never unbreakable" (strict but honest)

- **Source lines:** "Protection is voluntary and stoppable — never unbreakable." (`RoutineOnboardingFlow.kt:468`); "Shanivar will say Needs device access — not pretend you are protected." (`:524`); "Saved is not Active." (`:368`); "Essential access stays available. Stop protection anytime — no waiting, no payment, no forced practice, no server." (`:538`).
- **Insight:** strict apps usually lie, either about being unbreakable or about being on. Shanivar's stance is high standards plus honesty about its own state. This is where the founder's "strict" lives safely.
- **Hooks:**
  1. "Strict with you. Honest about itself."
  2. "An app that tells you when it is *not* protecting you."
  3. "No ransom to unlock. No fake 'you're protected'."
- **Must NOT claim:** any status accuracy, reliability or "Active" behaviour as tested before DEV-7 §3 passes; that "stop anytime" means a button on the blocked screen (the enforcement stance keeps "no bypass affordance… on the block surface"; stopping happens inside Shanivar, `ENFORCEMENT_STANCE_2026-09-12.md:96-99`, Jira DEV-10).

### A5 — "Set it once. Then get on with life." / "Not discipline theatre."

- **Source lines:** "Set it once.\nThen get on with life." (`RoutineOnboardingFlow.kt:351`); "Two tools. Both editable. Neither should need your attention every day." (`:352`); "This is not discipline theatre. Remove the decision when you already know what the right decision is." (`:377`); "Start when you said you would." (`:354`).
- **Insight:** high-velocity people lose to repeated micro-decisions, not to lack of willpower. Decide once while clear-headed.
- **Hooks:**
  1. "Decide once at 9am, not forty times by 11pm."
  2. "Watching discipline reels is discipline theatre. Removing the decision is the work."
  3. "Start when you said you would."
- **Must NOT claim:** "set and forget, guaranteed"; that alarms fire reliably or survive reboots (pending DEV-7 §5, §8); a hard productivity outcome.

### A6 — "Do the work. Need fewer prompts."

- **Source lines:** "Do the work. Need fewer prompts." (`RoutineOnboardingFlow.kt:282`); "Train attention until the practice can leave the phone and enter your life." (`:283`); "Shanivar is not here to keep you busy. It is here to make what matters harder to ignore." (`:287`).
- **Insight:** an app that aims to be needed less, which is the anti-engagement promise (ST-008; `docs/decisions/0014-…md:60`).
- **Hooks:**
  1. "The goal is an app you need less."
  2. "Shanivar is not here to keep you busy."
  3. "Do the work. Need fewer prompts."
- **Must NOT claim:** Sadhana levels, chapters, progression or unlocks (DEV-16 pending; "Progression waits", `RoutineOnboardingFlow.kt:283`); "graduation" outcomes; that Steady is publicly available (it remains "in development" in public copy, `marketing/shanivar/execution-2026-09-30/README.md`).

---

## 6. Phased rollout tied to build gates

**Funnel (the same for every phase):** Instagram bio link (and YouTube Shorts description once that channel exists) → **https://shanivar.com** → early-access **waitlist** form → (M1/M2) **tester invite** → (M3) **Google Play release**.

**UTM convention.** This matches the website worker's pattern: lowercase `utm_source` = platform, `utm_medium` = placement, `utm_campaign` = `m<milestone>_<goal>` (`website/README.md:68-81`, read only).

| Placement | URL |
| --- | --- |
| Instagram bio | `https://shanivar.com/?utm_source=instagram&utm_medium=bio&utm_campaign=m0_waitlist` |
| Instagram post / Reel CTA ("link in bio" caption) | bio link above (feed captions are not clickable); `utm_medium=post` is reserved for any clickable placement |
| Instagram Story link sticker | `https://shanivar.com/?utm_source=instagram&utm_medium=story&utm_campaign=m0_waitlist#early-access` |
| YouTube Shorts description / channel link (if D4 approved) | `https://shanivar.com/?utm_source=youtube&utm_medium=shorts&utm_campaign=m0_waitlist#early-access` |
| Tester invite phase (M1/M2) | same pattern with `utm_campaign=m1_tester_invite`, then `m2_tester_build` |
| Public release (M3) | `utm_campaign=m3_play_launch` (site then points to the Play listing) |

`site.js` captures only `utm_source`, `utm_medium` and `utm_campaign` (`website/site.js:11`), so per-post attribution uses the ledger's stable content ID and posting date rather than `utm_content`. Adding `utm_content` would need the website worker to extend `site.js`; that is a note for them, not an edit here. Waitlist metrics only exist once the form has a backend and the site is deployed.** Today the form is preview-only and shanivar.com is on parking nameservers (founder-confirmed). Those are founder decisions (D5).

### Phase 0 — now (flag off, DEV-7 pending)

- **Entry gate (met):** `ROUTINE_V3 = false` (`android-app/app/build.gradle.kts:33`); DEV-7 empty (`android-app/ROUTINE_V3_DEVICE_QA_2026-10-05.md`).
- **Content:** problem and idea posts (angles A1–A6 in "being built" form); an honest build log; labelled design previews; the existing reserve carousels only after a review against this plan (they use the older palette and Steady framing, `marketing/shanivar/execution-2026-09-30/README.md`).
- **Channels:** Instagram `@app.shanivar` (feed carousels plus Reels when rendering exists; optional Stories); shanivar.com waitlist once live. YouTube Shorts only if the founder approves creating that channel (D4).
- **Waitlist/tester:** the waitlist is the website worker's Netlify form (live once deployed). There is no tester programme yet: distribution is a LAN APK (`android-app/DEVICE_DISTRIBUTION.md`). The waitlist may say "join early access; we'll invite testers once the phone test passes". It may not promise a date.
- **Claims allowed:** "Shanivar is being built…", "in development", "Routine V3 is written; phone testing is next", verbatim UI lines presented as "from the build in development", "Android app" (the only platform in the repo).
- **Claims banned:** available, download, live, launch date, Active, tested, works, reliable, "blocks", "on your phone tonight", Play Store, iOS, any user count or outcome, "impossible to bypass", streaks/levels, detox, circadian/cortisol/dosha.
- **Owner:** Social Media Manager (brief, approval, ledger); Creative Writer (copy); Web Developer, coordinated by Codex (`ai_org/agents/web_developer.md`), for the site; founder for unpausing.
- **Waitlist metrics (once the form is live):** attributed site sessions by `utm_source/medium`; waitlist sign-ups (deduplicated); sign-up conversion = sign-ups ÷ attributed sessions (sessions need host analytics, so until then report sign-ups by source only); **Android phone-model mix** from the optional field (`website/index.html`, `phone_model`), which feeds the DEV-7 device matrix and tester selection. A qualifying question ("What do you most want back: morning / work time / night?") would need the website worker to add a field. That is a suggestion, not an edit here. Raw numerators and denominators always. No vanity totals.

### Phase 1 — founder phone test passes DEV-7

- **Entry gate:** `android-app/ROUTINE_V3_DEVICE_QA_2026-10-05.md` filled with device, Android version and build; sign-off "Overall: Pass" (or Partial with blockers explicitly listed); Jira DEV-7 moved off "Pending"; none of the release blockers in `android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md:121-133` remain for the tested build.
- **Content:** the founder's own documented test, using real screen recordings from the tested device with device/Android stated. "What passed, what didn't" posts. Tester-invite announcement to the waitlist.
- **Channels:** as Phase 0, plus email to waitlist sign-ups who consented, only to people who joined via the form (it states the email is "Used only to invite you to early testing", `website/index.html`); the email-sending tool is a founder decision (D5).
- **Claims allowed:** "Passed the founder's physical-device test on [make/model], Android [x], build [y]"; specific checks that passed, by QA row number.
- **Claims banned:** "works on all Android phones"; general availability; Play Store; testimonials from anyone but the founder (and his must be his real experience); anything about rows marked Fail or Skip.
- **Owner:** founder (test evidence); Social Media Manager (content and claim check); Market Fit Release Steward (release evaluation, `ai_org/agents/market_fit_release_steward.md`).
- **Waitlist metrics:** tester-invite opt-in rate = opted in ÷ waitlist contacted; qualifying-question mix; unsubscribe rate.

### Phase 2 — flag on, tester build distributed

- **Entry gate:** a committed build with `ROUTINE_V3 = true` (or V3 made the default path per handoff R4, `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:334-340`); a hosted tester channel actually set up (Firebase App Distribution is the recommended no-cost route, `android-app/DEVICE_DISTRIBUTION.md`, "Recommended hosted test path"); invitations sent to consenting waitlist members.
- **Content:** "early-access testers" posts; anonymised, aggregate, consented feedback themes; honest known-limits posts (browser not blocked, OEM battery settings, from QA §8.3).
- **Channels:** Instagram, waitlist email, Firebase tester invites. No public store link.
- **Claims allowed:** "Now with invited Android testers (Android 8.0+)" (`android-app/DEVICE_DISTRIBUTION.md`: "Supports Android 8.0/API 26 and newer"); "Join the waitlist to be invited".
- **Claims banned:** public availability; ratings, reviews or user counts not actually measured; outcomes beyond consented self-report; "Active" reliability claims beyond the tested devices.
- **Owner:** Social Media Manager; Social Publishing Operations (ledger); founder (tester list, consent).
- **Waitlist metrics:** invited → installed → completed V3 onboarding → saved a day plan → saved a first protection window → reached **Ready** (device-confirmed) per device class. **Confirmed usefulness** = testers answering "Did this help you make room for what you planned?" Yes/A little ÷ testers asked (adapted from `marketing/shanivar/v1-product-direction-handoff.md`, "Optional review" row). Stopping protection is recorded neutrally, never as failure. Today there is no in-app analytics or crash reporting (DEV-14 Open), so these are surveys and voluntary reports until instrumentation exists (`../wish-list.md`).

### Phase 3 — public release

- **Entry gate:** Jira DEV-17 resolved (release signing, API domain, Play Console declarations); a published Google Play listing with a dated release record; release blockers in the test guide cleared for the release build; founder approval of public release (`AGENTS.md:7`).
- **Content:** launch announcement; "how it works" posts filmed on real devices; honest-limits FAQ; tester-to-public story with consented quotes only.
- **Channels:** Instagram, YouTube Shorts (if created), waitlist email, Play listing. Paid acquisition only if the founder explicitly authorises a budget (`marketing/shanivar/marketing-operating-system.md:34`).
- **Claims allowed:** "Available on Google Play for Android"; feature claims limited to what the release build passed.
- **Claims banned:** "impossible to bypass" (forever); guaranteed productivity or transformation; clinical or mental-health benefits; Sadhana progression (until DEV-16 is decided and built); iOS.
- **Owner:** Social Media Manager; Market Fit Release Steward; founder (release decision).
- **Waitlist metrics:** waitlist → Play install conversion (where attributable); 7-day and 28-day confirmed-usefulness rate; voluntary repeat; user-confirmed "need fewer prompts" (ST-008). Never DAU or session time as success.

---

## 7. First two weeks of Phase 0 (proposed, not scheduled)

Format follows `marketing/shanivar/week-01-calendar.md` and the cadence in `marketing/shanivar/marketing-operating-system.md:32` (two Reels plus one carousel weekly once rendering exists; rendered carousels as fallback). Proposed slot: **19:30 Asia/Kolkata / 16:00 Europe/Berlin** (the existing planning convention, not a researched optimum; Berlin moves to CET on 25 Oct, so 19:30 IST = 15:00 CET from 26 Oct). **Dates are placeholders; marketing is paused.** Every item carries "Shanivar is in development" and points to the bio link (`utm_campaign=m0_waitlist`).

| ID | Proposed date | Format | Angle | Title / first line | Product claim level |
| --- | --- | --- | --- | --- | --- |
| SHN-R3-P0-01 | Mon 12 Oct 2026 | Carousel (5 slides, V3 palette) | A1 | "Stop leaving your life to chance." Manifesto: work, deadlines and other people get your structure | Idea only. Ends "Shanivar is being built for this. Early access: link in bio." |
| SHN-R3-P0-02 | Wed 14 Oct 2026 | Reel 20–30s (typographic or founder phone footage; Reel if rendered, else carousel) | A2 | "No 5am. Two fixed points." | Problem plus intention. No alarm or circadian claim |
| SHN-R3-P0-03 | Fri 16 Oct 2026 | Reel 20–30s | A4 | "Strict with you. Honest about itself." Saved is not Active | Stance. States phone testing has not happened yet |
| SHN-R3-P0-04 | Mon 19 Oct 2026 | Carousel | A3 | "Choose what loses access to you." Morning / Work / Night | Idea. Includes "browser and second phones are not blocked" honesty slide |
| SHN-R3-P0-05 | Wed 21 Oct 2026 | Reel | A5 | "Decide once at 9am, not forty times by 11pm." | Problem. No reliability claim |
| SHN-R3-P0-06 | Fri 23 Oct 2026 | Carousel (build log) | A6 + status | "Where Shanivar actually is today": written, flag off, phone test next, no release | Exact status, re-checked against the repo on the day |
| Stories (optional) | 2–3 per week | Poll / question | — | "What do you want back first? Morning / Work / Night" | Non-sensitive poll; answers private (`marketing/shanivar/engagement-reels-playbook.md`, Engagement) |

Each item needs a full package before it counts toward the ready buffer: final media, caption, alt text/subtitles, disclosure check, stance intake reference and approval revision (`marketing/shanivar/marketing-operating-system.md:38`).

---

## 8. Conflicts between existing marketing material and Routine V3 (and which wins)

Rule: **decision 0014 plus the marketing operating system win** for current direction (`docs/decisions/0014-…md`; `marketing/shanivar/marketing-operating-system.md:12`; `marketing/shanivar/voice-direction.md`, 2 Oct section: "SHANIVAR_STANCE and accepted decision 0014 control current direction… Older scroller, chart and AI-personalization language below is historical intention").

| # | Existing material | Conflict with Routine V3 | Winner |
| --- | --- | --- | --- |
| 1 | `marketing/shanivar/week-01-calendar.md` brand proposition: "personalized astrological charts… AI-guided reflection"; Reels about "Messages from Shanivar" and charts | Astrology/scroller positioning is not the product form; ST-007 says it "does not reintroduce astrology into the current launch surface" | **0014 + MOS.** Week-01 stays a historical record |
| 2 | `marketing/shanivar/audience-direction.md` chart-plus-AI pillar and scroller feature copy | Same | **0014 + MOS** (`ai_org/agents/social_media_manager.md` current mandate: MOS supersedes "scroller/astrology promotion") |
| 3 | `audience-direction.md` first pitch "one intention, one distracting app and a short protected session"; `v1-product-direction-handoff.md` home "Set an alarm / Start focus" | V3 is a day plan with anchors, optional alarm and up to 8 windows, with Routine as the launch destination | **0014** for direction. The handoff already carries a 30 Sept note deferring to 0014. Claims stay gated |
| 4 | Visual identity charcoal/bone/bronze (`voice-direction.md`; `execution-2026-09-30/README.md`; `week-01-calendar.md`) | V3's accepted visual contract is "black, cream-paper, sage and muted-gold" (`android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:62`) | **V3 palette** for new Routine content; existing rendered assets kept as historical/reserve |
| 5 | Voice: patient teacher, "maybe", questions (`voice-direction.md`) vs V3 "direct, serious, action-oriented coach" (wireframe README) and the founder's "very strict, high-velocity" | Tone tension | **Open founder decision (D1).** Working split proposed in §2 |
| 6 | Cadence: month-02 "three originals plus one companion" vs MOS "two Reels + one carousel" | Different weekly mix | **MOS** (`ai_org/agents/social_media_manager.md:5`: the MOS "supersedes conflicting older campaign cadence") |
| 7 | Breathing update's three-pillar pitch ("get up, protect chosen time, short breathing pause") | Compatible, but Steady sits inside Sadhana now and is the only Sadhana content (`0014:18`) | Compatible; describe Steady as part of Sadhana, in development |
| 8 | Optional Lord Shani framing (`audience-direction.md`) | Not present in V3 UI copy | Stays **optional** and not a lead hook; needs founder input before use in Routine content |
| 9 | Enforcement stance "neither the alarm nor the app lock should be escapable by an ordinary frustrated person" (`ENFORCEMENT_STANCE_2026-09-12.md:12-14`) vs UI "Stop protection anytime" (`RoutineOnboardingFlow.kt:538`) | Apparent contradiction for copywriters | Both hold: no bypass on the block screen, deliberate stop inside Shanivar (`ENFORCEMENT_STANCE…:96-99`; DEV-10). Marketing says "stoppable by you, inside Shanivar", never "one tap from the blocked app" and never "can't escape" |
| 10 | Wireframe README: personal growth "worth scheduling and tracking" | Native build does not track completion (`…HANDOFF…:154`) | **Build truth wins.** Tracking goes to `../wish-list.md` |
| 11 | `@app.shanivar` is both the Instagram handle (`execution-2026-09-30/operations.md`) and the Grok Bot's name (`AGENTS.md:3`) | Naming collision in internal docs | No conflict for the public. Internal docs should say "Instagram @app.shanivar" vs "Grok Bot app.shanivar" |
| 12 | Unposted calendars with past dates (month-02 14 Sep–13 Oct; execution batch 30 Sep–5 Oct) | Not published; slots elapsed | Treat as reserve/historical; roll forward only after review (`execution-2026-09-30/README.md`) |
| 13 | `marketing/shanivar/README.md` links to `week-01/`, `week-01-v2/`, `next-instagram-2026-09-25/`, `automation/social/` | Those paths are **not in this clone** | Cannot be relied on; flagged for Codex |

---

## 9. Risks and open founder decisions

**Risks**

1. **Premature claims.** The biggest risk is copy that implies Routine works before DEV-7. Mitigation: the claim gates in §6 and a "status line re-checked on posting day" rule.
2. **Strict tips into shame or lock-in.** Mitigation: the §2 guardrails, plus the review question "could this leave someone feeling judged?" (`marketing/shanivar/voice-direction.md`).
3. **Waitlist promises a date we can't keep.** DEV-17 has several unknowns (Play declarations for exact alarms and full-screen intents). Never publish a launch date before Phase 3.
4. **Play policy risk** on `USE_EXACT_ALARM` / `USE_FULL_SCREEN_INTENT` / `specialUse` services (DEV-17) could change what "alarm" features ship. Avoid alarm-led hero claims until resolved.
5. **Waitlist data and privacy.** The privacy notice is still a draft pending legal review (`website/index.html`, draft notice; `website/README.md:92`). Keep collection minimal (email, optional phone model, 18+ confirmation). Never birth data (`marketing/shanivar/audience-research.md`).
6. **Capacity.** Solo founder plus AI team; video rendering is not connected. Mitigation: carousel-first fallback and founder phone footage.
7. **Measurement gap.** No in-app analytics or crash reporting (DEV-14). Usefulness metrics depend on surveys until built.
8. **Palette and identity drift** between old assets and V3. Mitigation: reset plan in `social-presence-from-scratch.md`.

**Open founder decisions**

- **D1** Voice: confirm the split (V3 direct "strict coach" for Routine; patient observational voice for Sadhana/editorial), or choose one voice.
- **D2** "Track things": is completion tracking a Phase 2+ product wish (see `../wish-list.md`)? Until then marketing must not say "track".
- **D3** Audience sequencing: lead with high-velocity self-directed adults; hold the parent path until helper handback and Hindi exist.
- **D4** YouTube Shorts: create a channel (and which handle) or stay Instagram-only.
- **D5** Waitlist go-live: Netlify Forms is chosen (`website/README.md:83`). Still open: pointing shanivar.com off parking nameservers, legal review of the draft privacy notice, and which tool sends tester-invite emails.
- **D6** Unpausing marketing: when, and under which publishing authorization (`ai_org/agents/social_publishing_ops.md`).
- **D7** Any spend (video tools, connectors, ads, ChatGPT upgrade): none is authorised.
- **D8** Record "marketing paused, 6 Oct 2026" in a repo file so future sessions see it.

---

## Sources

Repo files read for this plan (relative paths):

`AGENTS.md` · `SHANIVAR_STANCE.md` · `docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md` · `docs/decisions/0013-sadhana-progression-and-the-gamification-question.md` · `docs/decisions/0009-behavioral-return-loop.md` · `docs/strategy/shanivar-tone-and-values.md` · `docs/strategy/shanivar-launch-backlog.md` · `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md` · `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md` · `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/style.css` · `android-app/ROUTINE_V3_TEST_GUIDE_2026-09-30.md` · `android-app/ROUTINE_V3_DEVICE_QA_2026-10-05.md` · `android-app/ENFORCEMENT_STANCE_2026-09-12.md` · `android-app/DEVICE_DISTRIBUTION.md` · `android-app/app/build.gradle.kts` · `android-app/app/src/main/java/com/shanivar/nativeapp/{RoutineOnboarding.kt, RoutineOnboardingFlow.kt, RoutineScreen.kt, RoutineProtectionScreen.kt, RoutineShell.kt, RoutinePlan.kt, ShanivarApp.kt, OnboardingFlow.kt}` · `marketing/shanivar/README.md` · `marketing/shanivar/marketing-operating-system.md` · `marketing/shanivar/audience-direction.md` · `marketing/shanivar/audience-research.md` · `marketing/shanivar/voice-direction.md` · `marketing/shanivar/ai-video-toolchain-2026-09-30.md` · `marketing/shanivar/creative-learning-log.md` · `marketing/shanivar/engagement-reels-playbook.md` · `marketing/shanivar/v1-product-direction-handoff.md` · `marketing/shanivar/breathing-feature-marketing-update-2026-09-25.md` · `marketing/shanivar/week-01-calendar.md` · `marketing/shanivar/month-02/{README.md, operations.md, calendar.md, first-post-analysis.md, QA.md}` · `marketing/shanivar/execution-2026-09-30/{README.md, operations.md, copy.md}` · `marketing/shanivar/ugc-pilot-2026-09-30/briefs.md` · `marketing/shanivar/research-led-pilot/{grok-concepts.md, SHN-COLOR-01-brief.md}` · `ai_org/agents/{social_media_manager.md, creative_writer.md, ugc_video_producer.md, social_video_editor.md, social_publishing_ops.md, market_fit_release_steward.md, content_designer.md}` · `marketing/shanivar/routine-v3-onboarding/website/{README.md, index.html, site.js, privacy.html}` at commit `257855f` (read only; owned by the website worker) · `source/ui-copy-inventory.md`.

Jira (read-only, 6 Oct 2026, site shanivar.atlassian.net): DEV-2, DEV-3, DEV-6, DEV-7, DEV-10, DEV-13, DEV-14, DEV-16, DEV-17.
