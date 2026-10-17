# Card-Writing Guide — Messages from Shanivar

For anyone (human or AI/prompt-engineered) writing scroller card copy. Read [`psychology-research.md`](./psychology-research.md) first for the reasoning behind these rules; this document is the operational checklist. Binding source: [`android-app/CLAUDE_REFLECTION_SCROLLER_HANDOFF.md`](../../android-app/CLAUDE_REFLECTION_SCROLLER_HANDOFF.md).

## Card structure (fixed shape, every card)

1. **Sender-observation** — one grounded observation. In `personalized`/`limited_chart` mode this must cite only supplied computed facts (a real natal placement, a real natal-to-transit aspect, a real house cusp — whatever the context snapshot actually contains). In `general` mode it's an observation about a universal, everyday pattern (attention, comparison, routine, relationships) with no chart claim at all.
2. **Interpretive bridge** — one clause connecting the observation to an everyday, ordinary-life theme. Explicitly interpretive language ("one way to read this," "this can show up as") — never stated as settled fact about the user.
3. **One question** — exactly one. Never stack two questions in a card ("Does that fit? And what would you do differently?"). The handoff doc requires one question per card; it also keeps cognitive load low enough that the response stays genuine rather than rushed.
4. **Response options** — 2–4 quick options plus free text; see [`response-option-design.md`](./response-option-design.md) for the full spec. Always include a non-disclosing option.
5. Optional: expandable "Why this reflection?" context, showing the actual context_ref (e.g., "Saturn transit to your natal Moon, as of today's positions") — never hidden reasoning, never a fact not shown in the card itself.

## Word count

**40–90 words**, per the handoff doc, counting the sender-observation, bridge, and question together (not response option labels). Practical bands:
- 40–55 words: tight, single-beat card — good default for `general` mode and for early cards in a session.
- 60–90 words: use when a personalized card needs to name the computed fact *and* make the everyday bridge legible without jargon (e.g., naming a transit and translating it) — don't spend words on throat-clearing ("Today I want to talk about...") to hit the higher end; spend them on precision.

If a draft needs more than 90 words to make its point, the point is probably two observations, not one — split it or cut the weaker half.

## Tone calibration: good vs. bad phrasing

Use these paired examples as a working reference. The "bad" column is not hypothetical — each maps to a named forbidden pattern.

