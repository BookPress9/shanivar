package com.shanivar.nativeapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class RoutineBoundaryLinkTest {
    @Test
    fun onlyAnEnabledSavedWindowLinksChoose() {
        val w = ProtectionWindowV1("w", LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(1))
        assertFalse(RoutineBoundaryLink.linked(RoutinePlanV1()))
        assertFalse(RoutineBoundaryLink.linked(RoutinePlanV1(windows = listOf(w.copy(enabled = false)))))
        assertFalse(RoutineBoundaryLink.linked(RoutinePlanV1(windows = listOf(w.copy(startDays = emptySet())))))
        assertTrue(RoutineBoundaryLink.linked(RoutinePlanV1(windows = listOf(w))))
    }
}
