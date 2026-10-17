package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Covers the recurrence math in isolation from AlarmManager/SharedPreferences - this is the part
 * that must be correct for "repeat on selected days" to mean anything, and the part a real device
 * cannot exercise for every calendar edge case in a reasonable test run.
 */
class AlarmRecurrenceTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    // 2026-09-14 is a Monday.
    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)

    @Test fun laterTodayWhenTodayIsSelectedAndTimeHasNotPassed() {
        val monday = 1 // our convention: 0=Sun..6=Sat, so Monday=1
        val result = AlarmRecurrence.nextOccurrence(LocalTime.of(7, 0), setOf(monday), at(14, 6, 0))
        assertEquals(at(14, 7, 0), result)
    }

    @Test fun rollsToNextWeekWhenOnlyTodayIsSelectedAndTimeAlreadyPassed() {
        val monday = 1
        // Same Monday, but the 7:00 slot is already behind "after".
        val result = AlarmRecurrence.nextOccurrence(LocalTime.of(7, 0), setOf(monday), at(14, 9, 0))
        assertEquals(at(21, 7, 0), result) // next Monday, one week later
    }

    @Test fun picksTheNearestOfMultipleSelectedDays() {
        // Monday(1)/Wednesday(3)/Friday(5), asked from Monday afternoon: Wednesday is nearest.
        val result = AlarmRecurrence.nextOccurrence(LocalTime.of(6, 15), setOf(1, 3, 5), at(14, 12, 0))
        assertEquals(at(16, 6, 15), result) // Wednesday the 16th
    }

    @Test fun wrapsFromSaturdayToNextSunday() {
        val sunday = 0
        // 2026-09-19 is a Saturday.
        val result = AlarmRecurrence.nextOccurrence(LocalTime.of(8, 0), setOf(sunday), at(19, 23, 0))
        assertEquals(at(20, 8, 0), result) // Sunday the 20th
    }

    @Test fun exactlyEqualInstantIsNotReturnedAsNext() {
        // "after" itself sitting exactly on the slot must not be reported as its own next
        // occurrence - dismissal advances the rule using "now" as `after`, and a dismissal that
        // lands exactly on the slot boundary must not immediately re-arm the same instant.
        val monday = 1
        val exact = at(14, 7, 0)
        val result = AlarmRecurrence.nextOccurrence(LocalTime.of(7, 0), setOf(monday), exact)
        assertEquals(at(21, 7, 0), result)
    }

    @Test fun emptyDaysReturnsNullRatherThanGuessing() {
        assertNull(AlarmRecurrence.nextOccurrence(LocalTime.of(7, 0), emptySet(), at(14, 6, 0)))
    }

    @Test fun outOfRangeDayReturnsNull() {
        assertNull(AlarmRecurrence.nextOccurrence(LocalTime.of(7, 0), setOf(7), at(14, 6, 0)))
        assertNull(AlarmRecurrence.nextOccurrence(LocalTime.of(7, 0), setOf(-1), at(14, 6, 0)))
    }

    @Test fun everyDaySelectedAlwaysMeansTomorrowAtWorstCase() {
        val allDays = (0..6).toSet()
        val result = AlarmRecurrence.nextOccurrence(LocalTime.of(7, 0), allDays, at(14, 9, 0))
        assertEquals(at(15, 7, 0), result) // Tuesday - the very next day, time already passed today
    }
}
