package com.shanivar.nativeapp

import com.shanivar.nativeapp.BreathingFixtures.at
import com.shanivar.nativeapp.BreathingFixtures.catalogue
import com.shanivar.nativeapp.BreathingFixtures.ordinarySchedule
import com.shanivar.nativeapp.BreathingFixtures.request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Section B of VBT_BREATHING_TEST_GUIDE_2026-09-25.md, fixture by fixture.
 *
 * Two distinctions carry most of the weight here. First, a *hard* constraint (`requires*`) removes a
 * practice from everything including the manual list, while a ranking hint only reorders. Second, the
 * clock may never open a door that needs an explicit answer - the sleep practice in particular is
 * reachable only by someone saying both that they are going to sleep and that they feel sleepy now.
 */
class BreathingRecommenderTest {
    private fun suggest(request: BreathingRequest) = BreathingRecommender.suggest(catalogue, request)

    @Test fun r01_unknownInputsLandOnTheModernGroundingExercise() {
        val result = suggest(request())
        assertEquals("external_grounding", result.orderedIds().first())
        assertTrue((result as BreathingSuggestion.Offered).reason.isNotBlank())
    }

    @Test fun r02_scatteredWithBreathComfortRanksBreathAwarenessFirst() {
        val result = suggest(request(feeling = BreathingFeeling.SCATTERED, breathComfort = true))
        assertEquals(listOf("natural_anchor", "breath_word", "external_grounding"), result.orderedIds())
    }

    @Test fun r03_breathDiscomfortRemovesEveryBreathPracticeEvenIfPreviouslyUsed() {
        val result = suggest(request(feeling = BreathingFeeling.SCATTERED, breathComfort = false, priorBreathComfort = true))
        assertEquals("external_grounding", result.orderedIds().first())
        assertFalse(result.allIds().any { catalogue.practice(it)!!.requiresBreathComfort })
    }

    @Test fun r04_tenseWithoutBreathComfortReachesTheEmotionPracticeAndNotHumming() {
        val result = suggest(request(feeling = BreathingFeeling.TENSE, breathComfort = false, soundConsent = true))
        assertEquals(listOf("emotion_space", "external_grounding"), result.orderedIds().take(2))
        // Humming is a breath practice that also makes sound; sound consent alone does not unlock it.
        assertFalse(result.allIds().contains("hum_listen"))
    }

    @Test fun r05_tenseWithBreathComfortButNoSoundConsentOffersNoToneOrHum() {
        val result = suggest(request(feeling = BreathingFeeling.TENSE, breathComfort = true))
        assertEquals("natural_anchor", result.orderedIds().first())
        assertFalse(result.allIds().contains("hum_listen"))
        assertFalse(result.allIds().contains("sound_listen"))
    }

    @Test fun r06_agitationGetsRoomForTheFeelingWithNoIntensityEscalation() {
        val result = suggest(request(feeling = BreathingFeeling.AGITATED, schedule = ordinarySchedule, now = at(14)))
        assertEquals("emotion_space", result.orderedIds().first())
        assertFalse(result.allIds().contains("breath_turn"))
    }

    @Test fun r07_lowEnergyNeverBecomesASleepAssignmentAtAnyHour() {
        listOf(at(8), at(23)).forEach { clock ->
            val result = suggest(
                request(
                    feeling = BreathingFeeling.LOW_ENERGY, context = BreathingContext.DAYTIME,
                    schedule = ordinarySchedule, now = clock,
                )
            )
            assertEquals("external_grounding", result.orderedIds().first())
            assertFalse(result.allIds().contains("sleep_threshold"))
        }
    }

    @Test fun r08_overstimulatedWithoutSoundConsentExcludesListening() {
        val result = suggest(request(feeling = BreathingFeeling.OVERSTIMULATED))
        assertEquals("external_grounding", result.orderedIds().first())
        assertFalse(result.allIds().contains("sound_listen"))
    }

    @Test fun r09_turnAwarenessStaysOutOfTheManualListWithoutPriorComfort() {
        val result = suggest(request(feeling = BreathingFeeling.STEADY, breathComfort = true, priorBreathComfort = false))
        assertFalse(result.allIds().contains("breath_turn"))
    }

    @Test fun r10_turnAwarenessBecomesEligibleButNeverCompulsory() {
        val result = suggest(request(feeling = BreathingFeeling.STEADY, breathComfort = true, priorBreathComfort = true))
        assertTrue(result.allIds().contains("breath_turn"))
        assertEquals("natural_anchor", result.orderedIds().first())
    }

    @Test fun r11_anExplicitSleepChoiceReachesTheSilentPracticeAtTheChosenLength() {
        val result = suggest(
            request(
                context = BreathingContext.GOING_TO_SLEEP, sleepIntent = true, sleepyNow = true, budgetSeconds = 180,
            )
        )
        assertEquals(listOf("sleep_threshold", "external_grounding"), result.orderedIds().take(2))
        val primary = (result as BreathingSuggestion.Offered).primary!!
        assertEquals(BreathingDuration.Ready(180, false), primary.duration)
        // A silent ending is structural: the sleep plan has no closing slice to show at all.
        assertFalse(primary.practice.stagePlan(180).any { it.stage == BreathingStageId.CLOSING })
    }

    @Test fun r12_theClockAloneNeverRoutesAnyoneIntoSleepPractice() {
        val restWindow = request(schedule = ordinarySchedule, now = at(1))
        assertEquals(ScheduleWindow.REST_WINDOW, ordinarySchedule.window(restWindow.now.toLocalTime()))
        assertFalse(suggest(restWindow).allIds().contains("sleep_threshold"))
        // Intending sleep without feeling sleepy is still not enough.
        assertFalse(suggest(restWindow.copy(sleepIntent = true)).allIds().contains("sleep_threshold"))
    }

