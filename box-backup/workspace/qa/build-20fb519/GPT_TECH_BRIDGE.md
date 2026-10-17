# GPT → engineering bridge

> **Current navigation/Routine authority (30 September 2026):** [decision 0014](docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md) and the [Routine V3 handoff](android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md) supersede three-tab or Dashboard assumptions in this earlier bridge. Preserve its technical and safety findings unless 0014 explicitly changes them.

**Purpose:** convert GPT's product/research thinking into engineering terms *once*, so the same
discussion does not happen twice and neither side waits on the other. Owner: Claude.
Created 29 September 2026 against `SHANIVAR_STANCE.md` (v. 29 Sep 2026) and
`research/deep-foundation/`.

**This file is not a summary of GPT's research.** It captures only what changes an engineering
decision. Philosophy, source analysis and audience work stay in their own files and are referenced,
not copied.

## Classification

| Class | Meaning | Engineering behavior |
|---|---|---|
| **A** | Technically clear | Proceed with design |
| **B** | Possible, needs a product decision | Document, give 2–3 concrete options, keep building around it |
| **C** | Research-dependent | Mark the dependency, design the interface anyway |
| **D** | Technically weak / unsafe / expensive | Explain precisely why, offer the nearest viable thing |
| **E** | Impossible under current constraints | State the limitation, propose the closest alternative |

**Escalate to founder only when the answer materially changes architecture or user experience.**
Nine of the fourteen entries below need no escalation at all.

---

## C-01 — A contextual Home that offers one relevant next action

**Source / date:** Founder brief §2, §10; `SADHAK_JOURNEY.md` teaching sequence · 29 Sep 2026

**Founder/GPT intent:** The user should never wonder what to press. Home opens into the most relevant
action; deeper areas appear only when needed. One meaningful decision per screen.

**User value:** Removes the cost of translating an internal taxonomy (Yoga / Tantra / VBT / Focus)
into "what do I do right now".

**Technical interpretation:** A pure function from local state to a ranked next action, rendered by
the existing single-Home shell. Not a navigation rewrite.

**Existing system support:** Strong. `V1Home.kt` is already one Home with a flat route switch and a
180ms `Crossfade`. Three cards (focus / alarm / breathe) exist. The breathing recommender is already
exactly this pattern: deterministic, explainable, injected clock, pure and unit-tested.

**Required components:** `HomeState` resolver (pure Kotlin, no Compose); inputs = pending alarm,
focus-guard `endsAt`, breathing schedule, last practice outcome, time of day.

**Data required:** All already local. Nothing new, nothing sensitive.

**AI required:** **None.** This is the highest-value item in GPT's direction and it needs no model at
all. Resisting the urge to put an LLM here is the correct engineering call.

**Platform dependencies:** None.

**Privacy implications:** None — no new data, no upload.

**Latency/cost implications:** Zero. Synchronous local computation.

**Major unknowns:** Whether a *chosen* action beats an *offered* one for this audience. That is a
pilot question, not an architecture question.

**Possible implementations:**
- **Option A — Pure resolver, three existing cards reordered.** Advantages: days of work, fully
  testable, reversible. Limitations: still a card list. Complexity: low.
- **Option B — Single primary action + "something else" disclosure.** Advantages: closest to the
  stated intent. Limitations: hides utilities a returning user wants directly; a wrong guess costs
  more than a card list does. Complexity: medium.
- **Option C — Resolver + remote-config ranking.** Advantages: tunable. Limitations: needs infra that
  does not exist; premature. Complexity: high.

**Recommended implementation:** **A now, measure, then consider B.** Ship the resolver behind the
existing cards first so the ranking logic is proven before the UI commits to hiding anything.

**Prototype needed:** No — small enough to build correctly.

**Decision required:** None to start.

**Status:** **Class A.** Ready. P1.

---

## C-02 — Six-layer knowledge architecture with preserved provenance

**Source / date:** Founder brief §7, §8; `ANCIENT_KNOWLEDGE_MAP.md`, `SOURCE_LEDGER_ANCIENT.md`,
`TRADITION_SEPARATION_MATRIX.md`; stance ST-001 · 29 Sep 2026

