# Sankalp and the Sittings: an initiation back into the body (design addendum)

- **Date:** 2026-10-09
- **Status:** **PROPOSED.** A design addendum, not a decision and not a build authorisation. Decision 0013 is still PROPOSED (`docs/decisions/0013-sadhana-progression-and-the-gamification-question.md:4`). No app code changes here.
- **Extends:** `docs/strategy/shanivar-sadhana-layer-above-player-proposal.md` §2 (sittings, `:47-82`) and §3 (Sankalp, `:86-107`). Not a restart.
- **Prepared by:** `app.shanivar` Grok Bot. Codex integrates, the founder accepts (`AGENTS.md:7-9`).
- **Read first** (`AGENTS.md:15-19`): `AGENTS.md`, `SHANIVAR_STANCE.md` (ST-001, 003, 004, 005, 008, 009), decisions 0009/0010/0011/0013/0014, `research/deep-foundation/PRACTICE_SAFETY.md`, `shanivar-vbt/content/breathing-mvp-v1.json`, `BreathingScreen.kt`, `BreathingSessionOwner.kt`, `BreathingSessionState.kt`, `BreathingTonePlayer.kt`, `ai_org/agents/{embodiment_practice_translator,founder_second_brain,instagram_keeper}.md`, website rewrites (`ebba45e`). The website is only a tone reference.
- **Labels:** **D** decided or built, **P** proposed here, **F** needs a founder decision, **S** suggestion or opinion, not a fact.
- **Founder brief (2026-10-09, summarised):** Sankalp takes the person toward their higher self. Bring them briefly into a clear state to see where they want to reach. Make the Sittings special: centering, presence, going inside, being in the body. Wide and intense, with audio and/or visual, possibly AI video, 3D or spatial audio. A first version "tonight". This is direction (D), not an accepted decision (`founder_second_brain.md:28`).

---

## 0. Two honest reframings (P)

| Brief says | This doc does | Why |
|---|---|---|
| "Higher self" | UI says **"where you want to reach"**. The app never names, voices or describes a higher self | Metaphysical claim, and a fabricated persona is out (`SHANIVAR_STANCE.md:13`). Purpose stays the person's own (ST-003, `:81-106`; 0010 "nothing generated", `0010-…md:37-40`) |
| "Initiation", "intense" | Internal name **Initiation Sitting**. UI name **"Your first sitting"**. Intensity is *attention*, not stimulation | An initiation implies rank or status, but depth is chosen, not earned (ST-005, `:145`). Intensity is not rewarded (`PRACTICE_SAFETY.md:24`) |

---

## 1. Sankalp "moment": 5 screens (P, text-first, ~3 min)

**Principle:** invitation, not instruction. Each screen has Skip and End. The only text the app composes is the frame. The line is the person's, stored verbatim and never edited or suggested (D, 0010:37-40). No scoring, no sync, no "did you keep it". 60 characters, the same cap as `FocusScheduleCard.kt:246` (D). The text field is the only input.

How a person gets to "still and clear" without a persona: **orient, ground, narrow, ask one question, write.** The stillness comes from the body (screens 1-2). The question comes from the person (screens 3-4).

| # | Screen | Exact on-screen copy (first version) | Button(s) |
|---|---|---|---|
| 1 | Stop | **Stop for three minutes.**<br>Sit somewhere safe. Put the phone where you can still read it.<br>You can end this at any time. | Begin · Not now |
| 2 | Arrive in the body | **Feel what holds you up.**<br>The chair, the floor, your own weight.<br>Leave your breathing alone.<br>Look around the room once. | Continue · Skip |
| 3 | Clear the view | **Let the noise be noise.**<br>Thoughts will keep running. You don't have to stop them.<br>Stay with the weight under you, and let them run. | Continue · Skip |
| 4 | One question | **Where do you want to reach?**<br>Not what you should want. Not what is realistic.<br>Take as long as you like. Nothing is generated for you here. | Continue · Skip |
| 5 | Your line | **Write it in your own words.**<br>One line. 60 characters. [text field, hint: your words]<br>It stays on this phone. Nobody scores it. Edit or clear it whenever you like. | Keep · Clear · Leave it blank |

