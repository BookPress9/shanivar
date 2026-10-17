# Claude technical integration — V1

> **Current navigation/Routine authority (30 September 2026):** [decision 0014](docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md) adopts Routine / Sadhana and authorizes the [Routine V3 build](android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md). Navigation and Routine proposals below are historical where they differ; platform constraints, defects and safety gates remain evidence.

**29 September 2026.** Owner: Claude (architecture, feasibility, systems).
Companions: [`SHANIVAR_TECHNICAL_MAP.md`](SHANIVAR_TECHNICAL_MAP.md) (capability-by-capability
reality) and [`GPT_TECH_BRIDGE.md`](GPT_TECH_BRIDGE.md) (GPT concept → engineering translation).

Scope boundary: GPT owns *what Shanivar could become*. This document owns *how it could be built
responsibly, and what stops us*. The founder owns what it will become.

Everything below is from source inspection and executed tests on this date. **Nothing is
device-verified.** That sentence recurs because it is the most important limitation in the project.

---

## 1. Current repository reality

Verified today: backend **305/305** Jest tests pass (22 suites, 8.9s). Android **160/160** JVM tests
pass, 0 skipped. `lintDebug` 0 errors. Production health 200 in 22.6s. Full detail in the technical
map; the four facts that reframe GPT's work:

1. **The shipped app makes zero AI calls.** The only AI-backed endpoint the client can reach
   (`/insights/today`) is called exclusively from `LegacyTabShell`, which `STREAMLINED_LAUNCH = true`
   compiles out. This is a greenfield AI product.
2. **There is no `strings.xml`.** `res/values/` holds only `colors.xml`. The international objective
   has no delivery mechanism.
3. **There is no CI and no analytics.** No `.github/` directory exists. A tester whose alarm fails to
   fire leaves no trace anywhere.
4. **Most of the backend is unreachable.** 4 of 19 routers are used by the client.

Roughly 60% of what reads as "built" is dormant code behind one compile-time flag.

---

## 2. Existing architecture

```text
┌─────────────────────────────── ANDROID (only client) ────────────────────────────────┐
│ MainActivity → ShanivarApp()                                                          │
│   └── STREAMLINED_LAUNCH=true ──► V1Home  (string route + Crossfade 180ms)             │
│         ├── focus   → FocusCard ──────────┐                                           │
│         ├── alarm   → AlarmCard ──────────┤                                           │
│         ├── breathe → BreathingHost       │                                           │
│         ├── pause   → LocalPauseScreen    │                                           │
│         └── settings/helper               │                                           │
│                                            │                                           │
│   └── STREAMLINED_LAUNCH=false ─► LegacyTabShell  ◄── DORMANT: reflections, practice   │
│                                    player, NoteCard (the ONLY AI call path)            │
│                                            │                                           │
│  ENGINES (local, no network)                │  PERSISTENCE (local)                      │
│  ├ AlarmScheduler  setAlarmClock            │  ├ SharedPreferences ×6 namespaces        │
│  ├ FocusScheduler  setExactAndAllowWhileIdle│  │   rhythm · focus_guard ·               │
│  ├ AlarmRecurrence / FocusWindowRecurrence  │  │   breathing_preferences ·              │
│  ├ FocusGuardService  UsageStats poll 1.5s  │  │   session_reviews · reflections        │
│  ├ BlockOverlayActivity  SYSTEM_ALERT_WINDOW│  ├ assets/breathing-v1.json (validated)   │
│  ├ AlarmForegroundService + AlarmRinger     │  ├ assets/techniques.json (dormant)       │
│  ├ BreathingSessionOwner (process-scoped)   │  └ EncryptedReflectionStore               │
│  └ BootRescheduleReceiver                   │      AndroidKeyStore AES/GCM              │
└──────────────────────────────────┬────────────────────────────────────────────────────┘
                                   │ HTTPS, JWT bearer, 13 endpoints total
                                   ▼
┌───────────────────────── RENDER free tier (~23s cold start) ──────────────────────────┐
│ Node/Express · 19 routers mounted · rate limited · xss sanitize                        │
│                                                                                        │
│  USED BY CLIENT (4)              │  UNREACHABLE FROM PRODUCT (15)                      │
│  ├ auth      JWT + bcrypt        │  gamification · billing · ecommerce · community     │
│  ├ users     /me                 │  journey · content · habits · journal · practices   │
│  ├ discipline  alarms, focus,    │  reflections (776 LOC) · chart · ai (Gemini) ·      │
│  │             focus-schedules   │  analytics · telemetry · whatsapp                   │
│  └ insights  /today  ◄── dormant client path only                                      │
│                                                                                        │
│  AI: Gemini gemini-3.6-flash via raw fetch, 15–20s timeout, rule-based fallback        │
│      (production key UNVERIFIED) — no SDK, no routing, no caching, no eval             │
│  ASTROLOGY: circular-natal-horoscope-js — deterministic, offline, no key               │
│  RETRIEVAL: none. No embeddings, no pgvector, no vector store.                          │
└──────────────────────────────────┬────────────────────────────────────────────────────┘
                                   ▼
        Supabase Postgres via Supavisor pooler (IPv4; direct host is IPv6-only)
        44 inline CREATE/ALTER TABLE at boot · no migration runner · no version history

ABSENT ENTIRELY: iOS · web client · push/FCM · object storage · CI/CD · analytics ·
                 crash reporting · remote config · strings.xml · release signing
```

