package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.GuestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DEV-47 AC2: leaving Proposition as a guest calls ensureGuest exactly once and never blocks. */
class InitiationSyncTest {
    private val engine = FakeSyncEngine()
    private val sync = enabledSync(engine)
    private fun controller() = InitiationController(OnboardingDraftRepository(MemDraftStore()), MemDraftStore(), FakeInitiationPorts(),
        onGuestReady = sync::onGuestChosen, onGuestComplete = sync::onInitiationCompleteAsGuest)

    private suspend fun throughFoundation(c: InitiationController) {
        c.takeCommand(); c.submitName("Asha"); c.continueAsGuest(); c.ready()
        c.saveAnchors(java.time.LocalTime.of(6, 30), java.time.LocalTime.of(22, 30))
        c.enableAlarm(); c.alarmContinue()
        c.toggleApp("com.instagram.android"); c.saveApps(); c.saveWindow(); c.accessContinue()
    }

    @Test fun completingAfterASuccessfulGuestStepDoesNotSignInAgain() = kotlinx.coroutines.test.runTest {
        val c = controller(); throughFoundation(c)
        assertEquals(1, engine.session.ensureGuestCalls)
        c.enter()
        assertEquals(InitiationCheckpoint.COMPLETE, c.draft.checkpoint)
        assertEquals(1, engine.session.ensureGuestCalls)
    }

    @Test fun completingAfterAnOfflineGuestStepTriesOnceMore() = kotlinx.coroutines.test.runTest {
        engine.session.result = GuestResult.Offline
        val c = controller(); throughFoundation(c)
        assertEquals(1, engine.session.ensureGuestCalls)
        engine.session.result = GuestResult.Ok("g1")
        c.enter()
        assertEquals(InitiationScreen.ROUTINE, c.ui.screen)
        assertEquals(2, engine.session.ensureGuestCalls); assertTrue(sync.hasSession)
    }

    @Test fun completionHookFiresOnceWhenInitiationCompletes() = kotlinx.coroutines.test.runTest {
        var calls = 0
        val c = InitiationController(OnboardingDraftRepository(MemDraftStore()), MemDraftStore(), FakeInitiationPorts(), onGuestComplete = { calls++ })
        c.enter() // Foundation not reached: completion blocked, no hook
        assertEquals(0, calls)
        throughFoundation(c); c.enter()
        assertEquals(1, calls)
    }

    @Test fun guestStepCallsEnsureGuestExactlyOnce() {
        val c = controller()
        c.takeCommand(); c.continueAsGuest("Asha")
        assertEquals(InitiationScreen.PROPOSITION, c.ui.screen)
        assertEquals(0, engine.session.ensureGuestCalls) // not before leaving Proposition
        c.ready()
        assertEquals(InitiationScreen.ANCHORS, c.ui.screen)
        assertEquals(1, engine.session.ensureGuestCalls)
        c.back(); c.ready() // back and forward again: session exists, no second sign-in
        assertEquals(1, engine.session.ensureGuestCalls)
    }

    @Test fun offlineGuestSignInNeverBlocksTheFlow() {
        engine.session.result = GuestResult.Offline
        val c = controller()
        c.takeCommand(); c.continueAsGuest(); c.ready()
        assertEquals(InitiationScreen.ANCHORS, c.ui.screen)
        assertEquals(1, engine.session.ensureGuestCalls)
    }

    @Test fun throwingHookNeverBlocksTheFlow() {
        val c = InitiationController(OnboardingDraftRepository(MemDraftStore()), MemDraftStore(), FakeInitiationPorts(), onGuestReady = { error("boom") })
        c.takeCommand(); c.continueAsGuest(); c.ready()
        assertEquals(InitiationScreen.ANCHORS, c.ui.screen)
    }
}
