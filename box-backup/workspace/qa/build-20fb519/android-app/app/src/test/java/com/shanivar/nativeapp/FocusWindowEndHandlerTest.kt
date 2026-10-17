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
 * DEV-2 / FOCUS-5: a focus window END must release only protection the closing occurrence owns.
 *
 * The old END path was `local.stopNow()` + `discipline.releaseFocus()` whenever the pending rule
 * had no active occurrence - with no idea who owned the protection in force. These tests fail if an
 * END goes back to stopping protection that another still-valid window or a manual session needs.
 */
class FocusWindowEndHandlerTest {
    /** Guard and store share state, so `start`/`stop` are visible through `local.active()` like the
     * real `focus_guard` preferences. */
    private class Device {
        var stored: LocalProtection? = null
        var startCalls = 0
        var stopCalls = 0
        val store = ProtectionStateStore { stored }
        val guard = object : FocusGuardController {
            override fun start(packageNames: Set<String>, endsAtEpochMillis: Long) {
                startCalls++; stored = LocalProtection(packageNames, endsAtEpochMillis)
            }
            override fun stop() { stopCalls++; stored = null }
        }
    }

    private class FakePerms(var usage: Boolean = true, var overlay: Boolean = true) : DisciplinePermissions {
        override fun hasUsageAccess() = usage
        override fun hasOverlayPermission() = overlay
        override fun hasExactAlarmPermission() = true
        override fun hasNotificationPermission() = true
    }

    private val zone = ZoneId.of("Asia/Kolkata")

