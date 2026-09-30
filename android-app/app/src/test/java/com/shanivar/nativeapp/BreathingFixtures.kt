package com.shanivar.nativeapp

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * A hand-built catalogue mirroring the gating fields of the eight shipped seed entries.
 *
 * Built in Kotlin rather than parsed from the asset so the recommender, schedule and session tests
 * stay pure JVM tests, as the test guide's section B/C require. The risk that comes with a duplicate
 * - drift from the real content - is closed by `BreathingCatalogTest.shippedAssetMatchesTheGatingFixture`,
 * which parses the actual asset and asserts these gating fields still agree.
 */
object BreathingFixtures {
    val ZONE: ZoneId = ZoneId.of("Europe/Berlin")

    fun at(hour: Int, minute: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 9, 25, hour, minute, 0, 0, ZONE)

    /** Wake 07:00, intended sleep 23:00 - the schedule used by the guide's T01/T02 fixtures. */
    val ordinarySchedule = BreathingSchedule(LocalTime.of(7, 0), LocalTime.of(23, 0))

    private fun practice(
        id: String,
        kind: BreathingKind,
        durations: List<Int>,
        default: Int,
        breathComfort: Boolean = false,
        soundConsent: Boolean = false,
        priorComfort: Boolean = false,
        sleepIntent: Boolean = false,
        sleepyNow: Boolean = false,
        tone: String? = null,
        sourced: Boolean = true,
    ) = BreathingPractice(
        id = id,
        title = id,
        kind = kind,
        contentVersion = 1,
        status = "implementation_seed",
        source = if (sourced) BreathingSource(
            work = "Vijnana-bhairava-tantra", verseLabels = listOf("24"), edition = "edition",
            url = "https://example.invalid", relationship = "modern_adaptation",
            adaptationNote = "note", verification = "checked",
        ) else null,
        allowedDurationsSeconds = durations,
        defaultDurationSeconds = default,
        suitableFeelings = emptySet(),
        suitableContexts = emptySet(),
        requiresBreathComfort = breathComfort,
        requiresSoundConsent = soundConsent,
        requiresPriorComfort = priorComfort,
        requiresSleepIntent = sleepIntent,
        requiresSleepyNow = sleepyNow,
        safeStationarySettingRequired = true,
        stages = mapOf(
            BreathingStageId.ARRIVAL to "arrival",
            BreathingStageId.INSTRUCTION to "instruction",
            BreathingStageId.PRACTICE to "practice",
        ) + if (kind == BreathingKind.SLEEP_ATTENTION) emptyMap() else mapOf(BreathingStageId.CLOSING to "closing"),
        presentation = BreathingPresentation(
            eyesOpenAllowed = true, keepScreenOn = false, closingBell = false,
            showCompletionQuestions = kind != BreathingKind.SLEEP_ATTENTION, toneAsset = tone,
        ),
    )

    val naturalAnchor = practice("natural_anchor", BreathingKind.BREATH_AWARENESS, listOf(60, 180, 300), 60, breathComfort = true)
    val breathTurn = practice("breath_turn", BreathingKind.BREATH_AWARENESS, listOf(60, 180, 300), 60, breathComfort = true, priorComfort = true)
    val humListen = practice("hum_listen", BreathingKind.BREATH_SOUND, listOf(60, 180), 60, breathComfort = true, soundConsent = true)
    val breathWord = practice("breath_word", BreathingKind.BREATH_AWARENESS, listOf(60, 180, 300), 60, breathComfort = true)
    val emotionSpace = practice("emotion_space", BreathingKind.ATTENTION, listOf(60, 180), 60)
    val soundListen = practice("sound_listen", BreathingKind.SOUND_ATTENTION, listOf(60, 180), 60, soundConsent = true, tone = "original_fading_tone")
    val sleepThreshold = practice("sleep_threshold", BreathingKind.SLEEP_ATTENTION, listOf(60, 180, 300), 180, sleepIntent = true, sleepyNow = true)
    val externalGrounding = practice("external_grounding", BreathingKind.EXTERNAL_ATTENTION, listOf(60, 180), 60, sourced = false)

    val catalogue = BreathingCatalogue(
        schemaVersion = 1,
        catalogVersion = "vbt-breathe-2026-09-25.1",
        status = "implementation_seed_review_before_public_release",
        scriptLanguage = "en",
        languageFallback = "English fallback disclosed.",
        globalGuidance = "Keep breathing comfortable.",
        practices = listOf(
            naturalAnchor, breathTurn, humListen, breathWord,
            emotionSpace, soundListen, sleepThreshold, externalGrounding,
        ),
    )

    /** A safe-setting-confirmed request with every optional input at its most restrictive default. */
    fun request(
        feeling: BreathingFeeling? = null,
        context: BreathingContext? = null,
        sleepIntent: Boolean = false,
        sleepyNow: Boolean = false,
        breathComfort: Boolean? = null,
        soundConsent: Boolean = false,
        priorBreathComfort: Boolean = false,
        budgetSeconds: Int? = 60,
        schedule: BreathingSchedule = BreathingSchedule.UNSET,
        now: ZonedDateTime = at(14),
        soundAssetAvailable: Boolean = true,
        feltWorseIds: Set<String> = emptySet(),
        safeSettingConfirmed: Boolean = true,
    ) = BreathingRequest(
        feeling = feeling, explicitContext = context, sleepIntent = sleepIntent, sleepyNow = sleepyNow,
        breathComfort = breathComfort, soundConsent = soundConsent, priorBreathComfort = priorBreathComfort,
        safeSettingConfirmed = safeSettingConfirmed, budgetSeconds = budgetSeconds, schedule = schedule,
        now = now, soundAssetAvailable = soundAssetAvailable, feltWorseIds = feltWorseIds,
    )
}

/** Ids in suggestion order, primary first. */
fun BreathingSuggestion.orderedIds(): List<String> {
    val offered = this as BreathingSuggestion.Offered
    return listOfNotNull(offered.primary) .plus(offered.alternatives).map { it.practice.id }
}

fun BreathingSuggestion.allIds(): List<String> =
    (this as BreathingSuggestion.Offered).allSuitable.map { it.practice.id }
