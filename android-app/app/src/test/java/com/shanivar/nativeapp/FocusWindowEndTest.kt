package com.shanivar.nativeapp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * DEV-2 / FOCUS-5: ending one window must not release protection that another window still owns.
 * Before the fix, END called releaseFocus() unconditionally, so this overlap left protection OFF.
 */
class FocusWindowEndTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    // 2026-09-14 is a Monday. Days use the 0=Sunday..6=Saturday convention.
    private val weekdays = setOf(1, 2, 3, 4, 5)
    private val packages = setOf("com.instagram.android")

    private fun at(hour: Int, minute: Int) = ZonedDateTime.of(2026, 9, 14, hour, minute, 0, 0, zone)

    private fun window(id: String, startHour: Int, endHour: Int) = FocusWindowEnd.Window(
        scheduleId = id,
        days = weekdays,
        startTime = LocalTime.of(startHour, 0),
        endTime = LocalTime.of(endHour, 0),
        blockPackages = packages,
    )

    @Test fun endingAWindowInsideAnotherDoesNotFullyReleaseAndLeavesTheOuterWindowArmed() = runBlocking {
        val outer = window("outer", 9, 17)
        val inner = window("inner", 10, 12)
        val atNoon = at(12, 0)
        val windows = listOf(outer, inner)

        assertFalse(FocusWindowEnd.shouldFullyRelease("inner", windows, atNoon))
        assertEquals(listOf("outer"), FocusWindowEnd.stillInEffect("inner", windows, atNoon).map { it.scheduleId })

        val next = FocusWindowEnd.soonestBoundary(windows, atNoon)
        assertEquals("outer", next?.scheduleId)
        assertEquals(FocusWindowAction.END, next?.action)
        assertEquals(at(17, 0), next?.at)

        val gateway = RecordingGateway(
            DisciplineStatus(
                ProtectionState.ACTIVE,
                alarmScheduled = false,
                focusEndsAtEpochMillis = at(17, 0).toInstant().toEpochMilli(),
                protectedPackages = packages,
            ),
        )
        val fullyReleased = FocusWindowEnd.apply("inner", windows, atNoon, gateway)
        assertFalse(fullyReleased)
        assertEquals(0, gateway.releaseCalls)
        assertEquals(0, gateway.startCalls)
        assertEquals(ProtectionState.ACTIVE, gateway.status.protection)
    }

    @Test fun endingTheEarlierWindowKeepsASequentialWindowThatStartsAtTheSameInstant() {
        val morning = window("morning", 9, 12)
        val afternoon = window("afternoon", 12, 15)
        val atNoon = at(12, 0)
        val windows = listOf(morning, afternoon)

        assertTrue(FocusWindowEnd.stillInEffect("morning", windows, atNoon).map { it.scheduleId }.contains("afternoon"))
        assertFalse(FocusWindowEnd.shouldFullyRelease("morning", windows, atNoon))
        val next = FocusWindowEnd.soonestBoundary(windows, atNoon)
        assertEquals("afternoon", next?.scheduleId)
        assertEquals(FocusWindowAction.END, next?.action)
    }

    @Test fun aLapsedSessionIsRestartedForTheWindowThatIsStillInEffect() = runBlocking {
        val outer = window("outer", 9, 17)
        val inner = window("inner", 10, 12)
        val atNoon = at(12, 0)
        val gateway = RecordingGateway(DisciplineStatus(ProtectionState.OFF, false))
        val fullyReleased = FocusWindowEnd.apply("inner", listOf(outer, inner), atNoon, gateway)
        assertFalse(fullyReleased)
        assertEquals(0, gateway.releaseCalls)
        assertEquals(1, gateway.startCalls)
        assertEquals(ProtectionState.ACTIVE, gateway.status.protection)
        assertEquals(packages, gateway.status.protectedPackages)
    }

    @Test fun endingTheOnlyWindowStillFullyReleases() = runBlocking {
        val only = window("only", 9, 12)
        val atNoon = at(12, 0)
        val gateway = RecordingGateway(DisciplineStatus(ProtectionState.ACTIVE, false, atNoon.toInstant().toEpochMilli(), packages))
        val fullyReleased = FocusWindowEnd.apply("only", listOf(only), atNoon, gateway)
        assertTrue(fullyReleased)
        assertEquals(1, gateway.releaseCalls)
        assertEquals(ProtectionState.OFF, gateway.status.protection)
    }

    private class RecordingGateway(var status: DisciplineStatus) : DisciplineGateway {
        var releaseCalls = 0
        var startCalls = 0

        override suspend fun status(): DisciplineStatus = status

        override suspend fun startFocus(minutes: Int, packageNames: Set<String>): DisciplineStatus {
            startCalls++
            status = DisciplineStatus(ProtectionState.ACTIVE, false, status.focusEndsAtEpochMillis, packageNames)
            return status
        }

        override suspend fun releaseFocus(): DisciplineStatus {
            releaseCalls++
            status = DisciplineStatus(ProtectionState.OFF, false)
            return status
        }
    }
}
