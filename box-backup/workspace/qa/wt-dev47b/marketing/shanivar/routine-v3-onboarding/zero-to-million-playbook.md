# Shanivar: zero-to-a-million playbook

Prepared 6 October 2026 for the Social Media Manager and the founder. **Strategy only.** Marketing is paused (founder instruction, 6 October 2026, given in this task). Nothing is published, scheduled, paid for or switched on. Budgets are founder decisions; none is authorised (`marketing/shanivar/marketing-operating-system.md:34`; `AGENTS.md:37`).

"A million" is a direction, not a forecast. Nothing in the repo supports a user-number projection. The roadmap below is built from **gates** (what must be true) rather than dates.

Read with `routine-onboarding-marketing-plan.md` (claims gates), `social-presence-from-scratch.md` (account setup) and `../wish-list.md` (wishes that marketing must never present as features).

---

## §A. Ground rules and positioning

**Founder positioning (his words, 6 Oct 2026):** Shanivar is *a very strict, high-velocity application for high-velocity people who want to move and track things, and who feel their life has been going off lately*. It is *framed as a calling, a moment to change your life.*

- **Audience:** high-velocity people who feel off-track. India-first, English, 18+ (`ai_org/agents/market_fit_release_steward.md:17`; `marketing/shanivar/audience-research.md:36`).
- **The calling as a growth hook:** "the moment you decide to take the day back". It works for launch moments (new month, new year, exam season, a new job) because the user chooses the moment. We never manufacture one with fake urgency.
- **Strict = high standards and a coach that expects work** (`android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md:14`, `:46-64`). **Never lock-in:** "Protection is voluntary and stoppable — never unbreakable" (`android-app/app/src/main/java/com/shanivar/nativeapp/RoutineOnboardingFlow.kt:468`); "Impossible… must never appear in… marketing" (`android-app/ENFORCEMENT_STANCE_2026-09-12.md:26-29`); decision 0014 preserves "honest enforcement limitations" (`docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md:57`).
- **No shame, no productivity-bro, no streak guilt.** Decision 0009 shows factual attempts "without streaks, ranks or scores" (`docs/decisions/0009-behavioral-return-loop.md:15`). ST-008 holds that less reliance on Shanivar can be success (`SHANIVAR_STANCE.md`; `docs/decisions/0014-…md:60`).
- **"Track things" is a wish, not a feature** (`android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:154`; `../wish-list.md`).
- **Open founder decision D1:** "strict, high-velocity" vs the patient-teacher and "notice, don't control" stance (`SHANIVAR_STANCE.md:11`; `marketing/shanivar/voice-direction.md`). This playbook does not resolve it.

**Growth-metric rule (ST-008):** no DAU, session time or streak counts as success metrics. We count waitlist sign-ups, testers activated, plans saved, Ready reached, confirmed usefulness (self-report), voluntary return and "needed fewer prompts". DAU may be *observed* for capacity planning only.

**Founder decision, define "user":** for this roadmap, proposed as *a person who completed onboarding and saved a day plan on their own phone*. Installs alone don't count.

---

## §B. What others did: case studies and what to steal

Confidence: **V** = verified at a primary or reputable source; **S** = secondary source only; **U** = unverified (treat as anecdote).

