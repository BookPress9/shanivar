package com.shanivar.nativeapp

import java.time.ZonedDateTime

/**
 * Everything the recommender is allowed to know. All of it is either something the person just
 * chose, something they saved as a preference, or an injected clock.
 *
 * There is deliberately no field for anything sensed, inferred or learned: no ML, no birth chart,
 * no usage history, no sensor state, no health signal (handoff section 4). If a future change wants
 * to weigh something, it has to appear here first - which makes the question "may we use this?"
 * unavoidable rather than incidental.
 */
data class BreathingRequest(
    val feeling: BreathingFeeling? = null,
    val explicitContext: BreathingContext? = null,
    val sleepIntent: Boolean = false,
    val sleepyNow: Boolean = false,
    /** Tri-state on purpose: null means "not asked". A practice requiring breath comfort needs an
     * explicit true and must never be routed on an unanswered question. */
    val breathComfort: Boolean? = null,
    val soundConsent: Boolean = false,
    val priorBreathComfort: Boolean = false,
    val safeSettingConfirmed: Boolean = false,
    /** What the person asked for, in seconds. null means "no preference, use the default". */
    val budgetSeconds: Int? = null,
    val schedule: BreathingSchedule = BreathingSchedule.UNSET,
    val now: ZonedDateTime,
    /** False when the tone asset is missing or unreadable, which excludes the tone practice (C07). */
    val soundAssetAvailable: Boolean = true,
    /** Practices the person said felt worse during this visit. Ephemeral - never persisted, never a
     * saved emotional profile (R18). */
    val feltWorseIds: Set<String> = emptySet(),
)

data class BreathingOption(val practice: BreathingPractice, val duration: BreathingDuration)

sealed interface BreathingSuggestion {
    /**
     * The setting was not confirmed safe and stationary. No practice may offer Start; Exit still
     * works. Driving or being in water is the case this exists for, and no amount of eligibility
     * elsewhere overrides it.
     */
    data object SafeSettingRequired : BreathingSuggestion

    /** Nothing valid survived filtering - a corrupt catalogue, or every entry disabled. Says so
     * plainly instead of inventing instructions (C07/R17). */
    data class Unavailable(val explanation: String) : BreathingSuggestion

    /**
     * [primary] is null only when every eligible practice was suppressed by a "felt worse" answer
     * this visit; the list stays available for deliberate manual choice.
     */
    data class Offered(
        val primary: BreathingOption?,
        val alternatives: List<BreathingOption>,
        val allSuitable: List<BreathingOption>,
        val reason: String,
    ) : BreathingSuggestion
}

/**
 * Deterministic, explainable selection. Same inputs always produce the same ordering and the same
 * explanation (R19) - there is no randomness, no tie-breaking on time, and no persisted weighting.
 *
 * The reason strings describe *what the person chose*. None of them may imply the app knows a
 * biological state; "based on the schedule you chose" is the strongest claim available, and it
 * names the schedule as the user's, because that is all it is.
 */
object BreathingRecommender {

    fun suggest(catalogue: BreathingCatalogue, request: BreathingRequest): BreathingSuggestion {
        if (!request.safeSettingConfirmed) return BreathingSuggestion.SafeSettingRequired

        val eligible = catalogue.practices.filter { it.enabled && isEligible(it, request) }
        if (eligible.isEmpty()) {
            return BreathingSuggestion.Unavailable(
                "No practice matches your current answers. You can change them or leave this for now."
            )
        }

        val rankedIds = candidateIds(request)
        val ranked = rankedIds.mapNotNull { id -> eligible.firstOrNull { it.id == id } }
        // Anything eligible but unranked still belongs in "All suitable options" - the ranking is a
        // suggestion order, not a permission list.
        val ordered = ranked + eligible.filterNot { it in ranked }
        val automatic = ordered.filterNot { it.id in request.feltWorseIds }

        return BreathingSuggestion.Offered(
            primary = automatic.firstOrNull()?.toOption(request),
            alternatives = automatic.drop(1).take(2).map { it.toOption(request) },
            allSuitable = ordered.map { it.toOption(request) },
            reason = reasonFor(request, excludedForSound = eligibleExceptSound(catalogue, request)),
        )
    }

    /**
     * The hard constraints. These apply identically to an automatic suggestion and to a manual pick
     * from "All suitable options" - a practice excluded here is not reachable at all this visit
     * (R09), which is the difference between a ranking hint and a safety rule.
     */
    fun isEligible(practice: BreathingPractice, request: BreathingRequest): Boolean {
        if (practice.requiresBreathComfort && request.breathComfort != true) return false
        if (practice.requiresSoundConsent && !request.soundConsent) return false
        if (practice.requiresPriorComfort && !request.priorBreathComfort) return false
        if (practice.requiresSleepIntent && !request.sleepIntent) return false
        if (practice.requiresSleepyNow && !request.sleepyNow) return false
        if (practice.presentation.toneAsset != null && !request.soundAssetAvailable) return false
        return true
    }

    private fun BreathingPractice.toOption(request: BreathingRequest) =
        BreathingOption(this, resolveDuration(request.budgetSeconds))

