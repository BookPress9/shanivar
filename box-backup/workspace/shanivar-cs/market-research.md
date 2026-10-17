# Shanivar: Market Research and Customer-Success Playbook

**Routine and discipline tools × meditation and stillness tools: where Shanivar should sit**

Prepared for: Shubham Kumar (founder) and the Shanivar PM · Date: 17 October 2026 (Europe/Berlin) · Version 1.0

---

## 0. How to read this report

**Evidence rules.** Every number has a source link next to it. If a number is an outside estimate (Business of Apps, research-firm market reports), the report says so. If data was not available, the report says that too.

**Primary data collected for this report (17 Oct 2026).**
- *Google Play listing data* (install band, star rating, rating count, in-app price range, last update) for 16 apps. I pulled it from each app's public Play page (US English store) and cross-checked it with the open-source `google-play-scraper` library. Raw files: `/workspace/shanivar-cs/src/`.
- *Review sample:* the **500 newest US-English Google Play reviews per app** (8,000 reviews total). For each app I counted the share of 1–2★ reviews and ran keyword theme matching on those 1–2★ reviews (price/paywall, bugs, bypass, permissions/battery, ads, login/sync, and so on). Limits: "newest" sorting over-weights recent updates and angry reviewers. Sample windows differ by app (Alarmy's 500 reviews span about 2 weeks; Waking Up's span about 2 years). Keyword matching is a rough signal, not a validated coding scheme. Treat the theme counts as directional.
- *Reddit:* Reddit blocked direct fetches from this environment (both www and old.reddit). Reddit findings below come from search-engine excerpts of the named threads. Each thread is linked, and I paraphrase rather than quote.

**What Shanivar is today** comes from the read-only local repo `/workspace/shanivar-v3`: README, decisions 0014/0017/0018, the initial MVP scope, the tone guide, the voice direction, and the Kotlin screen and feature names. Nothing in the repo was modified.

---

## 1. Executive summary