Rules (P): 2 and 3 are 20-30 s of reading time each and are not timed breath. The moment never prescribes a breath, a count, an eye-closing instruction (eyes open is allowed, `breathing-mvp-v1.json:41` `eyesOpenAllowed`) or a feeling to reach. It never says "higher self", "deepest", "manifest", "transform", "unlock" or "journey". Screen 5 ends with the app's existing close: "Look around when you are ready" (D, `natural_anchor` closing text).

Where the line shows later (D, base proposal `:97`): on the sitting's Ready and End screens, word for word. Nowhere else, and no notification.

---

## 2. Initiation Sitting v1 (P): arrive, one practice, close

A fixed three-step first sitting, about 7 minutes. It uses the base proposal's sitting shape (Arrive → Practice → Close, `:53-57`) with the middle step being one existing practice.

| Step | Content | Time | Source |
|---|---|---|---|
| Arrive | Moment screens 1-3 (new frame text, not a practice) | ~1 min | P, new text |
| Practice | One reviewed, unpaced practice from the catalogue (below), person-chosen. Default 180 s (D, `BreathingSessionOwner.kt:16`) so it counts as practised (`MIN_PRACTISED_SECONDS = 90`, `BreathingSessionState.kt:198`) | 3 min | D |
| Close | Moment screens 4-5 (question and line), then the existing End screen ("However that felt, it's information. Nothing to fix.", `BreathingScreen.kt:23`) | ~2-3 min | P |

### Which existing practice fits

Honest finding: **no catalogue practice is a body scan.** The nearest are ones that point attention at support and contact. A real body practice is a new script, and Phase 1 bans new scripts (`CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:53`).

| Practice (JSON line) | Body fit | Needs | Verdict |
|---|---|---|---|
| `external_grounding` "Notice the room" (`:479`) | "support beneath you", no breathing exercise | Nothing | **Default.** The equal external anchor (ST-004 `:135`; `PRACTICE_SAFETY.md:15-16`) |
| `emotion_space` "Make room for the feeling" (`:290`) | "support of your chair or the floor" | Nothing | Good alternative. Skip if the person is upset: it invites naming a feeling (`PRACTICE_SAFETY.md:26`) |
| `natural_anchor` "Arrive with your breath" (`:31`) | Breath as anchor | `requiresBreathComfort` | Offer only after "breathing feels comfortable" is self-confirmed (`BreathingPreferences.kt:27-28`) |
| `breath_turn` (`:97`) | Subtle breath attention | breath comfort **and** prior comfort | Not for a first sitting |
| `hum_listen` (`:162`), `sound_listen` (`:354`) | Sound | sound consent | Not for a first sitting |
| `sleep_threshold` | Sleep | sleep intent | Never. Sleep is last-only and has no Close (D, base `:57`) |

### Safety rules that apply (D unless marked)

| Rule | Source |
|---|---|
| Stationary, safe setting. Not while driving, cycling, swimming or operating equipment | `PRACTICE_SAFETY.md:32` |
| End always reachable, no confirmation maze. Pause, End and the safety note stay on screen | `PRACTICE_SAFETY.md:34`; `BreathingScreen.kt:330-341` |
| No unpaced → paced. No `prescribedBreathPhases`, no retention; the validator refuses them | `BreathingCatalog.kt:106-110`; 0011:16 |
| Eyes open allowed. No trauma disclosure, no memory revisiting, no feeling target | `PRACTICE_SAFETY.md:26` |
| No "dizzy → push through". Copy must say stop is a valid response | `PRACTICE_SAFETY.md:34` |
| No claim of treatment, clinical effect, "nervous system reset" or awakening | `PRACTICE_SAFETY.md:49-51`, ST-004 `:139` |
| No unlock, no streak, no completion badge, no alarm or focus substitution | ST-005; ST-006 `:201`; `BreathingSessionOwner.kt:23-38` |
| New frame text counts as a script: a qualified human reviews it before **public** release | `PRACTICE_SAFETY.md:41` (P: internal test build before then) |
| Sankalp text kept out of the breathing journal | `BreathingPreferences.kt:15-18` |

---

## 3. Media options (S = opinion, F = fact)

Evidence note: F: the catalogue has no voice audio (`rules.defaultVoiceAudio: false`, JSON `:9-17`) and only one synthetic tone (`BreathingTonePlayer.kt:26`, played once, low volume, media stream, never a loop). S: for "spatial", "7D", "8D" and "binaural for higher states" audio, I found no good evidence for the transcendent claims. Treat them as marketing terms. Binaural-beat research on anxiety and attention is small, mixed and not technique-specific (cf. `SHANIVAR_STANCE.md:129`). Not independently verified in this pass.

