package com.shanivar.nativeapp

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FakeInitiationPorts : InitiationPorts {
    var plan = RoutinePlanV1()
    var alarmResult = InitiationAlarm.SCHEDULED
    var protection: FocusRuleState? = FocusRuleState.READY
    var stopped = 0
    var day: LocalDate = LocalDate.parse("2026-10-12")
    var wall = 1_000L; var mono = 0L
    var access = AccessResume.Access(false, false)
    override suspend fun saveRoutine(plan: RoutinePlanV1): Result<Unit> { this.plan = plan; return Result.success(Unit) }
    override fun currentPlan() = plan
    override fun scheduleWakeAlarm(wake: LocalTime, days: Set<Int>) = alarmResult
    override fun cancelWakeAlarm() {}
    override fun access() = access
    override fun protectionState() = protection
    override fun stopProtectionNow() { stopped++ }
    override fun installedApps() = listOf(ProtectionAppOption("com.instagram.android", "Instagram", "Social", true))
    override fun today() = day
    override fun wallNow() = wall
    override fun mono() = mono
}

class MemDraftStore(var value: String? = null) : DraftStore {
    override fun read() = value
    override fun write(value: String) { this.value = value }
    override fun clear() { value = null }
}

class InitiationControllerTest {
    private val draftStore = MemDraftStore(); private val uiStore = MemDraftStore(); private val ports = FakeInitiationPorts()
    private fun controller() = InitiationController(OnboardingDraftRepository(draftStore), uiStore, ports)

    private suspend fun guestThroughFoundation(c: InitiationController) {
        c.takeCommand(); c.submitName("Asha"); c.continueAsGuest(); c.ready()
        c.saveAnchors(LocalTime.of(6, 30), LocalTime.of(22, 30))
        c.enableAlarm(); c.alarmContinue()
        c.toggleApp("com.instagram.android"); c.saveApps(); c.saveWindow(); c.accessContinue()
    }

    @Test fun guestFlowEndToEndUsesRealPlan() = runTest {
        val c = controller()
        guestThroughFoundation(c)
        assertEquals(InitiationScreen.FOUNDATION, c.ui.screen)
        assertEquals(LocalTime.of(6, 35), c.ui.practiceTime)
        assertEquals(LocalTime.of(6, 30), ports.plan.wakeTime)
        assertEquals(setOf("com.instagram.android"), ports.plan.protectedPackages)
        assertEquals((1..5).toSet(), ports.plan.windows.single().startDays)
        c.enter()
        assertEquals(InitiationScreen.ROUTINE, c.ui.screen)
        assertEquals(InitiationCheckpoint.COMPLETE, c.draft.checkpoint)
        assertEquals(IdentityMode.GUEST, c.draft.identityMode)
        val v = c.orbitView(); assertTrue(v.ring && v.alarmPoint && v.boundaryPoint); assertFalse(v.practicePoint)
    }

    @Test fun googleAndEmailHonestlyUnavailableGuestStillWorks() = runTest {
        val c = controller(); c.takeCommand(); c.submitName("Asha")
        c.google()
        assertEquals(InitiationScreen.AUTH, c.ui.screen)
        assertTrue(c.ui.message!!.contains("not available yet"))
        c.openEmail(); c.requestEmailCode("a@b.co")
        assertTrue(c.ui.message!!.contains("not available yet"))
        c.continueAsGuest()
        assertEquals(InitiationScreen.PROPOSITION, c.ui.screen)
    }

    @Test fun validationMessagesMatchWireframe() = runTest {
        val c = controller(); c.takeCommand(); c.submitName("  ")
        assertEquals("Enter a name, or start without an account.", c.ui.message)
        c.continueAsGuest(); c.ready(); c.saveAnchors(LocalTime.of(7, 0), LocalTime.of(23, 0)); c.alarmContinue()
        c.saveApps(); assertEquals("Select an app or choose Not now.", c.ui.message)
        c.toggleApp("x"); c.saveApps(); c.editWindow(start = LocalTime.of(9, 0), end = LocalTime.of(9, 0)); c.saveWindow()
        assertEquals("Choose distinct start and end times. Overnight windows are supported.", c.ui.message)
        c.begin(false)
        assertEquals("Confirm you are stationary and in a suitable place. You can return to Routine instead.", c.ui.message)
    }

    @Test fun resumesAtPersistedCheckpointAfterProcessDeath() = runTest {
        val c = controller(); c.takeCommand(); c.submitName("Asha"); c.continueAsGuest(); c.ready()
        c.saveAnchors(LocalTime.of(7, 0), LocalTime.of(23, 0))
        val again = controller()
        assertEquals(InitiationScreen.ALARM, again.ui.screen)
        assertEquals(LocalTime.of(7, 5), again.ui.practiceTime)
        assertTrue(again.orbitView().ring)
    }

    @Test fun skipPathsLeaveToolsVisiblyInactive() = runTest {
        val c = controller(); c.takeCommand(); c.continueAsGuest(); c.ready()
        c.saveAnchors(LocalTime.of(7, 0), LocalTime.of(23, 0)); c.skipAlarm(); c.skipProtection()
        assertEquals(InitiationScreen.FOUNDATION, c.ui.screen)
        assertEquals("Using your own alarm", c.alarmLabel()); assertEquals("Not configured", c.boundaryLabel())
        assertFalse(c.orbitView().alarmPoint)
    }

