# Sadhana V1 - derived build plan (decision 0017)

Logic first, then screens, as with initiation. Behind a build flag (debug only), so the release app is unchanged. Each step is committed locally.

| Step | What | Existing code it builds on |
|---|---|---|
| S1 | Practice domain: `PracticeDefinition` (status released, study_only, qualified_context, research_only; risk class; capability; durations), deterministic research-only lock, tests proving activity never unlocks it | `BreathingCatalog.kt`, `BreathingModels.kt` (reviewed catalogue validation) |
| S2 | Attempt timing and events: active seconds on a monotonic clock, foreground only; pause reasons; `anchor_changed`; process death becomes `interrupted`; no completion from wall clock; one practice at a time | `BreathingSessionOwner.kt`, `SessionTiming.kt`, `PracticeAttempt.kt`, `InitiationSitting.kt` (60 s records) |
| S3 | Record and progression: `PracticeReflection` (optional), `IndependentPractice` (editable and deletable), `GuidancePreference`, `CapabilityEvidence` with Arrive to Carry criteria v1; Orbit points from evidence families | `LivingOrbit.kt` (OrbitLog), `ReflectionStore.kt` |
| S4 | Living Spine data: three layers; `SourceRecord`; seven centres as study waypoints (no practice launch until passage-verified) | new; `TANTRA_PRACTICE_MAP.md`, `research/sadhak-inquiry/` |
| S5 | Screens A: Sadhana home, Living Spine, centre drawer, Path | `InitiationSadhanaScreens.kt`, `InitiationTheme.kt` (tokens close to the wireframe's, to reconcile) |
| S6 | Screens B: practice detail, Prepare (stationary and anchor), player (pause, anchor change, End, guidance levels, still planet), Return and reflection | replaces Steady `BreathingScreen.kt`; initiation practice screen |
| S7 | Screens C: Record, Without app, Deep study with research boundary | `InitiationOrbitScreens.kt` |
| S8 | Blocked/needs founder: content review manifests per practice, /v1 Sadhana API and sync (DEV-25), device QA, TalkBack and 200% font | backend, reviewers |
