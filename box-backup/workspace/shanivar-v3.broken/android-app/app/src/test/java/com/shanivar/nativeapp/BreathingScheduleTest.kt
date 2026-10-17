package com.shanivar.nativeapp

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Section C of VBT_BREATHING_TEST_GUIDE_2026-09-25.md. Pure: no host timezone, no present time.
 *
 * What these protect is narrow but load-bearing - the schedule decides what to *suggest*, so a
 * mistake here silently offers a bedtime practice at breakfast. It decides nothing about duration.
 */
class BreathingScheduleTest {
    private val dayShift = BreathingSchedule(LocalTime.of(7, 0), LocalTime.of(23, 0))
    private val nightShift = BreathingSchedule(LocalTime.of(16, 0), LocalTime.of(8, 0))

    private fun BreathingSchedule.at(hour: Int, minute: Int = 0) = window(LocalTime.of(hour, minute))

    @Test fun t01_justWokeWindowCoversTheFirstTwoHoursAfterTheChosenWakeTime() {
        assertEquals(ScheduleWindow.JUST_WOKE_WINDOW, dayShift.at(7, 0))
        assertEquals(ScheduleWindow.JUST_WOKE_WINDOW, dayShift.at(8, 59))
        assertEquals(ScheduleWindow.DAYTIME, dayShift.at(9, 0))
    }

    @Test fun t02_windDownAndRestBoundariesAreHalfOpen() {
        assertEquals(ScheduleWindow.DAYTIME, dayShift.at(20, 59))
        assertEquals(ScheduleWindow.WIND_DOWN_WINDOW, dayShift.at(21, 0))
        assertEquals(ScheduleWindow.WIND_DOWN_WINDOW, dayShift.at(22, 59))
        assertEquals(ScheduleWindow.REST_WINDOW, dayShift.at(23, 0))
        assertEquals(ScheduleWindow.REST_WINDOW, dayShift.at(0, 0))
        assertEquals(ScheduleWindow.REST_WINDOW, dayShift.at(6, 59))
    }

    @Test fun t03_anOvernightScheduleWorksTheSameWay() {
        assertEquals(ScheduleWindow.JUST_WOKE_WINDOW, nightShift.at(17, 0))
        assertEquals(ScheduleWindow.DAYTIME, nightShift.at(2, 0))
        assertEquals(ScheduleWindow.WIND_DOWN_WINDOW, nightShift.at(7, 0))
        assertEquals(ScheduleWindow.REST_WINDOW, nightShift.at(10, 0))
    }

    @Test fun t04_unusablePreferencesYieldUnknownRatherThanAGuessedDay() {
        val sameTime = BreathingSchedule(LocalTime.of(7, 0), LocalTime.of(7, 0))
        assertEquals(ScheduleWindow.UNKNOWN, sameTime.at(12))
        assertEquals(ScheduleWindow.UNKNOWN, BreathingSchedule.parse("not a time", "23:00").at(12))
        assertEquals(ScheduleWindow.UNKNOWN, BreathingSchedule.parse("07:00", null).at(12))
        assertEquals(ScheduleWindow.UNKNOWN, BreathingSchedule.UNSET.at(12))
        assertNull(BreathingSchedule.parse("", "").awakeSpanMinutes)
    }

    @Test fun t05_theAwakeSpanGuardRejectsImplausiblePairsOnly() {
        fun spanOf(minutes: Int): Int? {
            val wake = LocalTime.of(0, 0)
            return BreathingSchedule(wake, wake.plusMinutes(minutes.toLong())).awakeSpanMinutes
        }
        assertNull(spanOf(359))
        assertEquals(360, spanOf(360))
        assertEquals(1200, spanOf(1200))
        assertNull(spanOf(1201))
    }

    @Test fun t06_quietTimeIsNeverTreatedAsAnIntendedSleepTime() {
        // Rhythm.quietTime means "when the app goes quiet". Reading it as sleep onset would put a
        // time the user set for one purpose behind a different inference, so the schedule is UNKNOWN
        // until they state a sleep time of their own.
        val rhythm = Rhythm(wakeTime = "07:00", quietTime = "21:30")
        val schedule = BreathingSchedule.parse(rhythm.wakeTime, null)
        assertEquals(ScheduleWindow.UNKNOWN, schedule.at(22))
        assertNull(schedule.sleepTime)
    }

    @Test fun t12_staleOrMalformedPreferencesFallBackWithoutTouchingTheRhythm() {
        val rhythm = Rhythm(wakeTime = "07:00", quietTime = "21:30")
        val schedule = BreathingSchedule.parse("25:99", "99:99")
        assertEquals(ScheduleWindow.UNKNOWN, schedule.at(9))
        // The breathing schedule is a separate namespace; parsing junk cannot rewrite alarm state.
        assertEquals("07:00", rhythm.wakeTime)
        assertEquals("21:30", rhythm.quietTime)
    }
}
