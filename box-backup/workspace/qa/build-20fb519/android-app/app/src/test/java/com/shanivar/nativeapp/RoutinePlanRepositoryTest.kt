package com.shanivar.nativeapp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoutinePlanRepositoryTest {
    @get:org.junit.Rule val outboxReset = AppSyncOutboxReset()
    @Test fun saveAndReloadSurvivesProcessDeathSimulation() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val repo = SharedPrefsRoutinePlanRepository(app)
        val plan = RoutinePlanV1(
            wakeTime = LocalTime.of(6, 15),
            intendedSleepTime = LocalTime.of(22, 45),
            activities = listOf(
                RoutineActivityV1("id-stable", "Gym", LocalTime.of(7, 30), 60, (0..6).toSet(), true),
            ),
        )
        assertTrue(repo.save(plan).isSuccess)
        val reloaded = SharedPrefsRoutinePlanRepository(app)
        assertEquals(LocalTime.of(6, 15), reloaded.current().wakeTime)
        assertEquals("id-stable", reloaded.current().activities.single().id)
        assertEquals("Gym", reloaded.current().activities.single().title)
        assertTrue(reloaded.decodeHealthy())
    }

    @Test fun invalidPlanIsRejectedWithoutWriting() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val repo = SharedPrefsRoutinePlanRepository(app)
        val bad = RoutinePlanV1(
            activities = listOf(
                RoutineActivityV1("a1", "", LocalTime.of(9, 0), 30, (0..6).toSet(), true),
            ),
        )
        assertTrue(repo.save(bad).isFailure)
        assertTrue(repo.current().activities.isEmpty())
    }

    @Test fun migratesWakeFromBreathingPreferencesOnce() {
        val app = RuntimeEnvironment.getApplication()
        BreathingPreferences(app).wakeTime = "06:40"
        BreathingPreferences(app).intendedSleepTime = "22:10"
        val repo = SharedPrefsRoutinePlanRepository(app)
        assertEquals(LocalTime.of(6, 40), repo.current().wakeTime)
        assertEquals(LocalTime.of(22, 10), repo.current().intendedSleepTime)
    }


    @Test fun codecRoundTripsAndCorruptPayloadIsNull() {
        val plan = RoutinePlanV1(
            wakeTime = LocalTime.of(6, 30),
            intendedSleepTime = LocalTime.of(22, 0),
            activities = listOf(
                RoutineActivityV1("a1", "Gym", LocalTime.of(7, 30), 60, (0..6).toSet(), true),
            ),
            protectedPackages = setOf("com.instagram.android"),
            windows = listOf(
                ProtectionWindowV1("w1", LocalTime.of(21, 0), LocalTime.of(6, 0), setOf(1, 2, 3, 4, 5), true),
            ),
        )
        val encoded = RoutinePlanCodec.encode(plan)
        val decoded = RoutinePlanCodec.decode(encoded)!!
        assertEquals(plan, decoded)
        assertEquals(null, RoutinePlanCodec.decode("{not json"))
        assertEquals(null, RoutinePlanCodec.decode("""{"schemaVersion":99}"""))
    }

    @Test fun corruptStoredJsonMarksUnhealthy() {
        val app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("routine_plan_v1", 0).edit()
            .putInt("schema_version", 1)
            .putString("plan_json", "{broken")
            .apply()
        val repo = SharedPrefsRoutinePlanRepository(app)
        assertFalse(repo.decodeHealthy())
    }
}
