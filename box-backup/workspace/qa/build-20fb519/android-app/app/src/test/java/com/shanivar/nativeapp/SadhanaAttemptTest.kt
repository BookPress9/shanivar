package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test

class SadhanaAttemptTest {
    private val natural = SadhanaCatalog.byId("natural")!!
    private val saved = mutableListOf<SadhanaAttempt>()
    private fun tracker() = SadhanaAttemptTracker { saved += it }
    private fun SadhanaAttemptTracker.go(mono: Long = 0, min: Int = 1, stationary: Boolean = true) =
        start(natural, min, SadhanaAnchor.BREATH, stationary, true, 1_000, mono, "Europe/Berlin", "a1")

    @Test fun needsStationaryConfirmation() { assertFalse(tracker().go(stationary = false)) }
    @Test fun researchOnlyCannotStart() {
        val t = tracker()
        assertFalse(t.start(SadhanaCatalog.researchOnly.first(), 1, SadhanaAnchor.BREATH, true, true, 0, 0, "UTC"))
    }
    @Test fun unreviewedDurationRejected() { assertFalse(tracker().go(min = 20)) }
    @Test fun onlyOnePracticeAtATime() { val t = tracker(); assertTrue(t.go()); assertFalse(t.go()) }

    @Test fun pausedAndBackgroundTimeExcluded() {
        val t = tracker(); t.go(0)
        t.tick(10_000, 0); t.pause(PauseReason.USER, 10_000, 0)
        t.tick(500_000, 0)
        t.resume(500_000, 0); t.pause(PauseReason.BACKGROUND, 505_000, 0)
        assertEquals(15, t.attempt!!.activeSeconds)
        assertEquals(SadhanaOutcome.IN_PROGRESS, t.attempt!!.outcome)
        assertEquals(45_000, t.tick(900_000, 0))
    }

    @Test fun completesOnlyFromActiveTime() {
        val t = tracker(); t.go(0)
        assertEquals(0, t.tick(60_000, 2_000))
        assertEquals(SadhanaOutcome.COMPLETED, t.attempt!!.outcome)
        assertEquals(60, t.attempt!!.activeSeconds)
    }

    @Test fun wallClockExpiryNeverCompletes() {
        val t = tracker(); t.go(0); t.pause(PauseReason.BACKGROUND, 1_000, 0)
        t.tick(10_000_000, 99_999_999_999)
        assertEquals(SadhanaOutcome.IN_PROGRESS, t.attempt!!.outcome)
    }

    @Test fun processDeathBecomesInterrupted() {
        val t = tracker(); t.go(0); t.pause(PauseReason.BACKGROUND, 20_000, 0)
        val fresh = tracker(); fresh.restore(t.attempt!!, 5_000)
        assertEquals(SadhanaOutcome.INTERRUPTED, fresh.attempt!!.outcome)
        assertEquals(20, fresh.attempt!!.activeSeconds)
        assertFalse(fresh.isRunning)
    }

    @Test fun endKeepsActiveSecondsAndIsEnded() {
        val t = tracker(); t.go(0); t.end(12_000, 3_000)
        assertEquals(SadhanaOutcome.ENDED, t.attempt!!.outcome); assertEquals(12, t.attempt!!.activeSeconds)
    }

    @Test fun anchorChangePausesAndIsLoggedNotFailure() {
        val t = tracker(); t.go(0)
        assertEquals(SadhanaAnchor.ROOM, t.changeAnchor(5_000, 0))
        assertFalse(t.isRunning)
        assertEquals(SadhanaOutcome.IN_PROGRESS, t.attempt!!.outcome)
        val types = t.attempt!!.events.map { it.type }
        assertTrue(types.containsAll(listOf(SadhanaEventType.PRACTICE_PAUSED, SadhanaEventType.ANCHOR_CHANGED)))
        assertEquals(1, t.attempt!!.anchorChanges)
    }
}
