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

    @Test fun rejectsOvernightAndEmptyTargetsBeforeSaving() {
        val ready = FocusScheduleDraft(intention = "Read", packages = setOf("com.instagram.android"))
        assertNull(ready.problem())
        assertNotNull(ready.copy(start = "22:00", end = "02:00").problem())
        assertNotNull(ready.copy(days = emptySet()).problem())
        assertNotNull(ready.copy(packages = emptySet()).problem())
    }
}