---

## 3. Shipped capabilities relevant to this vision

| Capability | Why it matters to GPT's direction |
|---|---|
| Recurrence engines (`AlarmRecurrence`, `FocusWindowRecurrence`) | Most trustworthy code here. Pure, injected-clock, heavily tested. The pattern to copy |
| Breathing pillar (8 practices, 66 tests) | A **working** knowledge→experience pipeline with provenance, validation, safety gating and a governed status field. The template for C-02/C-03/C-13 |
| Deterministic recommender | Proves personalization can be explainable and model-free |
| Focus/app-lock | Strongest utility; see §16 and §5 for its defects |
| Alarm with verified dismissal | Real enforcement with documented escape guarantees |
| `EncryptedReflectionStore` | AndroidKeyStore AES/GCM already built for sensitive text |
| Deterministic astrology | Calculation already separated from generation, on the correct side of the line |
| Test discipline | 465 tests across both codebases, all green today |

---

## 4. Mock / planned / dormant capabilities

| Capability | Actual state | Trap |
|---|---|---|
| Reflection scroller | DORMANT, full stack, pre-0009 stance | Reads as shipped in older docs |
| Practice library (12 legacy) | DORMANT, `techniques.json` | Different schema from the breathing set — do not merge |
| Astrology / chart | DORMANT backend, ABSENT in product | `/chart` is never called |
| AI insight card | DORMANT — the only AI path | Legacy shell only |
| Gamification, billing, ecommerce, community, journey, habits, journal, whatsapp | Backend code, never called | Reads as product scope; it is not |
| 4 instrumented test classes | Written, **never executed** | Written against the forbidden emulator target |
| `Dockerfile`, `railway.json` | On disk, **untracked in git** | Committed deploy story ≠ real one |
| `frontend/` (Next.js) | On disk, **0 files in git** | Not a product surface |

---

## 5. Technical debt that directly blocks this vision

| # | Debt | What it blocks | Priority |
|---|---|---|---|
| 1 | **No migration runner** (44 inline boot DDL) | The entire knowledge/provenance architecture | **P0** |
| 2 | **Zero physical-device verification** | Any honest reliability claim; the pilot | **P0** |
| 3 | **FOCUS-1: a delayed START can create a 24-hour focus session** | User trust | **P0** |
| 4 | Unrotated leaked credentials (GitHub PAT, Render key) | Outside testers | **P0 (ops)** |
| 5 | **No `strings.xml`** | International audience; Hindi parents | **P1** |
| 6 | No analytics or crash reporting | Learning anything from a pilot | **P1** |
| 7 | No CI | Cross-agent regressions (two agents, one repo) | **P1** |
| 8 | No release signing; `api.thehybridmonk.com` NXDOMAIN | Any non-sideload distribution | **P1** |
| 9 | Session bridge is in-process only (FOCUS-4) | Background work that needs auth | **P1** |
| 10 | No overnight focus windows (FOCUS-2) | Sleep-preparation, the strongest evening use case | **P1** |
| 11 | SharedPreferences as database | Real observation history | P2 |
| 12 | No object storage | Voice-guided practice | P2 |

**FOCUS-1 in detail**, since it is a live defect rather than a missing feature:
`FocusScheduleReceiver.minutesUntilWindowEnd()` calls `nextBoundary(...)` and treats the result as the
window's end *regardless of which boundary it actually is*. If the exact-alarm delivery slips past the
window's own end (Doze, device off), `nextBoundary` returns the **next occurrence's START**; the
difference is clamped to `24*60`; and `startFocus(1440, …)` is called — which the backend now accepts
because `MAX_FOCUS_MINUTES` was raised to 1440 for the all-day preset. A user can lose their selected
apps for a day.
**Fix:** evaluate `FocusWindowRecurrence.isActiveAt(...)` at delivery and skip a stale START entirely.
Small change, clear test surface.

---

## 6. GPT concepts already technically compatible

These need no architectural change. Detail in the bridge file.

| Concept | Why it already fits |
|---|---|
| **C-01** Contextual Home, one next action | Single-Home shell exists; needs one pure resolver. **No AI required** |
| **C-03** Governed stance → production | Already implemented for breathing content (`reviewedForPublicRelease`, tested) |
| **C-04** Modes not levels | Recommender's hard constraints are explicit-choice-only by construction |
| **C-07** Block-screen transition | **Shipped 25 Sep**, including the user's own when/then text |
| **C-09** Astrology three-layer split | Calculation/generation boundary already drawn correctly |
| **C-13** Practice safety classes | Enforced in substance; needs only a named `risk_class` field |
| Experience before explanation (ST-002) | Matches the breathing selection flow as built |
| Practitioner may disagree (ST-003) | Skip / End / "felt worse" already functional and consequence-free |

---

## 7. GPT concepts requiring architecture changes