1. **Both categories leak users fast.** Across 93 popular Android mental-health apps, median retention was **3.9% at day 15 and 3.3% at day 30**. Mindfulness apps did a little better (median **4.7%** at day 30), and breathing-only apps had a median of **0%** ([Baumel et al., JMIR 2019](https://www.jmir.org/2019/9/e14567/)).
2. **The meditation leaders are shrinking.** Outside estimates put Headspace at **$140M revenue and 2.0M subscribers in 2025**, down from $235M and 3.3M in 2022 ([Business of Apps](https://www.businessofapps.com/data/headspace-statistics/)). Calm is put at **$210M and 3.5M subscribers in 2025**, down from $355M and 5.0M in 2022 ([Business of Apps](https://www.businessofapps.com/data/calm-statistics/)). In our Play sample, the newest 500 reviews averaged **1.93★ for Headspace** and **3.11★ for Calm**, against lifetime ratings of 4.25★ and 4.48★.
3. **The category is still growing.** Market estimates differ by definition: **$1.82B (2024) → $4.97B (2030), 17.7% CAGR** ([Grand View Research](https://www.grandviewresearch.com/horizon/statistics/mental-health-apps-market/application/meditation-management/global)), and **$2.16B (2025) → $7.91B (2030), 29.6% CAGR** ([The Business Research Company](https://www.thebusinessresearchcompany.com/report/mindfulness-meditation-application-global-market-report)).
4. **In blockers, friction works but fades.** one sec cut actual target-app openings by **57% after 6 weeks**, and the *option to dismiss* was the strongest ingredient ([PNAS 2023](https://www.pnas.org/doi/10.1073/pnas.2213114120)). On Reddit, people say pauses turn into muscle memory within weeks and get bypassed ([r/ProductivityApps](https://www.reddit.com/r/ProductivityApps/comments/1v314we/why_do_most_app_blockers_only_work_for_a_few_weeks/), [r/getdisciplined](https://www.reddit.com/r/getdisciplined/comments/1vqqrpb/app_blockers_stopped_working_for_me_after_a_few/)).
5. **Monetization is the biggest complaint everywhere.** Price/paywall/trial terms were the top 1–2★ theme for 12 of 16 sampled apps. The two donation-funded apps, **ScreenZen and Medito**, had the best recent sentiment (4.62★ and 4.65★ recent averages, 6% 1–2★ each).
6. **Users are already pairing the two categories on their own.** Blocker users praise the breathing pause. Routine-app users say routines fail because they doomscroll. Meditation users ask for a plain timer, less talk, and no guilt-driven streaks. No mainstream app makes **protected time** and **trained attention** into one daily skill.
7. **Recommended position for Shanivar:** *the Android practice that protects the hours you choose and trains the attention you bring to them, and that measures success partly by how much less you need it.*

---

## 2. Shanivar today (from the repo)

| Area | What exists or is decided | Repo evidence |
|---|---|---|
| Shell | Two destinations, **Routine** (opens first) and **Sadhana**. No dashboard. | `docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md`, `RoutineShell.kt`, `ShanivarShell.kt` |
| Routine | Wake and sleep anchors, editable personal activities, an optional **wake alarm** with movement or non-walking dismissal, a reusable set of distracting apps, **multiple protection windows** with weekly recurrence (including overnight), a daily timeline, and honest rule states (`DRAFT → SAVED_LOCAL → NEEDS_DEVICE_ACCESS → READY → ACTIVE → NEEDS_ATTENTION → OFF`). Local stop is always available. | `RoutineScreen.kt`, `RoutineProtectionScreen.kt`, `RoutineTimeline.kt`, `FocusWindowRecurrence.kt`, `AlarmScheduler.kt`, `MovementDismissal.kt`, `BlockOverlayActivity.kt` |
| Enforcement stance | Whole-app blocking through Usage access plus an overlay. No Device Admin and no accessibility service. No promise that blocking can't be bypassed. | `android-app/ENFORCEMENT_STANCE_2026-09-12.md`, decision 0002 |
| Sadhana | The "Living Spine" path (33 stages, visual only, unlocks nothing). Capabilities are *Arrive → Return → See → Choose → Carry*. Factual tracking of attempts, active seconds and outcomes. **Guidance tapers** (full/reduced/minimal) only when the user chooses. Practices run 1/3/5/10 minutes. All other apps are locked by default during practice. No streaks, XP or rank. | Decision 0017, `SadhanaHomeScreens.kt`, `SadhanaPracticeScreens.kt`, `SadhanaFocusLock.kt`, `SadhanaSpine.kt` |
| Attention self-check | Stage 1 "The base" is a 60-second sitting. Afterwards an optional self-check asks: did you reach the spine (Yes/Partly/No), how many times did attention drift (0…10+), and what pulled it away (thoughts, sounds, body discomfort, phone/notification urge, sleepiness, emotion, other), plus a note. Answers are stored locally as self-report and are never scored. | Decision 0018, `SadhanaStage1.kt`, `SadhanaRecordScreens.kt` |
| Voice and claims | Plain, honest and agency-first. No streak pressure, no health, energy or enlightenment claims. The app and marketing name no teacher or tradition (deep study is the only exception). | `docs/strategy/shanivar-tone-and-values.md`, `marketing/shanivar/voice-direction.md` |

**What this means.** Shanivar already has the parts that competitors split across separate apps: an alarm, scheduled protection, a lock during practice, and a reflective attention self-check. The open question is whether people understand the **link** between them and keep using it.

---

## 3. Part A: Routine, alarm and screen-protection apps

### 3.1 Opal (focus sessions, app blocking)
- **Core features:** scheduled and on-demand blocking sessions, "Deep Focus" sessions that can't be cancelled, app limits, a Focus Score, and breaks/pauses ([Opal pricing](https://www.opal.so/pricing)).
- **Pricing:** Opal Pro is **$99.99/yr**, **$19.99/mo** or **$399 lifetime**, with a 7-day trial. The free tier has basic blocking ([Opal pricing](https://www.opal.so/pricing), [help center](https://help.opalapp.com/article/how-and-why-should-i-subscribe-to-opal-pro)). Play lists in-app items at $2.99–$79.99 ([Play](https://play.google.com/store/apps/details?id=com.withopal.opal)).
- **Scale:** Play shows **500K+ installs, 4.67★ from 10,573 ratings** ([Play](https://play.google.com/store/apps/details?id=com.withopal.opal)). The company reports more than **$10M ARR** with 11 people ([Speedinvest](https://www.speedinvest.com/knowledge/scaling-smart-how-opal-built-a-10m-arr-business-in-just-2-years)) and is hiring to "scale from 1 to 10M+ daily active users" ([LinkedIn](https://www.linkedin.com/posts/kennethschlenker_opal-careers-join-team-opal-activity-7388877324668207104--5oi)). Both figures are company-reported. Opal is iOS-first, and Android is the smaller platform.
- **What users love:** blocks that really hold. *"doesn't let me just ignore the block like others I've tried"* (Play, 12 Sep 2026). Users also like the escalating wait: *"I do a couple minutes of deep breathing and then I don't even want to check anything anymore!"* (Play, 14 Oct 2026).
- **What users complain about:** In the newest-500 sample, 21.4% were 1–2★. The top themes were bugs (rules firing at the wrong times, apps being recategorized), paywall, and permissions. *"Half the time it blocks apps on the wrong days or the wrong times"* (Play, 10 Aug 2026). *"It's way to easy to bypass … All the options to ACTUALLY make it something useful are behind a paywall"* (Play, 24 Aug 2026). Users also say the rules can't handle split windows such as 9–11 and 2–5.
- **Churn reasons:** One r/nosurf tester uninstalled after 14 days, citing subscription dark patterns, battery drain, inflated "time saved" stats, and blocking that "treats symptoms". They reported their screen time was back to normal within 48 hours ([r/nosurf](https://www.reddit.com/r/nosurf/comments/1r0hjaq/i_tested_opal_screen_time_app_for_two_weeks_heres/), from a search excerpt).

### 3.2 Alarmy (mission-based alarm; "Alarmi")
- **Core features:** the alarm only turns off after a "mission" (math, memory, typing, squats, steps, photo or barcode scan), plus a "Wake Up Check" and sleep features ([alar.my](https://alar.my/en), [mission guide](https://alar.my/en/blog/how-to-choose-alarmy-mission)).
- **Pricing:** freemium with ads, and a Premium subscription that removes ads and adds missions. Play lists in-app items at **$2.90–$159.99** ([Play](https://play.google.com/store/apps/details?id=droom.sleepIfUCan)).
- **Scale:** Play shows **50M+ installs, 4.65★ from 2,175,324 ratings**. DelightRoom reports **2M DAU, KRW 30B annual revenue and KRW 19B operating profit (2024)**, and says it ranked #1 in 100 countries ([DelightRoom blog](https://delightroom.com/blog/en/daro-revealing-alarmy-ad-monetization-secrets)).
- **What users love:** it really gets people out of bed, and the math missions "focus" the mind. *"Love the Math missions"* (Play, 7 Oct 2026).
- **What users complain about:** 14.0% of the newest 500 were 1–2★. The top themes were missed or failed alarms, **ads at dismissal**, and paywall. *"Now every time I dismiss an alarm an ad pops up"* (Play, 14 Oct 2026). *"occasionally an alarm just won't trigger"* (Play, 15 Oct 2026). Some users report volume resetting to maximum after an update.
- **Churn reasons:** users dismiss the alarm half-asleep and go back to bed. *"It for sure gets me out of bed!..... until I just go lay on the couch and fall asleep again"* (Play, 9 Oct 2026). The same pattern shows up on Reddit ([r/GetOutOfBed](https://www.reddit.com/r/GetOutOfBed/comments/1b73g2r/even_alarmy_cant_wake_me_up/)). Ad load in the morning is a stated reason for leaving: *"after a year, I'm just about done with this app"*.

### 3.3 one sec (friction before distracting apps)
- **Core features:** a pause screen (breathing and a short delay) appears before chosen apps, with the option to close the app instead. Also re-interventions and in-app blocking such as Shorts ([FAQ](https://one-sec.app/faq/)).
- **Pricing:** the free tier covers 1 app. Pro is about **$3.99/mo or $19.99/yr, or $99.99 lifetime**, depending on store ([one sec FAQ](https://one-sec.app/faq/), [App Store](https://apps.apple.com/us/app/one-sec-screen-time-focus/id1532875441)).
- **Scale:** Play shows **1M+ installs, 4.76★ from 46,277 ratings** ([Play](https://play.google.com/store/apps/details?id=wtf.riedel.onesec)).
- **Evidence:** In a 280-person, 6-week field study, users closed the target app 36% of the time and made 37% fewer attempts, for **57% fewer actual openings**. The option to dismiss was the most effective component, and a reflection message alone was not effective ([PNAS 2023](https://www.pnas.org/doi/10.1073/pnas.2213114120)).
- **What users love:** a minimal tool that works. *"the first time it was so effective I quit doomscrolling for a year"* (Play, 6 Feb 2026).
- **What users complain about:** 18.6% were 1–2★, and price led the themes (35 of 93 low reviews). Users call it *"ABSURDLY priced for such a simple app"* (Play, 25 Jan 2026), say the Android version has fewer features than iOS, and report that Android turns the accessibility service off on its own.
- **Churn reasons:** the free tier is limited to one app, users resent paying a subscription for a simple utility, and the OS disables the service.

### 3.4 ScreenZen (free, donation-funded)
- **Core features:** delays and limits per app, "open intentionally" prompts (such as typing your reason), blocking parts of apps (Facebook vs Reels, YouTube Shorts) ([screenzen.co](https://screenzen.co/)).
- **Pricing:** free, with no premium tier; optional tips/donations ([screenzen.co](https://screenzen.co/)). Play lists $4.99–$19.99 items, which are tips.
- **Scale:** Play shows **1M+ installs, 4.79★ from 34,429 ratings** ([Play](https://play.google.com/store/apps/details?id=com.screenzen)).
- **What users love:** it is mindful without being punishing. *"works to make me mindful about app use without being so annoying or restrictive that I delete it within a week"* (Play, 5 Sep 2026). The written-reason intervention *"actually makes me reflect and pause"* (Play, 7 Sep 2026).
- **What users complain about:** only 6.0% were 1–2★ (the lowest share among the blockers). Complaints focus on **Android accessibility-service drops**, lag, and lost records. *"constantly being asked to turn accessibility services off/on"* (Play, 13 Sep 2026). Another user lost a 60-day Facebook-free record after an update.
- **Churn reasons:** reliability on phones with aggressive battery optimization, and on Reddit, delays that people learn to wait out ([r/nosurf](https://www.reddit.com/r/nosurf/comments/1r2zrvi/i_tried_screenzen_for_a_month_heres_what_nobody/), from a search excerpt).

### 3.5 Freedom (cross-device blocker)
- **Core features:** blocking across devices, scheduled sessions, and **Locked Mode**, which stops a running session from being ended or edited ([Locked Mode](https://support.freedom.to/en/articles/1802927-locked-mode)).
- **Pricing:** **$39.99/yr**, or lifetime at $199 (often on sale at $99.50) ([freedom.to/premium](https://freedom.to/premium), [Forever](https://freedom.to/forever/)).
- **Scale:** Play shows **1M+ installs, 4.56★ from 8,235 ratings** ([Play](https://play.google.com/store/apps/details?id=to.freedom.android2)).
- **What users love:** sessions that can't be cancelled. *"Other apps allow you to cancel block sessions, which defeats the purpose"* (Play, 25 Jul 2025). Users also mention ADHD and "time-blindness" support.
- **What users complain about:** 29.4% were 1–2★. Themes: price and annual-only plans, being easy to get around, daylight-saving and time bugs, and whole-domain blocking. *"Only install if you're sure you want to pay $40 for an app you're statistically unlikely to keep using"* (Play, 18 May 2026).
- **Churn reasons:** rigid "cold turkey" blocking with no daily allowance, sync and time errors, and cancellation friction.

### 3.6 Forest (gamified focus timer)
- **Core features:** a tree grows while you stay off your phone, with an allow-list, group focus, and coins that can plant real trees through Trees for the Future ([forestapp.cc](https://forestapp.cc/)).
- **Pricing:** free core with ads outside sessions. The old one-time Pro purchase has been replaced for new users by the **Forest Plus subscription** ([forestapp.cc](https://forestapp.cc/)).
- **Scale:** Play shows **10M+ installs, 4.31★ from 813,854 ratings** ([Play](https://play.google.com/store/apps/details?id=cc.forestapp)).
- **What users love:** social study sessions. *"I made all my friends get this app"* (Play, 2 Sep 2026).
- **What users complain about:** 45.4% were 1–2★, set off by a **September 2026 redesign**. Users report extra screens after each session, hidden totals, and **paid features removed**. *"monetisation and engagement are the focus, every UI transition is too long and flashy"* (Play, 16 Sep 2026).
- **Churn reasons:** broken trust after paid features were removed, and engagement mechanics that interrupt the focus the app is meant to protect.

### 3.7 Fabulous (routine and habit coach)
- **Core features:** guided habit "journeys" (morning, evening and other routines), habit stacking, and coaching content. The company presents it as rooted in Duke behavioral-economics work ([thefabulous.co](https://thefabulous.co/)).
- **Pricing:** checkout pricing is personalized, so no single public price was available. Play lists **$3.99–$299.99** items ([Play](https://play.google.com/store/apps/details?id=co.thefabulous.app)).
- **Scale:** Play shows **10M+ installs, 3.85★ from 589,928 ratings**.
- **What users love:** gentle structure and visuals. *"I remember how good my life was when i used it"* (Play, 9 Oct 2026).
- **What users complain about:** **57.6% were 1–2★**, and 212 of the 288 low reviews matched billing terms. Users describe surprise charges, overlapping bundles, and hard cancellation. They also call it *"a glorified reminder app"* with *"super persistent annoying reminders"* (Play, Jul–Aug 2026).
- **Churn reasons:** loss of trust over billing, notification overload, and a busy interface.

### 3.8 Routinery (step-by-step routine timer)
- **Core features:** routines broken into timed steps with voice prompts and a start alarm ([Play](https://play.google.com/store/apps/details?id=com.alt.goodmorning)).
- **Pricing:** freemium. The free tier was cut to 2 routines, and Play lists $0.99–$72.00 items.
- **Scale:** Play shows **1M+ installs, 4.41★ from 18,726 ratings**.
- **What users love:** ADHD-friendly structure. *"I know what routines will make my life better, but I am easily distracted"* (Play, 31 Dec 2025).
- **What users complain about:** 22.6% were 1–2★, about the free-tier cut, bugs, and loud default alarms. One review captures the intersection gap directly: *"I was hoping this app would help me stick on schedule since I doomscroll a lot in the mornings but there's no visual or audio alarm so I just keep doomscrolling"* (Play, 24 Sep 2025).

### 3.9 Sleep as Android (smart alarm and sleep tracking)
- **Core features:** sleep-cycle smart wake, CAPTCHA wake tasks, wearable integration, and cloud backup ([docs](https://sleep.urbandroid.org/docs/general/plan.html)).
- **Pricing:** monthly or yearly subscription, or an in-app lifetime purchase listed at **$75**. The old Unlock app is legacy ([docs](https://sleep.urbandroid.org/docs/general/plan.html)).
- **Scale:** Play shows **10M+ installs, 4.57★ from 392,616 ratings** ([Play](https://play.google.com/store/apps/details?id=com.urbandroid.sleep)).
- **What users love:** reliability and depth over years of use, plus responsive support (the top review has 118 likes).
- **What users complain about:** 27.0% were 1–2★. Themes: a **second paywall for users who already paid once**, ads with sound at bedtime, and alarms not going off. *"This is the 10th time my alarm did not go off"* (Play, 27 Jun 2026).

*(Habitica, 5M+ installs and 4.65★, was also sampled. Its 13.6% low share splits between subscription-perk and price complaints and server, sync and timezone problems. People who like it use it as an ad-free habit tracker and ignore the game layer.)*

### 3.10 Part A synthesis: what works and what doesn't

**Working**
- **Friction with a way out.** A pause plus the option to dismiss gave the strongest measured effect ([PNAS](https://www.pnas.org/doi/10.1073/pnas.2213114120)). Many 5★ reviews praise breathing or reflection prompts inside blockers (Opal, one sec, ScreenZen).
- **Commitment you can't undo mid-session,** chosen ahead of time: Freedom Locked Mode, Opal Deep Focus. A systematic review found blocking works when bypassing takes real effort, yet users *prefer* negotiable blocks ([Biedermann et al. 2021](https://onlinelibrary.wiley.com/doi/full/10.1111/jcal.12581)). The same review found that awareness-only dashboards were generally ineffective.
- **A physical action at wake-up.** Alarmy's missions work at very large scale.
- **Generous free tiers build goodwill.** ScreenZen had 6% low reviews and Medito 6%, compared with 21–58% for subscription-heavy peers.

**Not working**
- **Habituation.** Pauses become muscle memory within weeks ([r/ProductivityApps](https://www.reddit.com/r/ProductivityApps/comments/1v314we/why_do_most_app_blockers_only_work_for_a_few_weeks/)). Alarms get dismissed and people go back to bed.
- **Reliability on Android.** OEM battery killers and accessibility-service drops (one sec, ScreenZen), mis-timed rules (Opal, Freedom), and missed alarms (Alarmy, Sleep as Android). A tool that fails silently loses trust right away.
- **Monetization in the moment of need:** ads at alarm dismissal, paywalls on basic rules, surprise renewals, and paid features removed (Forest, Sleep as Android).
- **Engagement features that contradict the promise.** Forest's post-session reward screens and Fabulous's notification floods.
- **Symptom-only design.** Blocking without an inner skill: "screen time back to normal within 48 hours" after uninstalling ([r/nosurf](https://www.reddit.com/r/nosurf/comments/1r0hjaq/i_tested_opal_screen_time_app_for_two_weeks_heres/)).

---

## 4. Part B: Meditation, mindfulness and stillness apps

### 4.1 Headspace
- **Core features:** guided courses, sleep content, coaching and therapy (in some markets), and **Ebb, an AI companion** (with voice since Dec 2025) ([Headspace](https://www.headspace.com/ai-mental-health-companion), [BusinessWire](https://www.businesswire.com/news/home/20251208896917/en/Headspace-Rolls-out-Voice-Feature-for-Empathetic-AI-Companion-Ebb)).
- **Pricing:** subscription. Play lists **$4.99–$119.99** items ([Play](https://play.google.com/store/apps/details?id=com.getsomeheadspace.android)).
- **Scale:** Play shows **10M+ installs, 4.25★ from 339,983 ratings**. Estimates: **$140M revenue and 2.0M subscribers in 2025**, down from a 2022 peak ([Business of Apps](https://www.businessofapps.com/data/headspace-statistics/), outside estimate). The company cut 13% of staff in 2025 ([eMarketer](https://www.emarketer.com/content/headspace-cuts-13--of-its-workforce--transitions-staff-therapists-contract-part-time-roles)). Ebb users reportedly show 34% higher engagement ([Headspace](https://www.headspace.com/articles/headspace-brings-empathetic-ai-companion-ebb-to-uk-members), company claim).
- **What users love:** it teaches beginners. *"It taught me to meditate and encouraged me to stick with it"* (Play, 28 Jun 2026).
- **What users complain about:** the **newest-500 average was 1.93★ and 73.6% were 1–2★**. Themes: crashes and loading, a cluttered redesign, price, AI customer service and the AI feature. *"No room for 'headspace' when there's pop ups, freezes, recommendations"* (Play, 14 Jul 2026). *"This is an app that plays MP3's … It does not do it well"* (Play, 4 Oct 2026).
- **Churn reasons:** lost simplicity, reliability problems, price, and people outgrowing guided content ([r/Meditation](https://www.reddit.com/r/Meditation/comments/1hmuqfq/has_anyone_here_tried_calm_or_headspace_for_a/), from a search excerpt).

### 4.2 Calm
- **Core features:** sleep stories, music and soundscapes, guided meditation. New in 2025: a **Calm Sleep** app with personalized sleep plans and HealthKit sync ([Calm blog](https://blog.calm.com/blog/calm-sleep-press-release)).
- **Pricing:** **$14.99/mo, $69.99/yr or $399.99 lifetime**, with a limited free tier ([CNBC 2025](https://www.cnbc.com/2025/01/16/2-friends-spent-years-getting-turned-down-for-their-terrible-ideanow-the-calm-app-is-worth-2-billion.html)).
- **Scale:** Play shows **50M+ installs, 4.48★ from 616,435 ratings**. Estimates: **$210M revenue and 3.5M subscribers in 2025, with 7.3M downloads in 2025** ([Business of Apps](https://www.businessofapps.com/data/calm-statistics/), outside estimate). The last valuation was $2B (2020).
- **What users love:** the sleep library and voices. *"Still using Calm 6 years later"* (Play, 29 Aug 2026).
- **What users complain about:** 43.4% were 1–2★. Billing and cancellation matched 140 of 217 low reviews. *"the keep subscription was in the button and to cancel was in small print below it"* (Play, 12 Jul 2026). Users also report locked content and slow support.
- **Churn reasons:** subscription fatigue and perceived dark patterns ([r/Meditation "Calm App Greed"](https://www.reddit.com/r/Meditation/comments/1fyjgzv/calm_app_greed/), from a search excerpt). Many use it for sleep only, without a practice.

### 4.3 Insight Timer
- **Core features:** the largest free library (**300K+ free tracks**, 30M+ members), live events, a meditation timer, and paid courses ([Insight Timer support](https://help.insighttimer.com/support/solutions/articles/67000664719-what-is-insight-timer-)).
- **Pricing:** free core. MemberPlus is **$9.99/mo or $59.99/yr** ([pricing](https://help.insighttimer.com/support/solutions/articles/67000664579-how-much-does-insight-timer-cost-)).
- **Scale:** Play shows **10M+ installs, 4.70★ from 263,176 ratings**.
- **What users love:** the breadth and the free value, and a **simple timer** (88 of the 500 reviews mention the timer or silence).
- **What users complain about:** 21.6% were 1–2★. Clutter (quotes, stories, onboarding questions) and **compulsory goals and streaks**. *"I don't need an app to tell me off and guilt me if I meditate without it for a few days"* (Play, 19 Aug 2026). *"forces you to answer loads of questions … when I just want to use a basic timer"* (Play, 5 Oct 2026).
- **Churn reasons:** feature creep that pushes out the silent practitioner the app grew up on.

### 4.4 Waking Up
- **Core features:** an introductory course, a daily guided sitting, theory and conversations, and a non-dual and insight focus ([wakingup.com](https://www.wakingup.com/?c=GetStarted)).
- **Pricing:** **$19.99/mo or $129.99/yr**, a free scholarship on request, and a refund policy ([Waking Up](https://www.wakingup.com/?c=GetStarted), [help](https://help.wakingup.com/article/72-what-s-waking-up-s-risk-free-pricing-policy)).
- **Scale:** Play shows **1M+ installs, 4.59★ from 41,758 ratings**.
- **What users love:** depth, and a sense of real progress. *"After 15 sessions … that has changed for me"* (Play, 18 Mar 2025).
- **What users complain about:** 16.0% were 1–2★. Users dislike that there is **no free content without entering a card** (*"ZERO free content to evaluate"*, Play, 7 Jun 2025) and confusing trials. Some reviews are put off by the founder's public persona, which shows the **risk of a brand tied to one person**.

### 4.5 Medito (nonprofit)
- **Core features:** guided courses, a daily session, sleep, breathing and a timer, **all free with no ads** ([Medito Foundation](https://meditofoundation.org/free-meditation-app/)).
- **Pricing:** free and donation-funded ([About](https://meditofoundation.org/about/)).
- **Scale:** Play shows **1M+ installs, 4.78★ from 41,092 ratings**.
- **What users love:** *"Finally, an app that cares about the user experience and not just signing you up for a subscription"* (Play, 14 Apr 2026).
- **What users complain about:** only 6.0% were 1–2★: sign-up loops, repetitive daily content, and "smart" defaults that replaced fixed reminder times. *"Because of these 'smart' features, I rarely meditate. Before the changes, I meditated daily for two years"* (Play, 10 Jul 2026). Some users also object to streaks ("manipulation without consent").

### 4.6 Balance (adaptive, personalized)
- **Core features:** plans that adapt to your answers and progress, with mid-plan check-ins ([balanceapp.com](https://balanceapp.com/)).
- **Pricing:** the earlier "first year free" offer has ended. It is now a **30-day trial, then $69.99/yr** (US) ([Balance](https://get.balanceapp.com/account/subscription/new)).
- **Scale:** Play shows **1M+ installs, 4.63★ from 48,627 ratings**.
- **What users love:** personalization that remembers you. *"remembers when last you've done a meditation and doesn't repeat the instructions"* (Play, 16 Apr 2026).
- **What users complain about:** 23.8% were 1–2★. Themes: trial-to-annual charges, cancellation friction ("SEVEN different pages"), playback stopping mid-session, and *"the meditations talk too much and don't really give much time to meditate"* (Play, 25 Aug 2025).

### 4.7 Attention-training example: Healthy Minds Program
- **Core features:** free, skill-based training in Awareness, Connection, Insight and Purpose, with up to 600 days of content ([humin.org](https://www.humin.org/wellbeing-tools/app)).
- **Evidence:** a three-arm RCT found reduced distress after 8 weeks ([JMIR Mental Health 2020](https://mental.jmir.org/2020/11/e23825/)). A 4-week RCT with 662 school employees found improved wellbeing ([doi](https://doi.org/10.1037/edu0000739)).
- **Android Play data:** not retrieved. The package wasn't found in Play search from this environment, so no figures are given.
- **Relevance:** this is the "skill, not content" model Sadhana follows. Breath counting is a validated **behavioral** measure of mindfulness, and its accuracy tracks lower mind-wandering ([Levinson et al. 2014](https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2014.01202/full)). Experience sampling found minds wander in **46.9%** of waking moments ([Killingsworth & Gilbert 2010](https://www.science.org/doi/10.1126/science.1192439)). This supports a drift-and-return self-check as meaningful, if self-reported.

### 4.8 Part B synthesis

**Top challenges**
1. **Retention collapse.** About 69% of users open on day 1, 3.9% on day 15 and 3.3% on day 30, with the steepest drop between days 1 and 10 ([Baumel 2019](https://www.jmir.org/2019/9/e14567/)).
2. **Subscription fatigue and billing distrust.** Price/billing was the #1 low-review theme for Headspace, Calm, Insight Timer, Waking Up and Balance. Free and donation models (Medito, Insight Timer's core) hold the most goodwill.
3. **Content fatigue and bloat.** Libraries grow while the practice gets buried (Headspace, Insight Timer). People want *less*: a timer, less talk, and the app remembering their settings.
4. **Guided vs. silent.** Beginners need guidance, while experienced users leave for silence. Leaving because they've grown ([r/Meditation](https://www.reddit.com/r/Meditation/comments/1hmuqfq/has_anyone_here_tried_calm_or_headspace_for_a/)) is a normal path, and most apps treat it as churn.
5. **Measuring progress.** Minutes and streaks are the default metrics. Streaks create guilt (Insight Timer and Medito reviews), and there is no common measure of *attention quality*. Validated behavioral measures exist (breath counting) but are rarely productized.
6. **The phone is the distraction.** Phone-based mindfulness invites interruption ([The Atlantic 2023](https://www.theatlantic.com/health/archive/2023/06/meditation-app-mind-distraction/674500/)). Shanivar's lock during practice addresses this directly.
7. **Safety and claims.** In an MBCT sample, 6–14% reported lasting bad effects of more than a day ([Britton et al. 2021](https://pmc.ncbi.nlm.nih.gov/articles/PMC8845498/)). This supports Shanivar's no-breath-pacing and "End always reachable" rules.

**Usage patterns.** Meditation apps peak **twice a day, morning and night**. Other mental-health apps peak in the evening. Among active users, meditation apps had high daily minutes (median 21.5) but a low open rate (median 4% DAU/installs overall) ([Baumel 2019](https://www.jmir.org/2019/9/e14567/)). Sleep is the main job for Calm users (from review content).

**Market trends**
- **Size and growth:** $1.82B (2024) → $4.97B (2030), 17.7% CAGR for meditation-management apps ([Grand View](https://www.grandviewresearch.com/horizon/statistics/mental-health-apps-market/application/meditation-management/global)). $2.24B (2025) → $5.36B (2030), 18.6% CAGR ([TBRC](https://www.thebusinessresearchcompany.com/report/meditation-management-apps-global-market-report)). TBRC names Asia-Pacific the fastest-growing region.
- **India:** Statista projects the India meditation-app segment at **US$78.94M in 2025**, with 0.14% user penetration ([Statista](https://www.statista.com/outlook/hmo/digital-health/digital-fitness-well-being/health-wellness-coaching/meditation-apps/india); paywalled, so the figure comes from a search excerpt). Grand View puts India spiritual-wellness apps at **US$81.9M in 2025, with Android at 54.21% of revenue** ([Grand View](https://www.grandviewresearch.com/horizon/outlook/spiritual-wellness-app-market/india)). Free, teacher-led launches can spike: one free app passed 1M downloads within 15 hours of launch in Mar 2025 ([Times of India](https://timesofindia.indiatimes.com/technology/tech-news/sadhgurus-free-meditation-app-miracle-of-mind-beats-chatgpt-sees-1-million-plus-downloads-in-15-hours/articleshow/118674942.cms)).
- **AI personalization:** Headspace's Ebb has received 7M+ messages ([BusinessWire](https://www.businesswire.com/news/home/20251208896917/en/Headspace-Rolls-out-Voice-Feature-for-Empathetic-AI-Companion-Ebb)). Balance's adaptive plans are well liked. But AI also drew backlash in Headspace reviews (35 of the newest 500 mention AI; 25 of those are 1–2★).
- **Digital wellbeing and physical friction:** devices like Brick ($49–$60, no subscription) are part of a "bricking" and digital-minimalism trend ([Marketplace 2025](https://www.marketplace.org/story/2025/11/12/the-growing-business-of-disconnecting), [Slate 2025](https://slate.com/technology/2025/09/brick-app-review-digital-detox-instagram-tiktok.html)).
- **Wearables:** Oura and Apple Watch link mindfulness sessions with HRV and heart-rate feedback ([Oura](https://ouraring.com/blog/new-movement-partnerships/), [Apple](https://support.apple.com/guide/watch/start-a-reflect-or-breathe-session-apd371dfe3d7/watchos)). Smartwatch feedback improved mindfulness and stress, but not anxiety or depression, in one RCT ([PLOS One](https://journals.plos.org/plosone/article?id=10.1371%2Fjournal.pone.0322413)).

---

## 5. Competitor comparison table

Play data was retrieved on 17 Oct 2026 (US store). "Recent avg" and "1–2★ share" come from the newest 500 Play reviews per app. "Top low-review theme" is the most frequent keyword theme among the 1–2★ reviews in that sample.

| App | Side | Play installs | Lifetime ★ (ratings) | Recent-500 avg ★ | 1–2★ share (recent) | Top low-review theme | Model / price (sourced above) |
|---|---|---|---|---|---|---|---|
| Opal | A | 500K+ | 4.67 (10,573) | 3.86 | 21.4% | Bugs / mis-timed rules | Sub: $99.99/yr, $19.99/mo, $399 lifetime |
| Alarmy | A | 50M+ | 4.65 (2.18M) | 4.26 | 14.0% | Missed alarm / ads | Ads + Premium sub |
| one sec | A | 1M+ | 4.76 (46,277) | 4.04 | 18.6% | Price | 1 app free; ~$19.99/yr, $99.99 lifetime |
| ScreenZen | A | 1M+ | 4.79 (34,429) | 4.62 | 6.0% | Accessibility drops / bugs | Free, donations |
| Freedom | A | 1M+ | 4.56 (8,235) | 3.63 | 29.4% | Price / annual-only | $39.99/yr; lifetime $199 |
| Forest | A | 10M+ | 4.31 (813,854) | 3.02 | 45.4% | Price / redesign | Free + ads; Plus sub |
| Fabulous | A | 10M+ | 3.85 (589,928) | 2.60 | 57.6% | Billing | Personalized sub pricing |
| Routinery | A | 1M+ | 4.41 (18,726) | 3.92 | 22.6% | Free-tier cut / bugs | Freemium sub |
| Sleep as Android | A | 10M+ | 4.57 (392,616) | 3.75 | 27.0% | Second paywall / ads | Sub or $75 lifetime |
| Habitica | A | 5M+ | 4.65 (75,906) | 4.19 | 13.6% | Price / server-sync | Free + optional sub |
| Headspace | B | 10M+ | 4.25 (339,983) | **1.93** | **73.6%** | Price / crashes | Sub ($4.99–$119.99 items) |
| Calm | B | 50M+ | 4.48 (616,435) | 3.11 | 43.4% | Billing / cancellation | $69.99/yr, $399.99 lifetime |
| Insight Timer | B | 10M+ | 4.70 (263,176) | 4.02 | 21.6% | Clutter, forced streaks | Free core; $59.99/yr |
| Waking Up | B | 1M+ | 4.59 (41,758) | 4.29 | 16.0% | Card-gated trial | $129.99/yr; scholarships |
| Medito | B | 1M+ | 4.78 (41,092) | 4.65 | 6.0% | Sign-up bugs | Free nonprofit |
| Balance | B | 1M+ | 4.63 (48,627) | 3.98 | 23.8% | Trial → annual charge | 30-day trial, $69.99/yr |
| Healthy Minds | B | n/a (not retrieved) | n/a | n/a | n/a | n/a | Free |
| **Shanivar** | A+B | Pre-launch | n/a | n/a | n/a | n/a | Payments deferred (MVP scope) |

**Pattern:** the apps that monetize most aggressively get the angriest recent reviews. This is a correlation in a skewed sample, not proof of cause.

---

## 6. Part C: The intersection and Shanivar's position

### 6.1 Unmet needs that fall between the two categories

| Discipline tools give… | Meditation tools give… | Neither delivers |
|---|---|---|
| A wall around time | Content for the mind | **Training the attention you bring into the protected hour** |
| Friction that wears off within weeks | Practice that stays inside the app | **Practice that carries into the moment of urge** (the block surface as a practice cue) |
| Usage dashboards (shown to be weak) | Minutes and streaks | **An honest attention trend:** drifts, returns, what pulls you away |
| Morning missions | Morning sessions | **One morning sequence:** wake, a short sitting, a protected first hour, all optional |
| Lock-in and subscription traps | Growing libraries | **Graduation:** needing the app less counts as success |

Review evidence for this gap: blocker users praise breathing pauses (Opal and one sec 5★ reviews). A routine-app user says doomscrolling ruins their routine (Routinery). Meditators ask for silence and fewer prompts (Insight Timer, Balance). People leave meditation apps after outgrowing them (r/Meditation).

### 6.2 Positioning statement
> **For** Android users who know what they want to do with their day but keep losing it to their phone, **Shanivar is** a daily practice that **protects the hours you choose** (alarms and protection windows) **and trains the attention you bring to them** (Sadhana sittings with an honest drift-and-return self-check). **Unlike** blockers, which build walls that wear off, and meditation apps, which sell libraries that never touch your day, Shanivar treats discipline and stillness as **one skill**, and counts it as success when you need the app less.

### 6.3 Core user problems Shanivar should own
1. **"I set the boundary, then I break it."** Protection windows that hold, with honest state, an easy way to stop locally, and no guilt.
2. **"My attention leaves, and I don't notice until much later."** The Stage 1 self-check makes drift and return visible over time, as self-report and never a score.
3. **"Mornings slip into the feed."** Wake alarm, then an optional 60-second sitting, then a protected first window. Each step is independent and nothing chains automatically (decision 0007/MVP rule).
4. **"Meditation apps feel like more content, more phone."** Silent or minimal guidance, a lock during practice, and short fixed sittings.
5. **"Apps keep me hooked instead of making me capable."** No streaks or XP. Guidance tapers when you choose, and "Carry" means practising without the app.

### 6.4 Target user profiles
- **P1 – The self-directed scroller (primary).** Age 18–35, on Android, India first, English or Hindi. Has tried Digital Wellbeing, Opal, one sec or ScreenZen and relapsed. Wants focus for study or work. Evidence: ScreenZen and one sec 5★ reviews, Reddit relapse threads.
- **P2 – The practice-curious who is tired of content.** Has left Headspace or Calm, or uses only Insight Timer's timer. Wants silent, skill-based practice with a sense of progress but no streaks. Evidence: Insight Timer, Medito and Balance reviews.
- **P3 – The family introducer (hypothesis, from repo MVP scope).** An adult child introduces Shanivar to a willing parent, who keeps control. This is not yet validated, and the repo says so.

### 6.5 Risks and failure patterns to avoid (from competitors)
| Failure pattern | Seen in | Shanivar guard |
|---|---|---|
| Silent enforcement failure (OEM battery kill, permission drop, wrong times) | one sec, ScreenZen, Opal, Freedom | Show `NEEDS_ATTENTION` honestly, add a repair flow and OEM guidance (ticket SHAN-3) |
| Missed alarm | Alarmy, Sleep as Android, Routinery | Keep the watchdog, reboot rescheduling and device QA as release gates |
| Ads or paywall at the moment of need | Alarmy (ads at dismissal), Opal (paywalled basics) | Never monetize the alarm, block surface or practice surface |
| Billing dark patterns, removing paid features | Fabulous, Calm, Balance, Forest, Sleep as Android | Cancel in one tap, no card for a trial, keep features people paid for forever (SHAN-7) |
| Redesign bloat and engagement screens | Forest, Headspace, Insight Timer | No interstitials after sessions; protect the two-destination shell |
| Streak guilt and forced goals | Insight Timer, Medito | Already banned (decision 0013/0017); keep it that way |
| "Smart" defaults that override user choices | Medito | Personalization only suggests; the user confirms |
| Brand tied to one person | Waking Up | Name no teacher; speak only as Shanivar (already policy) |
| Habituation to the same pause | one sec, ScreenZen (Reddit) | Vary the prompt using the person's own words and self-check data (SHAN-2) |
| Health or energy over-claims; distress during practice | Category-wide; Britton 2021 | Keep SOP-05, no breath pacing, End always reachable |

### 6.6 Customer-success playbook

**North-star metric (proposed):** *Weekly protected-and-present hours.* This is the sum of protection-window time that reached engine-confirmed `ACTIVE` and was not released early, plus completed Sadhana seconds. Pair it with a **graduation signal**: self-reported practice without the app.

**Success metrics to track** (local-first and privacy-respecting, following SOP-09):
| Stage | Metric | Definition | Benchmark / target |
|---|---|---|---|
| Activation | Protection activated | First window reaches `ACTIVE` (engine-confirmed) within 48 h of install | No public benchmark; set a baseline in the pilot |
| Activation | First sitting | First Stage 1 sitting completed (not ended early) within 7 days | Baseline in pilot |
| Activation | Alarm armed | Native schedule confirmed (next occurrence shown) | Baseline in pilot |
| Adherence | Window hold rate | Share of scheduled windows that reached `ACTIVE` and finished without early release | Watch the trend, not the absolute |
| Adherence | Alarm follow-through | Fired → dismissed through movement or the accessible alternative (not proof of waking) | Separate from focus metrics (MVP rule) |
| Attention | Sitting-time trend | Median active seconds per sitting and chosen duration (1/3/5/10) over 4 weeks | Self-chosen; the trend is what matters |
| Attention | Return trend | Share of self-checks with "reached: Yes/Partly", and median self-reported drifts, over time | Self-report only, never shown as a score |
| Attention | Pull mix | Distribution of "what pulled you away"; the share that is "phone/notification urge" | Rising phone urge → suggest protection |
| Retention | D1 / D7 / D30 opens or rule activity | Measured from any device-backed rule firing, not only opens | Beat the category medians: 3.9% D15, 3.3% D30, 4.7% D30 for meditation ([Baumel](https://www.jmir.org/2019/9/e14567/)) |
| Graduation | Practice without the app | Count of self-reports of practising without the app | Counted as success, not churn |

**Key onboarding moments (first 7 days)**
1. **Minute 1:** the optional local pause before any account. Proof it helps before any permission is requested.
2. **First boundary:** one window, one app set. Permissions only when needed, and the status must truthfully reach `ACTIVE`.
3. **First block hit:** the block surface recalls the person's own intention, and the local pause is offered. This is the moment habituation starts, so the prompt has to change over time.
4. **First sitting and self-check:** the 60-second Stage 1. The person sees the first "drift, return" reflection.
5. **Day 3 to 7 link:** if the self-check says "phone urge", offer to protect the usual sitting time. The person confirms.
6. **End of week 1:** a quiet weekly mirror: protected hours held, sittings, drifts trend, top pulls. No grades.

**At-risk signals → playbook response**
| Signal | Likely meaning | Response (in-app, non-shaming) |
|---|---|---|
| Rule stuck in `NEEDS_DEVICE_ACCESS` or `NEEDS_ATTENTION` for more than 24 h; Usage access or overlay revoked | Silent failure, or OEM killed the service | Repair card with OEM-specific steps |
| Windows saved but never `ACTIVE` | Setup confusion | One-tap "test protection now" (2-minute window) |
| 3 or more early releases in a week | Window too long or at the wrong time | Suggest a shorter or moved window |
| 3 sittings in a row ended or interrupted, or "reached: No" 3 times in a row | Practice too hard, or wrong setting | Offer a shorter duration, posture tips, external-attention anchor |
| No device activity for 5 days after activation | Lapse | One gentle return note, then silence. No streak-loss messaging |
| Alarm disabled after fewer than 3 fires | Alarm too harsh or unreliable | Check sound and dismissal; offer the non-walking option |
| Pull mix dominated by "sleepiness" | Practice placed at a bad time of day | Suggest moving the sitting away from bedtime |

**Example user-facing copy** (Shanivar's poetic second-person voice; no teachers, traditions or claims):
- Block surface: *"You chose this hour for reading. The feed will wait. This hour will not."*
- Pause offer: *"Before you go back to your day, sit with yourself for one minute. Nothing will open. Only you will."*
- After the self-check: *"You drifted four times. Four times you came back. Each return was yours."*
- Return after a lapse: *"You have been away. Nothing was lost. Sit for one minute and begin from here."*
- Morning invitation (optional, after the alarm): *"The morning is here. Before the world reaches you, take one minute of your own."*
- Graduation: *"Today you sat without the app. That is the practice, carried."*

### 6.7 Feature and Jira ticket proposals for the PM

| # | Ticket | Problem (evidence) | Scope and acceptance criteria |
|---|---|---|---|
| SHAN-1 | **Local-first CS event layer** | Retention can't be managed without measuring it (Baumel benchmarks) | Typed events for rule state changes, alarm schedule/fire/dismiss, sitting start/end/outcome/seconds, and self-check submit. Stored on device; opt-in sync through the outbox. Dashboard query for activation, hold rate and D1/7/30. No content and no app names leave the device without consent (SOP-09). |
| SHAN-2 | **Block surface → "one-minute sitting" bridge** | Pauses get habituated (Reddit); the dismiss option is strongest (PNAS); breathing pauses are praised (Opal, one sec reviews) | From the block surface, offer the Stage 1 sitting inside the lock. It never unlocks. The prompt rotates using the person's own intention text and their last top pull. Measure: % of block hits → sitting started; % → "Return to my day". |
| SHAN-3 | **Protection health check and OEM repair guide** | Silent failures drive uninstalls (one sec, ScreenZen, Opal, Freedom reviews) | Daily local self-test of Usage access, overlay, battery-optimization exemption and exact-alarm permission. Show `NEEDS_ATTENTION` with OEM-specific steps (Xiaomi, Samsung, Oppo/Vivo, OnePlus). Acceptance: no screen shows `ACTIVE` without engine confirmation (decision 0014). |
| SHAN-4 | **Weekly attention mirror** | Dashboards alone don't work (Biedermann 2021), and there is no honest attention measure on the market | A weekly local card covering protected hours held, sittings, median active seconds, the drift trend, the top-3 pulls and practice without the app. No score, streak or ranking. "Clear" and "hide" controls. Copy reviewed under SOP-05. |
| SHAN-5 | **Self-check → Routine suggestion ("Choose")** | People pair the two categories on their own (Routinery and Opal reviews) | If "phone/notification urge" is the top pull in 2 of the last 3 self-checks, suggest a protection window around the usual sitting time. The user edits or declines, and nothing is created automatically. Resolves founder question 5 in decision 0017. |
| SHAN-6 | **Morning sequence, optional and decoupled** | Users dismiss the alarm and go back to bed (Alarmy reviews, Reddit) | After dismissal, an optional invitation to a 60-second sitting and/or to start the first window. Never starts automatically (MVP rule: dismissal never silently starts protection). Measure the take-up rate. |
| SHAN-7 | **Trust-first monetization spec (before payments)** | Billing/price is the #1 low-review theme in 12 of 16 sampled apps; donation models get the best sentiment | Principles: no card for a trial, cancel in one tap, reminder 3 days before renewal, keep anything already paid for forever, never charge for safety or alarm reliability. Test a supporter/donation tier against a premium tier in the pilot. |
| SHAN-8 | **Silent and minimal mode plus remembered settings** | Requests for timer/silence, "talks too much" and "smart defaults" complaints (Insight Timer, Balance, Medito) | One tap for minimal guidance. The last duration, guidance level and lock preference are remembered exactly; no "smart" overrides. Optional interval bell, which must not be breath-paced (PRACTICE_SAFETY). |

### 6.8 What to validate next
- Interview 10 P1 users who have relapsed with a blocker, and 10 P2 users who left a meditation app. Test whether the "one skill" framing makes sense to them.
- In the existing pilot worksheet, track the SHAN-2 take-up and the self-check completion rate. If fewer than half of the people who reach the block surface ever try the sitting, the bridge hypothesis is weak.
- Keep the family (P3) and India Hindi fit claims labelled as hypotheses until Hindi-complete pilots report.

---

## 7. Sources

**Academic:** [Baumel et al. 2019, JMIR](https://www.jmir.org/2019/9/e14567/) · [Grüning et al. 2023, PNAS (one sec)](https://www.pnas.org/doi/10.1073/pnas.2213114120) · [Biedermann et al. 2021, JCAL](https://onlinelibrary.wiley.com/doi/full/10.1111/jcal.12581) · [Lally et al. 2010, EJSP](https://doi.org/10.1002/ejsp.674) (median 66 days to habit automaticity, range 18–254) · [Killingsworth & Gilbert 2010, Science](https://www.science.org/doi/10.1126/science.1192439) · [Levinson et al. 2014, Frontiers](https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2014.01202/full) · [Britton et al. 2021](https://pmc.ncbi.nlm.nih.gov/articles/PMC8845498/) · [Healthy Minds RCT, JMIR MH 2020](https://mental.jmir.org/2020/11/e23825/) · [PLOS One smartwatch MBSR](https://journals.plos.org/plosone/article?id=10.1371%2Fjournal.pone.0322413)

**Market and company:** [Grand View Research](https://www.grandviewresearch.com/horizon/statistics/mental-health-apps-market/application/meditation-management/global) · [TBRC meditation apps](https://www.thebusinessresearchcompany.com/report/meditation-management-apps-global-market-report) · [TBRC mindfulness apps](https://www.thebusinessresearchcompany.com/report/mindfulness-meditation-application-global-market-report) · [Statista India](https://www.statista.com/outlook/hmo/digital-health/digital-fitness-well-being/health-wellness-coaching/meditation-apps/india) · [Grand View India spiritual wellness](https://www.grandviewresearch.com/horizon/outlook/spiritual-wellness-app-market/india) · [Business of Apps: Headspace](https://www.businessofapps.com/data/headspace-statistics/) · [Business of Apps: Calm](https://www.businessofapps.com/data/calm-statistics/) · [eMarketer](https://www.emarketer.com/content/headspace-cuts-13--of-its-workforce--transitions-staff-therapists-contract-part-time-roles) · [CNBC Calm](https://www.cnbc.com/2025/01/16/2-friends-spent-years-getting-turned-down-for-their-terrible-ideanow-the-calm-app-is-worth-2-billion.html) · [DelightRoom](https://delightroom.com/blog/en/daro-revealing-alarmy-ad-monetization-secrets) · [Speedinvest on Opal](https://www.speedinvest.com/knowledge/scaling-smart-how-opal-built-a-10m-arr-business-in-just-2-years) · [Headspace Ebb](https://www.headspace.com/articles/headspace-brings-empathetic-ai-companion-ebb-to-uk-members) · [BusinessWire Ebb voice](https://www.businesswire.com/news/home/20251208896917/en/Headspace-Rolls-out-Voice-Feature-for-Empathetic-AI-Companion-Ebb) · [Calm Sleep](https://blog.calm.com/blog/calm-sleep-press-release) · [Marketplace](https://www.marketplace.org/story/2025/11/12/the-growing-business-of-disconnecting) · [The Atlantic](https://www.theatlantic.com/health/archive/2023/06/meditation-app-mind-distraction/674500/) · [Times of India](https://timesofindia.indiatimes.com/technology/tech-news/sadhgurus-free-meditation-app-miracle-of-mind-beats-chatgpt-sees-1-million-plus-downloads-in-15-hours/articleshow/118674942.cms)

**Official pricing and feature pages:** [Opal](https://www.opal.so/pricing) · [Alarmy](https://alar.my/en) · [one sec FAQ](https://one-sec.app/faq/) · [ScreenZen](https://screenzen.co/) · [Freedom](https://freedom.to/premium) · [Forest](https://forestapp.cc/) · [Fabulous](https://thefabulous.co/) · [Sleep as Android](https://sleep.urbandroid.org/docs/general/plan.html) · [Insight Timer pricing](https://help.insighttimer.com/support/solutions/articles/67000664579-how-much-does-insight-timer-cost-) · [Waking Up](https://www.wakingup.com/?c=GetStarted) · [Medito](https://meditofoundation.org/free-meditation-app/) · [Balance](https://get.balanceapp.com/account/subscription/new) · [Healthy Minds](https://www.humin.org/wellbeing-tools/app)

**Google Play listings and review samples (retrieved 17 Oct 2026):** [Opal](https://play.google.com/store/apps/details?id=com.withopal.opal) · [Alarmy](https://play.google.com/store/apps/details?id=droom.sleepIfUCan) · [one sec](https://play.google.com/store/apps/details?id=wtf.riedel.onesec) · [ScreenZen](https://play.google.com/store/apps/details?id=com.screenzen) · [Freedom](https://play.google.com/store/apps/details?id=to.freedom.android2) · [Forest](https://play.google.com/store/apps/details?id=cc.forestapp) · [Fabulous](https://play.google.com/store/apps/details?id=co.thefabulous.app) · [Routinery](https://play.google.com/store/apps/details?id=com.alt.goodmorning) · [Sleep as Android](https://play.google.com/store/apps/details?id=com.urbandroid.sleep) · [Habitica](https://play.google.com/store/apps/details?id=com.habitrpg.android.habitica) · [Headspace](https://play.google.com/store/apps/details?id=com.getsomeheadspace.android) · [Calm](https://play.google.com/store/apps/details?id=com.calm.android) · [Insight Timer](https://play.google.com/store/apps/details?id=com.spotlightsix.zentimerlite2) · [Waking Up](https://play.google.com/store/apps/details?id=org.wakingup.android) · [Medito](https://play.google.com/store/apps/details?id=meditofoundation.medito) · [Balance](https://play.google.com/store/apps/details?id=com.elevatelabs.geonosis)

**Reddit (via search excerpts; direct fetch blocked):** [r/nosurf: Opal 2-week test](https://www.reddit.com/r/nosurf/comments/1r0hjaq/i_tested_opal_screen_time_app_for_two_weeks_heres/) · [r/nosurf: ScreenZen month](https://www.reddit.com/r/nosurf/comments/1r2zrvi/i_tried_screenzen_for_a_month_heres_what_nobody/) · [r/ProductivityApps: blockers fade](https://www.reddit.com/r/ProductivityApps/comments/1v314we/why_do_most_app_blockers_only_work_for_a_few_weeks/) · [r/getdisciplined: block screen problem](https://www.reddit.com/r/getdisciplined/comments/1vqqrpb/app_blockers_stopped_working_for_me_after_a_few/) · [r/GetOutOfBed: Alarmy](https://www.reddit.com/r/GetOutOfBed/comments/1b73g2r/even_alarmy_cant_wake_me_up/) · [r/Meditation: quit Calm/Headspace](https://www.reddit.com/r/Meditation/comments/1hmuqfq/has_anyone_here_tried_calm_or_headspace_for_a/) · [r/Meditation: Calm App Greed](https://www.reddit.com/r/Meditation/comments/1fyjgzv/calm_app_greed/)

*Raw data: `/workspace/shanivar-cs/src/reviews.json` (8,000 reviews), `themes.json`, `samples.txt`, and the scripts `reviews.py` and `themes.py`.*
