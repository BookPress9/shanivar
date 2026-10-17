package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RoutinePlanTest {
    private val everyDay = (0..6).toSet()

    private fun activity(
        title: String = "Gym",
        start: LocalTime = LocalTime.of(7, 30),
        duration: Int = 60,
        days: Set<Int> = everyDay,
        id: String = "a1",
    ) = RoutineActivityV1(id, title, start, duration, days, enabled = true)

    private fun window(
        id: String,
        start: String,
        end: String,
        days: Set<Int> = everyDay,
        enabled: Boolean = true,
    ) = ProtectionWindowV1(id, LocalTime.parse(start), LocalTime.parse(end), days, enabled)

    @Test fun emptyPlanIsValid() {
        assertEquals(RoutinePlanValidation.Ok, RoutinePlanRules.validate(RoutinePlanV1()))
    }

    @Test fun equalWakeAndSleepAreRejected() {
        val plan = RoutinePlanV1(wakeTime = LocalTime.of(7, 0), intendedSleepTime = LocalTime.of(7, 0))
        val result = RoutinePlanRules.validate(plan)
        assertTrue(result is RoutinePlanValidation.Error)
    }

    @Test fun blankActivityTitleCannotSave() {
        val blank = RoutinePlanV1(activities = listOf(activity().copy(title = "   ")))
        assertTrue(RoutinePlanRules.validate(blank) is RoutinePlanValidation.Error)
    }

    @Test fun customDaysRequireAtLeastOne() {
        val blankDays = RoutinePlanV1(activities = listOf(activity(days = emptySet())))
        assertTrue(RoutinePlanRules.validate(blankDays) is RoutinePlanValidation.Error)
    }

    @Test fun activityCapIsTwelve() {
        val tooMany = RoutinePlanV1(activities = (1..13).map { activity(id = "a$it", title = "A$it") })
        assertTrue(RoutinePlanRules.validate(tooMany) is RoutinePlanValidation.Error)
        val ok = RoutinePlanV1(activities = (1..12).map { activity(id = "a$it", title = "A$it") })
        assertEquals(RoutinePlanValidation.Ok, RoutinePlanRules.validate(ok))
    }

    @Test fun startEqualsEndWindowIsInvalidExceptMidnightAllDay() {
        val bad = RoutinePlanV1(windows = listOf(window("w1", "09:00", "09:00")))
        assertTrue(RoutinePlanRules.validate(bad) is RoutinePlanValidation.Error)
        val allDay = RoutinePlanV1(windows = listOf(window("w1", "00:00", "00:00")))
        assertEquals(RoutinePlanValidation.Ok, RoutinePlanRules.validate(allDay))
    }

    @Test fun overlappingEnabledWindowsAreRejectedAdjacentAreOk() {
        val overlap = RoutinePlanV1(
            windows = listOf(
                window("w1", "09:00", "12:00"),
                window("w2", "11:00", "14:00"),
            ),
        )
        assertTrue(RoutinePlanRules.validate(overlap) is RoutinePlanValidation.Error)

        val adjacent = RoutinePlanV1(
            windows = listOf(
                window("w1", "09:00", "11:00"),
                window("w2", "11:00", "14:00"),
            ),
        )
        assertEquals(RoutinePlanValidation.Ok, RoutinePlanRules.validate(adjacent))
    }

    @Test fun disabledOverlappingWindowsAreAllowed() {
        val plan = RoutinePlanV1(
            windows = listOf(
                window("w1", "09:00", "12:00", enabled = true),
                window("w2", "11:00", "14:00", enabled = false),
            ),
        )
        assertEquals(RoutinePlanValidation.Ok, RoutinePlanRules.validate(plan))
    }

    @Test fun windowCapIsEight() {
        // Nine windows regardless of overlap hits the count cap first.
        val nine = RoutinePlanV1(
            windows = (1..9).map { i ->
                window("w$i", "08:00", "09:00", days = setOf(i % 7), enabled = false)
            },
        )
        assertTrue(RoutinePlanRules.validate(nine) is RoutinePlanValidation.Error)
        val eight = RoutinePlanV1(
            windows = (1..8).map { i ->
                window("w$i", "08:00", "09:00", days = setOf(i % 7), enabled = false)
            },
        )
        assertEquals(RoutinePlanValidation.Ok, RoutinePlanRules.validate(eight))
    }

    @Test fun occurrenceKindsMapCorrectly() {
        assertEquals(ActivityOccurrenceKind.EVERY_DAY, RoutineActivityOccurrence.kindOf(everyDay))
        assertEquals(ActivityOccurrenceKind.WEEKLY, RoutineActivityOccurrence.kindOf(setOf(1)))
        assertEquals(ActivityOccurrenceKind.CUSTOM, RoutineActivityOccurrence.kindOf(setOf(1, 3, 5)))
    }

    @Test fun savingActivitiesDoesNotImplyAlarmOrFocusSideEffects() {
        // Structural contract: pure plan/recurrence code never arms device triggers.
        val planSrc = java.io.File("src/main/java/com/shanivar/nativeapp/RoutinePlan.kt").readText()
        listOf("AlarmScheduler", "scheduleNext", "armNative", "AlarmManager").forEach {
            assertFalse(planSrc.contains(it))
        }
        // Activity recurrence must stay calendar-only.
        val body = planSrc.substringAfter("object RoutineActivityRecurrence")
        assertFalse(body.contains("FocusScheduler"))
        assertFalse(body.contains("AlarmScheduler"))
    }

    @Test fun activityNextStartRespectsWeeklyDays() {
        // Monday 2026-09-14 08:00 — weekly Wednesday activity is next Wed.
        val now = ZonedDateTime.of(2026, 9, 14, 8, 0, 0, 0, ZoneId.of("UTC"))
        val wed = activity(days = setOf(3), start = LocalTime.of(7, 0))
        val next = RoutineActivityRecurrence.nextStart(wed, now)!!
        assertEquals(3, AlarmRecurrence.isoDayToOurConvention(next.dayOfWeek))
        assertEquals(LocalTime.of(7, 0), next.toLocalTime())
    }

    @Test fun quickAddsAreEditableStartersNotEngineTypes() {
        val gym = RoutineActivityQuickAdds.create(RoutineActivityQuickAdds.ALL.first())
        assertEquals("Gym", gym.title)
        assertNotNull(gym.id)
        assertEquals(everyDay, gym.days)
    }
}