**Founder/GPT intent:** Primary source → commentary → scholarship → contemporary interpretation →
Shanivar synthesis → product practice. Never flatten into one embedding pool. Every retrieved idea
keeps source, tradition, author, text, translation, interpretation type, confidence, approval state.

**User value:** Indirect but real — it is what separates Shanivar from a wellness app with Sanskrit
decoration, and it is what lets a curious user check where something came from.

**Technical interpretation:** A **relational** provenance model with a layer discriminator, plus an
approval state machine. Retrieval is a later, optional concern.

**Existing system support:** **Partially, in exactly the right place.** `breathing-v1.json` +
`BreathingCatalog.kt` already implement a working subset: per-practice `source{work, verseLabels,
edition, url, relationship, adaptationNote, verification}`, `source = null` meaning "modern
exercise", validation that rejects half-filled attribution and fabricated verse labels, and a
`status` field that cannot be mistaken for reviewed. **Generalize this rather than designing fresh.**

**Required components:** Migration runner (**hard prerequisite** — see map §1.4); `sources`,
`source_claims`, `practices`, `practice_sources` tables; a `layer` enum; an editorial review record.

**Data required:** No user data. Bibliographic metadata, short quotations, original synthesis. Do not
ingest copyrighted books.

**AI required:** None for storage. Optional later for retrieval.

**Platform dependencies:** None.

**Privacy implications:** None — no personal data.

**Latency/cost implications:** Negligible relational reads. A vector store would add cost and latency
for no current benefit.

**Major unknowns:** Corpus size. Nothing observed yet justifies more than hundreds of objects.

**Possible implementations:**
- **Option A — Static versioned JSON assets, relational mirror later.** Advantages: matches what
  already ships and works offline; zero infra. Limitations: no server-side query; editorial changes
  need an app release. Complexity: low.
- **Option B — Postgres provenance tables + JSON asset generated from them at build time.** Advantages:
  editorial workflow and offline delivery both work; the app stays offline-first. Limitations: needs
  the migration runner and a build step. Complexity: medium.
- **Option C — Postgres + pgvector, retrieval-first.** Advantages: scales to a large corpus.
  Limitations: solves a problem we do not have; provenance filtering in vector search is the hard part
  and would be built before it is understood. Complexity: high.

**Recommended implementation:** **B, deferred; A continues meanwhile.** 8 practices ship as validated
JSON today. Grow to ~50 the same way. Introduce B when editorial volume — not ambition — makes the
release step painful.

**Prototype needed:** No.

**Decision required:** None yet. The migration-runner decision (P0) is separate and should not wait
for this.

**Status:** **Class A** for the schema, **Class C** for retrieval (depends on corpus size nobody has
yet). P2 for tables, P0 for the migration runner that unblocks them.

---

## C-03 — `SHANIVAR_STANCE.md` as a governed artifact that constrains production

**Source / date:** Founder brief §7; `KNOWLEDGE_GOVERNANCE.md`; the stance file's own preamble · 29 Sep 2026

**Founder/GPT intent:** Research enters as `PROPOSED`. Only founder-accepted material influences
production behavior. Agents propose; a human accepts; production consumes accepted stance.

**User value:** None directly. It prevents a category of harm: unreviewed content reaching people.

**Technical interpretation:** Two separable things — (a) a document lifecycle, which is process, and
(b) a machine-checkable gate, which is engineering.

**Existing system support:** **The pattern already exists and works.** `breathing-v1.json` carries
`status: implementation_seed_review_before_public_release`; `BreathingCatalogue.reviewedForPublicRelease`
returns false for anything except an explicit reviewed status; a test asserts a schema pass cannot
flip it. That is C-03 already implemented for one content type.

**Required components:** Extend the same `approval_status` field to every future content object; a CI
check that no object with `PROPOSED` status is reachable in a release build.

**Data required:** None.

**AI required:** None. **Explicitly: no model may read `SHANIVAR_STANCE.md` at runtime to decide
behavior.** Accepted stance becomes code, content or config through human review — never a prompt
that an LLM interprets differently on each call.

**Platform dependencies:** None.

**Privacy/safety implications:** This *is* the safety mechanism for content.

