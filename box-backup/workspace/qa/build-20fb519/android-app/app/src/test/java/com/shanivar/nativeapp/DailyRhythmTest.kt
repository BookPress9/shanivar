package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class DailyRhythmTest {
    @Test fun adaptsToWakeTimeAndQuietTime() {
        val rhythm = Rhythm()
        assertEquals(DayMoment.MORNING, rhythm.moment(LocalTime.of(7, 0)))
        assertEquals(DayMoment.FOCUS, rhythm.moment(LocalTime.of(12, 0)))
        assertEquals(DayMoment.EVENING, rhythm.moment(LocalTime.of(19, 0)))
        assertEquals(DayMoment.NIGHT, rhythm.moment(LocalTime.of(22, 0)))
        assertEquals(DayMoment.NIGHT, rhythm.moment(LocalTime.of(6, 59)))
    }
    @Test fun supportsOvernightRoutines() {
        val rhythm = Rhythm(wakeTime = "18:00", quietTime = "09:00")
        assertTrue(rhythm.valid())
        assertEquals(DayMoment.MORNING, rhythm.moment(LocalTime.of(19, 0)))
        assertEquals(DayMoment.FOCUS, rhythm.moment(LocalTime.of(1, 0)))
        assertEquals(DayMoment.EVENING, rhythm.moment(LocalTime.of(7, 0)))
        assertEquals(DayMoment.NIGHT, rhythm.moment(LocalTime.of(12, 0)))
    }
    @Test fun rejectsInvalidTimesAndImpossiblePlans() {
        assertFalse(Rhythm(wakeTime = "25:00").valid())
        assertFalse(Rhythm(quietTime = "07:00").valid())
        assertFalse(Rhythm(focusMinutes = 0).valid())
        assertFalse(Rhythm(whatsappTime = "tomorrow").valid())
    }
}