    /** 2026-09-14 is a Monday. */
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)
    private fun ms(t: ZonedDateTime) = t.toInstant().toEpochMilli()

    private val monday = setOf(1)
    private val tuesday = setOf(2)
    private val weekdays = setOf(1, 2, 3, 4, 5)
    private val insta = "com.instagram.android"
    private val youtube = "com.google.android.youtube"

    private fun rule(id: String, days: Set<Int>, start: String, end: String, vararg packages: String) =
        FocusScheduler.Rule(id, days, LocalTime.parse(start), LocalTime.parse(end), packages.toSet())

    private fun local(device: Device, now: ZonedDateTime, perms: FakePerms = FakePerms()) =
        LocalProtectionController(device.store, device.guard, perms) { ms(now) }

    // ── Two windows ─────────────────────────────────────────────────────────────────────────────

    @Test fun earlierEndOfOverlappingWindowsDoesNotReleaseTheLaterWindow() {
        // A 09:00-11:00 (Instagram), B 10:00-12:00 (YouTube). A ends inside B.
        val a = rule("a", monday, "09:00", "11:00", insta)
        val b = rule("b", monday, "10:00", "12:00", youtube)
        val now = at(14, 11)
        val device = Device().apply { stored = LocalProtection(setOf(insta, youtube), ms(now)) }

        val outcome = FocusWindowEndHandler.apply(listOf(a, b), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.CONTINUED, outcome.result)
        assertEquals("B's protection was never stopped", 0, device.stopCalls)
        // Narrowed to what is still valid: A's app is released, B's stays until B's own end.
        assertEquals(setOf(youtube), device.stored?.packages)
        assertEquals(ms(at(14, 12)), device.stored?.endsAtEpochMillis)
        assertFalse(outcome.absorbedForeign)
    }

    @Test fun innerWindowEndingInsideAnOuterWindowLeavesTheOuterWindowProtected() {
        val outer = rule("outer", monday, "09:00", "12:00", insta)
        val inner = rule("inner", monday, "10:00", "11:00", insta, youtube)
        val now = at(14, 11)
        val device = Device().apply { stored = LocalProtection(setOf(insta, youtube), ms(now)) }

        val outcome = FocusWindowEndHandler.apply(listOf(outer, inner), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.CONTINUED, outcome.result)
        assertEquals(0, device.stopCalls)
        assertEquals(LocalProtection(setOf(insta), ms(at(14, 12))), device.stored)
    }

    @Test fun protectionAlreadyRecordedForTheLaterWindowIsKeptUntouched() {
        // B's START recorded the union until 12:00 after A's. A's END must not touch it.
        val a = rule("a", monday, "09:00", "11:00", insta)
        val b = rule("b", monday, "10:00", "12:00", youtube)
        val now = at(14, 11)
        val bProtection = LocalProtection(setOf(insta, youtube), ms(at(14, 12)))
        val device = Device().apply { stored = bProtection }

        val outcome = FocusWindowEndHandler.apply(listOf(a, b), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.KEPT, outcome.result)
        assertEquals(0, device.stopCalls)
        assertEquals(0, device.startCalls)
        assertEquals(bProtection, device.stored)
    }

    @Test fun theLastEndWithNothingStillValidReleases() {
        val a = rule("a", monday, "09:00", "11:00", insta)
        val b = rule("b", monday, "10:00", "12:00", youtube)
        val now = at(14, 12)
        val device = Device().apply { stored = LocalProtection(setOf(youtube), ms(now)) }

        val outcome = FocusWindowEndHandler.apply(listOf(a, b), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.RELEASED, outcome.result)
        assertEquals(1, device.stopCalls)
        assertNull(device.stored)
    }

    // ── Single rule (what v1 persists today) ────────────────────────────────────────────────────

    @Test fun windowEndDoesNotReleaseAManualSessionThatOutlivesIt() {
        // Live today with one rule: a manual 2-hour session started at 10:30 inside 09:00-11:00.
        val window = rule("w", weekdays, "09:00", "11:00", insta)
        val now = at(14, 11)
        val manual = LocalProtection(setOf(insta, youtube), ms(at(14, 12, 30)))
        val device = Device().apply { stored = manual }

        val outcome = FocusWindowEndHandler.apply(listOf(window), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.KEPT, outcome.result)
        assertEquals(0, device.stopCalls)
        assertEquals(manual, device.stored)
    }

    @Test fun normalSingleWindowEndReleasesItsOwnProtection() {
        val window = rule("w", weekdays, "09:00", "11:00", insta)
        val now = at(14, 11)
        val device = Device().apply { stored = LocalProtection(setOf(insta), ms(now)) }

        val outcome = FocusWindowEndHandler.apply(listOf(window), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.RELEASED, outcome.result)
        assertEquals(1, device.stopCalls)
    }

    @Test fun staleEndAfterTheWindowAlreadyEndedCannotHoldOrFalselyRelease() {
        // Monday 11:00 END delivered at 15:00. Protection already elapsed; cleanup, no new session.
        val window = rule("w", weekdays, "09:00", "11:00", insta)
        val now = at(14, 15)
        val device = Device().apply { stored = LocalProtection(setOf(insta), ms(at(14, 11))) }

        val outcome = FocusWindowEndHandler.apply(listOf(window), ms(at(14, 11)), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.RELEASED, outcome.result)
        assertEquals(0, device.startCalls)
    }

    @Test fun lateEndDeliveredInsideTheNextOccurrenceProtectsThatOccurrenceInsteadOfReleasing() {
        // Monday 11:00 END delivered Tuesday 09:30 (Doze/powered off). Tuesday's occurrence is
        // valid; the old path did nothing here and left it unprotected.
        val window = rule("w", weekdays, "09:00", "11:00", insta)
        val now = at(15, 9, 30)
        val device = Device().apply { stored = LocalProtection(setOf(insta), ms(at(14, 11))) }

        val outcome = FocusWindowEndHandler.apply(listOf(window), ms(at(14, 11)), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.CONTINUED, outcome.result)
        assertEquals(0, device.stopCalls)
        // Until Tuesday 11:00 - the occurrence active now - never a 24-hour session (DEV-1).
        assertEquals(LocalProtection(setOf(insta), ms(at(15, 11))), device.stored)
    }

    // ── Overnight, all-day (DEV-6) ──────────────────────────────────────────────────────────────

    @Test fun overnightEndReleasesOnTheMorningAfter() {
        val sleep = rule("sleep", monday, "22:00", "06:00", insta)
        val now = at(15, 6)
        val device = Device().apply { stored = LocalProtection(setOf(insta), ms(now)) }

        val outcome = FocusWindowEndHandler.apply(listOf(sleep), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.RELEASED, outcome.result)
    }

    @Test fun overnightEndDoesNotReleaseAMorningWindowThatAlreadyOpened() {
        val sleep = rule("sleep", monday, "22:00", "06:00", insta)
        val morning = rule("morning", tuesday, "05:00", "08:00", youtube)
        val now = at(15, 6)
        val device = Device().apply { stored = LocalProtection(setOf(insta, youtube), ms(now)) }

        val outcome = FocusWindowEndHandler.apply(listOf(sleep, morning), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.CONTINUED, outcome.result)
        assertEquals(0, device.stopCalls)
        assertEquals(LocalProtection(setOf(youtube), ms(at(15, 8))), device.stored)
    }

    @Test fun fullCalendarDayEndAtNextMidnightReleases() {
        val allDay = rule("all", monday, "00:00", "00:00", insta)
        val now = at(15, 0) // Tuesday 00:00, the exclusive end of Monday
        val device = Device().apply { stored = LocalProtection(setOf(insta), ms(now)) }

        val outcome = FocusWindowEndHandler.apply(listOf(allDay), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.RELEASED, outcome.result)
    }

    @Test fun fullCalendarDayEndDoesNotReleaseAnAdjacentWindowOpeningAtMidnight() {
        val allDay = rule("all", monday, "00:00", "00:00", insta)
        val early = rule("early", tuesday, "00:00", "02:00", insta)
        val now = at(15, 0)
        val device = Device().apply { stored = LocalProtection(setOf(insta), ms(now)) }

        val outcome = FocusWindowEndHandler.apply(listOf(allDay, early), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.CONTINUED, outcome.result)
        assertEquals(0, device.stopCalls)
        assertEquals(LocalProtection(setOf(insta), ms(at(15, 2))), device.stored)
    }

    // ── Never shorten foreign protection; truthful when it cannot enforce ───────────────────────

    @Test fun stillActiveWindowNotCoveredByAForeignSessionIsAddedWithoutShorteningIt() {
        val a = rule("a", monday, "09:00", "11:00", insta)
        val b = rule("b", monday, "10:00", "12:00", youtube)
        val now = at(14, 11)
        val manual = LocalProtection(setOf(insta), ms(at(14, 13)))
        val device = Device().apply { stored = manual }

        val outcome = FocusWindowEndHandler.apply(listOf(a, b), ms(now), now, local(device, now))

        assertEquals(FocusWindowEndHandler.Result.CONTINUED, outcome.result)
        assertTrue(outcome.absorbedForeign)
        assertEquals(0, device.stopCalls)
        assertEquals(LocalProtection(setOf(insta, youtube), ms(at(14, 13))), device.stored)
    }

    @Test fun cannotEnforceStillActiveWindowLeavesForeignProtectionAlone() {
        val a = rule("a", monday, "09:00", "11:00", insta)
        val b = rule("b", monday, "10:00", "12:00", youtube)
        val now = at(14, 11)
        val manual = LocalProtection(setOf(insta), ms(at(14, 13)))
        val device = Device().apply { stored = manual }

        val outcome = FocusWindowEndHandler.apply(listOf(a, b), ms(now), now, local(device, now, FakePerms(usage = false)))

        assertEquals(FocusWindowEndHandler.Result.CANNOT_ENFORCE, outcome.result)
        assertEquals(0, device.stopCalls)
        assertEquals(manual, device.stored)
    }

    @Test fun endHandlerNamesNoSessionOrApi() {
        val source = java.io.File("src/main/java/com/shanivar/nativeapp/FocusScheduleReceiver.kt").readText()
        val handler = source.substringAfter("internal object FocusWindowEndHandler")
        listOf("ShanivarSession", "ShanivarApi", "DisciplineEngine", "DisciplineApi", "releaseFocus").forEach {
            assertFalse("FocusWindowEndHandler must not reference $it", handler.contains(it))
        }
        val code = source.lines().filterNot { val t = it.trimStart(); t.startsWith("*") || t.startsWith("//") || t.startsWith("/*") }
            .joinToString("\n")
        assertFalse(
            "END must not stop protection unconditionally",
            Regex("""FocusWindowAction\.END\s*->\s*\{?\s*(if \(activeEnd == null\) )?local\.stopNow\(\)""").containsMatchIn(code),
        )
        assertTrue("END goes through the ownership-aware handler", code.contains("FocusWindowEndHandler.apply("))
    }
}
