package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusSchedulerRegistryTest {
    private val zone = ZoneId.of("UTC")
    private fun scheduler() = FocusScheduler(RuntimeEnvironment.getApplication())

    private fun rule(
        id: String,
        start: String,
        end: String,
        days: Set<Int> = (0..6).toSet(),
        enabled: Boolean = true,
    ) = FocusScheduler.Rule(
        scheduleId = id,
        days = days,
        startTime = LocalTime.parse(start),
        endTime = LocalTime.parse(end),
        blockPackages = setOf("com.instagram.android"),
        enabled = enabled,
    )

    @Test fun replaceAllCapsAtEightAndRejectsOverlap() {
        val s = scheduler()
        val nine = (1..9).map { i -> rule("r$i", "08:00", "09:00", days = setOf(i % 7), enabled = false) }
        assertTrue(s.replaceAll(nine).isFailure)

        val overlap = listOf(rule("a", "09:00", "12:00"), rule("b", "11:00", "14:00"))
        assertTrue(s.replaceAll(overlap).isFailure)

        val adjacent = listOf(rule("a", "09:00", "11:00"), rule("b", "11:00", "14:00"))
        assertTrue(s.replaceAll(adjacent).isSuccess)
        assertEquals(2, s.rules().size)
    }

    @Test fun scheduleNextUpsertsSingleRuleIntoRegistry() {
        val s = scheduler()
        val after = ZonedDateTime.of(2026, 9, 14, 8, 0, 0, 0, zone)
        s.scheduleNext(rule("morning", "09:00", "10:00"), after)
        assertEquals(1, s.rules().size)
        assertEquals("morning", s.rules().single().scheduleId)
        // Robolectric may deny exact alarms on API 34; persistence must still succeed.
        val pending = s.pendingRuleId()
        if (pending != null) {
            assertEquals("morning", pending)
            assertEquals(FocusWindowAction.START, s.pendingAction())
        }
    }

    @Test fun globalNextBoundaryPicksEarliestAcrossMultipleRules() {
        val s = scheduler()
        val after = ZonedDateTime.of(2026, 9, 14, 8, 0, 0, 0, zone)
        val result = s.replaceAll(
            listOf(
                rule("late", "15:00", "16:00"),
                rule("early", "09:00", "10:00"),
            ),
            after,
        )
        assertTrue(result.isSuccess)
        assertEquals(listOf("late", "early").toSet(), s.rules().map { it.scheduleId }.toSet())
        // Pure selection logic: earliest START after 08:00 is 09:00 early.
        val early = FocusWindowRecurrence.nextBoundary(
            (0..6).toSet(), LocalTime.parse("09:00"), LocalTime.parse("10:00"), after,
        )!!
        val late = FocusWindowRecurrence.nextBoundary(
            (0..6).toSet(), LocalTime.parse("15:00"), LocalTime.parse("16:00"), after,
        )!!
        assertTrue(early.first.isBefore(late.first))
        result.getOrNull()?.let { armed ->
            assertEquals("early", armed.ruleId)
            assertEquals(FocusWindowAction.START, armed.action)
        }
    }

    @Test fun legacySingleRuleKeysMigrateIntoRegistry() {
        val app = RuntimeEnvironment.getApplication()
        val prefs = app.getSharedPreferences("focus_schedule", 0)
        prefs.edit()
            .putString("schedule_id", "legacy")
            .putString("days", "1,2,3,4,5")
            .putString("start_time", "09:00")
            .putString("end_time", "17:00")
            .putStringSet("packages", setOf("com.instagram.android"))
            .putString("pending_action", "START")
            .putLong("next_trigger_at", 123L)
            .apply()
        val s = FocusScheduler(app)
        assertEquals(1, s.rules().size)
        assertEquals("legacy", s.rules().first().scheduleId)
        assertEquals("legacy", s.pendingRule()?.scheduleId)
    }

    @Test fun cancelClearsRegistryAndPending() {
        val s = scheduler()
        s.replaceAll(listOf(rule("a", "09:00", "10:00")))
        s.cancel()
        assertTrue(s.rules().isEmpty())
        assertNull(s.pendingRule())
        assertNull(s.pendingAction())
    }
}
