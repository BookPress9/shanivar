package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LivingOrbitTest {
    private val d1 = LocalDate.parse("2026-10-12")
    private fun ev(id: String, t: OrbitEventType, d: LocalDate = d1, s: OrbitSource = OrbitSource.CONFIGURED) = OrbitEvent(id, t, d, s)

    @Test fun dimInitially() {
        val v = LivingOrbit.derive(emptyList())
        assertFalse(v.ring || v.alarmPoint || v.boundaryPoint || v.practicePoint || v.secondPresence)
        assertEquals("Routine not yet created", v.description())
    }

    @Test fun ringAndThreePoints() {
        val log = OrbitLog()
        log.append(ev("r", OrbitEventType.ROUTINE_CREATED)); log.append(ev("a", OrbitEventType.ALARM_CONFIGURED))
        log.append(ev("b", OrbitEventType.BOUNDARY_SAVED)); log.append(ev("p", OrbitEventType.PRACTICE_ATTEMPTED, s = OrbitSource.DEVICE_OBSERVED))
        val v = LivingOrbit.derive(log.all)
        assertTrue(v.ring && v.alarmPoint && v.boundaryPoint && v.practicePoint); assertFalse(v.secondPresence)
    }

    @Test fun glowNeedsTwoDistinctReturnDaysNotSilence() {
        val log = OrbitLog(listOf(ev("r", OrbitEventType.ROUTINE_CREATED)))
        assertFalse(log.recordVisit(d1)) // same day is not a return
        assertTrue(log.recordVisit(d1.plusDays(1)))
        assertFalse(log.recordVisit(d1.plusDays(1))) // once per day
        assertFalse(LivingOrbit.derive(log.all).secondPresence)
        assertTrue(log.recordVisit(d1.plusDays(9))) // missed days in between erase nothing
        assertTrue(LivingOrbit.derive(log.all).secondPresence)
    }

    @Test fun independentRecordMustBeSelfReported() {
        val log = OrbitLog()
        assertFalse(log.append(ev("i1", OrbitEventType.INDEPENDENT_RECORD, s = OrbitSource.DEVICE_OBSERVED)))
        assertTrue(log.append(ev("i2", OrbitEventType.INDEPENDENT_RECORD, s = OrbitSource.SELF_REPORTED)))
        assertTrue(LivingOrbit.derive(log.all).secondPresence)
    }

    @Test fun appendOnlyAndDeduplicated() {
        val log = OrbitLog()
        assertTrue(log.append(ev("a", OrbitEventType.ALARM_CONFIGURED)))
        assertFalse(log.append(ev("a", OrbitEventType.ALARM_CONFIGURED)))
        assertEquals(1, log.all.size)
    }

    @Test fun firstWeekPromptsDismissibleAndUnlockNothing() {
        val p = FirstWeekPrompts(listOf(FirstWeekPrompt("x", 1, "Add one activity"), FirstWeekPrompt("y", 3, "Try a second minute")))
        assertEquals(1, p.visible(1).size); assertEquals(2, p.visible(3).size)
        p.dismiss("x"); assertEquals(listOf("y"), p.visible(3).map { it.id })
        assertTrue(p.visible(8).isEmpty()); assertFalse(p.unlocksAnything())
    }
}
