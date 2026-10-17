package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Section D of VBT_BREATHING_TEST_GUIDE_2026-09-25.md: transition ordering and terminal-event
 * idempotency, with an injected monotonic clock.
 *
 * These tests cannot prove a speaker actually went quiet - only a physical device can, and the guide
 * says so explicitly. What they do prove is that the state machine never reports a completion that
 * did not happen, never loses paused time, and never produces two terminal records for one session.
 */
class BreathingSessionTest {
    private val practice = BreathingFixtures.naturalAnchor
    private val sleep = BreathingFixtures.sleepThreshold

    private fun session(practice: BreathingPractice = this.practice, seconds: Int = 60) =
        BreathingSession.of(practice, seconds, "session-1")

    @Test fun s01_asecondStartCannotCreateASecondTimer() {
        val started = session().start(1_000)
        val again = started.start(9_000)
        assertSame(started, again)
        assertEquals(1_000L, again.countingSinceMillis)
        assertEquals("session-1", again.sessionId)
    }

    @Test fun s02_pausedTimeIsExcludedFromTheRemainingDuration() {
        val paused = session().start(0).pause(20_000)
        assertEquals(BreathingPhase.PAUSED, paused.phase)
        assertEquals(40_000L, paused.remainingMillis(20_000))
        // Waiting 30s while paused costs nothing.
        assertEquals(40_000L, paused.remainingMillis(50_000))
        val resumed = paused.resume(50_000)
        assertEquals(40_000L, resumed.remainingMillis(50_000))
        assertEquals(10_000L, resumed.remainingMillis(80_000))
    }

    @Test fun s03_repeatedPausingNearZeroNeitherGoesNegativeNorSkipsTheFinish() {
        var current = session().start(0)
        repeat(10) { index ->
            current = current.pause(1_000L * index).resume(1_000L * index)
        }
        assertTrue(current.remainingMillis(120_000) == 0L)
        val finished = current.pause(120_000)
        assertEquals(BreathingPhase.ENDED_TIMER, finished.phase)
        // A second terminal event does not overwrite the first one.
        assertEquals(BreathingPhase.ENDED_TIMER, finished.endByUser(130_000).phase)
        assertEquals(BreathingPhase.ENDED_TIMER, finished.interrupt(130_000).phase)
    }

    @Test fun s04_endingIsImmediateFromEveryPhaseAndRecordedOnce() {
        listOf(
            session(),
            session().start(0),
            session().start(0).pause(5_000),
            session(sleep, 60).start(0).tick(31_000),
        ).forEach { state ->
            val ended = state.endByUser(40_000)
            assertEquals(BreathingPhase.ENDED_USER, ended.phase)
            assertSame(ended, ended.endByUser(41_000))
        }
    }

    @Test fun s05_stateAndCuesSurviveRecompositionWithoutReplaying() {
        val running = session().start(0).withCuePlayed(BreathingStageId.ARRIVAL)
        val restored = running.copy()
        assertEquals(setOf(BreathingStageId.ARRIVAL), restored.cuesPlayed)
        assertEquals(running.remainingMillis(10_000), restored.remainingMillis(10_000))
        assertEquals(running.sessionId, restored.sessionId)
    }

    @Test fun s07_losingTheForegroundPausesOrdinaryPracticeAndNeverReplaysMissedCues() {
        val backgrounded = session().start(0).withCuePlayed(BreathingStageId.ARRIVAL).onHostBackgrounded(15_000)
        assertEquals(BreathingPhase.PAUSED, backgrounded.phase)
        val resumed = backgrounded.resume(300_000)
        assertEquals(45_000L, resumed.remainingMillis(300_000))
        assertEquals(setOf(BreathingStageId.ARRIVAL), resumed.cuesPlayed)
    }

    @Test fun s08_sleepPracticeRestsSilentlyAndKeepsRestingWhenTheScreenLocks() {
        val resting = session(sleep, 180).start(0).tick(30_000)
        assertEquals(BreathingPhase.RESTING_SILENTLY, resting.phase)
        assertFalse(sleep.presentation.keepScreenOn)
        assertFalse(sleep.presentation.closingBell)
        // Putting the phone down is the instruction here, so backgrounding is not an interruption.
        assertSame(resting, resting.onHostBackgrounded(60_000))
    }

    @Test fun s09_returningAfterTheSilentIntervalEndsOnTheTimerAndClaimsNothingMore() {
        val ended = session(sleep, 180).start(0).tick(30_000).tick(180_000)
        assertEquals(BreathingPhase.ENDED_TIMER, ended.phase)
        assertEquals(0L, ended.remainingMillis(400_000))
        assertNotEquals(BreathingPhase.ENDED_USER, ended.phase)
    }

    @Test fun s10_aSessionLostToProcessDeathIsInterruptedRatherThanCompleted() {
        listOf(session().start(0), session().start(0).pause(5_000), session(sleep, 180).start(0).tick(30_000))
            .forEach { state ->
                val interrupted = state.interrupt(10_000)
                assertEquals(BreathingPhase.INTERRUPTED, interrupted.phase)
                assertTrue(interrupted.phase.terminal)
                assertSame(interrupted, interrupted.interrupt(20_000))
            }
    }

    @Test fun s14_switchingPracticeBeforeStartingLeavesNoRunningSession() {
        val ready = BreathingSession.of(BreathingFixtures.emotionSpace, 60, "session-2")
        assertEquals(BreathingPhase.READY, ready.phase)
        assertEquals(null, ready.countingSinceMillis)
        assertEquals(60_000L, ready.remainingMillis(999_000))
    }

    @Test fun stagePlansSumExactlyToTheSelectedDurationAndSleepHasNoClosing() {
        listOf(60, 180, 300).forEach { duration ->
            assertEquals(duration, practice.stagePlan(duration).sumOf { it.seconds })
            assertEquals(duration, sleep.stagePlan(duration).sumOf { it.seconds })
        }
        assertTrue(practice.stagePlan(60).any { it.stage == BreathingStageId.CLOSING })
        assertFalse(sleep.stagePlan(60).any { it.stage == BreathingStageId.CLOSING })
    }

    @Test fun theCurrentStageFollowsAccumulatedActiveTimeNotWallClockTime() {
        val running = session(practice, 180).start(0)
        assertEquals(BreathingStageId.ARRIVAL, running.currentStage(practice, 5_000)!!.stage)
        assertEquals(BreathingStageId.INSTRUCTION, running.currentStage(practice, 15_000)!!.stage)
        assertEquals(BreathingStageId.PRACTICE, running.currentStage(practice, 40_000)!!.stage)
        assertEquals(BreathingStageId.CLOSING, running.currentStage(practice, 175_000)!!.stage)
        // Pausing freezes the stage as well as the countdown: an hour of wall time changes nothing.
        val paused = running.pause(15_000)
        assertEquals(BreathingStageId.INSTRUCTION, paused.currentStage(practice, 3_615_000)!!.stage)
    }

    @Test fun theJournalKeepsOnlyTheNewestHundredRecords() {
        val records = (1..150).map {
            BreathingRecord(
                sessionId = "s$it", practiceId = "external_grounding", catalogVersion = "v1", contentVersion = 1,
                entryContext = BreathingEntry.HOME, selectedDurationSeconds = 60, activeSeconds = 60,
                outcome = BreathingPhase.ENDED_TIMER,
            )
        }
        val trimmed = trimBreathingJournal(records)
        assertEquals(MAX_BREATHING_RECORDS, trimmed.size)
        assertEquals("s51", trimmed.first().sessionId)
        assertEquals("s150", trimmed.last().sessionId)
    }
}
