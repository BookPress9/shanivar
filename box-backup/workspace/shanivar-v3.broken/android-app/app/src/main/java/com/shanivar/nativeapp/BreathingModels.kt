package com.shanivar.nativeapp

/**
 * Data model for the "Breathe & reset" pillar (decision 0011, handoff
 * CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md).
 *
 * Deliberately free of any Android, Compose or network type. The test guide requires *pure JVM*
 * tests for the recommender, the schedule and the session reducer; if constructing a practice
 * needed a Context or a parsed asset, those tests would have to run under Robolectric and the
 * fixtures in section B/C of the guide could not be expressed at all.
 *
 * Note what is absent: there is no field for breath-phase durations. The seed JSON carries
 * `prescribedBreathPhases: []` and [BreathingCatalog] rejects any entry where it is non-empty, so
 * the shipped model has no way to express "inhale for N seconds" even if a future content edit
 * tried to. That is the point - the ban on prescribed pacing is structural, not a code review.
 */
enum class BreathingFeeling(val id: String, val label: String) {
    SCATTERED("scattered", "Scattered"),
    TENSE("tense", "Tense"),
    AGITATED("agitated", "Angry or frustrated"),
    LOW_ENERGY("low_energy", "Low energy"),
    OVERSTIMULATED("overstimulated", "Overstimulated"),
    WINDING_DOWN("winding_down", "Winding down"),
    STEADY("steady", "Just practice");

    companion object {
        /** Unknown ids resolve to null rather than throwing - a stale persisted feeling must not
         * crash selection (test guide R20), and feelings are ephemeral self-reports anyway. */
        fun fromId(id: String?): BreathingFeeling? = values().firstOrNull { it.id == id }
    }
}

enum class BreathingContext(val id: String, val label: String) {
    JUST_WOKE("just_woke", "Just woke up"),
    DAYTIME("daytime", "Daytime"),
    WINDING_DOWN("winding_down", "Winding down"),
    GOING_TO_SLEEP("going_to_sleep", "Going to sleep");

    companion object {
        fun fromId(id: String?): BreathingContext? = values().firstOrNull { it.id == id }
    }
}

/** The catalogue's `kind`. An unrecognised kind fails validation instead of defaulting, because
 * "which kind is this" decides whether a card may be labelled a breath technique at all (C08). */
enum class BreathingKind(val id: String) {
    BREATH_AWARENESS("breath_awareness"),
    BREATH_SOUND("breath_sound"),
    ATTENTION("attention"),
    SOUND_ATTENTION("sound_attention"),
    SLEEP_ATTENTION("sleep_attention"),
    EXTERNAL_ATTENTION("external_attention");

    companion object {
        fun fromId(id: String?): BreathingKind? = values().firstOrNull { it.id == id }
    }
}

enum class BreathingStageId(val id: String) {
    ARRIVAL("arrival"), INSTRUCTION("instruction"), PRACTICE("practice"), CLOSING("closing");

    companion object {
        fun fromId(id: String?): BreathingStageId? = values().firstOrNull { it.id == id }
    }
}

data class BreathingSource(
    val work: String,
    val verseLabels: List<String>,
    val edition: String,
    val url: String,
    val relationship: String,
    val adaptationNote: String,
    val verification: String,
)

data class BreathingPresentation(
    val eyesOpenAllowed: Boolean,
    val keepScreenOn: Boolean,
    val closingBell: Boolean,
    val showCompletionQuestions: Boolean,
    val toneAsset: String?,
)

data class BreathingPractice(
    val id: String,
    val title: String,
    val kind: BreathingKind,
    val contentVersion: Int,
    val status: String,
    val source: BreathingSource?,
    val allowedDurationsSeconds: List<Int>,
    val defaultDurationSeconds: Int,
    /** Ranking hints only - never a diagnosis or an exclusion. Kept as raw strings so an id the
     * app does not know about is ignored instead of failing the whole catalogue. */
    val suitableFeelings: Set<String>,
    val suitableContexts: Set<String>,
    val requiresBreathComfort: Boolean,
    val requiresSoundConsent: Boolean,
    val requiresPriorComfort: Boolean,
    val requiresSleepIntent: Boolean,
    val requiresSleepyNow: Boolean,
    val safeStationarySettingRequired: Boolean,
    val stages: Map<BreathingStageId, String>,
    val presentation: BreathingPresentation,
    val enabled: Boolean = true,
) {
    val isSleepPractice: Boolean get() = kind == BreathingKind.SLEEP_ATTENTION

    /** `external_grounding` carries `source = null` on purpose and must never be shown as VBT. */
    val originLabel: String
        get() = source?.let { "Modern adaptation · VBT verse ${it.verseLabels.joinToString(", ")}" }
            ?: "Modern attention exercise"

    fun shortestDurationSeconds(): Int = allowedDurationsSeconds.minOrNull() ?: defaultDurationSeconds
}