| Situation | Bad (rejected — names the pattern) | Good (ships) |
|---|---|---|
| Opening a personalized observation | "Your chart proves you struggle with commitment." (**overclaiming-as-fact**, `audience-direction.md` forbidden phrase) | "One way to read your natal Venus in a fixed sign is a pull toward steady, familiar commitments." |
| Framing the AI's authority | "I know you better than you know yourself." (**forbidden phrase**, obedience/privileged-access framing) | "You're the one who knows what today actually felt like — I'm just naming a pattern to check against it." |
| Encouraging continued use | "Keep coming back and you'll finally understand yourself." (**guaranteed self-realization**) | "This is one lens, today. Take what's useful and leave the rest." |
| Responding to "not today" | "Are you sure? This usually resonates with people." (**insistence after disagreement** — explicitly banned by the handoff doc) | "Understood — let's try a different angle." (next card changes direction, doesn't re-litigate) |
| A user who hasn't opened the app in days | "Shanivar noticed you've been away. Don't lose your progress!" (**shame/streak-guilt hook**) | (no such message exists — see response-option-design.md and the discipline-engine constraints; a return is simply available, never guilt-triggered) |
| Making a card feel urgent | "Your next message is waiting — don't miss it." (**manufactured scarcity**) | "Next card, whenever you're ready." (Continue action, no countdown, no loss framing) |
| Naming a difficult transit | "Saturn is punishing you right now." (**fear-based, deterministic-event framing**) | "Saturn's current position touches your natal Moon — for a lot of people that shows up as wanting more structure than usual. Does that match anything this week?" |
| Closing a card | "Answer honestly or this won't work." (**disclosure-as-toll**) | Any response, including "prefer not to share," is treated as complete — no copy implies a "right" or "real" answer exists. |
| Describing what the chart does | "This chart reveals your true self." (**diagnosis/proof framing**) | "This chart is a lens, not a verdict — useful for noticing, not for deciding who you are." |
| Asking the question | "Don't you think you're afraid of change?" (**leading question, presupposes the answer**) | "Does a pull toward the familiar show up for you today?" |

General tone notes:
- Second person, present tense, plain words. No astrology jargon left untranslated (a term like "square" or "retrograde" needs an everyday gloss in the same sentence, not a separate glossary).
- Gently challenging is fine ("before you reach for the next answer, pause") — insistent is not. The test: if the user says no, does the sentence still feel true and complete? "You struggle with commitment, don't you?" fails; "a pull toward steady commitments" doesn't need the user's agreement to have been an honest, non-coercive observation.
- Never write in a voice that implies concern about the user's absence, mood, or life choices unless the user has just told the product about them in this session. The product does not initiate emotional claims about the user's present state — see handoff doc: "Ground 'where you are today' in ... what the user says about their current experience," not in inference.

## Handling disagreement or "not today" — the load-bearing rule

This is the single most important behavioral rule in the whole feature, named directly in the handoff doc ("disagreement should change direction rather than provoke insistence that the chart is right") and in `audience-direction.md` ("let users disagree, skip, change direction... without shame").

Concretely, when the prior response was `"Not today"`, `"I'm unsure"`, disagreeing free text, or `"Prefer not to share"`:
1. **Do not restate or defend the previous card's premise.** The next card must not open with "But actually..." or re-explain why the reading was accurate.
2. **Do not treat disagreement as data about resistance.** Never write or imply "that's often what it looks like when it's true" — this is the exact insistence pattern named as forbidden.
3. **Change the angle, not just the wording.** If the previous card connected a transit to "routine," and the user said "not today," the next card should explore a different theme entirely (or zoom out to a more open, low-stakes question) rather than rephrasing the same claim about routine more gently.
4. **Treat "I'm unsure" as an invitation to slow down, not to push harder.** A good next card after "I'm unsure" often asks something more concrete and answerable ("What's one thing that did feel true about today?") rather than a more abstract or more pointed version of the same question.
5. **Never let the summary or session-end copy mention a disagreement or skip as a shortfall.** ("You skipped two questions today" is out; there is no framing of unanswered vs. answered as a personal score.)

A disagreement is a genuinely useful signal for what to explore next — it just isn't a target to correct. Treat it the way a good teacher treats "I don't think that's right": as information about where the student actually is, not as a mistake to fix.

## General mode vs. personalized mode

| | `general` mode | `personalized` / `limited_chart` mode |
|---|---|---|
| When used | No real chart on file, or missing birth data | Real natal chart present (personalized); some data present but incomplete, e.g. no birth time (limited_chart — omit houses/rising, time-sensitive facts) |
| Observation source | Universal, everyday themes: attention, comparison, routine, relationships, growing older, digital noise — same territory as `marketing/shanivar/audience-direction.md`'s content pillars | Only facts present in the server-computed context snapshot: real natal placements, real computed natal-to-transit aspects, real transit-as-of timestamp. **Never invent a placement, aspect, or house the snapshot doesn't contain.** |
| Bridge language | "A lot of people notice..." / "Many of us..." — openly speaks to a shared pattern, not a hidden truth about this specific user | "One way to read [specific supplied fact] is..." — ties the specific computed fact to the everyday theme; still explicitly interpretive, never asserted as settled fact |
| Provenance label shown to user | Labeled clearly as a general reflection (not personalized) — if a session was meant to be personalized but chart data is missing/uncertain, the mode downgrade must be visible, never silently passed off as personalized | Labeled with what it's grounded in when expanded ("Why this reflection?" shows the actual context_ref, e.g., "Natal Moon in [sign], current transit to it") |
| Risk to guard against | Being so generic it reads as filler, or accidentally implying personal knowledge it doesn't have ("you've been feeling this way lately" — implies observed history the general mode doesn't have access to) | Barnum-effect drift — writing something so vague it would apply to any chart, which defeats the purpose of having real data and risks feeling personal without being honest (see psychology-research.md Part A5/B5) |
| Example opening | "A lot of people open an app the moment things go quiet." | "Today's transit touches your natal Mercury." |

A `general`-mode card should never borrow personalized-mode phrasing patterns ("your chart," "your natal...") — that blurs the line the handoff doc requires to stay bright (no seeded/fake personalization). Conversely, a `personalized` card that could be copy-pasted into `general` mode without anyone noticing has failed its job — check every personalized draft against the actual context_ref it claims to use, not just against plausibility.

## Fast pre-ship checklist per card

- [ ] One observation, one bridge, one question — no stacked questions.
- [ ] 40–90 words (observation + bridge + question only).
- [ ] Every cited fact (mode = personalized/limited_chart) matches an actual context_ref; nothing invented.
- [ ] No phrase from the "forbidden" column above, or anything that reads as a paraphrase of one.
- [ ] Reads as complete and honest even if the user's answer is "no."
- [ ] If following a disagreement/skip: no defensiveness, changed angle, no "resistance" framing.
- [ ] Response options include a non-disclosing choice (see response-option-design.md).
- [ ] Patient-teacher voice: clear, personal, gently challenging — never demanding, never urgent, never ashamed-of-the-user.
