package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test

class SadhanaStage1Test {
    private val clock = FakeSadhanaClock()

    @Test fun stage1Is60sNoBreathPacingAndHonestCopy() {
        val p = SadhanaCatalog.byId(Stage1.ID)!!
        assertEquals(listOf(1), p.minutes)
        assertTrue(SadhanaCatalog.canLaunch(p, allowUnreviewed = true))
        val lines = listOf(0L, 20L, 50L).map { SadhanaGuidance.line(p, GuidanceDetail.FULL, false, 60, it) } + p.prompt + p.title
        lines.forEach { l -> listOf("inhale", "exhale", "breathe in", "breathe out", "energy", "kundalini", "chakra", "score", "streak").forEach {
            assertFalse("$it in $l", l.lowercase().contains(it)) } }
        assertTrue(lines[0].contains("base of the spine"))
    }

    @Test fun selfCheckAfterStage1StoredLocallyAndQueued() {
        val store = MemDraftStore(); val outStore = MemDraftStore()
        val c = SadhanaController(store, clock, true, outbox = SyncOutbox(outStore))
        c.chooseStage1(); c.setStationary(true); c.begin()
        clock.monoMs += 61_000; clock.wallMs += 61_000; c.tick()
        assertEquals(SadhanaView.COMPLETE, c.ui.view)
        assertNotNull(c.stage1Check)
        c.stage1Reach(Stage1Reach.PARTLY)
        repeat(12) { c.stage1Drifts(1) }
        assertEquals("10+", Stage1.driftLabel(c.stage1Check!!.drifts))
        c.stage1Drifts(-1); c.stage1Drifts(-1)
        c.stage1Reason(DriftReason.THOUGHTS); c.stage1Reason(DriftReason.PHONE); c.stage1Reason(DriftReason.PHONE)
        c.stage1Note("a dog | barked")
        c.saveStage1Check()
        assertTrue(c.stage1Saved)
        val saved = SadhanaController(store, clock, true).record.stage1Checks.single()
        assertEquals(Stage1Reach.PARTLY, saved.reach); assertEquals(8, saved.drifts)
        assertEquals(setOf(DriftReason.THOUGHTS), saved.reasons); assertEquals("a dog | barked", saved.note)
        val q = SyncOutbox(outStore).pending
        assertTrue(q.any { it.kind == "attempt" })
        val sr = q.single { it.kind == "self_report" }.payload
        assertTrue(sr.contains("\"reached_spine\":\"partly\"") && sr.contains("\"drift_count\":8") && sr.contains("\"drift_reasons\":[\"thoughts\"]"))
    }

    @Test fun otherPracticesHaveNoStage1CheckAndEmptyCheckIsNotSaved() {
        val c = SadhanaController(MemDraftStore(), clock, true)
        c.setStationary(true); c.begin(); c.end()
        assertNull(c.stage1Check)
        val c2 = SadhanaController(MemDraftStore(), clock, true)
        c2.chooseStage1(); c2.setStationary(true); c2.begin(); clock.monoMs += 5_000; c2.tick(); c2.end()
        c2.saveStage1Check()
        assertTrue(c2.record.stage1Checks.isEmpty())
    }
}
