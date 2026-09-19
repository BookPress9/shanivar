package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class FocusWindowRecurrenceTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    // 2026-09-14 is a Monday.
    private fun at(day: Int, hour: Int, minute: Int) = ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)
    private val weekdays = setOf(1, 2, 3, 4, 5) // Mon-Fri
    private val nineToFive = LocalTime.of(9, 0) to LocalTime.of(17, 0)

    @Test fun beforeTheWindowTodayNextBoundaryIsTodaysStart() {
        val (result, action) = FocusWindowRecurrence.nextBoundary(weekdays, nineToFive.first, nineToFive.second, at(14, 7, 0))!!
        assertEquals(at(14, 9, 0), result)
        assertEquals(FocusWindowAction.START, action)
    }

    @Test fun insideTheWindowNextBoundaryIsTodaysEndNotADistantStart() {
        // This is the case that would be wrong with any "am I active?" special-casing bug:
        // 9:00 has already passed, so naive reuse of nextOccurrence(startTime) alone would
        // wrongly report "next Monday" as the next event while the window is still open today.
        val (result, action) = FocusWindowRecurrence.nextBoundary(weekdays, nineToFive.first, nineToFive.second, at(14, 12, 0))!!
        assertEquals(at(14, 17, 0), result)
        assertEquals(FocusWindowAction.END, action)
    }

    @Test fun afterTheWindowTodayNextBoundaryIsTomorrowsStart() {
        val (result, action) = FocusWindowRecurrence.nextBoundary(weekdays, nineToFive.first, nineToFive.second, at(14, 18, 0))!!
        assertEquals(at(15, 9, 0), result) // Tuesday
        assertEquals(FocusWindowAction.START, action)
    }

    @Test fun weekendRollsToNextMondaysStart() {
        // 2026-09-19 is a Saturday.
        val (result, action) = FocusWindowRecurrence.nextBoundary(weekdays, nineToFive.first, nineToFive.second, at(19, 10, 0))!!
        assertEquals(at(21, 9, 0), result) // Monday the 21st
        assertEquals(FocusWindowAction.START, action)
    }

    @Test fun allDayPresetIsAlwaysActiveEveryDay() {
        val allDays = (0..6).toSet()
        val midnight = LocalTime.of(0, 0)
        val justBeforeMidnight = LocalTime.of(23, 59)
        assertTrue(FocusWindowRecurrence.isActiveAt(allDays, midnight, justBeforeMidnight, at(14, 3, 0)))
        assertTrue(FocusWindowRecurrence.isActiveAt(allDays, midnight, justBeforeMidnight, at(19, 23, 0))) // Saturday too
    }

    @Test fun isActiveAtIsFalseOutsideSelectedDaysEvenDuringTheTimeWindow() {
        assertFalse(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(19, 12, 0))) // Saturday
    }

    @Test fun isActiveAtIsFalseBeforeStartAndAtOrAfterEnd() {
        assertFalse(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(14, 8, 59)))
        assertFalse(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(14, 17, 0))) // end is exclusive
        assertTrue(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(14, 16, 59)))
        assertTrue(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(14, 9, 0))) // start is inclusive
    }

    @Test fun invalidWindowReturnsNullRatherThanGuessing() {
        assertNull(FocusWindowRecurrence.nextBoundary(weekdays, LocalTime.of(17, 0), LocalTime.of(9, 0), at(14, 12, 0)))
        assertNull(FocusWindowRecurrence.nextBoundary(emptySet(), nineToFive.first, nineToFive.second, at(14, 12, 0)))
        assertFalse(FocusWindowRecurrence.isActiveAt(weekdays, LocalTime.of(17, 0), LocalTime.of(9, 0), at(14, 12, 0)))
    }
}
