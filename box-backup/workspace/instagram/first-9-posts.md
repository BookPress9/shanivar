# Shanivar (@app.shanivar): first 9 Instagram posts. DRAFT for founder review

Drafted 6 Oct 2026 (Europe/Berlin). **Drafts only.** Nothing has been posted, scheduled, committed or pushed. Repo read at `/workspace/shanivar-v3` `main` @ `12fbecd` (read only).
Display name: `Shanivar · by The Hybrid Monk`. Bio (being set): `Life been drifting? Take the day back. / High standards. Honest tools. Your actual life. / In development`.
**No links and no "link in bio" in any caption** (shanivar.com isn't live yet).

**Titles, order, formats and pins** come from `marketing/shanivar/routine-v3-onboarding/social-presence-from-scratch.md:112-128` (§7 "First 9 posts"). Angles A1–A6 and claim gates are from `routine-onboarding-marketing-plan.md:81-171`. Quoted product lines are verbatim from `source/ui-copy-inventory.md`. I checked them against `android-app/app/src/main/java/com/shanivar/nativeapp/RoutineOnboardingFlow.kt` at HEAD, and the line numbers still match.

**Shared visual system** (website `marketing/shanivar/routine-v3-onboarding/website/style.css:7-12`; mark from the live `@app.shanivar` profile picture; plan `social-presence-from-scratch.md:128`):
- Colours: background V3 black `#0b0d0b`, panel `#0f120f`, ink `#f7f5ee`, muted `#c9cec7`. Cream-paper cards `#f2efe6` with ink `#111510`. Sage accent `#d8e5b7`, gold accent `#c9aa73` (`#7a5c2b` on paper).
- Type: serif headings (Iowan Old Style / Palatino / Georgia stack, weight 600, tight leading). Sans body (system-ui / Roboto). Small uppercase letter-spaced gold eyebrows.
- **Saturn mark (matches the live `@app.shanivar` profile picture; founder instruction, 6 Oct 2026):** a thin gold outline of a ringed planet (an outlined circle with a tilted elliptical ring, no fill, no moon), with `SHANIVAR` in spaced capitals underneath, on V3 black `#0b0d0b`. Gold is the V3 accent `#c9aa73` unless matched to the live file. **Reference:** the live Instagram profile picture. The closest source asset in the repo is the Android launcher icon, `android-app/app/src/main/res/drawable/ic_launcher_foreground.xml:2-5`: a thin gold `#C4AB72` outline circle with a ring tilted −22°, on `#0A0A0C` (`res/values/colors.xml:1`). It has no wordmark. The wordmark follows the in-app letter-spaced "SHANIVAR" (`RoutineOnboardingFlow.kt:112`). **Open item:** no repo file reproduces the full live lock-up, and the website `favicon.svg` is a different mark (a filled sage-to-black sphere with a small sage moon). Don't use the favicon mark in these posts. The mark is **decoration only and never a breathing pacer** (`website/index.html:66`, `style.css:79`). In Reels it may drift slowly but must not pulse in a breath rhythm.
- Format: 4:5 (1080×1350), with headline and mark kept inside the 3:4 grid crop. 9:16 Reels with burned-in captions. No app UI is shown. If any screen is ever shown, label it "DESIGN IN DEVELOPMENT — NOT RELEASED" (`social-presence-from-scratch.md:128`).
- Status footer used throughout: **"Shanivar · in development"**. The plan requires every item to say it is in development (`routine-onboarding-marketing-plan.md:209`).

Base hashtags (pick 3–8 per post; no #detox, #mindfulness, #meditation, #wellness, #selfcare, #horoscope or #productivityhacks): `#Shanivar #TheHybridMonk #DailyRoutine #Attention #Focus #BuildInPublic #AndroidApp #IndieApp`

Marked `[RE-CHECK ON POSTING DAY]` = a status line that must still be true on the day it's posted (`routine-onboarding-marketing-plan.md:58`, `:251`).

---

## 1. "Stop leaving your life to chance." Manifesto ★ Pin 1
**Format:** Carousel, 5 slides (per plan). Angle A1.

| Slide | On-image text |
|---|---|
| 1 | Eyebrow: DISCIPLINE FOR REAL LIFE · Heading: **Stop leaving your life to chance.** |
| 2 | Work gets calendars. Deadlines get reminders. Other people get your structure. |
| 3 | Your own growth gets memory and mood. |
| 4 | Shanivar is being built to give some of that structure back to you. |
| 5 | **Build the day you intend to live.** · Saturn mark · Shanivar · in development |

Sources: `RoutineOnboardingFlow.kt:274-276`, `:278`; slide 3 adapted from `website/index.html:77`. On slide 4 I changed the UI's "starts giving" (`:276`) to "is being built to give" because of the Phase 0 gate (`routine-onboarding-marketing-plan.md:57`).

**Caption:**
> Your manager's meeting has a calendar invite. Your own work has "whenever."
>
> Shanivar is a day planner being built around one idea: some of your structure should belong to you.
>
> In development. Not released yet.

**Hashtags:** #Shanivar #TheHybridMonk #DailyRoutine #Focus #BuildInPublic
**Alt text:** (1) Black slide, serif cream text: "Stop leaving your life to chance." Small gold label above: "Discipline for real life." (2) Cream paper card on black: "Work gets calendars. Deadlines get reminders. Other people get your structure." (3) Black slide: "Your own growth gets memory and mood." (4) Cream card: "Shanivar is being built to give some of that structure back to you." (5) Thin gold outline of a ringed planet with SHANIVAR in spaced capitals beneath it, on black, and the text "Build the day you intend to live. Shanivar, in development."
**Visual:** All type, alternating black and cream-paper slides. A gold eyebrow on slide 1, the Saturn mark only on slide 5, a lot of empty space.

---

## 2. "No 5am. Two fixed points."
**Format:** Reel, 20–30 s, typographic (per plan). Fallback: a 5-slide carousel with the same text, because video rendering isn't connected yet (`routine-onboarding-marketing-plan.md:214`, `:256`). Angle A2.

| Time | On-screen text |
|---|---|
| 0–3 s | No 5am. |
| 3–6 s | No ice bath. |
| 6–10 s | Give the day two fixed points. |
| 10–15 s | When you actually wake. / When you make room for sleep. |
| 15–21 s | Not an ideal morning. Your actual life. |
| 21–26 s | End card: Saturn mark · Shanivar · in development |

Sources: `RoutineOnboardingFlow.kt:304-305`, `:313`, `:320`; hook from plan A2 (`routine-onboarding-marketing-plan.md:100`).

**Caption:**
> A lot of routine advice starts with someone else's morning.
>
> Shanivar starts with two times you already know: when you usually wake, and when you make room for sleep. The rest of the day is built around those. A wake alarm stays a separate, optional choice.
>
> In development for Android.

**Hashtags:** #Shanivar #DailyRoutine #Attention #AndroidApp #BuildInPublic
**Alt text / description:** Typographic video on black. Cream serif lines appear one at a time: "No 5am. No ice bath. Give the day two fixed points. When you actually wake. When you make room for sleep. Not an ideal morning. Your actual life." It ends on a thin gold ringed-planet outline with SHANIVAR beneath it, and "in development." No music lyrics, no voiceover.
**Visual:** Black frame, one line at a time, hard cuts. "two fixed points" set as two small sage dots on a thin gold line. No clocks, sunrises or body imagery.
**Guard:** no circadian or "ideal timetable" wording, and no claim that wake/sleep times arm an alarm (`routine-onboarding-marketing-plan.md:103`; `RoutineOnboardingFlow.kt:322`; `docs/decisions/0014-…md:37`).

---

## 3. "Choose what loses access to you." ★ Pin 2
**Format:** Carousel, 5 slides (plan says carousel with an honesty slide). Angles A3 + A4.

| Slide | On-image text |
|---|---|
| 1 | **Choose what loses access to you.** |
| 2 | You already know which apps take more than they give. |
| 3 | Morning. Work. Night. Which one do you want back first? |
| 4 | Eyebrow: HONEST LIMITS · **What Shanivar can't block:** a browser. A second phone. Someone determined to get around it. We will never call it impossible to bypass. |
| 5 | Protection is voluntary and stoppable — never unbreakable. You set every boundary. You can stop it, inside Shanivar. · Shanivar · in development |

Sources: `RoutineOnboardingFlow.kt:364`, `:468`; `RoutineProtectionScreen.kt:236`; honesty slide from `android-app/ENFORCEMENT_STANCE_2026-09-12.md:108-113`, `:137-138` and the website FAQ (`website/index.html`, "Is app protection impossible to get around?").

**Caption:**
> Not less screen time. Fewer apps with a key to your evening.
>
> Shanivar's app protection is being built around a choice you make once: which apps lose access to you, and when. It's voluntary. You can stop it from inside Shanivar. It isn't unbreakable, and we won't pretend it is.
>
> In development. Not yet tested on a real phone. [RE-CHECK ON POSTING DAY]

**Hashtags:** #Shanivar #Attention #Focus #ScreenTime #AndroidApp #BuildInPublic
**Alt text:** (1) Black slide, cream serif: "Choose what loses access to you." (2) Cream card: "You already know which apps take more than they give." (3) Three small labels in a row, Morning, Work, Night, with the question "Which one do you want back first?" (4) Cream card labelled "Honest limits": Shanivar can't block a browser, a second phone, or someone determined to get around it, and will never be called impossible to bypass. (5) Black slide: "Protection is voluntary and stoppable, never unbreakable. You set every boundary. You can stop it, inside Shanivar."
**Visual:** Slide 3 uses three thin gold outline arcs, echoing the mark's ring, for Morning, Work and Night. Slide 4 is a cream paper card with a gold "HONEST LIMITS" eyebrow, made to look like a plain notice, not a warning.
**Guard:** no "blocks Instagram", no detox, and no claim that multiple windows work (`routine-onboarding-marketing-plan.md:113`). "Stop" means inside Shanivar, not from the blocked screen (`:123`, `:239`).

---

## 4. "Strict with you. Honest about itself."
**Format:** Reel, 20–30 s, typographic (per plan; same carousel fallback as post 2). Angle A4.

| Time | On-screen text |
|---|---|
| 0–3 s | Strict with you. Honest about itself. |
| 3–8 s | A strict app can fail you two ways: pretend it's unbreakable, or pretend it's on. |
| 8–13 s | Saved is not Active. |
| 13–19 s | Missing access? It's built to say "Needs device access", not pretend you are protected. |
| 19–24 s | Phone testing hasn't happened yet. That's next. [RE-CHECK ON POSTING DAY] |
| 24–27 s | End card: Saturn mark · Shanivar · in development |

Sources: `RoutineOnboardingFlow.kt:368`, `:468`, `:524`; plan A4 hooks (`routine-onboarding-marketing-plan.md:120-122`); plan:215 requires stating that phone testing hasn't happened yet.

**Caption:**
> High standards for you. The same standard for the app.
>
> Shanivar is being built to tell you plainly which state it's in. A saved plan is not the same as protection that's on. If something is missing, it should say so.
>
> In development. Phone testing is next. [RE-CHECK ON POSTING DAY]

**Hashtags:** #Shanivar #TheHybridMonk #Focus #BuildInPublic #IndieApp
**Alt text / description:** Typographic video on black. Lines: "Strict with you. Honest about itself." "A strict app can fail you two ways: pretend it's unbreakable, or pretend it's on." "Saved is not Active." "Missing access? It's built to say Needs device access, not pretend you are protected." "Phone testing hasn't happened yet. That's next." It ends on the gold ringed-planet outline with SHANIVAR beneath it.
**Visual:** Two status words, "SAVED" in muted grey and "ACTIVE" in sage, set side by side, with a thin gold "≠" between them. Typography only, no fake app screens.
**Guard:** no claim that status reporting is accurate or tested before DEV-7 §3 (`routine-onboarding-marketing-plan.md:123`).

---

## 5. "Decide once at 9am, not forty times by 11pm."
**Format:** Carousel, 5 slides (per plan). Angle A5.

| Slide | On-image text |
|---|---|
| 1 | **Decide once at 9am, not forty times by 11pm.** |
| 2 | 11pm: the same small question, again. "One more?" |
| 3 | 9am: you already know the answer. |
| 4 | **Set it once. Then get on with life.** |
| 5 | Remove the decision when you already know what the right decision is. · Shanivar · in development |

Sources: plan A5 hook (`routine-onboarding-marketing-plan.md:130`); `RoutineOnboardingFlow.kt:351`, `:377`.

**Caption:**
> Maybe an evening is rarely lost to one big decision. More often it's the same small one, over and over.
>
> Shanivar is being built so you can make that decision once, in the morning, while you're clear-headed. You can always change it.
>
> In development.

**Hashtags:** #Shanivar #DailyRoutine #Focus #Attention #BuildInPublic
**Alt text:** (1) Black slide, cream serif: "Decide once at 9am, not forty times by 11pm." (2) A dim slide marked 11pm with the small question "One more?" (3) A brighter cream slide marked 9am: "You already know the answer." (4) "Set it once. Then get on with life." (5) "Remove the decision when you already know what the right decision is. Shanivar, in development."
**Visual:** Slide 2 is black with a muted 11pm label. Slide 3 is cream paper with a gold 9am label, so the contrast does the work. Times are set in serif numerals.
**Guard:** no "set and forget, guaranteed" and no reliability claim about alarms (`routine-onboarding-marketing-plan.md:133`). "forty times" is figurative (see flags).

---

## 6. "If the last few months have felt off…"
**Format:** Reel, founder phone footage, no face required (per plan). 20–30 s. Angle A1 + positioning.

Shot list: a phone lying face down on a desk, a morning window, a hand writing two times in a notebook, a closed laptop. Natural light. All real footage shot by the founder.

| Time | On-screen text (or founder voiceover) |
|---|---|
| 0–4 s | If the last few months have felt off… |
| 4–9 s | maybe it isn't motivation. |
| 9–14 s | Maybe everyone else got your structure. |
| 14–19 s | This isn't a verdict. It's an invitation. |
| 19–24 s | Take the day back. |
| 24–27 s | End card: Saturn mark · Shanivar · in development |

Sources: plan hook (`routine-onboarding-marketing-plan.md:38`, `:91`); bio line "Take the day back" (`social-presence-from-scratch.md:76`).

**Caption:**
> You usually notice it yourself before anyone says it. The days run, and somehow they aren't yours.
>
> If that's where you are, this can be the moment to take the day back. Start small: decide what the day is for before the phone decides it.
>
> Shanivar is being built for that. In development.

**Hashtags:** #Shanivar #TheHybridMonk #DailyRoutine #Attention #BuildInPublic
**Alt text / description:** Handheld footage of an ordinary desk: a phone face down, morning light through a window, a hand writing two times in a notebook. Text over the footage: "If the last few months have felt off… maybe it isn't motivation. Maybe everyone else got your structure. This isn't a verdict. It's an invitation. Take the day back."
**Visual:** Real, lived-in footage graded slightly towards black and cream, with text in serif cream on a dark lower third. The end card is the gold outline Saturn mark with the SHANIVAR wordmark on `#0b0d0b`. No phone screen visible.
**Guard:** an invitation, never "you've wasted your life" (`routine-onboarding-marketing-plan.md:38`). No transformation promise (`SHANIVAR_STANCE.md:267`). If AI footage or a presenter replaces the founder footage, disclose it (`social-presence-from-scratch.md:167`).

---

## 7. "Build log: where Shanivar actually is today" ★ Pin 3
**Format:** Carousel, 5 slides (per plan). Status post. **Every line `[RE-CHECK ON POSTING DAY]`.**

| Slide | On-image text |
|---|---|
| 1 | Eyebrow: BUILD LOG · [DATE] · **Where Shanivar actually is today** |
| 2 | **Written:** Routine, your day plan with wake and sleep anchors, an optional wake alarm and app protection windows. |
| 3 | **Switched off:** the new Routine isn't turned on in any build people use yet. |
| 4 | **Next:** the founder's test on a real phone. It hasn't happened yet. |
| 5 | **Not decided:** a release date. **Not available:** download, Play Store, iPhone. · Shanivar · in development |

Status evidence (6 Oct 2026):
- Routine V3 is on `main` (`routine-onboarding-marketing-plan.md:13`).
- The flag is off: `android-app/app/build.gradle.kts:33` `ROUTINE_V3 = false`.
- The physical-device QA sheet has no results: `android-app/ROUTINE_V3_DEVICE_QA_2026-10-05.md:120`, "Overall" is unfilled.
- No Play listing (`routine-onboarding-marketing-plan.md:22`).
- Android only (`website/index.html` FAQ).
- Optional extra line for slide 2, if wanted: "Sadhana: one short natural-breath practice, Steady, in internal builds. Content review before any release." (`website/index.html:197`; `docs/decisions/0014-…md:18`).

**Caption:**
> Plain status, because "coming soon" says nothing.
>
> Written: Routine (day plan, optional wake alarm, app protection windows). Switched off: not turned on in any build people use yet. Next: a test on a real phone. No release date. Android only. Nothing to download yet.
>
> In development.

**Hashtags:** #Shanivar #BuildInPublic #IndieApp #AndroidApp #TheHybridMonk
**Alt text:** Five slides styled like a dated log. (1) "Build log, [date]: Where Shanivar actually is today." (2) Written: Routine, a day plan with wake and sleep anchors, an optional wake alarm and app protection windows. (3) Switched off: the new Routine isn't turned on in any build people use yet. (4) Next: the founder's test on a real phone, which hasn't happened yet. (5) Not decided: a release date. Not available: download, Play Store, iPhone.
**Visual:** Cream-paper "log sheet" slides on a black border, monospace-style dates, status words in gold (Written / Switched off / Next / Not decided), and a small gold outline Saturn mark on the cover.

---

## 8. "Shanivar is not here to keep you busy."
**Format:** Reel, 15–25 s, typographic (per plan; same carousel fallback as post 2). Angle A6.

| Time | On-screen text |
|---|---|
| 0–4 s | Shanivar is not here to keep you busy. |
| 4–9 s | It is here to make what matters harder to ignore. |
| 9–13 s | Do the work. Need fewer prompts. |
| 13–18 s | The goal is an app you need less. |
| 18–22 s | End card: Notice the mind. Don't control it. · Saturn mark · Shanivar · in development |

Sources: `RoutineOnboardingFlow.kt:282`, `:287`; plan A6 hook (`routine-onboarding-marketing-plan.md:140`); `website/index.html:210`.

**Caption:**
> Success for Shanivar isn't more time spent in Shanivar. The aim is that you leave more present than when you opened it.
>
> Notice what's already happening inside, before you react. Then choose what comes next.
>
> In development.

**Hashtags:** #Shanivar #Attention #SelfAwareness #TheHybridMonk #BuildInPublic
**Alt text / description:** Typographic video on black. Lines: "Shanivar is not here to keep you busy. It is here to make what matters harder to ignore. Do the work. Need fewer prompts. The goal is an app you need less." It ends with "Notice the mind. Don't control it." beside a thin gold ringed-planet outline with SHANIVAR beneath it.
**Visual:** Slow, still typography with long holds. The gold outline mark drifts very slowly, never pulsing (no breath-pacer look).
**Guard:** no Sadhana levels or progression, and no claim that Steady is public (`routine-onboarding-marketing-plan.md:143`). No lasting-change claim (ST-008, `SHANIVAR_STANCE.md:267`). Caption copy adapted from `website/index.html:216`.

---

## 9. "Not discipline theatre"
**Format:** Carousel, 5 slides (per plan). What removing a decision looks like on paper, with no app UI claims. Angle A5.

| Slide | On-image text |
|---|---|
| 1 | **Not discipline theatre.** |
| 2 | Theatre: saving another routine reel. Rewriting the perfect plan at midnight. |
| 3 | The work, on paper: "Gym: Mon / Wed / Fri, 18:00." "Feeds off: 21:30–07:30." |
| 4 | Remove the decision when you already know what the right decision is. |
| 5 | **Start when you said you would.** · Shanivar · in development |

Sources: `RoutineOnboardingFlow.kt:377`, `:354`; plan A5 hook 2 (`routine-onboarding-marketing-plan.md:131`); "Gym" quick-add example from `RoutinePlan.kt:76`.

**Caption:**
> Watching discipline content can feel like doing something. Maybe sometimes it is. Often it's one more way to postpone a decision you've already made.
>
> Removing a decision can be this plain: a day, a time, written down once. No app needed for this part.
>
> Shanivar is being built to help hold decisions like these once you've made them. In development.

**Hashtags:** #Shanivar #DailyRoutine #Focus #Attention #TheHybridMonk
**Alt text:** (1) Black slide, cream serif: "Not discipline theatre." (2) "Theatre: saving another routine reel. Rewriting the perfect plan at midnight." (3) Photo of a cream paper note with two handwritten lines: "Gym: Mon, Wed, Fri, 18:00" and "Feeds off: 21:30 to 07:30." (4) "Remove the decision when you already know what the right decision is." (5) "Start when you said you would. Shanivar, in development."
**Visual:** Slide 3 is a real photo of a handwritten note on cream paper on a dark desk, with gold ink or pencil. The other slides are type only. No phone screens.

---

## Flags: claims needing founder or legal sign-off

1. **Status lines in posts 3, 4 and 7** ("not tested on a real phone", "switched off", "phone testing next") are true per the files on 6 Oct. They must be re-checked against `build.gradle.kts:33`, the DEV-7 QA sheet and Jira DEV-7 on posting day (`routine-onboarding-marketing-plan.md:251`). I didn't re-read Jira for this draft, only the QA file.
2. **Post 3 honesty slide** names bypass routes (browser, second phone). The files support it (`ENFORCEMENT_STANCE_2026-09-12.md:108-113`; website FAQ). The founder should confirm publishing it.
3. **Post 4 "A strict app can fail you two ways…"** is worded so it doesn't accuse competitors. The plan's own hooks are closer to comparative claims ("strict apps usually lie", `routine-onboarding-marketing-plan.md:118`; "No ransom to unlock", `:122`). Get legal sign-off before using any comparative version.
4. **Post 5 "forty times"** is the plan's figurative number (`routine-onboarding-marketing-plan.md:130`), not a measured statistic. Founder to keep or soften it.
5. **Post 1 slide 4 and other "is being built to…" lines** adapt the UI's present tense (`RoutineOnboardingFlow.kt:276`) to Phase 0 intention language (`routine-onboarding-marketing-plan.md:170`). Founder to confirm.
6. **Post 6** needs real founder footage with music rights cleared (`marketing-operating-system.md:34`). Any AI substitute needs on-screen and platform disclosure (`social-presence-from-scratch.md:167`).
7. **Brand pairing "by The Hybrid Monk"** has to stay separate from the clothing business (`ai_org/agents/social_media_manager.md:47`).

## Flags: conflicts in the plan vs stance and decisions

1. **Marketing is paused.** The plan records a founder pause, "not yet recorded in a repo file" (`routine-onboarding-marketing-plan.md:3`; `social-presence-from-scratch.md:3`; decisions D6/D8 at `routine-onboarding-marketing-plan.md:267-269`). Publishing needs an unpause first.
2. **Voice (open decision D1).** The founder's "very strict, high-velocity" positioning (`routine-onboarding-marketing-plan.md:33`, `:48`) pulls against the patient-teacher instruction (`SHANIVAR_STANCE.md:11`) and the voice direction (`marketing/shanivar/voice-direction.md:5`, `:36`). My drafts keep the plan's direct UI lines but add "maybe", invitations and no verdicts. Posts 1, 4, 5 and 9 are the most direct. The founder should confirm the split or pick one voice.
3. **CTA.** The plan ends posts with "Early access: link in bio" (`routine-onboarding-marketing-plan.md:156`, `:213`), and both bio options end with "early access ↓" (`social-presence-from-scratch.md:70`, `:78`). Your brief forbids this, so every CTA was replaced with "In development". The bio you gave already drops it.
4. **Domain.** The repo treats `https://shanivar.com` as canonical (`social-presence-from-scratch.md:18`, `:83`; `website/README.md:5`). `shanivar.netlify.app` doesn't appear anywhere in the repo. Neither URL is used in these drafts.
5. **Two orders in the plan.** The first-9 grid (`social-presence-from-scratch.md:116-126`) differs from the two-week calendar (`routine-onboarding-marketing-plan.md:211-218`). The calendar puts A4 third and A3 fourth, and labels the build log "A6 + status". I followed the first-9 table as instructed.
6. **Grid position vs pins.** The plan says "post 9 sits top-left" (`social-presence-from-scratch.md:114`) and also pins 1, 3 and 7. Pinned posts take the top row, so post 9 won't be top-left (general Instagram behaviour, not verified in the app).
7. **"Change your life" calling (post 6)** has to stay an invitation (`routine-onboarding-marketing-plan.md:38`, `:46`) because of ST-008: "No claim of lasting transformation" (`SHANIVAR_STANCE.md:267`).
8. **Strict vs stoppable.** The enforcement stance says the lock shouldn't be "escapable by an ordinary frustrated person" (`ENFORCEMENT_STANCE_2026-09-12.md:12`), while the UI says "Stop protection anytime" (`RoutineOnboardingFlow.kt:538`). The plan resolves this as "stoppable by you, inside Shanivar" (`routine-onboarding-marketing-plan.md:239`), and posts 3 and 4 use that wording.
9. **No streaks or gamification** in any post, per `docs/decisions/0009-behavioral-return-loop.md:15`, `:17`, ST-005 (`SHANIVAR_STANCE.md:167`), `0014-…md:71` and `wish-list.md` F1. 0013 is still PROPOSED (`0013-…md:4`). Recent commits `a74aa3f` and `12fbecd` add a dated "Last practised" record to Steady. That is factual history, not a streak, and it isn't marketed here.
10. **Breathing pillar.** 0011 put "Breathe & reset" on the native Home (`0011-…md:16`). 0014 supersedes that context: Steady sits inside Sadhana and is the only Sadhana content (`0014-…md:18`). Only post 7 mentions Steady, optionally, as internal-only. None of the 9 claim meditation, nervous-system or clinical effects (`research/deep-foundation/PRACTICE_SAFETY.md:51`).
11. **Saturn motif in Reels** must not animate like a breathing pacer (`website/index.html:66`; `style.css:79`).
12. **Mark source (open).** These drafts follow the live profile mark: a thin gold ringed-planet outline with SHANIVAR beneath it. No repo file reproduces that full lock-up. The closest asset is the Android launcher icon (`ic_launcher_foreground.xml:2-5`, which has no wordmark), and the website `favicon.svg` is a different, sage-sphere mark. Export the live picture, or add a matching source file, before rendering.

---

## Instagram optimization

- **Grid and pinning (from the plan):** publish 1→9 in order and pin 1, 3 and 7 (`social-presence-from-scratch.md:116-126`, `:182`). *Suggestion:* pin 7 first, then 3, then 1, so the manifesto lands in the first slot (the most recent pin usually shows first; check in the app). Design every cover to read inside the 3:4 grid crop (`:128`).
- **Clean the grid first (plan):** export and audit existing posts, including BASE-01, and archive rather than delete anything off-strategy before post 1, so legacy posts don't sit among the first nine (`social-presence-from-scratch.md:102-108`). Archiving is a founder action.
- **Cadence (plan):** 3 feed posts a week (2 Reels + 1 carousel) once rendering works. The floor is 2 carousels a week (`social-presence-from-scratch.md:146`; `marketing-operating-system.md:32`). That makes these 9 roughly a 3-week run. The slot is 19:30 IST = 16:00 CEST, which becomes 15:00 CET from 26 Oct. This is a planning convention, not a measured optimum (`routine-onboarding-marketing-plan.md:209`). *Suggestion:* if Reels can't be rendered, post the carousel fallbacks and keep the order.
- **Highlights (plan):** Why · Routine · Protection · Honest · Build log · FAQ, with thin gold outline icons on black covers, in the line style of the live mark (the plan allows sage or gold). Seed Why, Honest and Build log first (`social-presence-from-scratch.md:89-98`, `:180`). Posts 1, 3/4 and 7 feed those three.
- **Profile category (plan):** "App page" or "Software". No email or phone buttons until there's a monitored inbox. Profile image is already live: the thin gold ringed-planet outline with SHANIVAR beneath it, on black, with no Lord Shani imagery (`social-presence-from-scratch.md:63`, `:87`; the plan doesn't specify which mark, so the live picture is the reference).
- **Bio link (plan):** add it only after shanivar.com serves the site and a test sign-up shows up in Netlify Forms. Use one link with no link-tree: `https://shanivar.com/?utm_source=instagram&utm_medium=bio&utm_campaign=m0_waitlist` (`social-presence-from-scratch.md:83`, `:179`). Until then, captions mention no link at all.
- **Accessibility and community (plan + suggestion):** write custom alt text on every image and burn captions into Reels. No "comment YES" bait, auto-DMs or giveaways (`social-presence-from-scratch.md:157`). *Suggestion:* try Trial Reels for hook tests once the account qualifies. Eligibility is unknown (`:149`).
