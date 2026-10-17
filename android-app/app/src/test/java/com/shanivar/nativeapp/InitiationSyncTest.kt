package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.GuestResult
import org.junit.Assert.assertEquals
import org.junit.Test

/** DEV-47 AC2: leaving Proposition as a guest calls ensureGuest exactly once and never blocks. */
class InitiationSyncTest {
    private val engine = FakeSyncEngine()
    private val sync = enabledSync(engine)
    private fun controller() = InitiationController(OnboardingDraftRepository(MemDraftStore()), MemDraftStore(), FakeInitiationPorts(), onGuestReady = sync::onGuestChosen)

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
