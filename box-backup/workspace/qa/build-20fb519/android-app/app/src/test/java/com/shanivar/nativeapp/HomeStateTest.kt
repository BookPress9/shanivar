package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Dashboard's next-action resolver. Pure, so the ordering can be pinned before any UI exists.
 *
 * The case worth protecting is the first one: an active focus window must resolve to *leaving*. It is
 * the one place where the product's stated purpose and the usual instinct of a product surface point in
 * opposite directions, and a later change that "improves engagement" here would break the mission while
 * looking like an enhancement.
 */
class HomeStateTest {
    private val now = 1_800_000_000_000L
    private fun snapshot(
        focusEndsAt: Long = 0L,
        nextAlarm: Long? = null,
        hasSchedule: Boolean = false,
        practised: Boolean = false,
        window: ScheduleWindow = ScheduleWindow.UNKNOWN,
    ) = HomeSnapshot(
        nowEpochMillis = now, focusEndsAtEpochMillis = focusEndsAt, nextAlarmEpochMillis = nextAlarm,
        hasFocusSchedule = hasSchedule, practisedToday = practised, scheduleWindow = window,
        intentionLabel = "Reading", focusMinutes = 30,
    )

    @Test fun anActiveFocusWindowOffersLeavingRatherThanAnythingToDoInTheApp() {
        val action = HomeState.resolve(snapshot(focusEndsAt = now + 20 * 60_000L, nextAlarm = now + 99_999_999L))
        assertEquals(NextActionKind.FOCUS_ACTIVE, action.kind)
        assertEquals("Return to my day", action.actionLabel)
        assertEquals(ShanivarTab.DASHBOARD, action.tab)
        assertTrue(action.detail.contains("Reading"))
        assertTrue(action.detail.contains("20 min"))
    }

    @Test fun anExpiredFocusWindowDoesNotStillClaimProtection() {
        val action = HomeState.resolve(snapshot(focusEndsAt = now - 1, hasSchedule = true, nextAlarm = now + 99_999_999L))
        assertEquals(NextActionKind.START_FOCUS, action.kind)
    }

    @Test fun anImminentAlarmOutranksEverythingExceptActiveProtection() {
        val action = HomeState.resolve(snapshot(nextAlarm = now + 30 * 60_000L, hasSchedule = true))
        assertEquals(NextActionKind.ALARM_SOON, action.kind)
        assertEquals(ShanivarTab.ROUTINE, action.tab)
        // But not while protection is running.
        val duringFocus = HomeState.resolve(snapshot(focusEndsAt = now + 60_000L, nextAlarm = now + 30 * 60_000L))
        assertEquals(NextActionKind.FOCUS_ACTIVE, duringFocus.kind)
    }

    @Test fun anAlarmFurtherOutThanAnHourIsNotTreatedAsImminent() {
        val action = HomeState.resolve(snapshot(nextAlarm = now + HomeState.ALARM_SOON_MILLIS + 1, hasSchedule = true))
        assertEquals(NextActionKind.START_FOCUS, action.kind)
    }

    @Test fun anUnconfiguredInstallIsSentToTheRoutineFirst() {
        val action = HomeState.resolve(snapshot())
        assertEquals(NextActionKind.SET_UP_ROUTINE, action.kind)
        assertEquals(ShanivarTab.ROUTINE, action.tab)
    }

    @Test fun aPracticeIsOfferedNearTheEdgesOfTheStatedDayAndNeverInTheMiddleOfIt() {
        val configured = snapshot(nextAlarm = now + 99_999_999L, hasSchedule = true)
        listOf(ScheduleWindow.JUST_WOKE_WINDOW, ScheduleWindow.WIND_DOWN_WINDOW).forEach { window ->
            assertEquals(NextActionKind.OFFER_PRACTICE, HomeState.resolve(configured.copy(scheduleWindow = window)).kind)
        }
        // Daytime, past the stated sleep time, and an unusable schedule all decline to suggest one.
        // REST_WINDOW in particular is not evidence of anything about the person.
        listOf(ScheduleWindow.DAYTIME, ScheduleWindow.REST_WINDOW, ScheduleWindow.UNKNOWN).forEach { window ->
            assertEquals(NextActionKind.START_FOCUS, HomeState.resolve(configured.copy(scheduleWindow = window)).kind)
        }
    }

    @Test fun havingPractisedSuppressesTheOfferWithoutBecomingAScore() {
        val configured = snapshot(nextAlarm = now + 99_999_999L, hasSchedule = true, window = ScheduleWindow.JUST_WOKE_WINDOW)
        assertEquals(NextActionKind.OFFER_PRACTICE, HomeState.resolve(configured).kind)
        val after = HomeState.resolve(configured.copy(practisedToday = true))
        assertEquals(NextActionKind.START_FOCUS, after.kind)
        // No count, streak or rank appears anywhere in what the Dashboard says (decision 0009).
        listOf("streak", "day 1", "level", "points", "score", "xp").forEach {
            assertTrue(!after.headline.lowercase().contains(it) && !after.detail.lowercase().contains(it))
        }
    }

    @Test fun theResolverIsDeterministicForIdenticalInput() {
        val input = snapshot(nextAlarm = now + 99_999_999L, hasSchedule = true, window = ScheduleWindow.WIND_DOWN_WINDOW)
        val first = HomeState.resolve(input)
        repeat(50) { assertEquals(first, HomeState.resolve(input)) }
    }

    @Test fun everyResolvedActionNamesSomethingTheUserCanDo() {
        listOf(
            snapshot(),
            snapshot(focusEndsAt = now + 60_000L),
            snapshot(nextAlarm = now + 60_000L),
            snapshot(nextAlarm = now + 99_999_999L, hasSchedule = true),
            snapshot(nextAlarm = now + 99_999_999L, hasSchedule = true, window = ScheduleWindow.JUST_WOKE_WINDOW),
        ).forEach { input ->
            val action = HomeState.resolve(input)
            assertTrue(action.headline.isNotBlank())
            assertTrue(action.detail.isNotBlank())
            assertTrue(action.actionLabel.isNotBlank())
        }
    }
}
