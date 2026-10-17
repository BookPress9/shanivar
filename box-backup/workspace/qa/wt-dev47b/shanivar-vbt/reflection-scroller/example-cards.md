# Example Cards — Messages from Shanivar

Illustrative bank of cards for Agent 3 (Generation Service) prompt-tuning, Codex UI fixtures, and QA review against the shipping acceptance criteria. **None of these are real chart readings.** Every "supplied computed facts" line in the personalized cards is fictional/illustrative — the kind of structured fact a real context snapshot would contain per the handoff doc, not an actual calculation. Mechanism notes reference sections of [`psychology-research.md`](./psychology-research.md). Structure and word counts follow [`card-writing-guide.md`](./card-writing-guide.md); option design follows [`response-option-design.md`](./response-option-design.md).

Audience segments per [`marketing/shanivar/audience-direction.md`](../../marketing/shanivar/audience-direction.md): Gen Z, Millennials, younger adults connecting with parents, parents/older adults, cross-life-stage.

---

## General mode (no chart data required)

### G1 — Gen Z, attention/comparison

**Sender-observation + question:** "You opened this app for one specific reason a minute ago. A lot of people lose track of that reason the moment a feed starts moving underneath their thumb. Before you scroll anywhere else today, what were you actually hoping to find, or change, in this exact moment?"

**Options:** "I remember" · "Not sure anymore" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Honest information gap (A2) — the gap is between the user and their own forgotten intention, not content the app is withholding; answering it is inherently useful to the user, independent of any return-to-app benefit.

---

### G2 — Millennial, identity beyond productivity

**Sender-observation + question:** "Most of what fills a day for you probably has someone else's name on it — a job, a family, a list. That's not a complaint, just a pattern worth noticing. Is there anything today that was just for you, even for two minutes?"

**Options:** "Yes, a little" · "No, not today" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Relatedness without personal claim (A1) — speaks to a shared adult experience ("a lot of people," implicit) without asserting private knowledge of this user's day, which general mode has no basis for.

---

### G3 — Connecting with parents

**Sender-observation + question:** "A short, present conversation often lands better with someone than a longer, distracted one — most people notice the difference without being told. If you talked to a parent or older relative this week, was your attention actually with them the whole time, or partly somewhere else?"

**Options:** "Mostly with them" · "Partly elsewhere" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Competence-building self-observation (A1) — the question builds the user's own noticing skill about presence; no guilt framing is applied regardless of the answer, per `audience-direction.md`'s explicit "avoid guilt or pressure" rule for this pillar.

---

### G4 — Parents / older adults, growing older with awareness

**Sender-observation + question:** "Priorities tend to quietly shift over years without announcing themselves — you usually only notice in hindsight, if you notice at all. Taking a moment now, what matters to you today that didn't feel important five years ago, and what changed?"

**Options:** "Something's changed" · "Not really" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Expressive-reflection value (A3) — the reward is in the act of articulating a real shift, not in any app-side payoff; matches `audience-direction.md`'s "let both generations answer for themselves, privately if preferred."

---

### G5 — Cross-life-stage, digital noise

**Sender-observation + question:** "Somewhere in the last hour, an app probably asked for your attention before you'd actually decided to give it — a notification, a habit, a reflex toward the screen. Did you notice the moment it happened, or only afterward, once you were already scrolling?"

**Options:** "Noticed it happening" · "Only afterward" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Autonomy-supportive framing (A1) — invites noticing rather than instructing a behavior change; no shame attached to "only afterward," which is the more common and non-penalized answer.

---

### G6 — Gen Z, identity/comparison

**Sender-observation + question:** "Deciding who to become is easier to do quietly than in view of everyone else's timeline, where the pressure is to already look finished. If no one could see your answer, what would you actually want to be better at this year, just for yourself?"

**Options:** "I know exactly" · "Still figuring it out" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Autonomy + honest curiosity gap (A1/A2) — the "if no one could see" framing explicitly removes comparison pressure rather than exploiting it, directly serving the audience-direction pillar "before you decide who to become, notice what matters to you today."

---

## Personalized mode (illustrative supplied chart/transit facts)

Each card states its fictional "supplied computed facts" as a real generation service would receive them — natal placement(s), the natal-to-transit aspect (computed in code, per the handoff doc, never claimed by the model), and mode. Treat every fact below as illustrative only.