    /** True when a sound-requiring practice was the only thing consent stood between. Used purely
     * to decide whether the explanation should mention sound at all. */
    private fun eligibleExceptSound(catalogue: BreathingCatalogue, request: BreathingRequest): Boolean =
        !request.soundConsent && catalogue.practices.any {
            it.enabled && it.requiresSoundConsent && isEligible(it, request.copy(soundConsent = true))
        }

    private fun candidateIds(request: BreathingRequest): List<String> {
        // An explicit sleep choice is the strongest signal there is, and it is the only route to
        // the sleep practice. The clock never opens that door on its own (R11/R12/R13).
        if (request.sleepIntent && request.sleepyNow) return SLEEP
        request.feeling?.let { return forFeeling(it) }
        request.explicitContext?.let { return forContext(it) }
        return when (request.schedule.window(request.now.toLocalTime())) {
            ScheduleWindow.JUST_WOKE_WINDOW -> JUST_WOKE
            // Past the stated sleep time we still only suggest winding down: being awake then is
            // information about the schedule, not proof the person wants to sleep.
            ScheduleWindow.WIND_DOWN_WINDOW, ScheduleWindow.REST_WINDOW -> WINDING_DOWN
            ScheduleWindow.DAYTIME, ScheduleWindow.UNKNOWN -> DEFAULT
        }
    }

    private fun forFeeling(feeling: BreathingFeeling): List<String> = when (feeling) {
        BreathingFeeling.SCATTERED -> SCATTERED
        BreathingFeeling.TENSE -> TENSE
        BreathingFeeling.AGITATED -> AGITATED
        BreathingFeeling.LOW_ENERGY -> JUST_WOKE
        BreathingFeeling.OVERSTIMULATED -> OVERSTIMULATED
        BreathingFeeling.WINDING_DOWN -> WINDING_DOWN
        BreathingFeeling.STEADY -> STEADY
    }

    private fun forContext(context: BreathingContext): List<String> = when (context) {
        BreathingContext.JUST_WOKE -> JUST_WOKE
        BreathingContext.DAYTIME -> DEFAULT
        // Choosing "going to sleep" without saying you feel sleepy gets ordinary rest, not the
        // sleep practice - which needs both answers regardless of what is offered here.
        BreathingContext.WINDING_DOWN, BreathingContext.GOING_TO_SLEEP -> WINDING_DOWN
    }

    private fun reasonFor(request: BreathingRequest, excludedForSound: Boolean): String {
        val parts = mutableListOf(goalSentence(request))
        if (request.breathComfort == false) parts += "You preferred an option without breath attention."
        if (excludedForSound) parts += "Sound is off, so humming and listening options are not included."
        return parts.joinToString(" ")
    }

    private fun goalSentence(request: BreathingRequest): String = when {
        request.sleepIntent && request.sleepyNow -> "You chose sleep and said you feel sleepy now."
        request.feeling != null -> when (request.feeling) {
            BreathingFeeling.SCATTERED -> "You said your attention feels scattered."
            BreathingFeeling.TENSE -> "You said you feel tense."
            BreathingFeeling.AGITATED -> "You said you feel angry or frustrated."
            BreathingFeeling.LOW_ENERGY -> "You said your energy is low."
            BreathingFeeling.OVERSTIMULATED -> "You said things feel overstimulating."
            BreathingFeeling.WINDING_DOWN -> "You said you are winding down."
            BreathingFeeling.STEADY -> "You chose to practise without naming a feeling."
        }
        request.explicitContext != null -> "You chose: ${request.explicitContext.label.lowercase()}."
        else -> when (request.schedule.window(request.now.toLocalTime())) {
            ScheduleWindow.JUST_WOKE_WINDOW -> SCHEDULE_PREFIX + "this is near the waking time you set."
            ScheduleWindow.WIND_DOWN_WINDOW -> SCHEDULE_PREFIX + "this is near the sleep time you set."
            ScheduleWindow.REST_WINDOW -> SCHEDULE_PREFIX + "this is past the sleep time you set."
            ScheduleWindow.DAYTIME -> SCHEDULE_PREFIX + "this is the middle of your day."
            ScheduleWindow.UNKNOWN -> "This is a plain starting point. You can tell us how you feel, or just begin."
        }
    }

    private const val SCHEDULE_PREFIX = "Based on the schedule you chose, "

    // The candidate tables from handoff section 4. Ids, not practices, so a disabled or removed
    // catalogue entry drops out of ranking without any code change.
    private val SLEEP = listOf("sleep_threshold", "external_grounding")
    private val SCATTERED = listOf("natural_anchor", "breath_word", "external_grounding")
    private val TENSE = listOf("natural_anchor", "hum_listen", "emotion_space", "external_grounding")
    private val AGITATED = listOf("emotion_space", "external_grounding", "natural_anchor")
    private val JUST_WOKE = listOf("external_grounding", "natural_anchor")
    private val OVERSTIMULATED = listOf("external_grounding", "sound_listen", "natural_anchor")
    private val WINDING_DOWN = listOf("natural_anchor", "hum_listen", "breath_word", "external_grounding")
    private val STEADY = listOf("natural_anchor", "breath_word", "breath_turn", "external_grounding")
    private val DEFAULT = listOf("external_grounding", "natural_anchor")
}