| Concept | Change required | Cost |
|---|---|---|
| **C-05** Three-mode Focus | Fix FOCUS-1…6; overnight recurrence; rule ownership; real status model | Medium |
| **C-06** Emergency access | New local-first release sequence with a written conflict contract; **contradicts an accepted decision** | Medium + founder decision |
| **C-11** Internationalization | String extraction; per-locale content with independent review state | Medium, mechanical |
| **C-10** Agency metrics | Event taxonomy; crash reporting; consented export | Low–medium |
| **C-02** Knowledge tables | Migration runner first, then provenance schema | Medium (blocked on P0) |
| **C-08** Messages from Shanivar | Generation, prefetch, caching, moderation, consent, offline fallback | **High** |
| **C-12** Deep personalization | Four-class memory model with correction and deletion paths | **High** |

---

## 8. What should stay research-only for now

| Concept | Why |
|---|---|
| Vector store / RAG | No corpus justifies it. 8→50 curated practices validate the experience with relational provenance alone. Building retrieval first means designing provenance filtering before understanding the queries |
| Persistent / Strong Focus | GPT's own gate is unmet: recovery and persistent state across restarts are unverified. Shipping it puts an unrecoverable lock in front of users |
| Class-3 memory (AI hypotheses about a person) | Needs a founder decision on whether it is acceptable at all. A defensible answer is no |
| Knowledge graph | A graph answers "how do these ideas relate", which no user has yet asked |
| iOS app-blocking | Needs a FamilyControls entitlement review nobody has done. See §16 |
| Push notifications | Standard engagement lever; 0009 bans engagement mechanics. Local scheduling may be the better fit, not merely the cheaper one |
| Community / social features | Backend code exists; nothing in the mission requires it |

---

## 9. Proposed knowledge architecture

**Principle: provenance is relational; retrieval is optional and later.**

Six layers, never flattened. `layer` is a first-class column, not a tag:

```text
L1 PRIMARY_TEXT        canonical reference, original language, no interpretation
L2 COMMENTARY          traditional, attributed to school + author
L3 SCHOLARSHIP         modern academic
L4 CONTEMPORARY        teachers, practitioners, modern literature
L5 SHANIVAR_SYNTHESIS  founder-approved interpretation  ◄── never presented as ancient quotation
L6 PRODUCT_PRACTICE    what a user actually receives
```

Delivery path — and note the direction of the arrow: **only L6 ships to the device**, carrying
references upward rather than embedding the content of higher layers.

```text
editorial (L1–L5, Postgres)  ──build step──►  versioned JSON asset (L6 + source refs)  ──►  app
                                              validated at load, offline, no network
```

### Schema (relational, post-migration-runner)

```sql
sources (                          -- one bibliographic object
  id, layer, work_title, author, translator, edition, canonical_reference,
  original_language, url, publication_year, copyright_status,
  confidence, tradition_id, created_at, reviewed_at, reviewed_by
)
traditions (id, name, family, notes)        -- Sāṃkhya ≠ Pātañjala ≠ Śaiva ≠ Buddhist
claims (                                    -- an assertion attributed to a source
  id, source_id, topic, summary, source_excerpt, interpretation_type,
  disagreement, confidence, approval_status, embedding_version NULL
)
practices (                                 -- L6, the shipped object
  id, title, kind, risk_class, content_version, approval_status,
  allowed_durations, default_duration, requires_* flags, stages_json
)
practice_sources (practice_id, source_id, relationship, adaptation_note, verification)
```

`approval_status ∈ {PROPOSED, REVIEWED, ACCEPTED, SUPERSEDED, REJECTED}` — the C-03 gate, and
**only `ACCEPTED` may appear in a release asset**, asserted in CI.

### Why not one embedding pool

Flattening loses exactly what makes this defensible. A vector search over mixed layers cannot reliably
distinguish "the *Vijñāna-bhairava-tantra* says X" from "a modern teacher says the VBT says X" from
"Shanivar's synthesis is X". That distinction is the product's integrity, and post-hoc metadata
filtering on a flat index is far harder than keeping the layers separate from the start.

### Options

| | **A — Versioned JSON assets only** (status quo, extended) | **B — Postgres editorial + generated asset** | **C — Postgres + pgvector retrieval** |
|---|---|---|---|
| Advantages | Already works and is tested; offline; zero infra; validation at load | Editorial workflow + offline delivery; queryable; review state in data | Scales to a large corpus; semantic search |
| Limitations | Content change needs an app release; no server-side query | Needs migration runner + build step | Solves a problem we do not have; provenance-filtered vector search is the hard part; cost and latency added |
| Complexity | Low | Medium | High |

**Recommendation: A now, B when editorial volume hurts, C only if a real corpus demands it.**
**Why:** the breathing catalogue already proves A at 8 practices; ~50 is the same mechanism. Retrieval
infrastructure built before the queries are understood is infrastructure built twice.

---

## 10. Proposed AI architecture

**Principle: deterministic by default. A model is used only where non-determinism is the point.**

Almost everything GPT describes is achievable without inference. Classify before reaching for a model:

| Interaction | Correct mechanism | Model? |
|---|---|---|
| Which practice to suggest | Deterministic recommender (built) | **No** |
| What Home should offer next | Pure `HomeState` resolver | **No** |
| Whether a practice is eligible/safe | Hard constraints in content metadata | **No** |
| Schedule / time-of-day context | `BreathingSchedule` arithmetic (built) | **No** |
| Chart / transit calculation | `astrology.js` (built) | **No** |
| Practice instruction text | Reviewed catalogue content | **No — never generated** |
| Personalized reflection wording | Generation over a structured record | Yes |
| Deep interpretive synthesis | Generation over explicit source claims | Yes |
| Long-form source analysis | Research-time only, never user-facing latency | Yes, offline |

### Model routing

| Class | Job | Latency budget | Notes |
|---|---|---|---|
| **Deterministic** | Everything in the table above | <16ms | The default. No provider, no cost, no failure mode |
| **Fast** | Short classification, query rewriting | <300ms | Smallest available model; cache aggressively |
| **Reflection** | One personalized message | <800ms perceived — **via prefetch, not raw speed** | Structured output; deterministic fallback mandatory |
| **Deep** | Interpretive synthesis on request | 2–5s with visible progress | Explicit user request only; never on a critical path |
| **Research** | Source analysis | Minutes, offline | Never in the app |
| **Safety** | Content gating | — | **Deterministic rules only.** A model must never be the safety boundary |

### Required before any model reaches a user

1. **A deterministic fallback for every generative path.** `ai.js` already does this correctly
   (rule-based on missing key or timeout) — keep that pattern, and make the fallback *visible in
   metrics* rather than silent.
2. **Structured output**, not free prose, so the app can validate before display.
3. **Prompt versioning** recorded with every stored output.
4. **A content gate** that is deterministic: no generated text containing a practice instruction, a
   medical claim or a physiological promise may render. The breathing catalogue's banned-phrase tests
   are the working precedent.
5. **Cost accounting per route** before launch, not after a bill.
6. **Provider abstraction** at one seam. Today Gemini is called by raw `fetch` in three places; one
   thin client keeps a provider change from being a rewrite.

### Options for the generative layer

| | **A — Pre-generated, human-reviewed pool + deterministic selection** | **B — Live generation with prefetch** | **C — On-demand per interaction** |
|---|---|---|---|
| Advantages | Instant; zero marginal cost; reviewable before anyone sees it; offline; no user context leaves the device | Genuinely per-person; scales in variety | Simplest to write |
| Limitations | Not per-person wording; editorial effort | Cost scales with users; needs consent, moderation, fallback; unreviewed text reaches people | Visible wait on every step — the exact failure the brief names |
| Complexity | Low | High | Low to build, bad to use |

**Recommendation: A first.** **Why:** it tests the actual open question — *do people find these
messages useful?* — with no cost curve, no privacy change and no moderation burden. If A works, B is a
measured upgrade. If A fails, B would have failed more expensively.

---

## 11. Proposed memory architecture

Four classes. The rule that matters: **class 3 never silently becomes class 1.**

| Class | Content | Store | Retention | In prompts? | In analytics? |
|---|---|---|---|---|---|
| **1 · Declared facts & preferences** | Wake time, intended sleep, sound consent, breath comfort, chosen apps, intention label | SharedPreferences → Room | Until changed or deleted | Yes | Never |
| **2 · Practice & interaction history** | Session events: practice id, duration, outcome, entry context | Local, bounded (100 today) | Rolling window; user-deletable | Aggregates only | Counts only, consented |
| **3 · AI-generated hypotheses** | "This person may be avoiding mornings" | **Does not exist. Do not build without a founder decision** | n/a | Only if confirmed by the user | **Never** |
| **4 · Sensitive reflective content** | Free-text reflection | `EncryptedReflectionStore`, AndroidKeyStore AES/GCM | User-controlled | **Only with explicit per-use consent** | **Never** |

### Non-negotiables

- **No hallucinated memory.** A statement about the user must be traceable to a class-1 or class-2
  record. If it cannot be, it is not said.
- **Correction is a first-class operation.** "That doesn't fit me" must change behavior, not just
  dismiss a card. Concretely: write a suppression record the recommender reads — the breathing
  `feltWorseIds` mechanism already does this for one visit and generalizes.
- **Deletion is real.** `BreathingPreferences.clearAll()` empties only its own namespace and a test
  proves it leaves alarms and focus untouched. Every future store needs the same guarantee and the
  same test.
- **`allowBackup="false"` stays.** No cloud restore of practice data.
- **Class 4 never leaves the device by default.** Sending reflection text to a third-party model is a
  material change to the app's posture and needs explicit, per-use, revocable consent.

**On "Shanivar should know the practitioner deeply":** knowing and profiling are the same
implementation with different intent, and only architecture separates them. Declared preference plus
recent history is a surprisingly strong personalization substrate and carries almost no risk. Start
there; prove it is insufficient before building anything that infers.

---

## 12. Proposed personalization architecture

Do not concatenate history into a prompt. Assemble a bounded context with a fixed budget:

