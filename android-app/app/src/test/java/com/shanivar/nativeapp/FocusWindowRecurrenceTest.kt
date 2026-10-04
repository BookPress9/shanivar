package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Recurrence gates from `ROUTINE_V3_TEST_GUIDE_2026-09-30.md`, plus the DEV-1 regression.
 *
 * The overnight cases are new in V3: `start > end` used to be rejected outright, which removed sleep
 * preparation entirely. It is now a window whose **start day owns the occurrence**, so Monday
 * 22:00-06:00 is a Monday rule that happens to finish on Tuesday.
 */
class FocusWindowRecurrenceTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    /** 2026-09-14 is a Monday; 2026-09-19 a Saturday; 2026-09-20 a Sunday. */
    private fun at(day: Int, hour: Int, minute: Int) = ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)

    private val weekdays = setOf(1, 2, 3, 4, 5) // Mon-Fri
    private val monday = setOf(1)
    private val nineToFive = LocalTime.of(9, 0) to LocalTime.of(17, 0)
    private val overnight = LocalTime.of(22, 0) to LocalTime.of(6, 0)

    // ── Same-day windows ────────────────────────────────────────────────────────────────────────

    @Test fun beforeTheWindowTodayNextBoundaryIsTodaysStart() {
        val (result, action) = FocusWindowRecurrence.nextBoundary(weekdays, nineToFive.first, nineToFive.second, at(14, 7, 0))!!
        assertEquals(at(14, 9, 0), result)
        assertEquals(FocusWindowAction.START, action)
    }

    @Test fun insideTheWindowNextBoundaryIsTodaysEndNotADistantStart() {
        // 9:00 has already passed, so naive reuse of nextOccurrence(startTime) alone would wrongly
        // report "next Monday" while the window is still open today.
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
        val (result, action) = FocusWindowRecurrence.nextBoundary(weekdays, nineToFive.first, nineToFive.second, at(19, 10, 0))!!
        assertEquals(at(21, 9, 0), result) // Monday the 21st
        assertEquals(FocusWindowAction.START, action)
    }

    @Test fun isActiveAtIsFalseOutsideSelectedDaysEvenDuringTheTimeWindow() {
        assertFalse(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(19, 12, 0))) // Saturday
    }

    @Test fun startIsInclusiveAndEndIsExclusive() {
        assertFalse(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(14, 8, 59)))
        assertTrue(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(14, 9, 0)))
        assertTrue(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(14, 16, 59)))
        assertFalse(FocusWindowRecurrence.isActiveAt(weekdays, nineToFive.first, nineToFive.second, at(14, 17, 0)))
    }

    // ── Overnight windows ───────────────────────────────────────────────────────────────────────

    @Test fun mondayNightOvernightWindowIsActiveMondayNightAndEndsTuesdayMorning() {
        val (days, start, end) = Triple(monday, overnight.first, overnight.second)
        assertTrue(FocusWindowRecurrence.isActiveAt(days, start, end, at(14, 23, 0)))  // Monday night
        assertTrue(FocusWindowRecurrence.isActiveAt(days, start, end, at(15, 2, 0)))   // Tuesday 02:00
        assertTrue(FocusWindowRecurrence.isActiveAt(days, start, end, at(15, 5, 59)))
        assertFalse(FocusWindowRecurrence.isActiveAt(days, start, end, at(15, 6, 0)))  // end exclusive
        // The end instant is Tuesday's 06:00, not Monday's.
        assertEquals(at(15, 6, 0), FocusWindowRecurrence.activeWindowEnd(days, start, end, at(14, 23, 0)))
        assertEquals(at(15, 6, 0), FocusWindowRecurrence.activeWindowEnd(days, start, end, at(15, 2, 0)))
    }

    @Test fun theSameOvernightWindowIsInactiveSundayNightUnlessSundayIsSelected() {
        // 2026-09-20 is a Sunday. Only Monday is selected, so Sunday night is not an occurrence.
        assertFalse(FocusWindowRecurrence.isActiveAt(monday, overnight.first, overnight.second, at(20, 23, 0)))
        // Monday morning is likewise not covered: that would belong to a Sunday-night occurrence.
        assertFalse(FocusWindowRecurrence.isActiveAt(monday, overnight.first, overnight.second, at(14, 2, 0)))
        // Adding Sunday makes Sunday night active, and it then ends Monday morning.
        val withSunday = setOf(0, 1)
        assertTrue(FocusWindowRecurrence.isActiveAt(withSunday, overnight.first, overnight.second, at(20, 23, 0)))
        assertTrue(FocusWindowRecurrence.isActiveAt(withSunday, overnight.first, overnight.second, at(21, 2, 0)))
    }

    @Test fun overnightBoundariesAlternateStartThenEndAcrossMidnight() {
        val (days, start, end) = Triple(monday, overnight.first, overnight.second)
        val beforeStart = FocusWindowRecurrence.nextBoundary(days, start, end, at(14, 20, 0))!!
        assertEquals(at(14, 22, 0), beforeStart.first)
        assertEquals(FocusWindowAction.START, beforeStart.second)

        val insideEvening = FocusWindowRecurrence.nextBoundary(days, start, end, at(14, 23, 0))!!
        assertEquals(at(15, 6, 0), insideEvening.first)
        assertEquals(FocusWindowAction.END, insideEvening.second)

        val insideMorning = FocusWindowRecurrence.nextBoundary(days, start, end, at(15, 3, 0))!!
        assertEquals(at(15, 6, 0), insideMorning.first)
        assertEquals(FocusWindowAction.END, insideMorning.second)

        // After the occurrence closes, the next event is the following Monday's start.
        val afterEnd = FocusWindowRecurrence.nextBoundary(days, start, end, at(15, 7, 0))!!
        assertEquals(at(21, 22, 0), afterEnd.first)
        assertEquals(FocusWindowAction.START, afterEnd.second)
    }

    @Test fun endDaysShiftForwardOnlyForOvernightWindowsAndWrapPastSaturday() {
        assertEquals(weekdays, FocusWindowRecurrence.endDaysFor(weekdays, nineToFive.first, nineToFive.second))
        assertEquals(setOf(2, 3, 4, 5, 6), FocusWindowRecurrence.endDaysFor(weekdays, overnight.first, overnight.second))
        // Saturday(6) start means a Sunday(0) end, not day 7.
        assertEquals(setOf(0), FocusWindowRecurrence.endDaysFor(setOf(6), overnight.first, overnight.second))
    }

    @Test fun customWeekCrossingMidnightComputesEachSelectedNightIndependently() {
        val friAndSat = setOf(5, 6)
        // 2026-09-18 is a Friday, 2026-09-19 a Saturday.
        assertTrue(FocusWindowRecurrence.isActiveAt(friAndSat, overnight.first, overnight.second, at(18, 23, 30)))
        assertTrue(FocusWindowRecurrence.isActiveAt(friAndSat, overnight.first, overnight.second, at(19, 5, 0)))
        assertTrue(FocusWindowRecurrence.isActiveAt(friAndSat, overnight.first, overnight.second, at(20, 5, 0))) // Sun morning
        assertFalse(FocusWindowRecurrence.isActiveAt(friAndSat, overnight.first, overnight.second, at(20, 23, 0))) // Sun night
    }

    // ── Invalid shapes ──────────────────────────────────────────────────────────────────────────

    @Test fun equalStartAndEndIsRejectedRatherThanInterpreted() {
        // "Zero minutes" and "a full 24 hours" are opposite readings of the same input, so it is
        // refused instead of guessed (decision 0014).
        val noon = LocalTime.of(12, 0)
        assertFalse(FocusWindowRecurrence.isValidShape(noon, noon))
        assertNull(FocusWindowRecurrence.nextBoundary(weekdays, noon, noon, at(14, 12, 0)))
        assertFalse(FocusWindowRecurrence.isActiveAt(weekdays, noon, noon, at(14, 12, 0)))
        assertNull(FocusWindowRecurrence.activeWindowEnd(weekdays, noon, noon, at(14, 12, 0)))
    }

    @Test fun noSelectedDaysHasNoOccurrence() {
        assertNull(FocusWindowRecurrence.nextBoundary(emptySet(), nineToFive.first, nineToFive.second, at(14, 12, 0)))
        assertFalse(FocusWindowRecurrence.isActiveAt(emptySet(), overnight.first, overnight.second, at(14, 23, 0)))
    }

    @Test fun anOvernightShapeIsNowValidWhereItUsedToBeRejected() {
        // Regression marker: this exact input returned null before V3 and is the reason sleep
        // preparation was unreachable (DEV-5).
        assertTrue(FocusWindowRecurrence.isValidShape(LocalTime.of(17, 0), LocalTime.of(9, 0)))
        assertNotNull(FocusWindowRecurrence.nextBoundary(weekdays, LocalTime.of(17, 0), LocalTime.of(9, 0), at(14, 12, 0)))
    }

    @Test fun midnightToOneMinuteBeforeMidnightLeavesARealGapSoItIsNotAllDay() {
        // Documents DEV-6: 00:00-23:59 is a same-day window with a 60-second hole every night, so it
        // must not be presented as continuous protection. Kept as a test rather than a comment so a
        // future "all day" preset cannot quietly reintroduce the claim.
        val allDays = (0..6).toSet()
        val midnight = LocalTime.of(0, 0)
        val justBefore = LocalTime.of(23, 59)
        assertTrue(FocusWindowRecurrence.isActiveAt(allDays, midnight, justBefore, at(14, 3, 0)))
        assertFalse(FocusWindowRecurrence.isActiveAt(allDays, midnight, justBefore, at(14, 23, 59)))
    }

    // ── Clock and zone behaviour ────────────────────────────────────────────────────────────────

    @Test fun daylightSavingTransitionsProduceOneDeterministicBoundary() {
        val berlin = ZoneId.of("Europe/Berlin")
        // Spring forward 2026-03-29: 02:00 -> 03:00 local, so 02:30 does not exist that night.
        val springStart = ZonedDateTime.of(2026, 3, 28, 23, 0, 0, 0, berlin)
        val gapWindow = FocusWindowRecurrence.nextBoundary(
            (0..6).toSet(), LocalTime.of(2, 30), LocalTime.of(4, 30), springStart,
        )
        assertNotNull(gapWindow)
        // Resolved once, forward, by java.time - never two candidate instants for one rule.
        assertEquals(gapWindow!!.first, FocusWindowRecurrence.nextBoundary(
            (0..6).toSet(), LocalTime.of(2, 30), LocalTime.of(4, 30), springStart,
        )!!.first)

        // Autumn overlap 2026-10-25: 02:00-03:00 local occurs twice.
        val autumnStart = ZonedDateTime.of(2026, 10, 24, 23, 0, 0, 0, berlin)
        val overlapWindow = FocusWindowRecurrence.nextBoundary(
            (0..6).toSet(), LocalTime.of(2, 30), LocalTime.of(4, 30), autumnStart,
        )
        assertNotNull(overlapWindow)
        assertEquals(FocusWindowAction.START, overlapWindow!!.second)
    }

    @Test fun boundariesAreRecomputedFromLocalRulesRatherThanStoredInstants() {
        // The same rule read in a different zone yields that zone's local time, not a shifted
        // instant - which is what makes a timezone change behave as "same 22:00 where you now are".
        val kolkata = FocusWindowRecurrence.nextBoundary(monday, overnight.first, overnight.second, at(14, 20, 0))!!
        val berlinNow = ZonedDateTime.of(2026, 9, 14, 20, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val berlin = FocusWindowRecurrence.nextBoundary(monday, overnight.first, overnight.second, berlinNow)!!
        assertEquals(LocalTime.of(22, 0), kolkata.first.toLocalTime())
        assertEquals(LocalTime.of(22, 0), berlin.first.toLocalTime())
        assertFalse(kolkata.first.toInstant() == berlin.first.toInstant())
    }

    // ── DEV-1 regression ────────────────────────────────────────────────────────────────────────

    @Test fun dev1_aStartDeliveredAfterItsWindowClosedHasNoActiveEndToMeasureAgainst() {
        // The defect: the receiver asked nextBoundary() for "the end", got the *next occurrence's
        // START* once delivery slipped past the real end, and turned that gap into a 24-hour session.
        // activeWindowEnd returns null instead, so no duration can be computed at all.
        val deliveredLate = at(14, 18, 30) // window was 09:00-17:00 Monday
        assertNull(FocusWindowRecurrence.activeWindowEnd(weekdays, nineToFive.first, nineToFive.second, deliveredLate))

        // What the old code would have measured, for contrast: a boundary almost a day away.
        val (staleBoundary, staleAction) = FocusWindowRecurrence.nextBoundary(
            weekdays, nineToFive.first, nineToFive.second, deliveredLate,
        )!!
        assertEquals(FocusWindowAction.START, staleAction)
        assertTrue(staleBoundary.toInstant().toEpochMilli() - deliveredLate.toInstant().toEpochMilli() > 12 * 3600_000L)
    }

    @Test fun dev1_anOnTimeStartStillMeasuresToItsOwnWindowEnd() {
        val onTime = at(14, 9, 0)
        assertEquals(at(14, 17, 0), FocusWindowRecurrence.activeWindowEnd(weekdays, nineToFive.first, nineToFive.second, onTime))
        // And an overnight window measures across midnight rather than to its own start day.
        assertEquals(at(15, 6, 0), FocusWindowRecurrence.activeWindowEnd(monday, overnight.first, overnight.second, at(14, 22, 0)))
    }

    @Test fun dev1_aLateEndFindsNoActiveOccurrenceSoItReleasesRatherThanSkipping() {
        // At the legitimate END instant nothing is active (end is exclusive), which is how the
        // receiver distinguishes "release now" from "a later occurrence owns this".
        assertNull(FocusWindowRecurrence.activeWindowEnd(weekdays, nineToFive.first, nineToFive.second, at(14, 17, 0)))
        // But if delivery slipped into the *next* occurrence, that occurrence is active and owns it.
        assertNotNull(FocusWindowRecurrence.activeWindowEnd(weekdays, nineToFive.first, nineToFive.second, at(15, 10, 0)))
    }
}