| Option | What it adds (S) | Risks | Cost / effort | Tonight? |
|---|---|---|---|---|
| **Text + silence + existing tone** | Calm, honest, accessible, zero new claims | Least "special" | Hours: strings and a composable | **Yes** |
| Recorded human voice guide (the founder's own, short) | Warmth and presence without a persona. Best for "being in the body" | Must be a named human voice, not a guru. Audio over sleep or driving. Needs sound consent (the pattern exists: `requiresSoundConsent`) | Half a day to record and cut. ~5 clips, bundled | **Stretch only**, if the script is ready |
| Synthetic TTS voice | Cheap to try | Reads as a coach persona (ST-003, `:13`). Quality and licence unchecked | Low-medium | No |
| Ambient bed (room tone, soft drone, no beats) | Marks the sitting as "different" | Loops can lull people (sleep). Volume and phone speaker. Keep stationary only | Medium: asset, new playback path, one-bed rule | Not tonight |
| Binaural or "spatial" 3D / "7D" | Novelty | Needs headphones, can cause dizziness, unproven claims. Marketing terms must not appear in the UI (`PRACTICE_SAFETY.md:49-51`) | Medium-high, plus review | No. Later, optional, **no claims** |
| Simple visual: a slow, low-contrast shape or fade between screens | Focuses attention. Shows the sitting begun | Photosensitivity (no flashing, no strobing, no sudden full-field changes), motion sickness, and reduced-motion must disable it | Low-medium | Static fade only, no motion loops |
| Generative visual | "Wide" feel | Same plus battery, heat, a keep-screen-on question (`keepScreenOn` is false) | High | Later |
| AI-generated video | Strong "wow" | A generated face or voice is a fabricated persona (ST-003 `:13`). Compute cost, review per clip, Play policy, uncanny risk. Weak fit for stillness | High | **No.** Maybe abstract, face-free, bundled, reviewed clips later (S) |

---

## 4. Phased plan

### A. One night (P, smallest useful: existing player, text, existing sound)

Internal test build only. **It does not make the app public.** This is not "ready" in the release sense: device checks and review are still owed (`PRACTICE_SAFETY.md:41,47`).

| What | Touch point |
|---|---|
| Entry button "Your first sitting" on the Sadhana tab, shown until used or dismissed. No streak, no reminders | `BreathingScreen.kt:84` (`PracticeSelection`) |
| Hard-coded 3-step state machine above the owner (not a general `sequences` type) | New `InitiationSitting.kt`, calling `BreathingSessionOwner.choose()`/`start()` (`:147`, `:154`) |
| Default practice `external_grounding` at 180 s | `defaultBudgetSeconds` (`BreathingSessionOwner.kt:27`) and `SADHANA_DEFAULT_SECONDS` (`:16`): no change |
| Frame screens 1-3 and 4-5 as plain composables with Skip and End | New file. Reuse `Heading`, `PrimaryAction`. Safety note from `BREATHING_SAFETY_NOTE` (`BreathingScreen.kt:418`) |
| Sankalp store (60-char verbatim, own Clear, never synced) | New `SankalpStore.kt`: a **separate** SharedPreferences, not `BreathingPreferences` (`:15-18`) |
| Show the line on the End screen | `PracticeEnded` (`BreathingScreen.kt:345`) next to "Done" (`:381`) |
| Journal rows: normal `BreathingRecord` only. No sitting field tonight | `BreathingSessionState.kt:162-195` unchanged |
| Tests | Validator-style test: no forbidden words in copy; Sankalp never in journal; End reachable on every screen |

### B. Next

Voice guide (one named human, bundled clips, sound-consent gate); ambient bed (one at a time, stationary, volume capped); optional `sittingId` on `BreathingRecord`; generalise to 1-3 practice sittings (base `:53-70`); a Routine "Open sitting" link (base `:79`).

### C. Later

A real body-scan script (new `PRACTICE_LIBRARY.md` entry through the embodiment translator: source, original script, stop/skip, claim limits, `embodiment_practice_translator.md:11`); abstract visual layer; spatial audio only if the evidence and review clear it; face-free AI visual only with a founder decision; A7 curation (base §6).

### Decisions needed

| Item | Why |
|---|---|
| **F:** Accept 0013 Option A, or a narrower "first sitting" exception | 0013 says nothing beyond existing Steady is built until resolved (`0013:4`; `0014:18`). A hard-coded first sitting is already past that line |
| **F:** Frame text exemption from "no new scripts" for an internal build | `CLAUDE_ROUTINE_V3_BUILD_HANDOFF…:53` |
| Amend 0011 only for a *new* practice type, e.g. a body scan or an Ujjayi-type recipe | 0011:16 (unpaced only) holds for tonight. No amendment needed |
| **F:** Sankalp storage, naming and a privacy-page line | `privacy.html:71-72` says "no analytics" and local. A local-only store keeps that true, but the page should name it |
| **F:** Sound or visual beyond text | New claims and new review |

---

## 5. Three-way test (S: judgement, not evidence)

| Element | Interesting | Behaviourally effective | Commercially viable |
|---|---|---|---|
| Sankalp moment (text) | High. A clear frame and the person's own line | Plausible. Writing a goal helps; the when-then form has the evidence (0009, 0010). The "clear state" effect is unproven | Medium, on brand |
| Initiation Sitting v1 | High | Unknown. Needs the body practice to land. First 3 min practice counts (D) | Medium. A good first-run hook if honest |
| Voice guide (named human) | High | Probably the strongest lift in presence (S, unproven here) | Medium. Recording cost is low |
| Ambient / spatial / "7D" | Medium | Low-unknown. Claims unsupported | Medium as marketing, **risky** as claims |
| Visuals | Medium | Low-unknown. Can distract from the body | Low-medium |
| AI video | High | Low. Pulls attention outward, the opposite of the brief | Medium-high wow, high claim and persona risk |
| Higher-self framing | High for founder | Unknown. Self-concept claims | Unclear. Metaphysical copy narrows the audience |

---

## 6. Conflicts found between files

| # | Conflict | Files | Resolution proposed |
|---|---|---|---|
| 1 | "Higher self" / guided-by-the-app vs no fabricated guru, purpose stays the person's | Brief vs `SHANIVAR_STANCE.md:13`, ST-003 `:81`, 0010:37-40 | Reframe (§0) |
| 2 | "Initiation" / "special" implies status vs depth not earned, no unlock | Brief vs ST-005, 0013:75-76, 0014:58 | Call it "first sitting"; no gating |
| 3 | "Intense", altered-state feel vs no rewarding intensity; unattended | Brief vs `PRACTICE_SAFETY.md:24,21` | Intensity = attention only |
| 4 | "Ready tonight" vs 0013 PROPOSED, no new scripts, public release gates | Brief vs `0013:4`, handoff `:53`, `PRACTICE_SAFETY.md:41` | Internal build only |
| 5 | Body-oriented sitting vs no body-scan practice exists | ST-004 / ST-009 #5 (`:295`) vs `breathing-mvp-v1.json` | Use external-grounding; script later |
| 6 | Base proposal §1 says the 60 s default conflicts with the 90 s rule; the code now defaults to 180 s from Sadhana | Base `:30-43` vs `BreathingSessionOwner.kt:16,27` | Base is superseded on this point (founder choice, 2026-10-06). Treat the conflict as resolved |
| 7 | "Sankalp" names this feature and the founder's second-brain agent *Sankalpa* | `founder_second_brain.md:1-3` | Default label "Intention"; Sankalp optional (base `:105`) |
| 8 | A "journey/wow" media layer vs ST-008 useful departure | ST-008 `:241` | Keep it short and skippable; no re-engagement |
| 9 | Repo state: the brief says 3 local commits ahead, `git status` shows 5 (`main...origin/main [ahead 5]`) | Request vs repo | Left untouched; not pushed |

---

## 7. Top 5 questions for Shubham

1. Is it OK to call it "your first sitting" and "where you want to reach" in the app, with "Sankalp" and "initiation" kept for the optional label and the story?
2. Is tonight an internal test build (text + existing tone only), with the voice, ambient and visual layers after review?
3. Will you record the guiding voice yourself (a named human, not a character)?
4. Do you accept a hard-coded first sitting now, ahead of 0013 and the general sittings type?
5. Should the body practice be `external_grounding` (exists today), or do you want a new body-scan script written and reviewed first?
