package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Section A of VBT_BREATHING_TEST_GUIDE_2026-09-25.md, run against the actual packaged asset.
 *
 * Robolectric rather than a pure test only because `org.json` is an Android type. Note the absent
 * `manifest = Config.NONE` other tests in this package use - assets are unreadable without it.
 *
 * A schema check cannot certify a faithful adaptation or a safe beginner instruction; the guide
 * requires a tradition reviewer and a breathing/health practitioner for that, and neither has
 * happened. What this file establishes is narrower: the shipped content cannot silently lose its
 * attribution, gain prescribed breath timing, or offer a duration it has no script for.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BreathingCatalogTest {

    private fun shipped(): BreathingCatalogue {
        val result = BreathingCatalog.load(RuntimeEnvironment.getApplication())
        assertTrue("shipped catalogue failed validation: $result", result is BreathingCatalogResult.Valid)
        return (result as BreathingCatalogResult.Valid).catalogue
    }

    @Test fun c01_theShippedAssetIsValidAndHoldsExactlyTheEightSeedPractices() {
        val catalogue = shipped()
        assertEquals("vbt-breathe-2026-09-25.1", catalogue.catalogVersion)
        assertEquals(
            listOf(
                "natural_anchor", "breath_turn", "hum_listen", "breath_word",
                "emotion_space", "sound_listen", "sleep_threshold", "external_grounding",
            ),
            catalogue.practices.map { it.id },
        )
        assertEquals(8, catalogue.practices.map { it.id }.distinct().size)
    }

    @Test fun c01_anImplementationSeedIsNeverTreatedAsReviewedForRelease() {
        // The single most tempting shortcut in this feature: letting a green schema check stand in
        // for a content review that has not happened.
        assertFalse(shipped().reviewedForPublicRelease)
        assertEquals("implementation_seed_review_before_public_release", shipped().status)
    }

    @Test fun c02_everyVbtSourcedPracticeCarriesCompleteAttributionInTheShippedAsset() {
        shipped().practices.mapNotNull { practice -> practice.source?.let { practice to it } }
            .forEach { (practice, source) ->
                assertEquals("Vijnana-bhairava-tantra", source.work)
                listOf(source.edition, source.url, source.relationship, source.adaptationNote, source.verification)
                    .forEach { assertTrue("${practice.id}: empty source field", it.isNotBlank()) }
                assertTrue("${practice.id}: no verse labels", source.verseLabels.isNotEmpty())
                // Attribution must survive in the asset whether or not anyone opens the drawer.
                assertTrue(practice.originLabel.startsWith("Modern adaptation · VBT verse "))
            }
    }

    @Test fun c03_theGroundingExerciseClaimsNoTraditionalSource() {
        val grounding = shipped().practice("external_grounding")!!
        assertNull(grounding.source)
        assertEquals("Modern attention exercise", grounding.originLabel)
        assertFalse(grounding.originLabel.contains("VBT"))
    }

    @Test fun c04_theEditionQualifiedPassageKeepsItsQualificationAndSetsNoBreathTarget() {
        val breathWord = shipped().practice("breath_word")!!
        assertEquals(listOf("156-A"), breathWord.source!!.verseLabels)
        assertTrue(breathWord.source!!.adaptationNote.contains("156-A"))
        assertTrue(breathWord.source!!.adaptationNote.contains("not a prescribed breath rate"))
    }

    @Test fun c05_noShippedPracticePrescribesPacingRetentionOrDelayedInhalation() {
        val banned = listOf("hold your breath", "hold the breath", "retain", "seconds in", "count of",
            "4-7-8", "box breathing", "do not inhale", "empty your lungs")
        shipped().practices.forEach { practice ->
            practice.stages.values.forEach { text ->
                banned.forEach { phrase ->
                    assertFalse("${practice.id} prescribes \"$phrase\"", text.lowercase().contains(phrase))
                }
            }
        }
    }

    @Test fun c06_everyOfferedDurationHasStagesThatSumToItExactly() {
        shipped().practices.forEach { practice ->
            practice.allowedDurationsSeconds.forEach { duration ->
                assertEquals(
                    "${practice.id} at ${duration}s",
                    duration,
                    practice.stagePlan(duration).sumOf { it.seconds },
                )
            }
            assertTrue(practice.defaultDurationSeconds in practice.allowedDurationsSeconds)
        }
    }

    @Test fun c06_aSleepPracticeMakesNoClosingDemand() {
        val sleep = shipped().practice("sleep_threshold")!!
        assertTrue(sleep.isSleepPractice)
        assertTrue(sleep.stages[BreathingStageId.CLOSING].isNullOrBlank())
        assertFalse(sleep.presentation.closingBell)
        assertFalse(sleep.presentation.keepScreenOn)
        assertFalse(sleep.presentation.showCompletionQuestions)
        assertFalse(sleep.stagePlan(180).any { it.stage == BreathingStageId.CLOSING })
    }

    @Test fun c07_aMissingOrCorruptAssetFailsWithoutInventingInstructions() {
        assertTrue(BreathingCatalog.parse("") is BreathingCatalogResult.Invalid)
        assertTrue(BreathingCatalog.parse("{\"schemaVersion\":1}") is BreathingCatalogResult.Invalid)
        // No hardcoded fallback script exists to quietly stand in - the guide only permits one whose
        // text has been independently validated, and none has been.
        val problems = (BreathingCatalog.parse("{}") as BreathingCatalogResult.Invalid).problems
        assertTrue(problems.any { it.contains("practices") })
    }

    @Test fun c07_anUnsupportedSchemaVersionIsRejectedRatherThanBestEffortParsed() {
        val bumped = assetText().replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        assertTrue(BreathingCatalog.parse(bumped) is BreathingCatalogResult.Invalid)
    }

    @Test fun c08_soundSleepAndEmotionPracticesAreNotLabelledBreathTechniques() {
        val catalogue = shipped()
        assertEquals(BreathingKind.SOUND_ATTENTION, catalogue.practice("sound_listen")!!.kind)
        assertEquals(BreathingKind.SLEEP_ATTENTION, catalogue.practice("sleep_threshold")!!.kind)
        assertEquals(BreathingKind.ATTENTION, catalogue.practice("emotion_space")!!.kind)
        assertEquals(BreathingKind.EXTERNAL_ATTENTION, catalogue.practice("external_grounding")!!.kind)
    }

    @Test fun c10_noShippedTextMakesAPhysiologicalOrSpiritualPromise() {
        val banned = listOf("dopamine", "cortisol", "hormone", "hrv", "heart rate", "guarantee",
            "will fall asleep", "cure", "heal", "nervous system", "reset your brain")
        val catalogue = shipped()
        val everything = catalogue.practices.flatMap { it.stages.values + it.title } +
            catalogue.globalGuidance + catalogue.languageFallback
        everything.forEach { text ->
            banned.forEach { assertFalse("\"$it\" appears in shipped content", text.lowercase().contains(it)) }
        }
    }

    @Test fun theValidatorRejectsEachContentMistakeItExistsToCatch() {
        fun problemsFor(mutate: (String) -> String): List<String> {
            val result = BreathingCatalog.parse(mutate(assetText()))
            assertTrue("expected invalid content", result is BreathingCatalogResult.Invalid)
            return (result as BreathingCatalogResult.Invalid).problems
        }
        assertTrue(problemsFor { it.replace("\"id\": \"breath_word\"", "\"id\": \"natural_anchor\"") }
            .any { it.contains("Duplicate practice id") })
        assertTrue(problemsFor { it.replace("\"prescribedBreathPhases\": []", "\"prescribedBreathPhases\": [\"inhale 4s\"]") }
            .any { it.contains("never prescribes breath timing") })
        assertTrue(problemsFor { it.replace("\"languageFallback\":", "\"languageFallbackRenamed\":") }
            .any { it.contains("languageFallback") })
        assertTrue(problemsFor { it.replace("\"156-A\"", "\"technique 12\"") }
            .any { it.contains("is not a verse label") })
        assertTrue(problemsFor { it.replace("\"work\": \"Vijnana-bhairava-tantra\"", "\"work\": \"\"") }
            .any { it.contains("source.work is missing") })
        assertTrue(problemsFor { it.replace("60,\n        180,\n        300", "45,\n        180,\n        300") }
            .any { it.contains("durations must be between") })
        assertTrue(problemsFor { it.replace("\"kind\": \"sound_attention\"", "\"kind\": \"pranayama\"") }
            .any { it.contains("unrecognised kind") })
        assertTrue(problemsFor { it.replace("\"requiresSoundConsent\": true", "\"requiresSoundConsent\": false") }
            .any { it.contains("must require sound consent") })
        assertTrue(problemsFor { it.replace("\"text\": \"\"", "\"text\": \"Sleep well.\"") }
            .any { it.contains("sleep practice must have no closing text") })
    }

    @Test fun shippedAssetMatchesTheGatingFixtureUsedByThePureTests() {
        // The pure recommender/session tests build their catalogue in Kotlin so they need no
        // Robolectric. This is the check that stops that duplicate from drifting away from reality.
        val catalogue = shipped()
        BreathingFixtures.catalogue.practices.forEach { fixture ->
            val real = catalogue.practice(fixture.id)
            assertNotNull("shipped asset is missing ${fixture.id}", real)
            real!!
            assertEquals(fixture.id, fixture.kind, real.kind)
            assertEquals(fixture.id, fixture.allowedDurationsSeconds, real.allowedDurationsSeconds)
            assertEquals(fixture.id, fixture.defaultDurationSeconds, real.defaultDurationSeconds)
            assertEquals(fixture.id, fixture.requiresBreathComfort, real.requiresBreathComfort)
            assertEquals(fixture.id, fixture.requiresSoundConsent, real.requiresSoundConsent)
            assertEquals(fixture.id, fixture.requiresPriorComfort, real.requiresPriorComfort)
            assertEquals(fixture.id, fixture.requiresSleepIntent, real.requiresSleepIntent)
            assertEquals(fixture.id, fixture.requiresSleepyNow, real.requiresSleepyNow)
            assertEquals(fixture.id, fixture.presentation.toneAsset != null, real.presentation.toneAsset != null)
            assertEquals(fixture.id, fixture.source == null, real.source == null)
        }
        assertEquals(BreathingFixtures.catalogue.practices.size, catalogue.practices.size)
    }

    /** Newlines normalised so the multi-line mutations below match: the asset is committed with
     * CRLF endings, and a pattern written with `\n` silently matches nothing against it. */
    private fun assetText(): String =
        RuntimeEnvironment.getApplication().assets.open(BreathingCatalog.ASSET_NAME)
            .bufferedReader().use { it.readText() }.replace("\r\n", "\n")
}
