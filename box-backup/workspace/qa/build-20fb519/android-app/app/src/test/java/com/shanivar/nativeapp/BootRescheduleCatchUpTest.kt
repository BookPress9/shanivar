package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Reboot catch-up must protect a still-open window with no account, network, or [ShanivarSession].
 *
 * DEV-21: `BootRescheduleReceiver` used to read `ShanivarSession.api ?: return` before starting
 * protection, so a reboot inside a live window did nothing unless a session was already warm.
 * [FocusScheduleReceiver] already starts locally first. These tests fail if catch-up grows a
 * session dependency again, or if a closed window is turned into a fresh session (DEV-1).
 */
class BootRescheduleCatchUpTest {
    private class FakeStore : ProtectionStateStore {
        override fun read(): LocalProtection? = null
    }

    private class FakeGuard : FocusGuardController {
        var startCalls = 0
        var lastPackages: Set<String>? = null
        var lastEndsAt: Long? = null
        override fun start(packageNames: Set<String>, endsAtEpochMillis: Long) {
            startCalls++
            lastPackages = packageNames
            lastEndsAt = endsAtEpochMillis
        }
        override fun stop() = Unit
    }

    private class FakePerms(var usage: Boolean = true, var overlay: Boolean = true) : DisciplinePermissions {
        override fun hasUsageAccess() = usage
        override fun hasOverlayPermission() = overlay
        override fun hasExactAlarmPermission() = true
        override fun hasNotificationPermission() = true
    }

    private val zone = ZoneId.of("Asia/Kolkata")

    /** 2026-09-14 is a Monday. */
    private fun at(hour: Int, minute: Int) = ZonedDateTime.of(2026, 9, 14, hour, minute, 0, 0, zone)

    private val weekdays = setOf(1, 2, 3, 4, 5)
    private val packages = setOf("com.instagram.android")

    private fun rule(start: LocalTime, end: LocalTime) = FocusScheduler.Rule(
        scheduleId = "weekday-focus",
        days = weekdays,
        startTime = start,
        endTime = end,
        blockPackages = packages,
    )

    private fun controller(now: ZonedDateTime, perms: FakePerms = FakePerms()): Pair<LocalProtectionController, FakeGuard> {
        val guard = FakeGuard()
        val local = LocalProtectionController(FakeStore(), guard, perms) { now.toInstant().toEpochMilli() }
        return local to guard
    }

    @Test fun rebootInsideALiveWindowResumesLocalProtectionWithNoSession() {
        val now = at(10, 0) // Monday 10:00, inside 09:00-11:00
        val (local, guard) = controller(now)
        var scheduledAt: ZonedDateTime? = null
        val outcome = BootFocusCatchUp.apply(
            rule = rule(LocalTime.of(9, 0), LocalTime.of(11, 0)),
            now = now,
            local = local,
            scheduleNext = { _, at -> scheduledAt = at },
        )
        val activeEnd = FocusWindowRecurrence.activeWindowEnd(weekdays, LocalTime.of(9, 0), LocalTime.of(11, 0), now)!!
        assertTrue(outcome.started)
        assertEquals(1, guard.startCalls)
        assertEquals(packages, guard.lastPackages)
        // Remaining time until the real end, not a false 24-hour session.
        assertEquals(activeEnd.toInstant().toEpochMilli(), guard.lastEndsAt)
        assertEquals(60, outcome.mirrorMinutes)
        assertTrue(outcome.mirrorMinutes!! < 24 * 60)
        assertEquals(now, scheduledAt)
    }

    @Test fun rebootAfterTheWindowEndedDoesNotStartProtectionButStillRearms() {
        val now = at(12, 0) // Monday 12:00, after 09:00-11:00
        val (local, guard) = controller(now)
        var scheduled = 0
        val outcome = BootFocusCatchUp.apply(
            rule = rule(LocalTime.of(9, 0), LocalTime.of(11, 0)),
            now = now,
            local = local,
            scheduleNext = { _, _ -> scheduled++ },
        )
        assertFalse(outcome.started)
        assertNull(outcome.mirrorMinutes)
        assertEquals(0, guard.startCalls)
        assertEquals(1, scheduled)
    }

    @Test fun rebootBeforeTheWindowDoesNotStartProtection() {
        val now = at(8, 0)
        val (local, guard) = controller(now)
        val outcome = BootFocusCatchUp.apply(
            rule = rule(LocalTime.of(9, 0), LocalTime.of(11, 0)),
            now = now,
            local = local,
            scheduleNext = { _, _ -> },
        )
        assertFalse(outcome.started)
        assertEquals(0, guard.startCalls)
    }

    @Test fun catchUpStillRearmsWhenTheDeviceCannotEnforce() {
        val now = at(10, 0)
        val (local, guard) = controller(now, FakePerms(usage = false))
        var scheduled = 0
        val outcome = BootFocusCatchUp.apply(
            rule = rule(LocalTime.of(9, 0), LocalTime.of(11, 0)),
            now = now,
            local = local,
            scheduleNext = { _, _ -> scheduled++ },
        )
        assertFalse("a start the device cannot enforce is not a session to mirror", outcome.started)
        assertNull(outcome.mirrorMinutes)
        assertEquals(0, guard.startCalls)
        assertEquals(1, scheduled)
    }

    @Test fun catchUpDoesNotRequireASession() {
        // Structural, and the reason DEV-21 is fixed rather than mitigated: if catch-up could name
        // a session or an API type, a future change could put `api ?: return` back in front of the
        // local start. The behavioral tests above call [BootFocusCatchUp.apply] with no session
        // argument at all; this fails if that function grows one.
        val catchUp = java.io.File("src/main/java/com/shanivar/nativeapp/BootRescheduleReceiver.kt").readText()
        val catchUpBody = catchUp.substringAfter("internal object BootFocusCatchUp")
        listOf("ShanivarSession", "ShanivarApi", "DisciplineEngine", "DisciplineApi").forEach {
            assertFalse("BootFocusCatchUp must not reference $it", catchUpBody.contains(it))
        }

        val receiver = catchUp.lines()
            .filterNot { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")
            }
            .joinToString("\n")
        val localAt = receiver.indexOf("LocalProtectionController")
        val sessionAt = receiver.indexOf("ShanivarSession")
        assertTrue("receiver must start local protection", localAt >= 0)
        assertTrue("receiver may mirror only after local protection", sessionAt > localAt)
        assertFalse(
            "catch-up must not return when no session exists",
            Regex("""ShanivarSession\.api\s*\?:\s*return""").containsMatchIn(receiver),
        )
    }
}
