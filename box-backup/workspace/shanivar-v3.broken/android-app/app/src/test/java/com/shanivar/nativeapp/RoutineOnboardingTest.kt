package com.shanivar.nativeapp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalTime

/** Pure page machine + plan writes for Routine V3 onboarding. */
class RoutineOnboardingMachineTest {
    private fun reduce(state: RoutineOnboardingState, event: RoutineOnboardingEvent) =
        RoutineOnboardingMachine.reduce(state, event)

    @Test fun welcomeAdvancesToRhythm() {
        val next = reduce(RoutineOnboardingState(), RoutineOnboardingEvent.StartBuilding)
        assertEquals(RoutineOnboardingPage.RHYTHM, next.page)
    }

    @Test fun identicalAnchorsAreRejected() {
        var state = RoutineOnboardingState(
            page = RoutineOnboardingPage.RHYTHM,
            draft = RoutineOnboardingDraft(wakeTime = LocalTime.of(7, 0), intendedSleepTime = LocalTime.of(7, 0)),
        )
        state = reduce(state, RoutineOnboardingEvent.UseTheseTimes)
        assertEquals(RoutineOnboardingPage.RHYTHM, state.page)
        assertEquals("Choose different wake and sleep times.", state.draft.error)
        assertFalse(state.anchorsCommitted)
    }

    @Test fun useTheseTimesCommitsAndOpensSetup() {
        var state = RoutineOnboardingState(page = RoutineOnboardingPage.RHYTHM)
        state = reduce(state, RoutineOnboardingEvent.SetWake(LocalTime.of(6, 30)))
        state = reduce(state, RoutineOnboardingEvent.SetSleep(LocalTime.of(22, 45)))
        state = reduce(state, RoutineOnboardingEvent.UseTheseTimes)
        assertEquals(RoutineOnboardingPage.SETUP, state.page)
        assertTrue(state.anchorsCommitted)
        assertNull(state.draft.error)
    }

    @Test fun setLaterStillOpensSetup() {
        val next = reduce(
            RoutineOnboardingState(page = RoutineOnboardingPage.RHYTHM),
            RoutineOnboardingEvent.SetRhythmLater,
        )
        assertEquals(RoutineOnboardingPage.SETUP, next.page)
        assertTrue(next.anchorsCommitted)
    }

    @Test fun optionalAlarmAndProtectionDoNotAutoArmFromSetup() {
        var state = RoutineOnboardingState(page = RoutineOnboardingPage.SETUP, anchorsCommitted = true)
        assertFalse(state.draft.alarmArmedOnDevice)
        assertFalse(state.draft.protectionSaved)
        state = reduce(state, RoutineOnboardingEvent.GoToRoutine)
        assertEquals(RoutineOnboardingPage.DONE, state.page)
        assertFalse(state.draft.alarmArmedOnDevice)
        assertFalse(state.draft.protectionSaved)
    }

    @Test fun openingProtectionSeedsOneWindowWithoutSaving() {
        val next = reduce(
            RoutineOnboardingState(page = RoutineOnboardingPage.SETUP, anchorsCommitted = true),
            RoutineOnboardingEvent.OpenProtection,
        )
        assertEquals(RoutineOnboardingPage.PROTECTION, next.page)
        assertNotNull(next.draft.window)
        assertFalse(next.draft.protectionSaved)
    }

    @Test fun protectionRequiresAppsWhenWindowEnabled() {
        val draft = RoutineOnboardingDraft(
            window = RoutineOnboardingMachine.defaultFirstWindow("w1"),
            packages = emptySet(),
        )
        val (_, error) = RoutineOnboardingMachine.protectionPlanOrError(RoutinePlanV1(), draft)
        assertEquals("Choose at least one app to protect your attention from.", error)
    }

    @Test fun protectionPlanAppliesWindowAndPackagesWithoutTouchingActivities() {
        val activity = RoutineActivityV1("gym", "Gym", LocalTime.of(7, 30), 60, (0..6).toSet(), true)
        val current = RoutinePlanV1(
            wakeTime = LocalTime.of(6, 0),
            intendedSleepTime = LocalTime.of(22, 0),
            activities = listOf(activity),
        )
        val draft = RoutineOnboardingDraft(
            wakeTime = LocalTime.of(6, 15),
            intendedSleepTime = LocalTime.of(22, 30),
            window = ProtectionWindowV1("w1", LocalTime.of(18, 0), LocalTime.of(19, 0), (0..6).toSet(), true),
            packages = setOf("com.instagram.android"),
        )
        val (plan, error) = RoutineOnboardingMachine.protectionPlanOrError(current, draft)
        assertNull(error)
        assertNotNull(plan)
        assertEquals(listOf(activity), plan!!.activities)
        assertEquals(setOf("com.instagram.android"), plan.protectedPackages)
        assertEquals(1, plan.windows.size)
        // Anchors on the protection plan come from current until planWithAnchors merges the draft.
        val withAnchors = RoutineOnboardingMachine.planWithAnchors(plan, draft)
        assertEquals(LocalTime.of(6, 15), withAnchors.wakeTime)
        assertEquals(LocalTime.of(22, 30), withAnchors.intendedSleepTime)
    }

