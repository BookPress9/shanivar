package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeAttemptTest {
    private val saved = mutableListOf<PracticeAttempt>()
    private fun tracker() = PracticeAttemptTracker(persist = { saved += it })

    @Test fun needsStationaryConfirmation() {
        assertFalse(tracker().start(false, "steady-v1", 0, 0))
        assertTrue(saved.isEmpty())
    }

    @Test fun completesOnlyAfterSixtyActiveSeconds() {
        val t = tracker(); t.start(true, "steady-v1", 0, 0, "a")
        assertEquals(1_000L, t.tick(59_000, 59_000))
        assertEquals(AttemptOutcome.IN_PROGRESS, t.attempt!!.outcome)
        assertEquals(0L, t.tick(60_000, 60_000))
        assertEquals(AttemptOutcome.COMPLETED, saved.last().outcome)
        assertEquals(60L, saved.last().activeSeconds)
    }

    @Test fun backgroundPausesAndIsPersisted() {
        val t = tracker(); t.start(true, "v", 0, 0)
        t.onBackground(20_000)
        assertEquals(20_000L, saved.last().activeMillis)
        assertEquals(40_000L, t.tick(500_000, 500_000)) // time away not counted
        t.resume(500_000)
        assertEquals(30_000L, t.tick(510_000, 510_000))
    }

    @Test fun killRestoreNeverInfersCompletion() {
        val t = tracker(); t.start(true, "v", 0, 0); t.onBackground(10_000)
        val t2 = tracker(); t2.restore(saved.last())
        assertFalse(t2.isRunning)
        assertEquals(50_000L, t2.tick(10_000_000, 10_000_000))
        assertEquals(AttemptOutcome.IN_PROGRESS, t2.attempt!!.outcome)
    }

    @Test fun earlyEndRecordsEndedNotCompleted() {
        val t = tracker(); t.start(true, "v", 0, 0)
        t.end(15_000, 15_000)
        assertEquals(AttemptOutcome.ENDED, saved.last().outcome)
        assertEquals(15L, saved.last().activeSeconds)
        t.tick(90_000, 90_000)
        assertEquals(AttemptOutcome.ENDED, t.attempt!!.outcome)
    }

    @Test fun pausedTimeNotCounted() {
        val t = tracker(); t.start(true, "v", 0, 0)
        t.pause(5_000); t.resume(100_000)
        assertEquals(50_000L, t.tick(105_000, 105_000))
    }
}
