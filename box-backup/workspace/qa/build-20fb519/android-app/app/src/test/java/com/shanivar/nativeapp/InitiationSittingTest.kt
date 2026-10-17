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

    @Test fun flowIsSixStepsInOrder() {
        val order = generateSequence(SittingStep.STOP) { s -> SittingFlow.next(s).takeIf { it != s } }.toList()
        assertEquals(listOf(SittingStep.STOP, SittingStep.SETTLE, SittingStep.SCAN, SittingStep.STAY,
            SittingStep.ASK, SittingStep.CLOSE), order)
    }

    @Test fun theClockMovesTheTimedMiddleOnAndSkipNeverMovesItBack() {
        assertEquals(SittingStep.SETTLE, SittingFlow.stepAtElapsed(0))
        assertEquals(SittingStep.SCAN, SittingFlow.stepAtElapsed(30))
        assertEquals(SittingStep.STAY, SittingFlow.stepAtElapsed(120))
        assertEquals(SittingStep.STAY, SittingFlow.shownStep(SittingStep.SETTLE, 150))
        assertEquals(SittingStep.STAY, SittingFlow.shownStep(SittingStep.STAY, 10))
        assertEquals(SittingStep.SCAN, SittingFlow.shownStep(SittingStep.SCAN, 10))
    }

    @Test fun afterThePracticeOnlyAFinishedTimerGoesOnToAsk() {
        assertEquals(SittingStep.ASK, SittingFlow.afterPractice(BreathingPhase.ENDED_TIMER))
        assertEquals(SittingStep.END, SittingFlow.afterPractice(BreathingPhase.ENDED_USER))
        assertEquals(SittingStep.END, SittingFlow.afterPractice(BreathingPhase.INTERRUPTED))
    }

    @Test fun theShippedCopyIsTheApprovedCopyAndHasNoBannedPhrases() {
        assertEquals("Stop.", InitiationSittingCopy.stop.title)
        assertEquals("Not now", InitiationSittingCopy.stop.endLabel)
        assertEquals("Scan, top to bottom.", InitiationSittingCopy.scan.title)
        assertTrue(InitiationSittingCopy.scan.lines.first().startsWith("Head, face, jaw, neck, shoulders, chest, stomach, hands, legs, feet"))
        assertEquals("Where do you want to reach?", InitiationSittingCopy.ASK_TITLE)
        assertEquals(60, SankalpStore.MAX_LENGTH)
        val all = InitiationSittingCopy.allText().joinToString("\n").lowercase()
        (SittingFlow.forbiddenWords + SittingFlow.bannedPhrases).forEach { assertFalse("banned: $it", all.contains(it)) }
        // The only breath word anywhere is the optional exhale line.
        assertTrue(all.contains("on the exhale"))
        assertFalse(all.contains("breath"))
    }

    @Test fun theScanStepDirectsNoBreathAtAll() {
        val scan = (listOf(InitiationSittingCopy.scan.title) + InitiationSittingCopy.scan.lines).joinToString(" ").lowercase()
        listOf("breath", "exhale", "inhale", "hold").forEach { assertFalse(it, scan.contains(it)) }
    }

    @Test fun iAllowIsOptionalAndNothingDependsOnIt() {
        assertTrue(InitiationSittingCopy.STAY_OPTIONAL.contains("Optional"))
        assertTrue(InitiationSittingCopy.STAY_OPTIONAL.contains("I allow"))
        assertTrue(InitiationSittingCopy.STAY_OPTIONAL.contains("leave this out"))
        assertFalse(InitiationSittingCopy.stay.lines.joinToString().contains("I allow"))
    }

    @Test fun noGuideAudioIsBundledAndTheHookNamesAreThePerStepKeys() {
        SittingStep.values().forEach { assertNull(SittingGuideAudio.clipResId(context, it)) }
        assertSame(SittingGuideAudio.None, InitiationSitting.guide)
        assertEquals("sitting_guide_settle", SittingGuideAudio.resourceName(SittingStep.SETTLE))
        assertEquals("sitting_guide_ask", SittingGuideAudio.resourceName(SittingStep.ASK))
    }

    @Test fun theHookIsCalledPerStepAndNeverPlaysAnythingByItself() {
        val seen = mutableListOf<SittingStep>()
        InitiationSitting.guide = object : SittingGuideAudio {
            override fun onStep(step: SittingStep) { seen += step }
            override fun onLeave() { seen += SittingStep.END }
        }
        InitiationSitting.begin(); InitiationSitting.skip(); InitiationSitting.end()
        assertEquals(listOf(SittingStep.STOP, SittingStep.SETTLE, SittingStep.END), seen)
        assertNull(InitiationSitting.step)
    }

    @Test fun the180SecondSittingCountsAsPractisedAndTheMinimumRuleIsUntouched() {
        assertEquals("body_scan_sitting", INITIATION_PRACTICE_ID)
        assertEquals(180, INITIATION_PRACTICE_SECONDS)
        assertEquals(90, MIN_PRACTISED_SECONDS)
        val row = BreathingRecord("s", INITIATION_PRACTICE_ID, "v1", 1, BreathingEntry.HOME,
            INITIATION_PRACTICE_SECONDS, INITIATION_PRACTICE_SECONDS, BreathingPhase.ENDED_TIMER)
        assertTrue(row.countsAsPractised)
        val short = BreathingRecord("s", INITIATION_PRACTICE_ID, "v1", 1, BreathingEntry.HOME, 180, 89, BreathingPhase.ENDED_USER)
        assertFalse(short.countsAsPractised)
    }

    @Test fun theBodyScanStageIsInCodeNotInTheCatalogueAndGroundingIsLeftAlone() {
        assertEquals(INITIATION_PRACTICE_ID, bodyScanPractice().id)
        assertFalse(bodyScanPractice().presentation.showCompletionQuestions)
        assertNull(bodyScanPractice().source)
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
    private fun tickTo(seconds: Long) {
        clock = seconds * 1000
        compose.runOnIdle { BreathingSessionOwner.tick(context) }
        compose.mainClock.advanceTimeBy(600)   // lets the on-screen loop notice the new time
        compose.waitForIdle()
    }
    private fun assertSkipAndEnd() {
        compose.onNodeWithText("Skip").assertExists(); compose.onNodeWithText("End").assertExists()
    }

    @Test fun entryIsOnTheSadhanaSelection() {
        open()
        compose.onNodeWithText("Your first sitting").assertExists()
    }

    @Test fun entryIsNotOfferedFromThePauseHosts() {
        compose.setContent { ShanivarTheme { BreathingHost(BreathingEntry.AFTER_ALARM, onExit = {}) } }
        compose.onAllNodesWithText("Your first sitting").assertCountEquals(0)
    }

    @Test fun beginAndSkipAreWithheldUntilSafeAndEveryStepHasSkipAndEnd() {
        open(); click("Start your first sitting")
        compose.onNodeWithText("Stop.").assertExists()
        compose.onNodeWithText("Begin").assertIsNotEnabled()
        compose.onNodeWithText("Skip").assertIsNotEnabled()
        compose.onNodeWithText("Not now").assertExists()
        click(InitiationSittingCopy.SAFE_CHIP)
        compose.onNodeWithText("Begin").assertIsEnabled()
        click("Begin")
        // Step 2, 3, 4: text, Skip, End (timer runs behind them)
        compose.onNodeWithText("Settle.").assertExists(); assertSkipAndEnd()
        compose.onNodeWithText("Notice how your body is held up.").assertExists()
        click("Skip")
        compose.onNodeWithText("Scan, top to bottom.").assertExists(); assertSkipAndEnd()
        click("Skip")
        compose.onNodeWithText("Stay with one spot.").assertExists(); assertSkipAndEnd()
        compose.onNodeWithText(InitiationSittingCopy.STAY_OPTIONAL).assertExists()
        click("Skip")
        // Step 5
        compose.onNodeWithText("Where do you want to reach?").assertExists(); assertSkipAndEnd()
        click("Skip")
        // Step 6
        compose.onNodeWithText("The time has passed.").assertExists()
        compose.onNodeWithText("Done").assertExists()
    }

    @Test fun beginStartsTheSessionAt180Seconds() {
        beginSitting()
        assertEquals(INITIATION_PRACTICE_SECONDS, BreathingSessionOwner.session?.durationSeconds)
        assertEquals(BreathingPhase.RUNNING, BreathingSessionOwner.session?.phase)
    }

    @Test fun notNowLeavesAtOnceWithNothingRecorded() {
        open(); click("Start your first sitting"); click("Not now")
        assertNull(InitiationSitting.step)
        assertNull(BreathingSessionOwner.session)
        assertTrue(BreathingPreferences(context).journal().isEmpty())
        compose.onNodeWithText("Breathe & reset").assertExists()
    }

    @Test fun theClockMovesSettleToScanToStay() {
        beginSitting()
        compose.onNodeWithText("Settle.").assertExists()
        tickTo(31); compose.onNodeWithText("Scan, top to bottom.").assertExists()
        tickTo(121); compose.onNodeWithText("Stay with one spot.").assertExists()
    }

    @Test fun skipMovesThroughEveryStepAndLeavesNoLineWithNoRowBeyondTheSkippedTimer() {
        beginSitting()
        click("Skip"); click("Skip"); click("Skip")
        compose.onNodeWithText("Where do you want to reach?").assertExists()
        click("Skip")
        compose.onNodeWithText("The time has passed.").assertExists()
        assertNull(SankalpStore(context).line)
        compose.onAllNodesWithText(InitiationSittingCopy.READY_LEAD).assertCountEquals(0)
    }

    @Test fun endDuringTheTimedMiddleGoesToTheEarlyEndScreenWithAShortFactualRow() {
        SankalpStore(context).save("Steady and kind")
        beginSitting()
        clock = 30_000
        click("End")
        compose.onNodeWithText("You stopped there.").assertExists()
        val row = BreathingPreferences(context).journal().single()
        assertFalse(row.countsAsPractised)
        assertEquals(BreathingPhase.ENDED_USER, row.outcome)
        click("Done"); assertNull(InitiationSitting.step)
    }

    @Test fun endOnAskLeavesAndEndIsOnEveryTimedStep() {
        beginSitting(); tickTo(31); compose.onNodeWithText("End").assertExists()
        tickTo(180)
        compose.onNodeWithText("Where do you want to reach?").assertExists()
        click("End")
        assertNull(InitiationSitting.step)
    }

    @Test fun full180SecondSittingCountsAsPractisedAndShowsTheLineBackOnClose() {
        beginSitting()
        tickTo(180)
        assertEquals(BreathingPhase.ENDED_TIMER, BreathingSessionOwner.session?.phase)
        compose.onNodeWithText("Where do you want to reach?").assertExists()
        compose.onNodeWithText("your words").performTextReplacement("Clear head, open hands")
        click("Keep")
        compose.onNodeWithText("The time has passed.").assertExists()
        compose.onNodeWithText(InitiationSittingCopy.READY_LEAD).assertExists()
        compose.onNodeWithText("Clear head, open hands").assertExists()
        val journal = BreathingPreferences(context).journal()
        assertEquals(1, journal.size)
        assertTrue(journal.single().countsAsPractised)
        assertEquals(180, journal.single().activeSeconds)
        assertEquals("Clear head, open hands", SankalpStore(context).line)
        assertFalse(context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).all.values.joinToString().contains("Clear head"))
        click("Done")
        assertNull(InitiationSitting.step)
    }

    @Test fun theTextFieldStopsAtSixtyCharactersAndClearEmptiesTheStoredLine() {
        SankalpStore(context).save("old line")
        beginSitting(); tickTo(180)
        compose.onNodeWithText("old line").assertExists()
        click("Clear")
        assertNull(SankalpStore(context).line)
        compose.onNodeWithText("your words").performTextInput("x".repeat(90))
        compose.onNodeWithText("x".repeat(60)).assertExists()
        click("Keep")
        assertEquals("x".repeat(60), SankalpStore(context).line)
    }

    @Test fun skipOnStayEndsTheTimerOnceAndGoesOnToAsk() {
        beginSitting(); click("Skip"); click("Skip")
        clock = 130_000
        click("Skip")
        compose.onNodeWithText("Where do you want to reach?").assertExists()
        assertEquals(1, BreathingPreferences(context).journal().size)
    }
}
