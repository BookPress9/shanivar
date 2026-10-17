package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class FakeSadhanaClock(var wallMs: Long = 1_760_300_000_000, var monoMs: Long = 0) : SadhanaClock {
    override fun wall() = wallMs
    override fun mono() = monoMs
    override fun zone(): ZoneId = ZoneId.of("Europe/Berlin")
    fun advance(ms: Long) { wallMs += ms; monoMs += ms }
}

class SadhanaControllerTest {
    private val store = MemDraftStore()
    private val clock = FakeSadhanaClock()
    private fun c() = SadhanaController(store, clock, allowUnreviewed = true)

    @Test fun beginNeedsStationary() {
        val c = c(); c.go(SadhanaView.PREPARE); c.begin()
        assertEquals(SadhanaView.PREPARE, c.ui.view)
        assertEquals("Confirm you are stationary, or return without beginning.", c.ui.message)
    }
    @Test fun fullPracticeCompletesAndRecords() {
        val c = c(); c.setStationary(true); c.begin()
        assertEquals(SadhanaView.PLAYER, c.ui.view)
        clock.advance(60_000); c.tick()
        assertEquals(SadhanaView.COMPLETE, c.ui.view); assertEquals(SadhanaOutcome.COMPLETED, c.ui.lastOutcome)
        assertEquals(1, c.record.attemptCount)
        assertTrue(c.hasCapability(SadhanaCapability.ARRIVE))
    }
    @Test fun cannotNavigateAwayFromActivePractice() {
        val c = c(); c.setStationary(true); c.begin(); c.go(SadhanaView.HOME)
        assertEquals(SadhanaView.PLAYER, c.ui.view)
    }
    @Test fun processDeathRestoresInterruptedNotCompleted() {
        val c = c(); c.setStationary(true); c.begin(); clock.advance(20_000); c.background()
        clock.advance(10_000_000)
        val again = c()
        assertEquals(SadhanaView.HOME, again.ui.view)
        assertEquals(SadhanaOutcome.INTERRUPTED, again.record.attempts.single().outcome)
        assertEquals(20, again.record.attempts.single().activeSeconds)
    }
    @Test fun recordPersistsAcrossReload() {
        val c = c(); c.setStationary(true); c.begin(); clock.advance(5_000); c.end()
        c.reflect(Usefulness.UNCOMFORTABLE); assertNotNull(c.ui.message)
        c.go(SadhanaView.INDEPENDENT); c.recordOwn()
        val again = c()
        assertEquals(1, again.record.attemptCount); assertEquals(1, again.record.independentCount)
        assertEquals(Usefulness.UNCOMFORTABLE, again.record.reflections.values.single().usefulness)
        assertEquals(c.record.attempts.single().events.size, again.record.attempts.single().events.size)
    }
    @Test fun hideGuidanceRecordsNothing() {
        val c = c(); c.leaveToPractise()
        assertEquals(0, c.record.independentCount); assertTrue(c.minimal)
    }
    @Test fun releaseBuildBlocksUnreviewed() {
        val c = SadhanaController(store, clock, allowUnreviewed = false); c.setStationary(true); c.begin()
        assertEquals(SadhanaView.HOME, c.ui.view); assertNotNull(c.ui.message)
    }
    @Test fun anchorChangePausesWithMessage() {
        val c = c(); c.setStationary(true); c.begin(); c.changeAnchor()
        assertFalse(c.running); assertEquals("Anchor changed to the room. Resume when ready.", c.ui.message)
    }

    @Test fun selfCheckPersistsLocallyAndClears() {
        val c = c(); c.setSelfCheck(SelfCheck.LOW)
        assertEquals(SelfCheck.LOW, c().ui.selfCheck)
        c.setSelfCheck(SelfCheck.LOW); assertNull(c().ui.selfCheck)
    }

    @org.junit.Test
    fun initiationAttemptImportedOnceAndUnfinishedIgnored() {
        val done = PracticeAttempt("a1", "v1", 1000, 60_000, AttemptOutcome.COMPLETED, 61_000)
        val c1 = SadhanaController(store, clock, true, initiationAttempt = { done })
        org.junit.Assert.assertEquals(1, c1.record.attempts.count { it.id == "initiation-a1" && it.outcome == SadhanaOutcome.COMPLETED })
        val c2 = SadhanaController(store, clock, true, initiationAttempt = { done })
        org.junit.Assert.assertEquals(1, c2.record.attempts.size)
        org.junit.Assert.assertNull(InitiationImport.toSadhana(done.copy(outcome = AttemptOutcome.IN_PROGRESS), clock.zone()))
    }

    @org.junit.Test
    fun selfReportActualMinutesEditableAndPersisted() {
        val c = c(); c.setOwnMinutes(3); c.recordOwn()
        val id = c.record.independent.single().id
        c.adjustOwnActual(id, 2); c.adjustOwnActual(id, -10)
        org.junit.Assert.assertEquals(1, c.record.independent.single().actualMinutes)
        c.adjustOwnActual(id, 4)
        org.junit.Assert.assertEquals(5, c().record.independent.single().actualMinutes)
        org.junit.Assert.assertEquals(0, c().record.attemptCount)
    }
}
