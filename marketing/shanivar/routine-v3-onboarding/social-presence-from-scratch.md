# Shanivar social presence: building it from scratch

Prepared 6 October 2026 for the Social Media Manager (`ai_org/agents/social_media_manager.md`). **Setup plan only.** Marketing is paused by founder instruction (6 October 2026, in this task; not yet recorded in a repo file). No account has been created, edited, archived or posted to. No handle has been checked live and no tool has been signed up for. Every step below that touches a live account needs founder approval first (`AGENTS.md:7`, `:37`).

Read with `routine-onboarding-marketing-plan.md` (gates and claims), `source/ui-copy-inventory.md` (allowed product language) and `zero-to-million-playbook.md` (growth path).

---

## 1. Starting point (what exists today)

| Item | Status | Evidence |
| --- | --- | --- |
| Instagram `@app.shanivar` | **Exists.** Professional/creator status, follower count and Insights access unknown to the repo | `marketing/shanivar/execution-2026-09-30/operations.md:36-40`; `marketing/shanivar/month-02/README.md:5` |
| Published posts | One known: BASE-01, founder-reported, https://www.instagram.com/p/DdMA9N7lAT3/. Creative, caption and metrics not in the repo | `marketing/shanivar/marketing-operating-system.md:60`; `marketing/shanivar/month-02/first-post-analysis.md` |
| Ready but unpublished | Three 4-slide carousels (STEADY, LAMP, CUPS) in charcoal/ivory/bronze; slots 30 Sep–5 Oct have passed | `marketing/shanivar/execution-2026-09-30/README.md`, `copy.md` |
| Publishing automation | n8n workflow inactive; no credentials connected; `automation/social/` not in this clone | `marketing/shanivar/marketing-operating-system.md:52`; `marketing/shanivar/README.md` (link target missing) |
| YouTube, X, LinkedIn, Threads, Facebook Page | **No account recorded anywhere in the repo** | repo-wide search, 6 Oct 2026 |
| Website | **shanivar.com, owned by the founder** (Hostinger, registered 2025-10-04, currently on parking nameservers; founder-confirmed). Site built by the website worker; waitlist on Netlify Forms with founder go-live approval (6 Oct 2026), but not reachable yet (no response at 06:21 CEST, 6 Oct) | `website/README.md:3-5`, `:83-85`; `website/index.html:260-279` (read only) |
| Brand container | "Shanivar · by The Hybrid Monk". The Hybrid Monk is also a clothing business and the two are kept separate | `ai_org/agents/social_media_manager.md:47`; `AGENTS.md:14-18` |
| Naming collision | `app.shanivar` is also the internal name of the Grok Bot collaborator | `AGENTS.md:3` |

So this is **from scratch in practice, not in fact.** We reset and reposition an existing account. We do not open a new one.

---

## 2. Positioning for the profile

**Founder positioning (his words, 6 Oct 2026):** *a very strict, high-velocity application for high-velocity people who want to move and track things, and who feel their life has been going off lately, framed as a calling, a moment to change your life.*

The profile turns this into **"high standards, honest tools, your actual life"**:

- **Who it's for:** high-velocity people who feel off-track (plan §4).
- **The calling:** "If the last few months have felt off, take the day back." Always an invitation, never a verdict.
- **Strict =** a coach that expects work (`android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md:14`). **Never** lock-in. Protection is "voluntary and stoppable — never unbreakable" (`android-app/app/src/main/java/com/shanivar/nativeapp/RoutineOnboardingFlow.kt:468`; `android-app/ENFORCEMENT_STANCE_2026-09-12.md:26-29`; `docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md:56-57`).
- **Never:** shame, "no excuses", hustle/grindset, 5am superiority, detox, "fix your life in 30 days", "track everything" (tracking is not built: `android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:154`; see `../wish-list.md`).
- **Open founder decision D1:** how far "strict, high-velocity" goes against the patient-teacher instruction (`SHANIVAR_STANCE.md:11`) and voice direction (`marketing/shanivar/voice-direction.md`). Profile copy below stays inside both.