    @Test fun r13_anExplicitSleepChoiceWorksInBroadDaylightForShiftWorkers() {
        val result = suggest(
            request(sleepIntent = true, sleepyNow = true, schedule = ordinarySchedule, now = at(14), budgetSeconds = 180)
        )
        assertEquals(ScheduleWindow.DAYTIME, ordinarySchedule.window(at(14).toLocalTime()))
        assertEquals("sleep_threshold", result.orderedIds().first())
    }

    @Test fun r14_anUnconfirmedSettingRemovesStartForEverything() {
        assertEquals(
            BreathingSuggestion.SafeSettingRequired,
            suggest(request(feeling = BreathingFeeling.SCATTERED, breathComfort = true, safeSettingConfirmed = false)),
        )
    }

    @Test fun r15_aLongerBudgetIsOfferedAtTheLongestLengthTheScriptActuallyHas() {
        val result = suggest(request(feeling = BreathingFeeling.OVERSTIMULATED, budgetSeconds = 300))
        val primary = (result as BreathingSuggestion.Offered).primary!!
        assertEquals("external_grounding", primary.practice.id)
        assertEquals(BreathingDuration.Ready(180, true), primary.duration)
    }

    @Test fun r16_aBudgetBelowTheShortestScriptRequiresAnExplicitChoiceInsteadOfSilentExtension() {
        listOf(30, 0, -5).forEach { budget ->
            val primary = (suggest(request(budgetSeconds = budget)) as BreathingSuggestion.Offered).primary!!
            assertEquals(BreathingDuration.NeedsLongerConfirmation(60, budget), primary.duration)
        }
    }

    @Test fun r17_aDisabledCatalogueProducesAnUnavailableStateRatherThanAnInventedPractice() {
        val disabled = catalogue.copy(practices = catalogue.practices.map { it.copy(enabled = false) })
        val result = BreathingRecommender.suggest(disabled, request())
        assertTrue(result is BreathingSuggestion.Unavailable)
    }

    @Test fun r17_anUnreadableToneAssetExcludesOnlyTheTonePractice() {
        val result = suggest(request(feeling = BreathingFeeling.OVERSTIMULATED, soundConsent = true, soundAssetAvailable = false))
        assertFalse(result.allIds().contains("sound_listen"))
        assertTrue(result.allIds().contains("external_grounding"))
    }

    @Test fun r18_feltWorseSuppressesAutomaticSuggestionForThisVisitButNotManualChoice() {
        val result = suggest(
            request(feeling = BreathingFeeling.SCATTERED, breathComfort = true, feltWorseIds = setOf("natural_anchor"))
        )
        assertEquals("breath_word", result.orderedIds().first())
        assertTrue(result.allIds().contains("natural_anchor"))
    }

    @Test fun r19_identicalInputsProduceAnIdenticalOrderingAndExplanationEveryTime() {
        val input = request(feeling = BreathingFeeling.TENSE, breathComfort = true, schedule = ordinarySchedule, now = at(21))
        val first = suggest(input) as BreathingSuggestion.Offered
        repeat(100) {
            val again = suggest(input) as BreathingSuggestion.Offered
            assertEquals(first.allSuitable.map { it.practice.id }, again.allSuitable.map { it.practice.id })
            assertEquals(first.reason, again.reason)
        }
    }

    @Test fun r20_anUnrecognisedStoredFeelingOrContextIsIgnoredRatherThanInterpreted() {
        assertEquals(null, BreathingFeeling.fromId("dopamine_depleted"))
        assertEquals(null, BreathingContext.fromId("third_quarter_moon"))
        val result = suggest(request(feeling = BreathingFeeling.fromId("dopamine_depleted")))
        assertEquals("external_grounding", result.orderedIds().first())
    }

    @Test fun t07_anExplicitContextOutranksTheScheduleButNotTheSleepGates() {
        val windDownClock = request(context = BreathingContext.JUST_WOKE, schedule = ordinarySchedule, now = at(22))
        assertEquals(ScheduleWindow.WIND_DOWN_WINDOW, ordinarySchedule.window(at(22).toLocalTime()))
        assertEquals("external_grounding", suggest(windDownClock).orderedIds().first())
        // "Going to sleep" chosen without current sleepiness gets ordinary rest, not sleep practice.
        val notSleepy = request(context = BreathingContext.GOING_TO_SLEEP, sleepIntent = true, breathComfort = true)
        assertEquals("natural_anchor", suggest(notSleepy).orderedIds().first())
        assertFalse(suggest(notSleepy).allIds().contains("sleep_threshold"))
    }

    @Test fun theExplanationDescribesWhatThePersonChoseAndNeverABodilyState() {
        val offered = suggest(
            request(feeling = BreathingFeeling.TENSE, breathComfort = false, schedule = ordinarySchedule, now = at(21))
        ) as BreathingSuggestion.Offered
        assertTrue(offered.reason.contains("You said you feel tense."))
        assertTrue(offered.reason.contains("without breath attention"))
        listOf("nervous system", "cortisol", "dopamine", "we detect", "your body").forEach {
            assertFalse(offered.reason.lowercase().contains(it))
        }
    }

    @Test fun aScheduleDerivedExplanationNamesTheScheduleAsTheUsersOwn() {
        val offered = suggest(request(schedule = ordinarySchedule, now = at(22))) as BreathingSuggestion.Offered
        assertTrue(offered.reason.startsWith("Based on the schedule you chose,"))
    }
}
