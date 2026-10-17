# 0011 — Add VBT breathing and meditation as a third pillar

- **Date:** 2026-09-25
- **Status:** Accepted scope direction; implementation and release verification pending

## Context

The founder explicitly requested VBT-derived breathing/meditation for different feelings, conditions and times of day, embedded alongside action-oriented alarms and focus protection, and asked for a Claude build handoff with tests. The audience is broadly people seeking to avoid unwanted scrolling and fragmented attention; family introduction is one route, not the only audience definition.

## Options

Keep the practice library deferred; reactivate the entire old 12-technique shell; or introduce a small local feature with reviewed adaptations, transparent origins and deterministic context suggestions. The founder's request supersedes the blanket deferral for this feature. Restoring the old library wholesale would also restore retention instructions and player behavior that do not meet the new brief.

## Decision

Add Breathe & reset to the current native Home, using the eight-entry implementation seed and linked handoff. Preserve a full research inventory separately, including specialist/retention passages that are not available in the beginner feature. Use unpaced comfortable awareness, sound and attention practices. Treat time-of-day matching as schedule-aware product behavior, not a measurement of circadian phase or a clinical prescription.

The player has no authority to dismiss alarms or release focus. Pre-focus and post-alarm entries are optional; the overlay preserves device-Home exit behavior. Scripts, source/adaptation notes and versions stay distinct. Build locally without a backend or LLM dependency.

This decision supersedes only the older blanket practice-library deferral and conflicting VBT router/player instructions for this slice. Alarms, focus protection and their safety boundaries remain controlling. Other deferred feature families stay deferred.

## Consequences

There is a third Home entry and a small catalogue to maintain. Matching rules are unvalidated hypotheses. The internal build can proceed now; public release needs content/comfort, accessibility and physical-device evidence. It will not offer counted retention, a complete 112-practice course, clinical treatment or validated body-clock optimization.

## Revisit when

Users repeatedly need a missing context, a plain pause works just as well, a practice produces discomfort, a source correction changes attribution, or supervised specialist instruction becomes a separately justified product. Add advanced practices only with their own source, instruction, population and evaluation requirements.

References: [catalogue](../../research/shanivar-vbt-breathing-catalog-2026-09-25.md), [Claude handoff](../../android-app/CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md), [starter scripts](../../shanivar-vbt/content/breathing-mvp-v1.json), [test guide](../../android-app/VBT_BREATHING_TEST_GUIDE_2026-09-25.md).