### P1 — Gen Z, attention/identity

**Supplied computed facts:** Natal Mercury in Gemini, 3rd house. Current transit: Uranus square natal Mercury (orb 1.5°). Mode: `personalized`.

**Sender-observation + question:** "Your natal Mercury sits in a sign that likes fast, varied input — and right now a moving Uranus is squaring it, which for a lot of charts shows up as extra restlessness in how you take in information. Today, did your attention feel more scattered than usual, or about the same?"

**Options:** "More scattered" · "About the same" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Real personalization via a genuinely computed aspect (A5), phrased as "for a lot of charts" and "one way to read this" rather than asserted fact about this user's inner state — avoids Barnum drift by staying tied to the specific supplied aspect rather than a universally-true statement.

---

### P2 — Gen Z, comparison/pressure

**Supplied computed facts:** Natal Venus in Capricorn, 11th house. No current major transit to Venus. Mode: `personalized`.

**Sender-observation + question:** "Venus in Capricorn in your chart often points toward valuing steady, earned recognition over quick approval — the slow kind, not the instant kind. When you compare yourself to people online, is it usually their achievements you notice first, or something quieter, like how settled they seem?"

**Options:** "Their achievements" · "Something else" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Competence-oriented self-inquiry (A1) grounded in a real supplied placement rather than a generic "you compare yourself to others" line that would apply to any chart — the specificity is what keeps this out of Barnum territory.

---

### P3 — Millennial, burnout/responsibility

**Supplied computed facts:** Natal Saturn in Virgo, 6th house (natal). Current transit: Saturn (transiting) conjunct natal Sun (orb 2°). Mode: `personalized`.

**Sender-observation + question:** "Transiting Saturn is currently sitting close to your natal Sun — for a lot of people that period asks something like 'what am I actually responsible for, versus what I've just taken on.' Does that distinction feel relevant to your week?"

**Options:** "Very relevant" · "Not really" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Honest curiosity gap tied to real transit data (A2/A5) — the gap is between a named astrological pattern and the user's own assessment of their week; the card explicitly invites the user to be the judge ("does that feel relevant") rather than asserting it.

---

### P4 — Millennial, identity beyond productivity

**Supplied computed facts:** Natal Moon in Taurus, 2nd house. Current transit: Jupiter trine natal Moon (orb 3°). Mode: `personalized`.

**Sender-observation + question:** "Moon in Taurus in your chart tends to find real comfort in simple, physical things — food, rest, a familiar space. With Jupiter currently trine that Moon, this can be a good stretch for actually letting yourself enjoy those, not just scheduling them in. Have you had a moment like that recently?"

**Options:** "Yes, recently" · "Not really" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Positive-valence personalization (A5) — deliberately not every personalized card should probe a difficulty; an easy, affirming transit read still requires the user's own confirmation rather than assuming the answer, keeping autonomy intact even on a "good news" card.

---

### P5 — Connecting with parents (adult child's own chart)

**Supplied computed facts:** Natal Moon in Cancer, 4th house. Current transit: Chiron conjunct natal Moon (orb 2°). Mode: `personalized`.

**Sender-observation + question:** "Your natal Moon in Cancer sits close to home and family themes, and it's currently meeting a slow-moving Chiron transit — a period that can bring up older, tender feelings about family more than usual. If you called a parent this week, is there anything you'd actually want to ask them, beyond the usual check-in?"

**Options:** "Yes, something specific" · "Not really" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Relatedness-serving prompt toward real connection (A1), not guilt-based ("you should call more") — offers an opening without implying the user has been failing to connect, matching `audience-direction.md`'s explicit "avoid guilt or pressure" instruction for this pillar.

---

### P6 — Connecting with parents (framed for the older-generation user)

**Supplied computed facts:** Natal Sun in Leo, 5th house. Current transit: Venus trine natal Sun (orb 1°). Mode: `personalized`.

**Sender-observation + question:** "Sun in Leo in the 5th house often lights up around being genuinely seen by the people closest to you, not just dutifully checked on. With Venus currently trine your Sun, warmth tends to land easily right now. Is there something you'd like a son, daughter, or younger relative to actually know about how you're doing — not just 'fine'?"

**Options:** "Yes, there is" · "Not particularly" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Autonomy-respecting framing for the older-adult participant (A1) — `audience-direction.md` states parents are "participants with their own preferences, not people their children must fix"; this card addresses the older adult directly as the one with agency, not as a subject of a younger relative's concern.