```text
┌ STABLE PROFILE ──────── declared only, ~200 tokens ─────────────────┐
│ intention label · wake/sleep (if set) · sound consent · breath      │
│ comfort · preferred anchor · language                                │
├ RECENT CONTEXT ──────── last 3 sessions, ~150 tokens ───────────────┤
│ practice id · outcome · optional usefulness · entry context          │
├ RELEVANT KNOWLEDGE ──── 1–2 claims max, ~300 tokens ────────────────┤
│ retrieved by deterministic rule today, not similarity               │
├ PROGRESSION ─────────── declared mode, ~50 tokens ──────────────────┤
├ OPTIONAL ASTROLOGY ──── explicitly enabled only, ~100 tokens ───────┤
└ CURRENT INTERACTION ─── what they just did, ~100 tokens ────────────┘
                     ≈ 900 tokens, hard-capped
```

A fixed budget is what makes cost predictable and prompt behavior debuggable. Unbounded history is how
personalization becomes both expensive and untraceable.

**Correction loop:** disagreement writes a suppression record scoped to a visit (ephemeral) or a
preference (persistent) — and the user chooses which. Never a hidden score.

**Provenance in output:** any claim shown must carry its source id so "where did that come from?" is
answerable. Structured output, not prose, is what makes this enforceable.

---

## 13. Proposed sadhak progression architecture

Not `Level 1 → 2 → 3`. Independent, mostly declared dimensions:

| Dimension | Values | Source | Notes |
|---|---|---|---|
| Familiarity | new / some / experienced | Declared | Never inferred from sessions |
| Consistency | factual count of recent practice | Derived (factual) | A count, not a judgement |
| Practice tolerance | 1 / 3 / 5 min | Declared | Already implemented |
| Preferred modality | breath / sound / external / body | Declared + observed choice | Already implemented |
| Theoretical curiosity | has opened source details | Derived (factual) | A behavior, not a rank |
| Embodiment comfort | breath comfort tri-state | Declared **only** | Already a hard constraint |
| Reflection depth | declared interest | Declared | Optional |
| Available time | now | Declared per visit | Ephemeral |

The question the engine answers is **"what is an appropriate next experience?"** — never "what level
is this person?". Two structural guarantees, both already true in the breathing code:
`requiresPriorComfort` can be satisfied *only* by an explicit choice, and no engagement signal unlocks
anything. Keep both. No XP, no streak-gated source access, no attainment score — ST-005 and decision
0009 already forbid them and the schema should make them unrepresentable.

---

## 14. Navigation options

| | **A — Cards, resolver-ordered** | **B — Single primary action + disclosure** | **C — Conversational sequence** |
|---|---|---|---|
| Shape | Today's Home, cards ranked by relevance | One action; "something else" reveals the rest | Question → response → action, one screen at a time |
| Advantages | Days of work; reversible; returning users reach utilities directly; ranking proven before anything is hidden | Closest to "the interface disappears"; genuinely one decision | Matches the NOTICE→UNDERSTAND→EXPERIENCE arc most literally |
| Limitations | Still a list to scan | A wrong guess costs more than a list does; utilities become two taps | Slow for someone who just wants to set an alarm; a conversation is a *worse* interface for a utility |
| Complexity | Low | Medium | High |
| Risk | Under-delivers on the vision | Mis-ranking frustrates | Turns a 2-tap utility into a dialogue |

**Recommendation: A now, instrument it, then B for the practice/reflection surface only —
keeping alarm and focus directly reachable.**
**Why:** Shanivar is two different products in one app. The practice side benefits from guided
sequence; the alarm and app-lock side is a *tool*, and a tool that makes you converse before it works
is a worse tool. Do not unify them for aesthetic consistency.

Implementation for A: a pure `HomeState` resolver (no Compose, unit-testable) returning a ranked list.
Do not adopt Navigation-Compose — 7 routes do not justify it, and the current `rememberSaveable`
string + `Crossfade` is adequate until roughly 15.

---

## 15. Messages-from-Shanivar architecture

If built, the state machine must be explicit:

```text
IDLE ──enter──► PREFETCHING(n=3) ──ready──► SHOWING(i)
                     │ fail                    │ advance
                     ▼                         ▼
              DETERMINISTIC_FALLBACK    SHOWING(i+1) ──background refill──► PREFETCHING
                                               │ exit (always available, any state)
                                               ▼
                                             LEFT
```

Requirements, from the brief and from how this fails in practice:

| Requirement | Implementation |
|---|---|
| Next card feels immediate | Prefetch ≥2 ahead; never generate on the advance gesture |
| Exit always available | Leaving is a first-class action in every state, with no confirmation |
| No response required | "Nothing" and no answer are both valid terminal answers |
| Offline works | Pre-generated pool must cover the offline case entirely |
| Failure is invisible | Deterministic fallback text, logged as a fallback |
| Moderation | Deterministic gate before display: no practice instruction, no medical or physiological claim |
| No silent memory writes | A card may not create a class-1 fact |

**The premise worth challenging before any of this is built:** a vertically scrolling card feed is
structurally the interaction pattern of the products Shanivar exists to help people leave, and
decision 0009 explicitly bans a content feed for V1. The mission is "one small intervention, then
leave more present than you arrived." **One card at one well-chosen moment may be the stronger
product and is dramatically cheaper.** That is a founder question, not an engineering one, and it
should be answered before the prefetch machinery is written.

