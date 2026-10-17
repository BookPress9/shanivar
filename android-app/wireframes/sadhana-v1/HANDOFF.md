# Sadhana experience — implementation handoff

Status: founder-review wireframe. The two-destination shell is accepted, while Sadhana progression remains a founder collaboration area. This prototype implements the repository’s recommended progression-without-status direction and stable capability spine for review; it is not a public practice release.

## Experience architecture

| Surface | User purpose | Evidence created |
| --- | --- | --- |
| Sadhana | Enter today’s chosen practice quickly | none until practice begins |
| Path | Understand the current capability invitation | none; viewing is not progress |
| Practice detail | Know demand, duration, source, choice and boundary | selected options only |
| Prepare | Confirm suitable setting and choose breath or external anchor | explicit user choice |
| Player | Practise with diminishing guidance | active foreground seconds, pauses and end reason |
| Return | End without performance theatre; reflect optionally | completed/ended + optional self-report |
| Record | Inspect factual activity by evidence type | no composite score |
| Without app | Put the player away; optionally record later | self-reported only |
| Study | Read sources and disagreements | opened source; never readiness |

## Living Spine architecture

The spinal form is the primary information architecture, with three deliberately separate layers:

1. **Anatomical orientation:** a stylized vertical body reference only. Do not attach chakras to nerves, glands, vertebrae or measured physiology without specific evidence. The UI must say it is not an anatomical scan.
2. **Tradition map:** named chakra/centre records, each tied to a named system, primary passage or qualified secondary source, edition and access depth. There is no universal chakra map; a contemporary seven-centre overview is one selectable teaching map.
3. **Practice trace:** app-observed attempts, active minutes and self-reported independent practice. It may illuminate the central bronze trace but never display “energy level,” blockage, purity, kundalini movement or chakra health.

Each centre drawer needs: display name and transliteration; map/tradition ID; symbolic location as described by that source; source and passage; interpretation; what Shanivar adapted; practice status (`released`, `study_only`, `qualified_context`, `research_only`); evidence type; and explicit claim limits. Until source records are passage-verified, the centre remains a study waypoint and cannot launch centre-specific practice.

The current repository supports the boundary more strongly than the detailed map: `TANTRA_PRACTICE_MAP.md` allows subtle-body visualization as scholarly/lineage context while excluding chakra diagnosis, energy-ascent coaching and kuṇḍalinī induction. `research/sadhak-inquiry/` records that the commercial seven-colour model is not a validated diagnostic method or universal historical canon, and that exact source passages still need verification. Product implementation must preserve that state rather than silently filling gaps.

The vagal rendering is a separate modern learning layer. Its glowing/dimming rhythm may respond only to explicit interface state such as the user choosing `activated`, `mixed`, `steady`, or `low energy`. Store this as `self_reported_check_in`; do not label it vagal tone, autonomic measurement, dysregulation diagnosis or achieved regulation. The visual target “steadier” expresses intention. Practice completion cannot change it automatically. Any future sensor integration needs its own evidence, consent, device validation and product decision.

Knowledge chambers can visually reveal with practice history to create discovery, but passage/source access remains available by choice. A reveal means “context presented,” not “energy unlocked.” Never make activity the gate for health information, exact source notes, stop guidance or research-only status.

## Capability and progression rules

`Arrive`, `Return`, `See`, `Choose`, and `Carry` are stable capabilities, not levels. UI may show a current invitation based on explicit varied evidence, but must label what produced it and allow any earlier practice. Long practice, streaks, unusual experiences or strong feelings never establish capability or readiness.

For a first pilot, capability suggestions should remain deterministic and auditable:

```text
Arrive: user can begin/end a bounded foundation session and identify alternatives.
Return: explicit attempt at one ordinary return cue.
See: completed reviewed event/story/choice exercise.
Choose: connected one reviewed challenge to a user-selected Routine boundary.
Carry: voluntary independent-practice report or decision to reduce prompts.
```

These are product interactions, not proof of inner ability. Missing evidence stays missing. Provide a manual “use fewer prompts” control at any time; an experienced practitioner must not grind earlier content. Return to earlier guidance carries no loss.