| Company | What they did | Key figures | Steal | Avoid | Sources |
|---|---|---|---|---|---|
| **Opal** (screen time) | Creator-led educational content, including its own creator account; moved core to free | >1M DAU; paid conversion fell from ~20% to ~9% after going freemium; students ≈ two-thirds of DAU; ≈$10M ARR (S, founder interview). Creator account 0 → ~700K followers (S) | **Educational, creator-style channel; free core** | Subscription pressure; "cult" framing | revenuecat.com/blog/growth/kenneth-schlenker-sub-club-podcast-2026 · subclub.com/episode/why-opal-stopped-chasing-revenue-kenneth-schlenker-opal · gtmsauce.com · linkinbio.news |
| **one sec** (friction) | Research-backed friction before opening apps; published a peer-reviewed study | PNAS 2023: 57% fewer app openings after 6 weeks; 280 of 719 participants completed; the option to dismiss was part of what worked (V). User numbers U | **Lead with honest research; keep a dismiss option.** This mirrors "stoppable" | Overclaiming study results | pnas.org/doi/10.1073/pnas.2213114120 |
| **Forest** | A visible, shareable artefact (a tree) per focus session; real tree planting | 60M+ downloads since 2014; 2M+ real trees (V, company site) | **Shareable, concrete artefact of time kept** | "Plant Together" (group tree dies), a loss/shame mechanic | forestapp.cc |
| **Finch** | Product built for shareable screenshots; gentle tone | "Several million downloads" (S); 10M+ Play installs U | **Gentle tone that makes sharing safe** | Pet-guilt dynamics | howtodistribute.com/finch |
| **Calm** | Launched DoNothingFor2Minutes.com, a one-idea interactive page | 1M unique visitors in 6 days, Jan 2011 (V); ~100k emails (U) | **One-idea web experience plus email capture before the app** | — | techcrunch.com/2011/01/26/donothingfor2minutes-com-goes-from-zero-to-1-million-unique-visitors-in-6-days/ |
| **Duolingo** | Consistent platform-native character voice on short video | Q4 2024 DAU 40.5M (+51%), MAU 116.7M; Shorts views +430%, Reels +450% YoY (V) | **One consistent, platform-native voice; post natively per platform** | Streak guilt / owl threats | investors.duolingo.com (Q4 FY2024 shareholder letter; SEC mirror) |
| **BeReal** | Paid campus ambassadors | DAU ~15M (Oct 2022) → <6M (Mar 2023) (V, via NYT/Apptopia) | Campus seeding, but **unpaid** and only with a product that retains | Hype without a retention loop | businessinsider.com/new-social-media-app-bereal-used-campus-ambassadors-to-grow-2022-4 · theverge.com/2023/4/26/23698859 |
| **Superhuman** | Qualifying waitlist survey; founder onboarding calls; PMF "very disappointed" survey | ~180k waitlist (S) | **Qualified waitlist, founder onboarding, PMF survey (≥40% "very disappointed")** | Gatekeeping as a status play | review.firstround.com/how-superhuman-built-an-engine-to-find-product-market-fit/ · waitlister.me |
| **Notion** | Unpaid ambassadors; template gallery made users into distributors | ~1M users Sept 2019 (S) | **Ambassadors and shareable templates** (Shanivar: shareable routine card, a wish) | — | distribution-agents.com/research/notion |
| **Clubhouse** | Invite-only scarcity | Downloads 3.5M → 8.1M, 1–16 Feb 2021; installs 9.6M Feb → ~922k Apr 2021 (V) | — | **Fake scarcity; growth without a reason to return** | bbc.co.uk/news/technology-56127226 · businessinsider.com/clubhouse-downloads-900000-in-april-2021-5 |
| **Kuku FM** (India) | Vernacular audio for Tier-2+ India | 10M+ paid subscribers by Oct 2025; 70–80% Tier 2+ (V/S) | **Vernacular and Bharat later** (Hindi blocked by DEV-13 today) | — | techcrunch.com/2025/10/15/indias-kuku-snags-85m-as-mobile-content-wars-intensify/ · inc42.com |
| **Pratilipi** (India) | Regional-language content | 10M → 20M MAU in 5 months of 2020 (S) | Regional language as growth lever | — | inc42.com/startups/the-secret-behind-pratilipi-gaining-20-mn-maus-in-five-months/ |
| **ShareChat** (India) | WhatsApp-native sharing | ~300–350k daily users (2016) → ~7.5M DAU (2018) (S) | **WhatsApp sharing in India** (backlog Later) | — | entrackr.com/2018/10/sharechats-growth-story-bharat/ · lsvp.com/stories/sharechat-and-the-whatsapp-economy/ |
| **Habitica** | Kickstarter community funding | $41,191 from 2,817 backers (V); >1M users 2016 (U) | Community as co-builders | **RPG HP-loss mechanics** (conflicts with 0009/ST-005) | kickstarter.com/projects/lefnire/habitrpg-mobile |
| **Cult.fit** (India) | Offline-first community | ~10k members 2017 → 200+ centres (S) | Local in-person community, much later | — | qz.com/india/1043219 · blog.cult.fit |
| **Headspace** | Founder-voice content | ~1M users 2014 (U) | — | — | fastcompany.com/3041402 (secondary; not verified) |

