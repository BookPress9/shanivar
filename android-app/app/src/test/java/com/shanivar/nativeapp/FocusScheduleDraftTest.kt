package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FocusScheduleDraftTest {
    @Test fun presetsUseOneRuleShapeAndRetainChosenActivity() {
        val initial = FocusScheduleDraft(intention = "Read", packages = setOf("com.instagram.android"))
        val allDay = initial.preset("all_day")
        assertEquals((0..6).toSet(), allDay.days)
        assertEquals("00:00", allDay.start)
        assertEquals("23:59", allDay.end)
        assertEquals("Read", allDay.intention)
        assertEquals((1..5).toSet(), allDay.preset("days").days)
        assertEquals("09:00", allDay.preset("custom").start)
    }

    @Test fun rejectsEmptyTargetsButNowAcceptsAnOvernightWindow() {
        val ready = FocusScheduleDraft(intention = "Read", packages = setOf("com.instagram.android"))
        assertNull(ready.problem())
        // Routine V3 (decision 0014): an end earlier than the start is an overnight window, and the
        // selected day owns the start. This assertion was inverted before - overnight was refused,
        // which is what made sleep preparation unreachable (DEV-5).
        assertNull(ready.copy(start = "22:00", end = "02:00").problem())
        // Equal times stay refused: "no minutes" and "a whole day" are opposite readings.
        assertNotNull(ready.copy(start = "09:00", end = "09:00").problem())
        assertNotNull(ready.copy(days = emptySet()).problem())
        assertNotNull(ready.copy(packages = emptySet()).problem())
        assertNotNull(ready.copy(intention = " ").problem())
    }
}