---

## 3. Platforms

| Priority | Platform | Why | Status / decision |
| --- | --- | --- | --- |
| 1 | **Instagram** (`@app.shanivar`) | Already exists; the MOS and every campaign file are Instagram-first (`marketing/shanivar/marketing-operating-system.md`); 3-minute Reels and the taller grid (theverge.com/2025/1/18/24346567) suit explainers and typographic posts | Reset and reposition (§5). Founder approves any profile edit |
| 2 | **YouTube Shorts** | Same vertical files with no re-shoot; Shorts can run up to 3 minutes since 15 Oct 2024 (blog.youtube/news-and-events/tall-updates-coming-to-shorts/); Duolingo reported Shorts views +430% YoY in Q4 2024 (Duolingo Q4 FY2024 shareholder letter, investors.duolingo.com) | **No channel exists. Founder decision D4** (create, and which handle). Cross-post only originals, with disclosure fields completed (support.google.com/youtube/answer/14328491) |
| Later | WhatsApp (sharing, not broadcast) | India distribution; the backlog lists WhatsApp invites as Later (`docs/strategy/shanivar-launch-backlog.md`) | Not before Phase 2 testers |
| Hold | X / LinkedIn | Useful for build-in-public founder posts, but no account exists and capacity is one founder plus AI roles | Founder decision; not in this plan's cadence |
| No | Paid ads, TikTok (banned in India since June 2020, bbc.co.uk/news/technology-53225720) | No budget authorised (`marketing/shanivar/marketing-operating-system.md:34`) | — |

---

## 4. Handle

- **Keep `@app.shanivar`.** It already exists, holds BASE-01 and appears in every operations file. Changing it breaks existing links and ledgers.
- **Display name:** `Shanivar · by The Hybrid Monk` (house rule, `ai_org/agents/social_media_manager.md:47`).
- `@shanivar` (no prefix): **availability unverified**. Checking or claiming it is a founder action. If it is available and claimed later, use it on YouTube for consistency, or keep `@app.shanivar` everywhere. Founder decision.
- **Internal naming fix:** in briefs, write "Instagram @app.shanivar" for the account and "Grok Bot (app.shanivar)" for the collaborator (`AGENTS.md:3`).

---

## 5. Profile kit (draft, not applied)

**Profile image:** the Shanivar mark on V3 black `#0b0d0b` with ink `#f7f5ee` (`android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/style.css:3-5`). No Lord Shani imagery in the avatar; that framing is optional and founder-gated (`marketing/shanivar/audience-direction.md`).

**Bio (max 150 characters). Option A, recommended (V3 copy):**

```
Discipline is freedom.
Choose what loses access to you. Set it once.
Android app in development · early access ↓
```

**Option B (calling, founder positioning):**

```
Life been drifting? Take the day back.
High standards. Honest tools. Your actual life.
In development · join early access ↓
```

Both use only "in development" status and verbatim or near-verbatim lines from `RoutineOnboardingFlow.kt:274`, `:351`, `:364` and `RoutineScreen.kt:98`. Never write "Download", "Live" or "Available".

**Link in bio:** `https://shanivar.com/?utm_source=instagram&utm_medium=bio&utm_campaign=m0_waitlist`, a single link with no link-tree. Add it **only once** shanivar.com actually serves the site. The form backend (Netlify Forms) is already chosen (`website/README.md:83`). Until then, leave the bio link empty rather than sending people to a parked page.

**Funnel path:** bio → shanivar.com → waitlist → tester invite (M1/M2, `utm_campaign=m1_tester_invite` / `m2_tester_build`) → Google Play (M3, `m3_play_launch`). See plan §6.

**Category / contact buttons:** "App page" or "Software" category; no email or phone button until there is a monitored inbox (founder).