Not researched for this playbook: Lenskart, Sarvam.

**Top 5 patterns to steal (ranked for Shanivar):**

1. **Opal:** a creator-style educational channel about attention, with the free core product as the destination.
2. **Calm:** a one-idea interactive web moment at shanivar.com ("decide once") plus waitlist capture, before the app is public. Building it is a wish for the website worker; it is not built.
3. **Superhuman:** a qualifying waitlist (the form already asks for phone model; one "what do you want back?" question would need the website worker to add a field), founder-led onboarding of the first testers, and the PMF survey.
4. **Notion:** unpaid ambassadors plus a shareable routine card/template once the product exists (wish).
5. **Duolingo:** one consistent, platform-native voice, using the V3 coach voice within D1.

Also usable: **one sec** (cite real research honestly; keep a dismiss/stop option, which is already the stance) and **ShareChat/Kuku** (vernacular, WhatsApp, Bharat once Hindi exists).

---

## §C. Reel trends, platform changes and formats

| Item | Date | Implication | Source |
|---|---|---|---|
| Instagram Reels up to 3 minutes; taller 3:4 grid | Jan 2025 | Room for honest explainers; design feed images for the 3:4 crop | theverge.com/2025/1/18/24346567 · threads.com/@mosseri/post/DE-efFqyStv · theverge.com/2025/1/17/24346304 |
| Originality ranking: reposts replaced with originals; accounts with 10+ unoriginal posts in 30 days excluded from recommendations | Apr 2024 | Only original edits; no aggregator reposts | creators.instagram.com/blog/recommendations-and-originality · theverge.com/2024/4/30/24144571 |
| Meta: 75% of US recommendations from original posts | Jan 2026 | Same | `marketing/shanivar/engagement-reels-playbook.md` |
| Trial Reels (test with non-followers) | Dec 2024; wider access reported July 2025 | Hook A/B tests without hurting the grid | about.fb.com/news/2024/12/trial-reels-try-content-non-followers-first-see-what-perfoms-best/ · thekeyword.co (S) |
| Edits app (Instagram's free editor) | 22 Apr 2025 | Free editing; no new paid tool | creators.instagram.com/blog/edits-video-creation-app |
| YouTube Shorts up to 3 minutes | from 15 Oct 2024 | Cross-post the same originals | blog.youtube/news-and-events/tall-updates-coming-to-shorts/ |
| Screen-time "yearly reveal" posts | Dec 2025 | Non-shaming reflection hook at year end | ypulse.com/newsfeed/2025/12/31/ (S) |
| Dopamine-detox backlash (experts call it unscientific) | 2025 | Never say "detox"; agency framing instead | health.clevelandclinic.org/dopamine-detox · english.elpais.com/science-tech/2025-08-10/ |
| "That Girl" / 5am / romanticise backlash | 2025 | "Your actual life" counter-position | mdpi.com/2076-0760/14/8/450 |
| AI-content labelling (Meta, YouTube) | Apr 2024 → | Disclose AI presenters | about.fb.com/news/2024/04/metas-approach-to-labeling-ai-generated-content-and-manipulated-media/ · support.google.com/youtube/answer/14328491 |
| "I tried X for 30 days", faceless voiceover, build-in-public | ongoing | Low-confidence (S, blogs only) | — |
| Duet/stitch/remix effectiveness | — | Not verified | — |

**Top 5 reel formats for Shanivar:**

1. **Anti-5am "your actual life"**: "No 5am. Two fixed points." A text-led contrast between the ideal routine and a real day (A2; rides the backlash, mdpi.com/2076-0760/14/8/450).
2. **Non-shaming screen-time reflection**: "You already know which apps take more than they give." No shame, no detox; agency (A3; year-end reveal moment, ypulse).
3. **Typographic POV / faceless text-overlay**: "POV: you decided at 9am so 11pm-you doesn't have to." Cheap to make with the local renderer or Edits (A5).
4. **Honest founder build log**: phone footage of the actual build and what is and isn't done ("Routine V3 is written; the phone test hasn't run yet"). The strongest trust signal (A4; one sec research-honesty pattern).
5. **3-minute honest explainer** (after Phase 1): how protection works and what it can't block (browser, second phone), filmed on a tested device. Uses the 3-minute length and the originality signals.

Hook rule: the first line states a contradiction (V3 pattern "truth → contradiction → instruction → action", wireframe README:56-58), never an insult.

---

## §D. Kickoff paths

| Path | What | Cost | Fit | Risk |
|---|---|---|---|---|
| **K1** Honest build-in-public log | Dated status, tests, failures | Free | High: true today; matches the honesty stance | Slow; must stay exact |
| **K2** Manifesto + waitlist (Calm-style idea-first) | Manifesto posts → shanivar.com waitlist | Free (Netlify Forms free tier, `website/README.md:3`) | High: needs no product claims | Waitlist without a date can go cold |
| **K3** Creator-led educational account (Opal) | Faceless or founder creator account on attention | Time or paid creators | Medium now, high at 10k+ | Spend means founder decision |
| **K4** Campus/volunteer ambassadors (BeReal/Notion) | Students seed in colleges | Free if unpaid | Medium after a retaining product exists | BeReal decay without retention |
| **K5** Superhuman-style qualified waitlist + founder onboarding | One qualifying question; founder onboards testers | Founder time | High at M1/M2 | Founder capacity |
| **K6** Paid ads | Meta/YouTube ads | Money | Low before PMF | **Not authorised** |

**Chosen sequence:** **K1 + K2 at M0** → **K5 at M1/M2** → **K4 (unpaid, Notion-style) at M3 / 1k** → **K3 at 10k+ if funded**. K6 only after PMF and a founder budget.

---

## §E. Milestone roadmap

Funnel throughout: Instagram bio (and YouTube once D4 is approved) → shanivar.com → waitlist → tester invite (M1/M2) → Google Play (M3). UTM pattern `utm_source=<platform>&utm_medium=<placement>&utm_campaign=m<n>_<goal>` (`website/README.md:81`).

| Stage | Entry gate | Goals | Channels | Content mix | Experiments | Metrics (ST-008-safe) | Budget | Owner | Exit criteria |
|---|---|---|---|---|---|---|---|---|---|
| **M0** Waitlist | Founder unpauses (D6); shanivar.com serving the site (Netlify Forms backend already chosen, `website/README.md:83`) | First real waitlist; validate hooks | Instagram; shanivar.com | 2 Reels + 1 carousel/week (floor 2 carousels); build log weekly | Hook A/B with Trial Reels (if eligible); bio Option A vs B | Sign-ups by UTM `source`; sign-up conversion (once host analytics exist); phone-model mix | ₹0 | Social Media Manager; Web Developer | DEV-7 Pass **and** a waitlist large enough to recruit the first testers (size: founder decision) |
| **M1** Founder test public | DEV-7 signed off Pass, device recorded | Prove honesty in public; first tester invites | + waitlist email | Real device footage; "what passed/failed" | Invite-copy test | Invite opt-in rate; unsubscribes | ₹0 | Founder; SMM | Flag-on tester build ready |
| **M2** Invited testers | Committed flag-on build; Firebase App Distribution set up (`android-app/DEVICE_DISTRIBUTION.md`) | Find PMF signal | + Firebase invites; founder onboarding calls (K5) | Tester stories (consented); honest limits | PMF survey; onboarding-copy tests | invited → installed → onboarding done → plan saved → Ready; confirmed usefulness; PMF "very disappointed" % | ₹0 | Founder; Market Fit Release Steward | DEV-17 closed; usefulness signal judged sufficient by the founder |
| **M3** Play release | Play listing live (DEV-17) | Convert waitlist; first public users | + Play listing; YouTube Shorts (if D4) | Launch; 3-min explainers; FAQ | Store-listing copy tests | Waitlist → install; "user" (onboarded + plan saved); 7/28-day confirmed usefulness | Founder decision | SMM; Steward | 1k users |
| **1k** | 1k users | Retention truth; first community | + unpaid ambassadors (K4) | Ambassador-made originals; templates (if built) | Ambassador pilot at 2–3 colleges | Voluntary return; usefulness; "fewer prompts" | Founder decision | SMM | Clear retained-usefulness signal |
| **10k** | 10k users | Scalable content engine | + creator-led educational account (K3) if funded; WhatsApp sharing | Creator collabs (disclosed); educational series | Creator pilots; Hindi editorial once DEV-13 is done | Same, by cohort and source | Founder decision | SMM; Creative Writer | Repeatable acquisition at acceptable cost |
| **100k** | 100k users | Bharat / vernacular expansion | + regional languages; possibly paid (founder) | Vernacular originals (Kuku/Pratilipi pattern) | Language-market tests | Usefulness by language/cohort | Founder decision | Team | Sustainable unit economics (founder) |
| **1M** | 1M users | Category position: the honest attention app | All of the above | Community-led | — | Same; no DAU vanity | Founder decision | Team | — |

---

## §F. First 30 days after unpause: checklist for the Social Media Manager

1. Record the unpause and the D1/D4/D5 decisions in the creative learning log (`marketing/shanivar/creative-learning-log.md`).
2. Audit existing posts (`social-presence-from-scratch.md` §6) and record archive decisions.
3. Apply the profile kit once approved; add the UTM bio link only after shanivar.com serves the site.
4. Re-render the reserve carousels in the V3 palette if used; re-check status lines.
5. Build three complete packages for the ready buffer (MOS).
6. Publish posts 1–9 (`social-presence-from-scratch.md` §7); pin 1, 3, 7.
7. Run one Trial Reels hook test per week if eligible.
8. Weekly scorecard (MOS line 62) plus waitlist metrics.
9. Re-verify build status every Monday (flag, DEV-7, DEV-17) before any status post.
10. End of day 30: write a learning-log entry with raw numbers; propose the next month.

---

## §G. Banned claims by gate

| Until… | Never say |
|---|---|
| Always | "impossible to bypass", "unbreakable", "can't escape", "no excuses", "detox", "fix your life", clinical/mental-health benefits, "cortisol", "dosha", user counts we haven't measured, testimonials not real and consented, "streaks", "level up" |
| DEV-7 Pass | "works", "tested", "reliable", "Active", "blocks [app]" as fact |
| Flag on + tester build | "try it", "get early access build" |
| Play listing live | "download", "available", "on Google Play", any release date |
| DEV-16 decided and built | Sadhana progression, chapters, unlocks |
| Tracking built (wish) | "track your habits/progress/completion" |
| iOS exists | "iPhone", "iOS" |

---

## §H. Risks

- Positioning drift into hustle/shame (guardrails §A; D1).
- Waitlist without a date going cold: monthly honest build-log email, no fake dates.
- Clubhouse-style scarcity backlash: no fake invite scarcity; invites gated only by real tester capacity.
- Play policy on exact alarms and full-screen intents (DEV-17) could reshape the alarm story.
- Measurement blind spots: no analytics or crash reporting (DEV-14); surveys until built.
- Founder capacity: K5 onboarding calls don't scale. Cap the tester cohort.

## §I. Founder decisions this playbook needs

D1 voice · D4 YouTube · D5 deployment of shanivar.com, privacy-notice legal review, invite-email tool · D6 unpause · D7 any spend · definition of "user" · waitlist size that triggers tester invites · ambassador programme (unpaid) at 1k.

## Sources

Repo files cited inline. Web: all URLs in §B and §C tables. Confidence markers V/S/U as stated; S and U figures should not be quoted publicly.