**Latency/cost implications:** None.

**Major unknowns:** None technical.

**Possible implementations:** One sensible path: keep the field in content, assert it in CI.

**Recommended implementation:** Reuse the breathing pattern verbatim for each new content type. Add
the CI assertion when CI exists (P1).

**Prototype needed:** No.

**Decision required:** None. **One correction for GPT:** five documents the stance file links to do
not exist — `SHANIVAR_DEEP_FOUNDATION_REPORT_V1.md`, `SOURCE_LEDGER.md`,
`MODERN_PSYCHOLOGY_BRIDGE.md`, `PRACTICE_SAFETY.md`, `PRODUCT_DECISION_LOG.md`. A governance file
with broken links to its own evidence undercuts the governance. Worth fixing on GPT's side.

**Status:** **Class A.** Already substantially done. P1 for the CI gate.

---

## C-04 — Sadhak modes, not levels

**Source / date:** `SADHAK_JOURNEY.md`; stance ST-005 · 29 Sep 2026

**Founder/GPT intent:** Explorer → Beginner → Returning → Committed → Deep are *chosen relationships
to practice*, not measured levels. No XP, no leaderboards, no source access gated by streaks.
Readiness is contextual.

**User value:** A tired long-term practitioner can take a ten-second anchor without "regressing"; a
newcomer can read an edition note before trying anything.

**Technical interpretation:** A capability/state model, not an ordinal. Independent dimensions,
mostly *declared* rather than inferred. This is straightforwardly a set of optional preferences plus
recent history.

**Existing system support:** The breathing recommender already demonstrates the exact shape:
`requiresPriorComfort` is a hard constraint set **only** by an explicit user choice, never inferred
from minutes practised or sessions completed. Decision 0009 already bans gamification for V1.

**Required components:** A `PracticeContext` preference object; reuse of the recommender's
hard-constraint pattern.

**Data required:** Declared preferences; optional per-practice usefulness reports (already stored,
capped at 100, locally, no feeling labels).

**AI required:** None.

**Platform dependencies:** None.

