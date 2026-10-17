package com.shanivar.nativeapp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Real FocusScheduler registry behind the Routine gateway: saving the plan writes the rules. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidProtectionGatewayTest {
    private val now = ZonedDateTime.of(2026, 9, 14, 8, 0, 0, 0, ZoneId.of("UTC"))

    @Test fun savingThePlanReplacesTheRegistryAndActivitiesDoNot() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val gateway = AndroidProtectionGateway(app)
        val repo = ProtectionSyncingRoutinePlanRepository(SharedPrefsRoutinePlanRepository(app), gateway) { now }
        val plan = RoutinePlanV1(
            protectedPackages = setOf("com.instagram.android"),
            windows = listOf(
                ProtectionWindowV1("work", LocalTime.of(9, 0), LocalTime.of(17, 0), (1..5).toSet()),
                ProtectionWindowV1("night", LocalTime.of(22, 0), LocalTime.of(6, 0), (0..6).toSet()),
                ProtectionWindowV1("sat", LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, setOf(6), enabled = false),
            ),
        )
        assertTrue(repo.save(plan).isSuccess)
        val rules = FocusScheduler(app).rules()
        assertEquals(listOf("work", "night", "sat"), rules.map { it.scheduleId })
        assertEquals(listOf(true, true, false), rules.map { it.enabled })
        assertTrue(rules.all { it.blockPackages == setOf("com.instagram.android") })

        // Activity edit: registry untouched (same instance contents), no sync recorded.
        val withActivity = plan.copy(activities = listOf(RoutineActivityV1("g", "Gym", LocalTime.of(7, 0), 30, (0..6).toSet())))
        assertTrue(repo.save(withActivity).isSuccess)
        assertEquals(null, repo.lastSync)
        assertEquals(rules, FocusScheduler(app).rules())

        // Remove all windows → registry empty, nothing armed.
        assertTrue(repo.save(withActivity.copy(windows = emptyList())).isSuccess)
        assertTrue(FocusScheduler(app).rules().isEmpty())
        assertEquals(null, FocusScheduler(app).pendingArmed())
    }
}