**Story Highlights (covers in V3 palette, sage or gold icon on black):**

| Highlight | Contents |
| --- | --- |
| Why | Manifesto: "Stop leaving your life to chance." (`RoutineOnboardingFlow.kt:275`) |
| Routine | "Two fixed points", "Your actual life", design previews labelled "in development" |
| Protection | "Choose what loses access to you", honest limits (browser and second phone not blocked) |
| Honest | Saved vs Active, "voluntary and stoppable — never unbreakable", AI-disclosure policy |
| Build log | Dated status posts (flag off, phone test pending, and so on), each re-checked on the day |
| FAQ | Android only; no iOS; no release date; no account required for scheduling (handoff Phase 1 rule); privacy summary from the website |

---

## 6. Audit of existing content (before relaunch)

1. Founder exports or screenshots every existing post with its Insights (where available) into the ledger. Repo agents cannot see the account.
2. Check each against: decision 0014 direction, the claims gates (plan §6), and AI-disclosure rules (`marketing/shanivar/marketing-operating-system.md:34`, `:88`).
3. Posts with astrology/scroller framing, availability claims or the old Focus-only pitch: **archive** (reversible), never delete. Archiving is a founder action.
4. BASE-01: keep unless the founder chooses to archive it. Record the decision in `marketing/shanivar/creative-learning-log.md`.
5. The three ready carousels (STEADY/LAMP/CUPS): keep as reserve. If used, re-render in the V3 palette with the existing `render.py` (`marketing/shanivar/execution-2026-09-30/README.md`) and re-check status lines.

---

## 7. First 9 posts (the grid a new visitor sees)

The 3-column grid shows newest first, so post 9 sits top-left. Three are **pinned** (Instagram allows three pinned posts).

| # | Format | Title / hook | Angle (plan §5) | Pinned |
| --- | --- | --- | --- | --- |
| 1 | Carousel, 5 slides | **"Stop leaving your life to chance."** Manifesto | A1 | ★ Pin 1 |
| 2 | Reel 20–30s, typographic | "No 5am. Two fixed points." | A2 | |
| 3 | Carousel | **"Choose what loses access to you."** Includes "what Shanivar can't block" honesty slide | A3 + A4 | ★ Pin 2 |
| 4 | Reel | "Strict with you. Honest about itself." Saved is not Active | A4 | |
| 5 | Carousel | "Decide once at 9am, not forty times by 11pm." | A5 | |
| 6 | Reel, founder phone footage (no face required) | "If the last few months have felt off…" Calling | A1 + positioning | |
| 7 | Carousel | **"Build log: where Shanivar actually is today"**: written, flag off, phone test next, no release date | Status | ★ Pin 3 |
| 8 | Reel | "Shanivar is not here to keep you busy." Need fewer prompts | A6 | |
| 9 | Carousel | "Not discipline theatre": what removing a decision looks like on paper (no app UI claims) | A5 | |

Visual system: V3 black background with cream-paper cards (`#f2efe6` paper, `#111510` ink), sage `#d8e5b7` and gold `#c9aa73` accents, large editorial headings (`android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:62`; wireframe `style.css:3-5`). Use 4:5 feed images, set to read correctly in the 3:4 grid preview (taller grid, theverge.com/2025/1/17/24346304). Every app screen shown is labelled "DESIGN IN DEVELOPMENT — NOT RELEASED".

---

## 8. Content pillars

| Pillar | Share | Purpose | Example |
| --- | --- | --- | --- |
| Your actual life | 30% | Anti-ideal-routine, two fixed points | A2 |
| Choose what loses access | 25% | Agency over attention, honest limits | A3, A4 |
| Decide once | 20% | Removing decisions, "not discipline theatre" | A5 |
| Build in public | 15% | Exact dated status, tests, what failed | Build log |
| Practice (Sadhana/Steady) | 10% | Patient observational voice; "in development" | Existing STEADY carousel, re-rendered |

