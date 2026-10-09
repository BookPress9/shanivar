package com.shanivar.nativeapp

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import java.time.ZonedDateTime
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Pure flow rules, copy rules, and the UI walk-through of "Your first sitting" (Phase A). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class InitiationSittingTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private var clock = 0L

    @Before fun reset() {
        context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).edit().clear().commit()
        context.getSharedPreferences(SankalpStore.NAMESPACE, 0).edit().clear().commit()
        BreathingSessionOwner.resetForTest()
        InitiationSitting.resetForTest()
        clock = 0L
        BreathingSessionOwner.monotonicMillis = { clock }
        BreathingSessionOwner.wallClock = { ZonedDateTime.of(2026, 10, 9, 14, 0, 0, 0, BreathingFixtures.ZONE) }
        BreathingSessionOwner.newSessionId = { "sitting-session" }
    }

    @After fun after() { BreathingSessionOwner.resetForTest(); InitiationSitting.resetForTest() }

    // ---- pure ----

    @Test fun flowIsArriveThenOnePracticeThenCloseThenEnd() {
        val order = generateSequence(SittingStep.STOP) { s -> SittingFlow.next(s).takeIf { it != s } }.toList()
        assertEquals(
            listOf(SittingStep.STOP, SittingStep.ARRIVE, SittingStep.NOISE, SittingStep.PRACTICE,
                SittingStep.QUESTION, SittingStep.LINE, SittingStep.END), order)
    }

    @Test fun afterThePracticeOnlyAFinishedTimerGoesOnToTheClose() {
        assertEquals(SittingStep.QUESTION, SittingFlow.afterPractice(BreathingPhase.ENDED_TIMER))
        assertEquals(SittingStep.END, SittingFlow.afterPractice(BreathingPhase.ENDED_USER))
        assertEquals(SittingStep.END, SittingFlow.afterPractice(BreathingPhase.INTERRUPTED))
    }

    @Test fun everyFrameScreenCarriesAnEndAndTheCopyIsTheApprovedCopy() {
        assertEquals("Stop for three minutes.", InitiationSittingCopy.stop.title)
        assertEquals("Not now", InitiationSittingCopy.stop.endLabel)
        assertEquals("Where do you want to reach?", InitiationSittingCopy.question.title)
        assertEquals(60, SankalpStore.MAX_LENGTH)
        val all = InitiationSittingCopy.allText().joinToString("\n").lowercase()
        SittingFlow.forbiddenWords.forEach { assertFalse("forbidden: $it", all.contains(it)) }
        assertFalse(all.contains("breathe in"))
    }

    @Test fun noGuideAudioIsBundledAndTheHookDefaultsToSilence() {
        SittingStep.values().forEach { assertNull(SittingGuideAudio.clipResId(context, it)) }
        assertSame(SittingGuideAudio.None, InitiationSitting.guide)
        assertEquals("sitting_guide_question", SittingGuideAudio.resourceName(SittingStep.QUESTION))
    }

    @Test fun theHookIsCalledPerStepAndNeverPlaysAnythingByItself() {
        val seen = mutableListOf<SittingStep>()
        InitiationSitting.guide = object : SittingGuideAudio {
            override fun onStep(step: SittingStep) { seen += step }
            override fun onLeave() { seen += SittingStep.END }
        }
        InitiationSitting.begin(); InitiationSitting.skip(); InitiationSitting.end()
        assertEquals(listOf(SittingStep.STOP, SittingStep.ARRIVE, SittingStep.END), seen)
        assertNull(InitiationSitting.step)
    }

    @Test fun theSittingPracticeIsTheCatalogueGroundingAt180SecondsAndTheMinimumRuleIsUntouched() {
        assertEquals("external_grounding", INITIATION_PRACTICE_ID)
        assertEquals(180, INITIATION_PRACTICE_SECONDS)
        assertEquals(90, MIN_PRACTISED_SECONDS)
        val row = BreathingRecord("s", INITIATION_PRACTICE_ID, "v1", 1, BreathingEntry.HOME,
            INITIATION_PRACTICE_SECONDS, INITIATION_PRACTICE_SECONDS, BreathingPhase.ENDED_TIMER)
        assertTrue(row.countsAsPractised)
    }

    // ---- UI ----

    private fun open() { compose.setContent { ShanivarTheme { BreathingHost(BreathingEntry.HOME, onExit = {}) } } }
    private fun click(text: String) { compose.onNodeWithText(text).performScrollTo().performClick(); compose.waitForIdle() }
    private fun beginSitting() {
        open()
        click("Start your first sitting")
        click(InitiationSittingCopy.SAFE_CHIP)
        click("Begin")
    }
    private fun runToReady() { beginSitting(); click("Continue"); click("Continue") }

    @Test fun entryIsOnTheSadhanaSelection() {
        open()
        compose.onNodeWithText("Your first sitting").assertExists()
    }

    @Test fun entryIsNotOfferedFromThePauseHosts() {
        compose.setContent { ShanivarTheme { BreathingHost(BreathingEntry.AFTER_ALARM, onExit = {}) } }
        compose.onAllNodesWithText("Your first sitting").assertCountEquals(0)
    }

    @Test fun beginIsWithheldUntilSafeAndEveryFrameHasSkipAndEnd() {
        open(); click("Start your first sitting")
        compose.onNodeWithText("Begin").assertIsNotEnabled()
        compose.onNodeWithText("Skip").assertExists(); compose.onNodeWithText("Not now").assertExists()
        click(InitiationSittingCopy.SAFE_CHIP)
        compose.onNodeWithText("Begin").assertIsEnabled()
        click("Begin")
        listOf("Feel what holds you up.", "Let the noise be noise.").forEach {
            compose.onNodeWithText(it).assertExists()
            compose.onNodeWithText("Skip").assertExists(); compose.onNodeWithText("End").assertExists()
            click("Continue")
        }
        compose.onNodeWithText("Notice the room").assertExists()
        compose.onNodeWithText("Skip").assertExists(); compose.onNodeWithText("End").assertExists()
    }

    @Test fun endOnAnyScreenLeavesAtOnceWithNothingRecorded() {
        beginSitting(); click("Continue"); click("End")
        assertNull(InitiationSitting.step)
        assertNull(BreathingSessionOwner.session)
        assertTrue(BreathingPreferences(context).journal().isEmpty())
        compose.onNodeWithText("Breathe & reset").assertExists()
    }

    @Test fun skipMovesThroughEveryFrameAndThePracticeWithoutAJournalRow() {
        beginSitting()
        click("Skip"); click("Skip"); click("Skip")   // arrive, noise, practice (Ready)
        compose.onNodeWithText("Where do you want to reach?").assertExists()
        click("Skip")
        compose.onNodeWithText("Write it in your own words.").assertExists()
        click("Leave it blank")
        compose.onNodeWithText("You stopped there.").assertExists()
        assertNull(SankalpStore(context).line)
        assertTrue(BreathingPreferences(context).journal().isEmpty())
    }

    @Test fun fullSittingAt180SecondsCountsAsPractisedAndShowsTheLineBackOnReadyAndEnd() {
        SankalpStore(context).save("Steady and kind")
        runToReady()
        // Ready shows the line verbatim.
        compose.onNodeWithText("Steady and kind").assertExists()
        click("Start")
        assertEquals(INITIATION_PRACTICE_SECONDS, BreathingSessionOwner.session?.durationSeconds)
        clock = 180_000
        compose.runOnIdle { BreathingSessionOwner.tick(context) }
        compose.waitForIdle()
        assertEquals(BreathingPhase.ENDED_TIMER, BreathingSessionOwner.session?.phase)
        compose.onNodeWithText("Where do you want to reach?").assertExists()
        click("Continue")
        // Write a new line, keep it.
        compose.onNodeWithText("your words").performTextReplacement("Clear head, open hands")
        click("Keep")
        compose.onNodeWithText("The time has passed.").assertExists()
        compose.onNodeWithText("Clear head, open hands").assertExists()
        val journal = BreathingPreferences(context).journal()
        assertEquals(1, journal.size)
        assertTrue(journal.single().countsAsPractised)
        assertEquals(180, journal.single().activeSeconds)
        assertEquals("Clear head, open hands", SankalpStore(context).line)
        // The line is not in the journal.
        assertFalse(context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).all.values.joinToString().contains("Clear head"))
        click("Done")
        assertNull(InitiationSitting.step)
    }

    @Test fun theTextFieldStopsAtSixtyCharactersAndClearEmptiesTheStoredLine() {
        SankalpStore(context).save("old line")
        beginSitting(); click("Skip"); click("Skip"); click("Skip"); click("Skip")
        compose.onNodeWithText("old line").assertExists()
        click("Clear")
        assertNull(SankalpStore(context).line)
        compose.onNodeWithText("your words").performTextInput("x".repeat(90))
        compose.onNodeWithText("x".repeat(60)).assertExists()
        click("Keep")
        assertEquals("x".repeat(60), SankalpStore(context).line)
    }

    @Test fun endingThePracticeEarlyGoesStraightToTheEndScreenAndTheShortRowIsStillFactual() {
        runToReady(); click("Start")
        clock = 30_000
        click("End")
        compose.onNodeWithText("You stopped there.").assertExists()
        val row = BreathingPreferences(context).journal().single()
        assertFalse(row.countsAsPractised)
        assertEquals(BreathingPhase.ENDED_USER, row.outcome)
    }

    @Test fun skipDuringThePracticeEndsItOnceAndGoesOnToTheClose() {
        runToReady(); click("Start")
        clock = 100_000
        click("Skip to the close")
        compose.onNodeWithText("Where do you want to reach?").assertExists()
        assertEquals(1, BreathingPreferences(context).journal().size)
    }
}