## 33-segment practice-current logic

The Living Spine uses 33 visible segments grouped as C7, T12, L5, S5 and Co4. This is a Shanivar interaction model based on common vertebral segmentation; it is not the chakra count, not a claim from *Kundalini Tantra*, and not a medical model of energy flow.

```text
segment = min(33, 1 + completedReviewedAttempts + explicitIndependentReports)
```

- `completedReviewedAttempts` includes only attempts reaching their intended active duration with an exact released content version.
- `explicitIndependentReports` includes user-created records marked self-reported. Users can edit/delete them; recalculation must follow.
- Paused, ended, interrupted, browsed or source-open events do not advance a segment.
- Missed days never subtract segments. There is no streak.
- Duplicate/offline events use UUID/idempotency reconciliation before advancing.
- The base begins at segment 1 so the map is visible before any completion.

Current proposed context thresholds are Mūlādhāra 1, Svādhiṣṭhāna 5, Maṇipūra 11, Anāhata 17, Viśuddha 24, Ājñā 29 and Sahasrāra 33. These are visual pacing choices for the prototype, not historical vertebral correspondences. Brahma, Vishnu and Rudra granthi may be shown as source-labelled study gates near the base, chest and brow respectively, but may not become psychological diagnoses or automated readiness tests.

Crossing a threshold changes presentation only: a chakra context drawer opens automatically and the prior drawers stay available. It never releases higher-risk practice. Direct source access remains open at every segment.

## Domain model

| Entity | Core fields |
| --- | --- |
| PracticeDefinition | id, contentVersion, status, capability, title, durations, instruction stages, alternatives, source record, risk class, review manifest ID |
| PracticeAttempt | UUID, definition/version, startedAt, endedAt, activeSeconds, intendedSeconds, outcome completed/ended/interrupted, anchor changes, evidenceType app_observed |
| PracticeReflection | attemptId, usefulness useful/neutral/uncomfortable nullable, note nullable, createdAt; always optional |
| CapabilityEvidence | capability, evidenceType app/device/self_report/reviewed_demonstration, recordId, criteriaVersion, occurredAt |
| GuidancePreference | detail full/reduced/minimal, reminders normal/light/off, changedBy user/suggestion |
| IndependentPractice | localDate, intendedMinutes nullable, actualMinutes nullable, practiceId nullable, evidenceType self_report, privateByDefault |
| SourceRecord | tradition, work, passage, edition, URL, accessDepth, relationship direct/adaptation/modern, limitations |
| ContentReviewManifest | exact content hash/version, sourceReview, practiceReview, accessibilityReview, releaseStatus |

Store time in UTC plus the user’s IANA zone for local-day grouping. Active seconds use a monotonic process clock and advance only while the practice is actively running in the foreground. Browsing, paused time and background time do not count. Reconcile process death as `interrupted`; never infer completion from wall-clock expiry.

## Event semantics

- `practice_started`: after explicit begin and suitability confirmation.
- `practice_paused`: user pause, background, interruption or anchor change. Record reason.
- `practice_resumed`: explicit resume.
- `anchor_changed`: from/to and active elapsed time; never interpreted as failure.
- `practice_ended`: user End; outcome `ended`, active seconds retained.
- `practice_completed`: active seconds reach intended duration in the process-owned timer.
- `reflection_recorded`: optional categorical response; no response remains null.
- `independent_practice_recorded`: explicit self-report only, editable/deletable.
- `guidance_changed`: explicit preference or accepted suggestion, never silent.
- `source_opened`: reading event only; no capability/readiness effect.

Use existing `BreathingSessionOwner` process ownership and reviewed catalogue validation as the starting point. Preserve the rule that only one practice runs at once. Map the interface to the exact released content version rather than copying prototype strings into production.

## Aura and motion contract

The visual system uses void `#080908`, ivory `#f0ece1`, bronze `#c99b62`, ash `#a7a39a`. The planet and orbit represent continuity of practice history. Points may appear for concrete evidence families, but never form a numeric aura score. A missed day does not remove, crack or dim past evidence. Independent practice can make the UI quieter rather than more decorated.