    @Test fun alarmPermissionAndFailureStatesAreHonest() = runTest {
        val c = controller(); c.takeCommand(); c.continueAsGuest(); c.ready(); c.saveAnchors(LocalTime.of(7, 0), LocalTime.of(23, 0))
        ports.alarmResult = InitiationAlarm.PERMISSION_REQUIRED; c.enableAlarm()
        assertEquals("Saved · device access needed", c.alarmLabel()); assertEquals(InitiationScreen.ALARM, c.ui.screen)
        ports.alarmResult = InitiationAlarm.FAILED; c.enableAlarm(); assertEquals("Saved · scheduling failed", c.alarmLabel())
    }

    @Test fun boundaryLabelFollowsDeviceTruthAndLocalOffStops() = runTest {
        val c = controller(); guestThroughFoundation(c); c.enter()
        ports.protection = FocusRuleState.NEEDS_DEVICE_ACCESS; assertEquals("Saved · device access needed", c.boundaryLabel())
        ports.protection = FocusRuleState.ACTIVE; assertEquals("Active", c.boundaryLabel())
        c.toggleProtection()
        assertEquals(1, ports.stopped); assertEquals("Off · local exit available", c.boundaryLabel())
        assertFalse(ports.plan.windows.single().enabled)
    }

    @Test fun editChangedWakeInvalidatesAlarmReadinessAndAddsActivity() = runTest {
        val c = controller(); guestThroughFoundation(c); c.enter()
        c.saveEdit(LocalTime.of(6, 0), LocalTime.of(6, 5), LocalTime.of(22, 30), "Read", LocalTime.of(21, 0), 30, "Every day")
        assertEquals(InitiationAlarm.PERMISSION_REQUIRED, c.ui.alarm)
        assertEquals("Read", ports.plan.activities.single().title)
        c.saveEdit(LocalTime.of(6, 0), LocalTime.of(6, 5), LocalTime.of(22, 30), "Walk", LocalTime.of(7, 0), 0, "Every day")
        assertEquals("Choose an activity time and a duration from 1 to 1440 minutes.", c.ui.message)
    }

    @Test fun practiceCompletesOnlyWithSixtyForegroundSeconds() = runTest {
        val c = controller(); guestThroughFoundation(c); c.enter()
        c.go(InitiationScreen.SADHANA); c.begin(true)
        assertEquals(InitiationScreen.PRACTICE, c.ui.screen); assertTrue(c.orbitView().practicePoint)
        ports.mono = 30_000; assertEquals(30, c.tick())
        c.pausePractice(); ports.mono = 900_000; assertEquals(30, c.tick()) // paused/background time not counted
        c.togglePause(); ports.mono = 930_000; c.tick()
        assertEquals(InitiationScreen.RETURNED, c.ui.screen); assertTrue(c.lastCompleted)
    }

    @Test fun earlyEndIsNotCompletion() = runTest {
        val c = controller(); guestThroughFoundation(c); c.enter(); c.begin(true)
        ports.mono = 10_000; c.endPractice()
        assertEquals(InitiationScreen.RETURNED, c.ui.screen); assertFalse(c.lastCompleted)
        assertEquals(AttemptOutcome.ENDED, c.practice.attempt!!.outcome)
    }

    @Test fun killedDuringPracticeResumesOnEntryNotRunning() = runTest {
        val c = controller(); guestThroughFoundation(c); c.enter(); c.begin(true); ports.mono = 5_000; c.pausePractice()
        val again = controller()
        assertEquals(InitiationScreen.SADHANA, again.ui.screen); assertFalse(again.practice.isRunning)
    }

    @Test fun orbitGlowAfterSecondDistinctDayOrIndependentRecord() = runTest {
        val c = controller(); guestThroughFoundation(c); c.enter()
        assertFalse(c.orbitView().secondPresence)
        ports.day = ports.day.plusDays(3)
        val later = controller() // real app open on a later day
        assertTrue(later.orbitView().secondPresence)
        val c2 = InitiationController(OnboardingDraftRepository(MemDraftStore()), MemDraftStore(), FakeInitiationPorts())
        c2.independent(); assertTrue(c2.orbitView().secondPresence)
    }

    @Test fun weekDayFromFirstAnchorNotAReviewSelector() = runTest {
        val c = controller(); guestThroughFoundation(c)
        assertEquals(1, c.weekDay()); ports.day = ports.day.plusDays(2); assertEquals(3, c.weekDay())
        ports.day = ports.day.plusDays(30); assertEquals(7, c.weekDay())
    }

    @Test fun corruptUiStateStartsCleanly() {
        uiStore.value = "garbage=1"
        assertEquals(InitiationScreen.ARRIVAL, controller().ui.screen)
        assertNull(InitiationUiCodec.decode("v=2"))
    }

    @Test fun guidanceIsTextOnlyByRemainingSeconds() {
        assertEquals("Notice the breath already happening.", InitiationController.guidance(60))
        assertEquals("One more return.", InitiationController.guidance(10))
    }
}