---

## 16. Clock / Focus Mode feasibility by platform

### Android — what is actually true

| Mechanism | Reality |
|---|---|
| `UsageStatsManager` | Special access via Settings deep link. Supplies foreground-app information. **It is not an API that makes another app impossible to open** |
| Detection | Polling at 1500ms → a visible flash of the blocked app is expected, and is disclosed to the user |
| Overlay | `SYSTEM_ALERT_WINDOW`; also what exempts the service from Android 10+ background-activity-start restrictions |
| Foreground service | `specialUse` type + per-service subtype justification. **Play Console review will evaluate this**, and this is one of the fastest-moving areas of Android policy |
| Exact alarms | `SCHEDULE_EXACT_ALARM` (revocable on API 33+) and `USE_EXACT_ALARM` (restricted to qualifying apps) both declared |
| Full-screen intent | `USE_FULL_SCREEN_INTENT` is **auto-revoked on Android 14+** without a completed Play Console "core functionality: alarms" declaration |
| Known bypasses | Force-stop, permission revocation, uninstall, browser version of the site, a second device |
| Declined | Device Admin and accessibility-service enforcement (decision 0002) — stalkerware-adjacent and a removal risk |

**Honest ceiling: "maximally inconvenient to bypass," never "impossible."** That is already the
documented stance and it must survive contact with marketing.

### iOS — no claim can be made

The right mechanism would be **FamilyControls / ManagedSettings / DeviceActivity**, which is an
entitlement-gated framework with a different authorization model, different capabilities and a
different review path from Android overlays. **It is not a port; it is a separate implementation.**
Nobody has done the entitlement review. Any iOS timeline today would be invented.

### Web / desktop

Not applicable. There is no web client (the on-disk Next.js app is untracked), and browser-based
blocking cannot restrict native apps.

### Mode feasibility

| Mode | Feasible today? | Blocker |
|---|---|---|
| Scheduled Focus | **Yes, with defects** | FOCUS-1 … FOCUS-6 |
| Contextual Focus | **Yes** | Thin layer over the manual session; mostly labelling |
| Overnight windows | **No** | `startTime >= endTime` returns null (FOCUS-2) |
| Persistent / Strong | **No — and should not ship** | Recovery and persistent state across restarts are unverified. GPT's own gate |
| Emergency access | **Yes, technically** | Needs founder acceptance (C-06) + a written conflict contract |

---

## 17. Safety architecture

Two independent safety systems. Do not merge them.

**A · Enforcement safety** (accepted, in force):
alarm must never ring after its task is done; must always be escapable; never claim enforcement we
cannot deliver; no Device Admin. Structurally guaranteed for breathing by a test that fails the build
if any `Breathing*.kt` file so much as names an enforcement type.

**B · Practice safety** — formalize what currently holds by construction:

| Risk class | Auto-deliverable? | Gate |
|---|---|---|
| LOW_RISK | **Yes** | Ordinary attention, grounding, body awareness |
| CONTEXT_REQUIRED | No | Explicit user confirmation of setting |
| CAUTION | No | Explicit acknowledgement + contraindications shown |
| QUALIFIED_GUIDANCE | No | Reference only; no self-guided execution script |
| DO_NOT_AUTO_DELIVER | Never | Research record only |

Rules: **only LOW_RISK may be recommended automatically**; no engagement signal may escalate anyone; a
model may never make an eligibility decision; content carrying prescribed breath timing must fail
validation (already true — the model has no field to express it). Add `risk_class` explicitly and
assert the gate in CI.

---

## 18. Data / privacy model

| Data | Location | Leaves device? | Encrypted | Notes |
|---|---|---|---|---|
| Account email, password hash | Postgres | n/a | bcrypt | Server-side |
| JWT | Memory + prefs | Sent as bearer | TLS | In-process bridge gap (FOCUS-4) |
| Alarm/focus schedule | Local + Postgres | Yes | TLS | Needed server-side for cross-device |
| When/then commitment | Local + Postgres | Yes | TLS | Stored **verbatim**, never generated |
| Practice journal | Local only, ≤100 rows | **No** | No | No feeling labels, no free text, no package names |
| Breathing preferences | Local only | **No** | No | Own namespace; deletable without touching alarms |
| Feelings, comfort answers, safe-setting confirmation | **Ephemeral** | **No** | n/a | Never persisted, by design |
| Reflections (dormant) | Local file | Only if the dormant feature is revived | **AndroidKeyStore AES/GCM** | Class 4 |
| Blocked package names | Local + Postgres | Yes | TLS | Must never appear in a share artifact |
| Foreground app detected | **Memory only** | **No** | n/a | Never stored, never uploaded |

**Principles to hold:** minimize before encrypting; ephemeral by default for anything about inner
state; `allowBackup="false"`; deletion per namespace with a test proving isolation; and no new
permission acquired to enrich research data.

**The live tension, named plainly:** the current app sends almost nothing off-device. Personalized
generation would send user context to a third-party model provider. That is a real change of posture,
not an implementation detail, and it deserves explicit consent rather than a privacy-policy line.