    @Test fun backStackMatchesWireframeOrder() {
        var state = RoutineOnboardingState(page = RoutineOnboardingPage.PERMISSIONS)
        state = reduce(state, RoutineOnboardingEvent.Back)
        assertEquals(RoutineOnboardingPage.SETUP, state.page)
        state = reduce(state, RoutineOnboardingEvent.Back)
        assertEquals(RoutineOnboardingPage.RHYTHM, state.page)
        state = reduce(state, RoutineOnboardingEvent.Back)
        assertEquals(RoutineOnboardingPage.WELCOME, state.page)
    }

    @Test fun debugPathOnlyWhenDebugBuildAndPref() {
        assertTrue(shouldUseRoutineV3Onboarding(routineV3 = true, debugBuild = false, debugPrefEnabled = false))
        assertFalse(shouldUseRoutineV3Onboarding(routineV3 = false, debugBuild = false, debugPrefEnabled = true))
        assertFalse(shouldUseRoutineV3Onboarding(routineV3 = false, debugBuild = true, debugPrefEnabled = false))
        assertTrue(shouldUseRoutineV3Onboarding(routineV3 = false, debugBuild = true, debugPrefEnabled = true))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoutineOnboardingPlanWriteTest {
    @Test fun useTheseTimesWritesAnchorsOnly() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val repo = SharedPrefsRoutinePlanRepository(app)
        val draft = RoutineOnboardingDraft(
            wakeTime = LocalTime.of(6, 20),
            intendedSleepTime = LocalTime.of(22, 40),
        )
        val plan = RoutineOnboardingMachine.planWithAnchors(repo.current(), draft)
        assertTrue(repo.save(plan).isSuccess)
        val reloaded = SharedPrefsRoutinePlanRepository(app).current()
        assertEquals(LocalTime.of(6, 20), reloaded.wakeTime)
        assertEquals(LocalTime.of(22, 40), reloaded.intendedSleepTime)
        assertTrue(reloaded.windows.isEmpty())
        assertTrue(reloaded.protectedPackages.isEmpty())
        assertTrue(reloaded.activities.isEmpty())
    }

    @Test fun protectionSaveWritesWindowAndDoesNotClaimActivitiesArmAnything() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val gateway = object : ProtectionGateway {
            var synced: RoutinePlanV1? = null
            override fun registryRules() = emptyList<FocusScheduler.Rule>()
            override fun sync(plan: RoutinePlanV1, now: java.time.ZonedDateTime): ProtectionSyncResult {
                synced = plan
                return ProtectionSyncResult.Synced(armed = null, enabledRules = plan.windows.count { it.enabled })
            }
            override fun rearm(now: java.time.ZonedDateTime) = null
            override fun evidence() = ProtectionDeviceEvidence(
                registry = emptyList(), armed = null,
                hasExactAlarm = true, hasUsageAccess = true, hasOverlay = true, localProtection = null,
            )
            override fun stopNow() {}
        }
        val repo = ProtectionSyncingRoutinePlanRepository(SharedPrefsRoutinePlanRepository(app), gateway)
        // Anchors first, as the flow does.
        assertTrue(
            repo.save(
                RoutineOnboardingMachine.planWithAnchors(
                    repo.current(),
                    RoutineOnboardingDraft(wakeTime = LocalTime.of(7, 0), intendedSleepTime = LocalTime.of(23, 0)),
                ),
            ).isSuccess,
        )
        val draft = RoutineOnboardingDraft(
            wakeTime = LocalTime.of(7, 0),
            intendedSleepTime = LocalTime.of(23, 0),
            window = ProtectionWindowV1("onboard-w1", LocalTime.of(18, 0), LocalTime.of(19, 0), (0..6).toSet(), true),
            packages = setOf("com.instagram.android"),
        )
        val (plan, error) = RoutineOnboardingMachine.protectionPlanOrError(repo.current(), draft)
        assertNull(error)
        assertTrue(repo.save(RoutineOnboardingMachine.planWithAnchors(plan!!, draft)).isSuccess)
        assertEquals("onboard-w1", repo.current().windows.single().id)
        assertEquals(setOf("com.instagram.android"), repo.current().protectedPackages)
        assertNotNull(gateway.synced)
        // Sync happened because protection config changed — still not Active; ArmedBoundary is null here.
        assertTrue(repo.lastSync is ProtectionSyncResult.Synced)
        assertNull((repo.lastSync as ProtectionSyncResult.Synced).armed)
    }
}