**Privacy implications:** **The main risk in this entry.** A stored "practitioner level" is a
sensitive inference about a person's inner life. GPT's own stance (ST-005, "do not store a
consciousness rank") is the correct constraint and it is enforceable: store the declared preference,
never a derived rank.

**Latency/cost implications:** None.

**Major unknowns:** Which dimensions actually predict a useful next experience. Unknown, and cheap to
learn from a pilot.

**Possible implementations:**
- **Option A — Declared preferences only.** Advantages: no inference, no privacy exposure, trivially
  correctable. Limitations: asks the user questions. Complexity: low.
- **Option B — Declared + narrow derived signals** (e.g. "has completed this practice before").
  Advantages: less asking. Limitations: every derived signal needs a correction path and a deletion
  story. Complexity: medium.
- **Option C — Inferred readiness model.** Advantages: none that justify it. Limitations: it is a
  consciousness score with a different name; ST-005 forbids it and so does 0009. Complexity: high.

**Recommended implementation:** **A, with B only where the signal is factual** ("you have done this
before") rather than evaluative ("you are ready for more").

**Prototype needed:** No. GPT's J-01 experiment is the right test and needs no new infrastructure
beyond per-arm builds (see C-11 on flags).

**Status:** **Class A.** P2 — it does not gate anything shipping sooner.

---

## C-05 — Three-mode Focus redesign

**Source / date:** `CLOCK_FOCUS_RESEARCH.md`; stance ST-006 · 29 Sep 2026

**Founder/GPT intent:** Scheduled Focus ("protect a regular time"), Contextual Focus ("protect this
activity"), Persistent Focus ("keep these apps unavailable" — explicitly a later capability).
Overnight windows via a start-day convention. Distinct `Scheduled` / `Active` / `Needs attention` /
`Off` states.

**User value:** High. This is Shanivar's strongest utility and its effect is visible in ordinary life.

**Existing system support:** Scheduled Focus largely exists (`FocusScheduleCard.kt`, one schedule,
all-day / selected-days / custom presets, verbatim when/then). Contextual Focus is mostly a labelling
and duration layer over the existing manual session. Persistent Focus does not exist.

**Technical interpretation:** Mode 1 = fix and extend what exists. Mode 2 = thin layer, low risk.
Mode 3 = new semantics requiring rule ownership and verified recovery.

**Required components:** Fixes **FOCUS-1 … FOCUS-6** (map §2); overnight recurrence; a real status
model derived from permission + guard state; rule ownership before any second rule exists.

**Data required:** Nothing new. **No location, calendar or microphone access** — GPT explicitly rules
these out for v1 and I agree; contextual labels are user-chosen strings.

**AI required:** None.

**Platform dependencies:** `UsageStatsManager` + `SYSTEM_ALERT_WINDOW` + exact alarms. Ceiling is
real and already documented honestly.

**Privacy implications:** Package-level blocking cannot partially block a mixed-purpose app —
pausing WhatsApp pauses family messages. Already disclosed in the card; must stay disclosed.

**Latency/cost implications:** 1.5s detection interval; a visible flash of the blocked app is
expected and disclosed.

**Major unknowns:** Why people keep or disable blocking after two days. GPT is right that no
competitor documentation answers this and that "disabled" is not automatically failure.

**Possible implementations:**
- **Option A — Fix reliability, then Contextual, defer Persistent.** Advantages: fixes a defect that
  can lock someone out for 24 hours; each step independently shippable. Limitations: no new headline
  capability for a while. Complexity: low then medium.
- **Option B — Build all three modes, then harden.** Advantages: a complete story to market.
  Limitations: multiplies FOCUS-5 (rule ownership) across three modes before it is solved once.
  Complexity: high.
- **Option C — Persistent first (the loudest differentiator).** Advantages: none. Limitations: GPT's
  own gate — "if we cannot verify recovery and persistent state across restarts, do not ship this
  mode" — is currently unmet, and shipping it would put an unrecoverable lock in front of users.
  Complexity: high.

**Recommended implementation:** **A.** FOCUS-1 is a P0 bug fix, not a redesign, and it should land
before any of this.

**Prototype needed:** No prototype — a **physical-device test matrix** (reboot, cold process, late
delivery, expired credentials, offline, DST, timezone change, revoked permissions). That is GPT's CF02
and it is the gate.

**Decision required:** Founder acceptance for Persistent-mode scope, and for departing from the
current one-schedule V1.

**Status:** **Class B.** Mode 1 fixes = P0/P1. Mode 2 = P2. Mode 3 = blocked on verified recovery.

---

## C-06 — An emergency "Need essential access?" path

**Source / date:** `CLOCK_FOCUS_RESEARCH.md` safety section — recorded as a **new founder
requirement** · 29 Sep 2026

**Founder/GPT intent:** A clearly labelled essential-access path from active status and the block
surface. Not a "five more minutes" bypass. Must expose emergency calling and a local safety release
when an app was mistakenly restricted. No quota, timer, puzzle, walking, breath retention, guilt copy,
login or remote approval.

**User value:** Very high, and it is a safety property rather than a feature.

**Technical interpretation:** **This directly contradicts an accepted decision.** The current stance
is "no bypass affordance anywhere" and `BlockOverlayActivity` deliberately has none — ending focus
early is a `releaseFocus()` call from inside the app. GPT is right to flag that this needs explicit
founder acceptance rather than silent resolution in either direction.

**Existing system support:** Partial and useful — the overlay's exit already goes to the device home
screen, never back into the blocked app, and the app-side release path exists.

**Required components:** A local-first release sequence with a written conflict-resolution contract:
stop the guard/overlay **first**, mark protection suspended, cancel/reconcile pending boundaries,
*then* sync remotely. Idempotent and recoverable if the process dies mid-release. Must prevent the
next timer callback from immediately re-locking. The user's explicit local suspension must win over
stale network state.

**Data required:** None new.

**AI required:** None. A model must never sit between a person and emergency access.

**Platform dependencies:** Emergency dialling is outside our guard already; the essential-app list
needs OS-aware exclusions (dialler, Settings, launcher, accessibility/assistive tools, authenticators,
health tools).

**Privacy implications:** A reason for release must be optional and never a condition of release.

**Safety implications:** **This is the highest-stakes entry in this file.** The failure mode is a
person unable to reach something they urgently need.

**Latency/cost implications:** Must work fully offline. No server round-trip may gate release.

**Major unknowns:** Whether a visible safety path measurably increases casual bypass. Testable, and
the safety argument likely outweighs the result either way.

**Possible implementations:**
- **Option A — Local release, always available, logged locally, no friction.** Advantages: safest;
  offline-correct. Limitations: could become the normal exit. Complexity: medium (the conflict
  contract is the work).
- **Option B — Local release + one plain confirmation naming the consequence.** Advantages: keeps it
  deliberate without obstructing. Limitations: a confirmation is still a step under stress.
  Complexity: medium.
- **Option C — Quota or cooldown** (the Opal/Brick pattern). Advantages: discourages casual use.
  Limitations: **rejected.** A consumable safety allowance can be exhausted precisely when needed.
  Complexity: medium.

**Recommended implementation:** **B**, with the confirmation naming exactly what resumes and when,
and with emergency calling reachable without any confirmation at all.

**Prototype needed:** Yes — spike the release sequence with induced process death mid-release.

**Decision required:** **Yes — founder must accept the exact behavior**, because it changes an
accepted enforcement decision. Recommend a short ADR.

**Status:** **Class B, escalate.** P0 for the decision; P1 for implementation.

---

## C-07 — A small transition on the block screen instead of "BLOCKED"

**Source / date:** Founder brief §12 "Focus Mode + Shanivar"; `CLOCK_FOCUS_RESEARCH.md` CF04 · 29 Sep 2026

**Founder/GPT intent:** "You came here automatically. Take one breath." Then return, or continue
intentionally per the focus contract. Awareness, not punishment.

**Technical interpretation:** Already built, minus the "continue intentionally" half.

**Existing system support:** **Strong — this shipped on 25 September.** `BlockOverlayActivity` offers
an inline one-minute pause and an inline breathing practice, shows the user's own verbatim when/then
commitment when one exists, discloses that the app-flash is expected, and exits to device home. The
breathing player structurally cannot release protection (enforced by a test that fails the build if a
`Breathing*.kt` file so much as names an enforcement type).

**Required components:** Only the "continue intentionally" path, which is really C-06.

**AI required:** None. The prompt is reviewed catalogue content, not generated.

**Safety implications:** A practice must never become an unlock — currently guaranteed structurally.

**Recommended implementation:** No new build. Run GPT's CF04 comparison (return-only vs optional
grounding) against what exists.

**Decision required:** None.

**Status:** **Class A, largely delivered.** The remaining question is C-06.

---

## C-08 — "Messages from Shanivar": vertically moving AI reflection cards

**Source / date:** Founder brief; integration-mode brief · 29 Sep 2026

**Founder/GPT intent:** A scrollable sequence of AI-guided reflection cards. The next card must feel
immediate — not like waiting for a chatbot.

**User value:** Plausibly high, entirely unvalidated.

**Technical interpretation:** The hardest thing in GPT's direction, because it is the only concept
that puts **generative AI on the interaction critical path**. Everything else can be deterministic.

**Existing system support:** More than expected but stance-mismatched. `ReflectionScreen.kt`,
`ReflectionState.kt`, `ReflectionStore.kt` (AndroidKeyStore AES/GCM), `ReflectionContract.kt`,
`reflections.js` (776 LOC) and a native contract document all exist — **dormant** behind
`!STREAMLINED_LAUNCH`, and built before decision 0009 banned feed/engagement mechanics.

**Required components:** Prefetch/generation strategy; card state machine; caching; moderation;
offline and failure states; a deterministic fallback.

**Data required:** Whatever personalization is chosen — the sensitive-data question in this file.

**AI required:** Yes, and this is the entry where cost and latency actually bite.

**Latency/cost implications:** A per-card model call at scroll speed is the expensive design. At
10k daily users × 5 cards, that is 50k generations/day. Prefetch and pre-generation change the cost
profile by an order of magnitude and are the difference between viable and not.

**Privacy implications:** Personalized generation means user context leaves the device to a third-party
model provider. That needs explicit consent and a clear data boundary, and it is a genuine change to
the app's current "almost nothing leaves the device" posture.

**Major unknowns:** Whether a *sequence* of cards is better than one well-chosen card. **An unexamined
premise worth challenging:** a vertically scrolling card feed is structurally the interaction pattern
of the products Shanivar exists to help people escape, and decision 0009 explicitly bans a content
feed for V1. The mission is "one small intervention, then leave."

**Possible implementations:**
- **Option A — Pre-generated pool, deterministically selected.** Advantages: instant; no per-view cost;
  fully reviewable before a human sees it; works offline; no user context leaves the device.
  Limitations: not per-person wording. Complexity: low.
- **Option B — 3-card prefetch with real generation, background-filled.** Advantages: feels instant;
  genuinely personalized. Limitations: cost scales with users; needs consent, moderation and a
  fallback; unreviewed text reaches people. Complexity: high.
- **Option C — On-demand generation per card.** Advantages: simplest to write. Limitations: every card
  is a visible wait; exactly the "waiting for a chatbot" failure named in the brief. Complexity: low
  to build, bad to use.

**Recommended implementation:** **A first, and challenge the feed premise before building B.** A
curated pool with deterministic selection tests whether people find the cards useful *at all* — the
actual open question — with no cost curve, no privacy change and no moderation burden. If A proves the
value, B becomes a measured upgrade rather than a leap.

**Prototype needed:** **Yes — spike S-2**, and it should measure Option A's perceived quality against
B's latency, not assume B is required.

**Decision required:** Founder: does a card *sequence* survive 0009's feed ban, or is this one card at
one moment?

**Status:** **Class B, escalate.** P1 for the decision, P2 for any build.

---

## C-09 — Astrology: calculation / tradition / generated reflection kept separate

**Source / date:** Founder brief §17; stance ST-007 · 29 Sep 2026

**Technical interpretation:** Three distinct steps that must not collapse into one opaque model call,
with provenance a user can eventually inspect.

**Existing system support:** **Better than GPT may realize.** `backend/src/utils/astrology.js`
computes natal charts and transits deterministically and offline via `circular-natal-horoscope-js`;
`summarizeTransitsForPrompt` is already a separate step; and `chart.js` carries a comment recording
that it *used to* ask Gemini and was deliberately changed to deterministic calculation. The
calculation/generation boundary is already drawn on the correct side.

**Missing:** the middle layer — a named-tradition interpretation record between calculation and
generation. Today it is calculation → prompt.

**AI required:** Only for the third layer, and only over an explicit interpretation record.

**Recommended implementation:** Keep dormant. When revived: add an `interpretations` table keyed to
calculated factors, with tradition and source named, and let generation consume *that* rather than raw
chart data.

**Decision required:** None now — ST-007 explicitly does not reintroduce astrology to the launch
surface.

**Status:** **Class A** when revived. P3.

---

## C-10 — "Useful departure" and agency metrics

**Source / date:** Stance ST-008; founder brief §2; integration brief analytics section · 29 Sep 2026

**Founder/GPT intent:** Measure practice started/completed, returned to intended task, voluntary
reflection, focus period completed, user correction, successful skip, app exited after intervention.
Never score silence as success. Do not optimize time-in-app.

**Technical interpretation:** An event taxonomy plus an explicit rule that absence of data is not a
positive signal.

**Existing system support:** **Effectively none.** One local append-only JSON self-report in
`SessionReview.kt`. The backend has `analytics.js` and `telemetry.js`; the client calls neither.

**Required components:** A local event log with a consented export; crash reporting.

**Data required:** Event names and timestamps. **Not** reflection content, not feeling labels, not
package names.

**Privacy implications:** The honest version of this is consent-gated and local-first. Note the
tension worth naming: "did the person return to their intended task?" is only measurable by watching
what they do after leaving — and watching more closely to prove we help people disengage is
self-undermining. Self-report is the more defensible instrument here even though it is weaker.

**Major unknowns:** Whether self-report survives contact with a real cohort.

**Possible implementations:**
- **Option A — Local event log + explicit export, self-report for outcomes.** Advantages: consistent
  with the mission; no silent upload; enough for a 20-person pilot. Limitations: no aggregate view;
  manual. Complexity: low.
- **Option B — Consented telemetry to the existing backend route.** Advantages: aggregate learning.
  Limitations: builds a pipeline before knowing which events matter. Complexity: medium.
- **Option C — Third-party analytics SDK.** Advantages: fast. Limitations: ships a tracking SDK inside
  an app about reclaiming attention; sends behavioral data to a vendor. Complexity: low, cost high in
  credibility. Recommend against.

**Recommended implementation:** **A for the pilot.** Add crash reporting regardless — a tester whose
alarm fails currently leaves no trace anywhere, which is the single biggest blind spot.

**Status:** **Class A.** P1 (crash reporting), P2 (event log).

---

## C-11 — International audience and multilingual delivery

**Source / date:** Founder brief §9; `INTERNATIONAL_AUDIENCE_RESEARCH.md`; stance preamble · 29 Sep 2026

**Founder/GPT intent:** International explorers of Indian contemplative traditions. Multilingual
source metadata, multiple translations, terminology mapping, Sanskrit transliteration, locale-sensitive
explanation. Do not make English canonical when primary sources are not English.

**Technical interpretation:** Two separate problems repeatedly conflated: **UI localization** and
**content translation with provenance**.

**Existing system support:** **Content: partially ready. UI: nothing at all.**
`breathing-v1.json` declares `scriptLanguage: "en"` and an explicit `languageFallback` statement, and
a test asserts the app never claims Hindi guidance it does not have — that is the content half done
correctly. But `res/values/` contains **only `colors.xml`**. There is **no `strings.xml` anywhere**.
Every user-facing string is hardcoded Kotlin. `preferred_language` is persisted server-side and
changes no client string.

**Required components:** String extraction to resources (mechanical, one pass); per-locale content
variants keyed to the same practice id with independent review status; transliteration held as
metadata, never generated at runtime.

**AI required:** **None, and machine translation must not be used for practice instructions.** A
mistranslated "stop if you feel dizzy" is a safety failure. Translation needs a reviewer, and the
review must be per exact content version.

**Latency/cost implications:** None.

**Major unknowns:** Which languages first. Hindi is already gated on a comprehension review that has
not happened.

**Recommended implementation:** Extract strings to resources **before** any translation work. Keep
content translation as reviewed per-locale variants with their own approval state, reusing the C-03
pattern.

**Decision required:** Founder: which locale first, and who reviews it.

**Status:** **Class A** technically, **Class C** on reviewer availability. **P1** — this is a hard
blocker and it gets more expensive every week the surface grows.

---

## C-12 — "Shanivar should know the practitioner deeply"

**Source / date:** Integration brief; founder brief §11 guru model; stance ST-003, ST-005 · 29 Sep 2026

**Technical interpretation:** Four storage classes that must not blur, in increasing order of risk:
1. **User-declared facts and preferences** — authoritative, correctable, small.
2. **Practice/interaction history** — factual events, bounded, local.
3. **AI-generated hypotheses** — provisional, never promoted to fact silently, always attributed to
   the system rather than the person.
4. **Sensitive reflective content** — encrypted, local, never in analytics, never in a prompt without
   explicit per-use consent.

**Existing system support:** Classes 1 and 2 exist (preferences + bounded journal). Class 3 does not
exist and **should not be created without a correction path**. Class 4 has real encryption already.

**Required components:** A retention and deletion policy per class; a user-facing "that doesn't fit
me" correction that materially changes behavior; a hard rule that class 3 never becomes class 1
without an explicit user confirmation.

**Privacy implications:** The entry with the greatest potential for harm. "Knowing the practitioner"
and "profiling the practitioner" are the same implementation with different intent, and only
architecture keeps them apart. GPT's own stance already forbids storing a consciousness rank or
inferring trauma, diagnosis, belief or caste — that constraint is enforceable and should be enforced
in the schema, not in review.

**AI required:** Only for class 3, which is the class we should be slowest to build.

**Major unknowns:** Whether personalization beyond declared preference improves anything. Untested.

**Recommended implementation:** **Declared preferences + recent history only for V1.** No inferred
profile, no embeddings of personal reflections, no hypothesis store. Revisit only if a pilot shows
declared preference is insufficient — which it may well not.

**Prototype needed:** No. The cheap test is whether explicit preference already feels personal enough.

**Decision required:** Founder, before any class-3 work: is an app-held hypothesis about a person's
inner state acceptable at all? A defensible answer is no.

**Status:** **Class B, escalate before building.** P2 at the earliest.

---

## C-13 — Practice safety classes

**Source / date:** Founder brief §16; stance ST-004 · 29 Sep 2026

**Founder/GPT intent:** LOW RISK / CONTEXT REQUIRED / CAUTION / QUALIFIED GUIDANCE RECOMMENDED /
DO NOT AUTO-DELIVER. AI must never escalate a user into advanced practice because engagement is high.

**Existing system support:** **Already implemented in substance for the breathing set.** Hard
constraints (`requiresBreathComfort`, `requiresPriorComfort`, `requiresSleepIntent`,
`requiresSleepyNow`) gate eligibility; `requiresPriorComfort` is set only by explicit choice; the
validator refuses content carrying prescribed breath timing at all, so the model cannot express a
retention instruction; a "felt worse" answer suppresses a practice for the visit; and no engagement
signal can unlock anything.

**Required components:** A named `risk_class` field to make implicit classification explicit, and a
rule that only LOW RISK is auto-deliverable.

**AI required:** None. Deterministic rules plus content metadata. Correct as-is.

**Recommended implementation:** Add `risk_class` to the practice schema and assert in CI that nothing
above LOW RISK is reachable through automatic recommendation.

**Decision required:** None.

**Status:** **Class A.** P2 — formalizing a constraint that currently holds by construction.

---

## C-14 — UGC from genuine product events

**Source / date:** Founder brief §13; `UGC_AND_SOCIAL_EXPERIMENTS.md`; `CLOCK_FOCUS_RESEARCH.md` closing · 29 Sep 2026

**Founder/GPT intent:** Shareable milestones from real behavior — mornings protected, a chosen task
completed, a rule adjusted after learning. Regained agency, not shame. No fabricated numbers.

**Technical interpretation:** Requires (a) trustworthy event data, which does not exist yet (C-10),
and (b) a share artifact generator.

**Existing system support:** None. Sharing is not implemented.

**Privacy implications:** Reflection content must never be shareable by default. Package names should
not appear in a shared artifact — "Instagram paused 14 mornings" discloses more than the user may
intend.

**Recommended implementation:** **Blocked on C-10.** Do not build sharing on top of event data that
is not yet trustworthy; a wrong number in a public post is worse than no feature. GPT's neutral prompt
("What did you make room for?") is the right framing when it exists.

**Decision required:** None yet.

**Status:** **Class C**, dependent on C-10. P3.

---

## Summary

| ID | Concept | Class | Priority | Escalate? |
|---|---|---|---|---|
| C-01 | Contextual Home | A | P1 | No |
| C-02 | Knowledge architecture | A / C | P0 (migrations), P2 (tables) | No |
| C-03 | Governed stance | A | P1 | No (one correction for GPT) |
| C-04 | Sadhak modes | A | P2 | No |
| C-05 | Three-mode Focus | B | P0/P1/blocked | Persistent scope only |
| C-06 | Emergency access | B | P0 decision | **Yes** |
| C-07 | Block-screen transition | A | Delivered | No |
| C-08 | Messages from Shanivar | B | P1 decision | **Yes** |
| C-09 | Astrology layering | A | P3 | No |
| C-10 | Agency metrics | A | P1/P2 | No |
| C-11 | Internationalization | A / C | **P1** | Locale + reviewer |
| C-12 | Deep personalization | B | P2 | **Yes, before building** |
| C-13 | Practice safety classes | A | P2 | No |
| C-14 | UGC | C | P3 | No |

**Three escalations, not fourteen.** Everything else can proceed or wait without a founder decision.

**Two things GPT should know that change its premises:**
1. The shipped app makes **zero AI calls**. This is a greenfield AI product, not an existing one.
2. There is **no `strings.xml`**. The international direction has no delivery mechanism today.