---

## 19. Analytics philosophy

**Do not optimize time-in-app.** Candidate signals, all reflecting agency:

| Signal | Measurable today? | How |
|---|---|---|
| Practice started / completed | Yes (local journal) | Already recorded |
| Ended early by user | Yes | `ENDED_USER` vs `ENDED_TIMER` — already distinguished |
| "Felt worse" | Yes | Already recorded |
| Focus period completed | Partially | Needs the real status model (FOCUS-6) |
| Returned to intended task | **No** | Self-report only — see below |
| Voluntary repeat | Yes | Journal |
| User correction | Yes | Suppression records |
| Successful skip | Yes | Skip is already consequence-free |
| Useful departure | **No, and possibly never** | Absence of data is not a positive signal |

**Two rules.** First, **never score silence as success** — a person who stopped may have grown
independent, or been failed by a bug, and we cannot tell. Second, and this is the uncomfortable one:
*"did they return to their task?"* is only measurable by watching what they do after they leave.
**Surveilling people more closely to prove we help them disengage is self-undermining.** Self-report
is weaker evidence and the right instrument.

**Minimum viable, in order:** crash reporting (P1 — today a failed alarm leaves no trace anywhere);
then a local event log with consented export; then aggregation only if the pilot shows which events
matter. **No third-party analytics SDK** — shipping a tracking SDK inside an app about reclaiming
attention costs more in credibility than it returns in insight.

---

## 20. Internationalization implications

**Two problems, routinely conflated.**

**UI localization** — status: nothing exists. No `strings.xml`; `res/values/` holds only `colors.xml`.
Every string is hardcoded Kotlin. `preferred_language` persists server-side and changes no client
string. Extraction is mechanical and gets more expensive every week the surface grows. **P1.**

**Content translation** — status: partially correct already. The breathing catalogue declares
`scriptLanguage` and an explicit `languageFallback`, and a test asserts the app never claims Hindi
guidance it does not have. Per-locale variants should key to the same practice id with **independent
approval state**, reusing the C-03 gate.

Hard rules: **no machine translation of practice instructions** — a mistranslated "stop if you feel
dizzy" is a safety failure; review is per exact content version; transliteration is stored metadata,
never generated at runtime; and English is not canonical where the primary source is not English.

---

## 21. Cost and latency risks

Order-of-magnitude reasoning, not a quote. **Verify current Gemini pricing before committing** — I am
not treating my training-data prices as current.

| Scenario | Model calls/day | Dominant risk |
|---|---|---|
| Today | **0** | None. No AI cost exists |
| One reflection per user per day, 1k users | ~1k | Negligible |
| Card feed, 5 cards, 10k users | **~50k** | This is the design that changes the cost conversation by an order of magnitude |
| Deep synthesis on request, 1% of 100k users | ~1k long-context | Long context dominates unit cost |

**Latency is the harder problem than cost.** Render free tier alone is ~23s cold start, before any
model call. That is already unacceptable for a first mobile request and must be fixed independently of
AI.

Controls, in order of effectiveness: **pre-generation** (converts per-view cost to one-time editorial
cost, and is why §10 recommends Option A); **caching** by (practice, context-bucket); **fixed context
budget** (§12, ~900 tokens); **cheapest adequate model per route**; **a hard per-user daily cap** with
graceful deterministic fallback; and **cost per route recorded from day one** rather than reconstructed
from a bill.

---

## 22. Technical spikes required

Small, measurable, time-boxed. Each resolves a real uncertainty instead of an argument.

| ID | Question | Method | Success measure | Box |
|---|---|---|---|---|
| **S-1** | Does the focus engine survive real-world conditions? | Physical device: reboot, cold process, late delivery, revoked permission, offline, DST, timezone change, expired credentials | No false Active state; no missed release; **no START outside an active window** (FOCUS-1) | 2 days, **P0** |
| **S-2** | Do generated reflections need live generation to feel good? | Build both arms: pre-generated pool vs 3-card prefetch with real Gemini calls. Log p50/p95 latency, token cost, and blind-rated quality | If A is rated within noise of B, **A wins and C-08 gets dramatically cheaper** | 3 days, P1 |
| **S-3** | Can essential access be released safely, offline, mid-crash? | Induce process death during the release sequence; verify idempotent recovery and that the next boundary does not re-lock | 100% recovery; zero server dependence | 2 days, P1 |
| **S-4** | What does a migration runner cost here, with 44 inline DDL statements live? | Introduce a runner; baseline the existing schema; add one real migration | Existing tests still pass; rollback demonstrated | 2 days, **P0** |
| **S-5** | Is the 1.5s detection interval acceptable to real users? | Device test across 2+ OEMs; measure actual flash duration and battery cost | Users describe it as acceptable; battery impact measured, not assumed | 1 day, P1 |
| **S-6** | How long does string extraction actually take? | Extract one screen end-to-end, measure, extrapolate | A credible estimate for the whole surface | 0.5 day, P1 |

**S-1 and S-4 are P0 and independent — they can run in parallel.**

---