data class BreathingCatalogue(
    val schemaVersion: Int,
    val catalogVersion: String,
    val status: String,
    val scriptLanguage: String,
    val languageFallback: String,
    val globalGuidance: String,
    val practices: List<BreathingPractice>,
) {
    fun practice(id: String): BreathingPractice? = practices.firstOrNull { it.id == id }

    /** The seed ships as `implementation_seed_review_before_public_release`. Nothing may infer
     * clinical or tradition review from a schema check passing (test guide, section A). */
    val reviewedForPublicRelease: Boolean get() = status == REVIEWED_STATUS

    companion object {
        const val REVIEWED_STATUS = "reviewed_for_public_release"
    }
}

/** One slice of the player's pacing. These are *UI reading* times, never breath-phase times. */
data class BreathingStageSlice(val stage: BreathingStageId, val seconds: Int, val text: String)

/**
 * Stage pacing from the catalogue's `stageTiming` rules: arrival 10s, instruction 20s, practice
 * `duration - 40`, closing 10s; a sleep practice instead ends silently with `duration - 30` of
 * practice and no closing (a bedtime closing screen is exactly the bright, demanding ending the
 * test guide's section F forbids).
 *
 * The slices sum to the selected duration exactly. Callers rely on that: the player derives
 * "which stage am I in" from accumulated active time, so a plan that summed to less than the
 * duration would leave the last seconds in no stage at all.
 */
fun BreathingPractice.stagePlan(durationSeconds: Int): List<BreathingStageSlice> {
    val practiceSeconds = durationSeconds - if (isSleepPractice) 30 else 40
    require(practiceSeconds > 0) { "duration ${durationSeconds}s is too short for $id" }
    val slices = mutableListOf(
        BreathingStageSlice(BreathingStageId.ARRIVAL, 10, stages[BreathingStageId.ARRIVAL].orEmpty()),
        BreathingStageSlice(BreathingStageId.INSTRUCTION, 20, stages[BreathingStageId.INSTRUCTION].orEmpty()),
        BreathingStageSlice(BreathingStageId.PRACTICE, practiceSeconds, stages[BreathingStageId.PRACTICE].orEmpty()),
    )
    if (!isSleepPractice) {
        slices += BreathingStageSlice(BreathingStageId.CLOSING, 10, stages[BreathingStageId.CLOSING].orEmpty())
    }
    return slices
}

/**
 * How a selected time budget resolves against what a practice actually has a script for.
 *
 * Shortening is fine and is simply offered. Lengthening is not: if every supported duration is
 * longer than the budget the person chose, the player must say so and wait for an explicit choice
 * (test guide R15/R16) rather than quietly running a longer session than was asked for.
 */
sealed interface BreathingDuration {
    val seconds: Int

    data class Ready(override val seconds: Int, val shorterThanBudget: Boolean) : BreathingDuration
    data class NeedsLongerConfirmation(override val seconds: Int, val budgetSeconds: Int) : BreathingDuration
}

fun BreathingPractice.resolveDuration(budgetSeconds: Int?): BreathingDuration {
    if (budgetSeconds == null) return BreathingDuration.Ready(defaultDurationSeconds, false)
    val longestThatFits = allowedDurationsSeconds.filter { it <= budgetSeconds }.maxOrNull()
        ?: return BreathingDuration.NeedsLongerConfirmation(shortestDurationSeconds(), budgetSeconds)
    return BreathingDuration.Ready(longestThatFits, longestThatFits < budgetSeconds)
}
