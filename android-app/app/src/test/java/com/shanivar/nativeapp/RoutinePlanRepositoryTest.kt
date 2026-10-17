package com.shanivar.nativeapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RoutinePlanRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun clearStores() {
        listOf(
            "rhythm",
            RoutinePlanRepository.PREFS_NAME,
            "discipline_alarm",
            "focus_schedule",
            "focus_guard",
        ).forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun migrationIsIdempotentAndDoesNotRewriteRhythm() {
        val rhythm = context.getSharedPreferences("rhythm", Context.MODE_PRIVATE)
        rhythm.edit()
            .putString("wake", "06:15")
            .putString("quiet", "22:45")
            .putString("alarmId", "keep-me")
            .putString("alarmDays", "1,3,5")
            .putBoolean("alarmEnabled", true)
            .putInt("focus", 40)
            .commit()
        val before = HashMap(rhythm.all)

        val first = RoutinePlanRepository(context).load()
        assertEquals("06:15", first.plan.wakeTime)
        assertEquals("22:45", first.plan.sleepTime)
        assertTrue(first.plan.activities.isEmpty())
        assertTrue(first.migratedFromRhythm)
        assertFalse(first.activitiesArmed)
        assertEquals(before, rhythm.all)

        rhythm.edit().putString("wake", "11:11").putString("quiet", "18:00").commit()
        val second = RoutinePlanRepository(context).load()
        val third = RoutinePlanRepository(context).load()
        assertEquals(first.plan, second.plan)
        assertEquals(second, third)
        assertEquals("06:15", third.plan.wakeTime)
        assertEquals("22:45", third.plan.sleepTime)
        assertTrue(third.migratedFromRhythm)
        assertFalse(third.activitiesArmed)
        assertEquals("11:11", rhythm.getString("wake", null))
        assertEquals("18:00", rhythm.getString("quiet", null))
        assertEquals("keep-me", rhythm.getString("alarmId", null))
        assertEquals("1,3,5", rhythm.getString("alarmDays", null))
        assertTrue(rhythm.getBoolean("alarmEnabled", false))
        assertEquals(40, rhythm.getInt("focus", -1))
    }

    @Test fun missingAnchorIsNotFilledFromRhythmDefaults() {
        context.getSharedPreferences("rhythm", Context.MODE_PRIVATE).edit()
            .putString("wake", "05:40")
            .commit()
        val snapshot = RoutinePlanRepository(context).load()
        assertEquals("05:40", snapshot.plan.wakeTime)
        assertNull(snapshot.plan.sleepTime)
        assertFalse(snapshot.plan.sleepTime == "21:30")
        assertTrue(snapshot.migratedFromRhythm)

        clearStores()
        context.getSharedPreferences("rhythm", Context.MODE_PRIVATE).edit()
            .putString("quiet", "23:05")
            .commit()
        val sleepOnly = RoutinePlanRepository(context).load()
        assertNull(sleepOnly.plan.wakeTime)
        assertEquals("23:05", sleepOnly.plan.sleepTime)
    }

    @Test fun migrationWaitsUntilAnchorsExistThenRunsOnce() {
        val untouched = RoutinePlanRepository(context).load()
        assertFalse(untouched.migratedFromRhythm)
        assertNull(untouched.plan.wakeTime)
        assertNull(untouched.plan.sleepTime)
        assertFalse(context.getSharedPreferences(RoutinePlanRepository.PREFS_NAME, 0).contains("plan"))

        context.getSharedPreferences("rhythm", Context.MODE_PRIVATE).edit()
            .putString("wake", "08:00")
            .putString("quiet", "23:00")
            .commit()
        val migrated = RoutinePlanRepository(context).load()
        assertEquals("08:00", migrated.plan.wakeTime)
        assertEquals("23:00", migrated.plan.sleepTime)
        assertTrue(migrated.migratedFromRhythm)

        context.getSharedPreferences("rhythm", Context.MODE_PRIVATE).edit()
            .putString("wake", "04:00")
            .commit()
        val again = RoutinePlanRepository(context).load()
        assertEquals(migrated.plan, again.plan)
    }

    @Test fun savedPlanIsNotOverwrittenWhenRhythmAnchorsAppearLater() {
        val plan = RoutinePlanV1(
            wakeTime = "07:30",
            sleepTime = null,
            activities = listOf(reading()),
        )
        RoutinePlanRepository(context).save(plan)
        context.getSharedPreferences("rhythm", Context.MODE_PRIVATE).edit()
            .putString("wake", "09:00")
            .putString("quiet", "20:00")
            .commit()
        val loaded = RoutinePlanRepository(context).load()
        assertEquals(plan, loaded.plan)
        assertFalse(loaded.migratedFromRhythm)
        assertEquals("09:00", context.getSharedPreferences("rhythm", 0).getString("wake", null))
    }

    @Test fun activitiesSurviveANewRepositoryInstance() {
        val plan = RoutinePlanV1(
            wakeTime = "06:00",
            sleepTime = "22:00",
            activities = listOf(
                reading(),
                RoutineActivityV1(
                    id = "gym",
                    title = "Gym",
                    time = "18:30",
                    durationMinutes = 50,
                    recurrence = RoutineRecurrenceV1.weekly(1),
                ),
                RoutineActivityV1(
                    id = "call",
                    title = "Family call",
                    time = "19:15",
                    durationMinutes = 20,
                    recurrence = RoutineRecurrenceV1.customDays(setOf(0, 6)),
                ),
            ),
        )
        assertTrue(plan.valid())
        val saved = RoutinePlanRepository(context).save(plan)
        assertFalse(saved.activitiesArmed)

        val restored = RoutinePlanRepository(context).load()
        assertEquals(plan, restored.plan)
        assertEquals(listOf("read", "gym", "call"), restored.plan.activities.map { it.id })
        assertEquals(RoutineRecurrenceKind.EVERY_DAY, restored.plan.activities[0].recurrence.kind)
        assertEquals(setOf(1), restored.plan.activities[1].recurrence.days)
        assertEquals(setOf(0, 6), restored.plan.activities[2].recurrence.days)
        assertFalse(restored.activitiesArmed)
        val raw = context.getSharedPreferences(RoutinePlanRepository.PREFS_NAME, 0).getString("plan", "")!!
        assertFalse(raw.contains("armed"))
    }

    @Test fun repositoryDoesNotTouchFocusOrAlarmTypes() {
        context.getSharedPreferences("discipline_alarm", 0).edit().putString("sessionId", "alarm-1").commit()
        context.getSharedPreferences("focus_schedule", 0).edit().putString("rules", "[]").commit()
        context.getSharedPreferences("focus_guard", 0).edit().putLong("endsAt", 1234L).commit()
        val alarmBefore = HashMap(context.getSharedPreferences("discipline_alarm", 0).all)
        val focusBefore = HashMap(context.getSharedPreferences("focus_schedule", 0).all)
        val guardBefore = HashMap(context.getSharedPreferences("focus_guard", 0).all)

        val repo = RoutinePlanRepository(context)
        repo.save(RoutinePlanV1(wakeTime = "07:00", sleepTime = "21:30", activities = listOf(reading())))
        repo.load()

        assertEquals(alarmBefore, context.getSharedPreferences("discipline_alarm", 0).all)
        assertEquals(focusBefore, context.getSharedPreferences("focus_schedule", 0).all)
        assertEquals(guardBefore, context.getSharedPreferences("focus_guard", 0).all)

        val bytes = RoutinePlanRepository::class.java
            .getResourceAsStream("RoutinePlanRepository.class")!!
            .readBytes()
        val utf = String(bytes, Charsets.ISO_8859_1)
        listOf(
            "FocusScheduler",
            "FocusWindowEnd",
            "FocusScheduleReceiver",
            "FocusGuard",
            "AlarmScheduler",
            "AlarmManager",
            "BootRescheduleReceiver",
            "DisciplineEngine",
            "DisciplineGateway",
        ).forEach { forbidden ->
            assertFalse("repository class references $forbidden", utf.contains(forbidden))
        }
    }

    private fun reading() = RoutineActivityV1(
        id = "read",
        title = "Reading",
        time = "08:00",
        durationMinutes = 30,
        recurrence = RoutineRecurrenceV1.everyDay(),
    )
}