---

### P7 — Parents / older adults, changing roles

**Supplied computed facts:** Natal Saturn in Cancer, 10th house. Current transit: Pluto sextile natal Saturn (orb 2.5°). Mode: `personalized`.

**Sender-observation + question:** "Saturn in Cancer in your 10th house often ties a sense of purpose to caretaking roles — for family or work. With a slow Pluto transit currently supporting that placement, this can be a natural time to notice which parts of that role you'd choose again, and which you've simply kept out of habit. Which feels closer to true right now?"

**Options:** "I'd choose it again" · "More out of habit" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Competence and autonomy (A1) — invites active re-evaluation of a long-held role without implying the role was wrong; "which feels closer to true" keeps the user as the sole authority on the answer.

---

### P8 — Older adults, legacy/reflection

**Supplied computed facts:** Natal Jupiter in Sagittarius, 9th house. No current major transit to Jupiter. Mode: `personalized`.

**Sender-observation + question:** "Jupiter in Sagittarius in the 9th often points to meaning built through what you've learned and passed on, more than what you've accumulated. If someone younger asked what you'd learned that actually mattered, is there something that comes to mind right away?"

**Options:** "Yes, right away" · "Nothing comes to mind" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Narrative-transportation-supported relevance (A5) grounded in a real placement, aimed at genuine legacy reflection rather than nostalgia-baiting; no claim that the user *has* wisdom to share, only an open invitation to check.

---

### P9 — Gen Z, disagreement-branch example (shows direction-change after "Not today")

**Supplied computed facts:** Natal Mars in Aries, 1st house. Current transit: Mars (transiting) square natal Mars (orb 2°). **Context:** this card follows a prior card (about impulsiveness/reactivity) that the user answered "Not today" to.

**Sender-observation + question:** "Let's set that aside and try a different angle instead. Your chart shows a lot of natural directness — Mars in Aries, in the 1st house, close to the surface. Where has that directness actually served you well recently, rather than caused friction with someone else?"

**Options:** "It helped recently" · "Can't think of one" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Direction-change after disagreement (B6/card-writing-guide "Handling disagreement") — deliberately does not re-argue the prior reactivity framing; pivots to a related but distinct, affirming angle on the same placement rather than a softer restatement of the same claim.

---

### P10 — Millennial, limited_chart mode (no birth time on file)

**Supplied computed facts:** Natal Sun in Scorpio (sign only — no birth time, so no houses/rising available). Current transit: Sun (transiting) opposite natal Sun (this year's "half-birthday" point). Mode: `limited_chart`.

**Sender-observation + question:** "We don't have your birth time on file, so this stays at the sign level: your Sun is in Scorpio, and the sky is currently at the point directly opposite it — roughly your half-year mark. That's often a natural moment to check in on something you started earlier this year. Is there anything you set out to do that's worth revisiting?"

**Options:** "Yes, there is" · "Nothing comes to mind" · "I'm unsure" · "Prefer not to share" (+ free text)

**Mechanism:** Honest mode-downgrade transparency (Part C, structural constraint) — the card explicitly names the missing data and what's therefore omitted (houses/rising) rather than silently presenting a thinner reading as if it were full personalization; builds trust rather than borrowing the credibility of a fuller chart it doesn't have.

---

## Coverage summary

| Card | Mode | Segment |
|---|---|---|
| G1 | general | Gen Z |
| G2 | general | Millennial |
| G3 | general | Connecting with parents |
| G4 | general | Parents / older adults |
| G5 | general | Cross-life-stage |
| G6 | general | Gen Z |
| P1 | personalized | Gen Z |
| P2 | personalized | Gen Z |
| P3 | personalized | Millennial |
| P4 | personalized | Millennial |
| P5 | personalized | Connecting with parents (adult child) |
| P6 | personalized | Connecting with parents (older-adult participant) |
| P7 | personalized | Parents / older adults |
| P8 | personalized | Older adults |
| P9 | personalized | Gen Z (disagreement-branch pattern) |
| P10 | personalized (`limited_chart`) | Millennial |

16 cards total: 6 general, 10 personalized (including one explicit `limited_chart` example and one explicit post-disagreement branch example), spanning all four audience-direction segments plus cross-life-stage.