The player progressively removes explanatory copy according to the user’s guidance preference. Saturn motion is slow ornament and never corresponds to inhale/exhale timing. Reduced motion presents the same static geometry. All state meaning has text equivalents. End stays in the top-right safe area and remains reachable with large text.

## Content and safety gates

Only exact practices with complete content-review manifests may be delivered. The existing implementation seed is not automatically a public release. Preserve ordinary breathing, immediate stopping, eyes-open/external alternatives and no target state.

Intensive breath manipulation, retention, fasting, sleep restriction, sensory deprivation, strong somatic work, kundalini-oriented induction, sexuality-related practices and altered-state techniques remain research/qualified-context records. They are never unlocked by attempts, minutes, completion, self-report or payment. “Intense” in the shipped unattended experience means disciplined attention, reduced prompting, longer ordinary practice only within reviewed durations, and real-life application.

Uncomfortable reflection should suggest stopping that practice and offer the room/ordinary action; it must not diagnose or prescribe. Persistent or severe unwanted experiences require separately reviewed support copy and qualified help, not an automatically intensified or corrective practice.

## Backend/API proposal

Reconcile with existing authenticated practice routes before implementation; these are desired contracts, not claims of current endpoints.

- `GET /v1/sadhana` → current practice invitation, guidance preference, factual summary, content versions.
- `GET /v1/practices/:id` → exact released definition and review manifest reference.
- `POST /v1/practice-attempts` with client UUID/idempotency key → created attempt.
- `PATCH /v1/practice-attempts/:id` with expected revision → pause/resume/end/completion and active seconds.
- `POST /v1/practice-attempts/:id/reflection` → optional user report.
- `GET /v1/practice-record?cursor=` → evidence-labelled immutable history plus corrections.
- `POST /v1/independent-practices` → explicit self-report, private by default.
- `PUT /v1/sadhana/preferences` → guidance/reminder choices with revision.
- `GET /v1/sadhana/path` → transparent criteria version and evidence links; no opaque score.

Offline attempts use client-generated UUIDs, queue locally and synchronize idempotently. The server validates practice/content versions and rejects unreleased content. User may edit/delete self-reports and reflections. Practice facts should not be emitted as generic engagement telemetry. Use active practice minutes as the product metric; never total Sadhana screen time.

## Loading state contract

Use the full-phone kaleidoscope surface for genuine waits only: initial state hydration, network-backed source retrieval and comparable blocking work. Instant local navigation remains instant. The image breathes from 1.015× to 1.065× over 5.5 seconds without rotation, progress theatre or a spinner; reduced-motion mode holds a static 1.03× crop. Set the application region to `aria-busy="true"` while visible and provide a short live status label describing the work.

Production should expose one shared loading controller with explicit begin, end and error paths, a timeout that resolves to a recoverable error state, and asset preloading or bundling so the loading image itself never depends on the request it represents. The prototype demonstrates boot hydration and source retrieval. The inspector preview is review tooling only.

`assets/shanivar-loading-kaleidoscope.jpg` is copied from the founder-supplied fourth reference image. Ownership and production-use rights have not been verified in the repository; confirm them before public release or replace it with an owned asset preserving the approved composition.

## Acceptance gates

1. Every exact practice passes source, qualified practice, language, accessibility and release review.
2. Unit tests prove pause/background/process-death time is excluded and completion cannot be inferred.
3. Tests prove activity cannot unlock research-only risk classes.
4. End is reachable through TalkBack, 200% font, portrait and landscape; external anchor is equal, not hidden.
5. Offline start/end and later sync deduplicate correctly; self-report deletion works.
6. Missed days cause no loss state; browsing does not count; source reading creates no readiness signal.
7. Physical-device review covers process death, screen lock, call interruption, audio focus if later added and reduced motion.
8. Founder reviews progression meaning and exact visual language before implementation is accepted; public release remains a separate decision.
