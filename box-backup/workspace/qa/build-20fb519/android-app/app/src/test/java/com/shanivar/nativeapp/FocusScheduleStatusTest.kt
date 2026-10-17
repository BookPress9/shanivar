package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * DEV-3 / FOCUS-6: a saved focus schedule must never be reported as Active (or Ready) without
 * device evidence. These fail if the card goes back to deriving its state from "a rule is saved"
 * or "a trigger is pending".
 */
class FocusScheduleStatusTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val insta = "com.instagram.android"
    private val youtube = "com.google.android.youtube"

    /** 2026-09-14 is a Monday. */
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)
    private fun ms(t: ZonedDateTime) = t.toInstant().toEpochMilli()

    /** Weekdays 09:00-17:00, pausing Instagram. */
    private val rule = FocusScheduler.Rule("s1", setOf(1, 2, 3, 4, 5), LocalTime.of(9, 0), LocalTime.of(17, 0), setOf(insta))

    private fun inputs(
        now: ZonedDateTime,
        saved: FocusScheduler.Rule? = rule,
        enabled: Boolean = true,
        armed: FocusScheduler.Rule? = rule,
        nextTrigger: Long? = null,
        exactAlarm: Boolean = true,
        usage: Boolean = true,
        overlay: Boolean = true,
        guard: LocalProtection? = null,
    ) = FocusScheduleInputs(now, saved, enabled, armed, nextTrigger, exactAlarm, usage, overlay, guard)

    private fun resolve(i: FocusScheduleInputs) = FocusScheduleStatusResolver.resolve(i)

    // ── The bug: saved / armed is not Active ────────────────────────────────────────────────────

    @Test fun savedAndArmedInsideTheWindowWithoutAGuardIsNotActive() {
        // Saved at 10:00 on Monday: the scheduler arms the 17:00 END, but nothing started the guard.
        // The old card showed "Next change: Mon 17:00" in the success colour - read as "on".
        val now = at(14, 10)
        val status = resolve(inputs(now, nextTrigger = ms(at(14, 17))))
        assertNotEquals(FocusRuleState.ACTIVE, status.state)
        assertEquals(FocusRuleState.NEEDS_ATTENTION, status.state)
        assertEquals(FocusScheduleStatus.Problem.WINDOW_OPEN_NOT_PROTECTING, status.problem)
        assertNull(status.activeUntilEpochMillis)
    }

    @Test fun savedAndArmedOutsideTheWindowIsReadyNotActive() {
        val now = at(14, 7)
        val status = resolve(inputs(now, nextTrigger = ms(at(14, 9))))
        assertEquals(FocusRuleState.READY, status.state)
        assertEquals(ms(at(14, 9)), status.nextChangeEpochMillis)
        assertNull(status.activeUntilEpochMillis)
        assertTrue(status.describe { "T" }.contains("not on yet"))
    }

    @Test fun savedButNotArmedOnThisDeviceIsSavedNotReady() {
        // e.g. a second device or a reinstall: the account copy exists, this phone has no trigger.
        val status = resolve(inputs(at(14, 7), armed = null, nextTrigger = null))
        assertEquals(FocusRuleState.SAVED_LOCAL, status.state)
        assertEquals("Saved", status.label)
    }

    @Test fun anArmedTriggerForADifferentVersionOfTheRuleIsNotReady() {
        // An edit that failed to re-arm leaves the old times armed; "Ready" would describe the wrong window.
        val stale = rule.copy(endTime = LocalTime.of(12, 0))
        val status = resolve(inputs(at(14, 7), armed = stale, nextTrigger = ms(at(14, 9))))
        assertEquals(FocusRuleState.SAVED_LOCAL, status.state)
    }

    @Test fun aGuardRunningOutsideTheWindowDoesNotMakeTheScheduleActive() {
        // A manual focus session at 07:00 is not this schedule protecting anything.
        val now = at(14, 7)
        val manual = LocalProtection(setOf(insta), ms(at(14, 8)))
        val status = resolve(inputs(now, nextTrigger = ms(at(14, 9)), guard = manual))
        assertEquals(FocusRuleState.READY, status.state)
    }

    // ── Engine confirmation is Active ───────────────────────────────────────────────────────────

    @Test fun insideTheWindowWithTheGuardConfirmingIsActiveUntilTheGuardEnd() {
        val now = at(14, 10)
        val guard = LocalProtection(setOf(insta), ms(at(14, 17)))
        val status = resolve(inputs(now, nextTrigger = ms(at(14, 17)), guard = guard))
        assertEquals(FocusRuleState.ACTIVE, status.state)
        assertEquals(ms(at(14, 17)), status.activeUntilEpochMillis)
        assertEquals("Active", status.label)
    }

    @Test fun anElapsedGuardRecordIsNotConfirmation() {
        val now = at(14, 10)
        val elapsed = LocalProtection(setOf(insta), ms(at(14, 9, 30)))
        assertEquals(FocusRuleState.NEEDS_ATTENTION, resolve(inputs(now, nextTrigger = ms(at(14, 17)), guard = elapsed)).state)
    }

    @Test fun aGuardThatDoesNotCoverTheRulesAppsIsNotConfirmation() {
        val now = at(14, 10)
        val other = LocalProtection(setOf(youtube), ms(at(14, 17)))
        assertEquals(FocusRuleState.NEEDS_ATTENTION, resolve(inputs(now, nextTrigger = ms(at(14, 17)), guard = other)).state)
    }

    @Test fun aWiderGuardThatCoversTheRulesAppsIsConfirmation() {
        val now = at(14, 10)
        val wider = LocalProtection(setOf(insta, youtube), ms(at(14, 18)))
        assertEquals(FocusRuleState.ACTIVE, resolve(inputs(now, nextTrigger = ms(at(14, 17)), guard = wider)).state)
    }

    @Test fun fullCalendarDayIsActiveOnlyWithTheGuard() {
        // DEV-6 shape: every day 00:00-00:00 is always "inside the window"; that alone must not read as Active.
        val allDay = FocusScheduler.Rule("s1", (0..6).toSet(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, setOf(insta))
        val now = at(14, 10)
        val noGuard = inputs(now, saved = allDay, armed = allDay, nextTrigger = ms(at(15, 0)))
        assertEquals(FocusRuleState.NEEDS_ATTENTION, resolve(noGuard).state)
        val guarded = noGuard.copy(localProtection = LocalProtection(setOf(insta), ms(at(15, 0))))
        assertEquals(FocusRuleState.ACTIVE, resolve(guarded).state)
    }

    // ── Device access and other non-green states ────────────────────────────────────────────────

    @Test fun revokedUsageAccessIsNeedsDeviceAccessEvenWithALiveGuardRecord() {
        // Without Usage access the guard cannot see the foreground app, so its record proves nothing.
        val now = at(14, 10)
        val guard = LocalProtection(setOf(insta), ms(at(14, 17)))
        val status = resolve(inputs(now, nextTrigger = ms(at(14, 17)), usage = false, guard = guard))
        assertEquals(FocusRuleState.NEEDS_DEVICE_ACCESS, status.state)
        assertEquals(setOf(FocusDeviceAccess.USAGE_ACCESS), status.missingAccess)
        assertTrue(status.describe { "T" }.contains("Usage access"))
    }

    @Test fun revokedExactAlarmIsNeedsDeviceAccessNotReady() {
        val status = resolve(inputs(at(14, 7), nextTrigger = ms(at(14, 9)), exactAlarm = false, overlay = false))
        assertEquals(FocusRuleState.NEEDS_DEVICE_ACCESS, status.state)
        assertEquals(setOf(FocusDeviceAccess.EXACT_ALARM, FocusDeviceAccess.OVERLAY), status.missingAccess)
        val text = status.describe { "T" }
        assertTrue(text.contains("Alarms & reminders"))
        assertTrue(text.contains("Display over other apps"))
    }

    @Test fun aMissedBoundaryIsNeedsAttention() {
        val status = resolve(inputs(at(14, 8), nextTrigger = ms(at(14, 7))))
        assertEquals(FocusRuleState.NEEDS_ATTENTION, status.state)
        assertEquals(FocusScheduleStatus.Problem.MISSED_BOUNDARY, status.problem)
    }

    @Test fun aSlightlyLateDeliveryIsStillReady() {
        val status = resolve(inputs(at(14, 7, 2), nextTrigger = ms(at(14, 7))))
        assertEquals(FocusRuleState.READY, status.state)
    }

    @Test fun disabledIsOffAndNothingSavedIsDraft() {
        assertEquals(FocusRuleState.OFF, resolve(inputs(at(14, 10), enabled = false, guard = LocalProtection(setOf(insta), ms(at(14, 17))))).state)
        assertEquals(FocusRuleState.DRAFT, resolve(inputs(at(14, 10), saved = null)).state)
    }

    @Test fun savedReadyAndActiveAreDistinctStates() {
        val now = at(14, 10)
        val saved = resolve(inputs(at(14, 7), armed = null)).state
        val ready = resolve(inputs(at(14, 7), nextTrigger = ms(at(14, 9)))).state
        val active = resolve(inputs(now, nextTrigger = ms(at(14, 17)), guard = LocalProtection(setOf(insta), ms(at(14, 17))))).state
        assertEquals(3, setOf(saved, ready, active).size)
        assertFalse(resolve(inputs(at(14, 7), nextTrigger = ms(at(14, 9)))).describe { "T" }.startsWith("Active"))
    }
}