---

## 9. Cadence and workflow

- **Target:** 3 feed posts a week (2 Reels + 1 carousel) once rendering works. **Floor:** 2 carousels a week from the local renderer (`marketing/shanivar/marketing-operating-system.md:32`). Optional 2–3 Stories (polls and questions).
- Proposed slot: 19:30 Asia/Kolkata (planning convention, not a researched optimum).
- Buffer, deadlines and ledger as in the MOS (`marketing-operating-system.md:38`, `:44-56`).
- Use Trial Reels (test a Reel with non-followers first; launched 10 Dec 2024 for eligible creators, https://about.fb.com/news/2024/12/trial-reels-try-content-non-followers-first-see-what-perfoms-best/) for hook tests once the account qualifies. Eligibility for `@app.shanivar` is unknown.
- Originality: post original edits; no reposted or watermarked clips (creators.instagram.com/blog/recommendations-and-originality).

---

## 10. Community rules

- Reply as Shanivar, warm and direct, never scolding (`android-app/ENFORCEMENT_STANCE_2026-09-12.md`, redirect design note).
- **No** "comment YES for the link", engagement bait, auto-DMs or giveaways (`marketing/shanivar/engagement-reels-playbook.md`; ST-008).
- Never ask for birth data, health details or screen-time screenshots in public (`marketing/shanivar/audience-research.md`).
- If someone describes self-harm, crisis or a medical issue: a short caring reply pointing to local emergency help, then escalate to the founder. No coaching in comments.
- No testimonials, no "users say", no follower-count claims until real and consented (`marketing/shanivar/marketing-operating-system.md:34`).
- Never argue enforcement edge cases in public. Link the honest-limits Highlight.

---

## 11. Disclosure

- AI UGC is illustrative, never testimony. Use "AI-generated presenter | Fictional scene" on screen when used (`marketing/shanivar/ugc-pilot-2026-09-30/briefs.md`) plus the platform AI label where required (about.fb.com/news/2024/04/metas-approach-to-labeling-ai-generated-content-and-manipulated-media/; support.google.com/youtube/answer/14328491).
- Record model/tool, prompts, rights and disclosure per asset (`marketing/shanivar/marketing-operating-system.md:88`; `chatgpt-subscription-assessment.md`).
- Paid partnerships: none authorised. If ever used, the platform's paid-partnership label is mandatory.

---

## 12. Launch checklist for the account reset (all founder-approved, none done)

- [ ] D6: founder unpauses marketing and records it in a repo file
- [ ] Founder confirms D1 (voice) and D4 (YouTube)
- [ ] Export and audit existing posts (§6); archive decisions recorded
- [ ] Apply profile image, display name, bio (A or B), category
- [ ] Confirm shanivar.com serves the site and a test sign-up appears in Netlify Forms with its `source` value, then add the bio link with UTM
- [ ] Create 6 Highlight covers; seed "Why", "Honest" and "Build log"
- [ ] Produce posts 1–9 as complete packages (ready buffer)
- [ ] Publish per the ledger; pin 1, 3, 7
- [ ] Weekly scorecard (`marketing/shanivar/marketing-operating-system.md:62`) plus waitlist metrics (plan §6)

## Sources

Repo: files cited inline above. Web: theverge.com/2025/1/18/24346567 · theverge.com/2025/1/17/24346304 · blog.youtube/news-and-events/tall-updates-coming-to-shorts/ · investors.duolingo.com (Q4 FY2024 shareholder letter) · creators.instagram.com/blog/recommendations-and-originality · about.fb.com/news/2024/12/trial-reels-try-content-non-followers-first-see-what-perfoms-best/ · bbc.co.uk/news/technology-53225720 (India app ban, 2020) · about.fb.com/news/2024/04/metas-approach-to-labeling-ai-generated-content-and-manipulated-media/ · support.google.com/youtube/answer/14328491.