## 23. Roadmap

### P0 — Foundational / expensive to reverse
1. **Fix FOCUS-1.** A user can lose their apps for 24 hours. Days of work, not weeks.
2. **Physical-device verification (S-1).** The eight-check protocol has never run. Everything else is
   built on an unverified foundation.
3. **Migration runner (S-4)** — before the first knowledge table, not after.
4. **Rotate the leaked credentials.** Ops, not code, and still P0 before outside testers.
5. **Founder decision on C-06 (emergency access)** — it changes an accepted enforcement decision.

### P1 — Validate early
6. String extraction to resources (S-6) — blocks the whole international direction.
7. Crash reporting — today a failed alarm leaves no trace.
8. CI running `npm test` + `testDebugUnitTest` + `lintDebug` — two agents share this repo.
9. `HomeState` resolver (C-01) — highest-value GPT concept, needs no AI.
10. Overnight focus windows (FOCUS-2) — unlocks sleep preparation.
11. FOCUS-6 real status model — stop reporting "scheduled" as "protected".
12. Confirm the production `GEMINI_API_KEY`, then run S-2.
13. Release signing + resolve `api.thehybridmonk.com`.

### P2 — After validation
14. Contextual Focus labels (C-05 mode 2).
15. Local event log with consented export (C-10).
16. Knowledge provenance tables (C-02 Option B), once migrations exist.
17. `risk_class` field + CI gate (C-13).
18. Sadhak preference dimensions (C-04).
19. Room, when observation history becomes a feature.
20. Local feature flags for counterbalanced pilot arms.

### P3 — Later
21. Messages from Shanivar (C-08) — **after** the feed-premise decision.
22. Astrology revival with the interpretation layer (C-09).
23. Object storage + voice guidance.
24. UGC sharing (C-14), after C-10 is trustworthy.
25. iOS — only after a FamilyControls entitlement review.
26. Vector retrieval — only if a real corpus demands it.

**Nothing GPT has proposed belongs in P0.** P0 is entirely defects, verification and one decision.
That is the honest state of the project, and holding that line is the most useful thing this document
does.

---

## 24. Founder decisions needed

| # | Decision | Why it cannot be made by an agent | Blocks |
|---|---|---|---|
| 1 | **Emergency "essential access" behavior** — exact mechanics | Contradicts the accepted "no bypass affordance" decision. A safety property, and the failure mode is someone unable to reach something urgent | C-06, and Focus messaging |
| 2 | **Does a card *sequence* survive 0009's feed ban?** One card at one moment, or a scrollable feed? | 0009 bans a content feed; a vertical card feed is structurally the pattern Shanivar helps people leave | C-08, and a large cost difference |
| 3 | **May the app hold AI-generated hypotheses about a person's inner state?** | An app-held theory about someone's psyche is an ethical question, not a technical one. A defensible answer is no | C-12, class-3 memory |
| 4 | **Persistent Focus scope**, and departing from one-schedule V1 | Changes the enforcement promise; GPT's own gate is unmet | C-05 mode 3 |
| 5 | **First locale, and who reviews it** | Needs a named human reviewer; Hindi is already gated on a comprehension review | C-11 |
| 6 | **Is production `GEMINI_API_KEY` set?** | Needs Render dashboard access | Every AI estimate |
| 7 | **Play Console declarations** for `USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, two `specialUse` FGS | Needs the developer account | Any store release |
| 8 | **Keep, freeze or delete the 15 unused backend routers** | Product scope, not engineering | Repo clarity, attack surface |

---

## 25. Explicit limitations of this document

1. **Nothing here is device-verified.** Every behavioral claim is source inspection plus JVM and
   Robolectric tests. A Robolectric test proving a tone player received `stop()` is not evidence a
   speaker went quiet.
2. **I cannot confirm production environment variables**, including whether Gemini is live.
3. **I did not assess iOS.** A FamilyControls claim without an entitlement review would be invented.
4. **Cost figures are order-of-magnitude reasoning, not quotes.** Verify current pricing.
5. **I did not evaluate GPT's philosophical or textual research on its merits.** That is outside my
   scope; I assessed only what it implies for engineering. Where I flagged broken links in
   `SHANIVAR_STANCE.md`, that is a governance observation, not a judgement on the content.
6. **Latency budgets in §10 are targets, not measurements.** S-2 exists to replace them with data.
7. **No Play Console state is known** — declarations may already exist or may have been rejected.
8. **The `frontend/` directory is untracked**, so I could not assess it as a product surface and have
   treated it as absent.
9. **This is a snapshot.** Two agents share this repo with no CI. Re-verify before relying on any
   figure here.
10. **I have not built any of the above.** This document is architecture and sequencing only; the one
    thing I changed today was writing these three files.

---

## Working agreement

**GPT expands the possibility space. Claude makes it buildable. The founder decides what it becomes.**

When GPT proposes something, it arrives here as a bridge entry with a class, options and a
recommendation — not as a question sent back. When I find a limitation, the entry states the
limitation, why it exists, the nearest viable implementation, and the experiment that would resolve the
uncertainty. Three of fourteen current concepts need a founder decision; the rest can proceed.
