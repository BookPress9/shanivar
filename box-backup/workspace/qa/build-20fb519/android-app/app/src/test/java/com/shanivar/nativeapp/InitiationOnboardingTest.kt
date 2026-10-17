package com.shanivar.nativeapp

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InitiationOnboardingTest {
    @get:Rule val tmp = TemporaryFolder()

    private class MemStore(var value: String? = null) : DraftStore {
        override fun read() = value
        override fun write(value: String) { this.value = value }
        override fun clear() { value = null }
    }

    private fun throughIdentity(repo: OnboardingDraftRepository) {
        repo.setDisplayName("  Asha ")
        InitiationAuthCoordinator(repo).continueAsGuest()
    }

    @Test fun freshDraftStartsAtArrival() {
        assertEquals(InitiationCheckpoint.ARRIVAL, OnboardingDraftRepository(MemStore()).draft.checkpoint)
    }

    @Test fun emptyNameBlockedAndNameTrimmed() {
        val repo = OnboardingDraftRepository(MemStore())
        assertTrue(repo.setDisplayName("   ") is DraftResult.Blocked)
        repo.setDisplayName("  Asha ")
        assertEquals("Asha", repo.draft.displayName)
        assertEquals(InitiationCheckpoint.AUTHENTICATION, repo.draft.checkpoint)
    }

    @Test fun interruptionAtEveryCheckpointResumesThere() {
        val store = MemStore()
        val repo = OnboardingDraftRepository(store)
        throughIdentity(repo)
        var cp = InitiationCheckpoint.PROPOSITION
        while (cp < InitiationCheckpoint.FOUNDATION) {
            assertEquals(cp, OnboardingDraftRepository(store).draft.checkpoint) // simulated process death
            repo.advanceFrom(cp)
            cp = InitiationCheckpoint.entries[cp.ordinal + 1]
        }
        repo.complete(1234L)
        val resumed = OnboardingDraftRepository(store).draft
        assertEquals(InitiationCheckpoint.COMPLETE, resumed.checkpoint)
        assertEquals(1234L, resumed.completedAt)
        assertEquals("Asha", resumed.displayName)
        assertEquals(IdentityMode.GUEST, resumed.identityMode)
    }

    @Test fun cannotSkipIdentity() {
        val repo = OnboardingDraftRepository(MemStore())
        assertTrue(repo.advanceFrom(InitiationCheckpoint.PROPOSITION) is DraftResult.Blocked)
        assertTrue(repo.complete(1) is DraftResult.Blocked)
    }

    @Test fun goingBackNeverLosesProgress() {
        val repo = OnboardingDraftRepository(MemStore())
        throughIdentity(repo)
        repo.advanceFrom(InitiationCheckpoint.PROPOSITION)
        repo.advanceFrom(InitiationCheckpoint.ANCHORS)
        repo.advanceFrom(InitiationCheckpoint.PROPOSITION) // re-visit earlier screen
        assertEquals(InitiationCheckpoint.ALARM, repo.draft.checkpoint)
    }

    @Test fun corruptOrUnknownVersionStartsFreshWithoutCrash() {
        assertEquals(InitiationCheckpoint.ARRIVAL, OnboardingDraftRepository(MemStore("%%garbage")).draft.checkpoint)
        val future = OnboardingDraftCodec.encode(OnboardingDraft(schemaVersion = 99, checkpoint = InitiationCheckpoint.APPS))
        assertEquals(InitiationCheckpoint.ARRIVAL, OnboardingDraftRepository(MemStore(future)).draft.checkpoint)
        assertNull(OnboardingDraftCodec.decode("schemaVersion=1\ncheckpoint=NOPE"))
    }

    @Test fun fileStorePersistsAtomically() {
        val f = tmp.newFolder().resolve("onboarding/draft.properties")
        val repo = OnboardingDraftRepository(FileDraftStore(f))
        throughIdentity(repo)
        assertEquals(InitiationCheckpoint.PROPOSITION, OnboardingDraftRepository(FileDraftStore(f)).draft.checkpoint)
        assertTrue(!f.resolveSibling("draft.properties.tmp").exists())
        repo.reset()
        assertEquals(InitiationCheckpoint.ARRIVAL, OnboardingDraftRepository(FileDraftStore(f)).draft.checkpoint)
    }

    private class FakeGoogle(val outcome: AuthOutcome) : GoogleAuthAdapter { override suspend fun signIn() = outcome }
    private class FakeEmail(val outcome: AuthOutcome) : EmailCodeAuthAdapter {
        override suspend fun requestCode(email: String) = Result.success(EmailChallenge("c1", 0, 0))
        override suspend fun verify(challengeId: String, code: String) = outcome
    }

    private fun repoAtAuth(store: MemStore = MemStore()) = OnboardingDraftRepository(store).also { it.setDisplayName("Asha") }

    @Test fun authFailuresKeepDraftOnAuthentication() = runTest {
        for (o in listOf(AuthOutcome.Cancelled, AuthOutcome.Offline, AuthOutcome.Error("down"))) {
            val store = MemStore(); val repo = repoAtAuth(store)
            val r = InitiationAuthCoordinator(repo, google = FakeGoogle(o)).signInWithGoogle()
            assertTrue("$o", r is DraftResult.Blocked)
            val after = OnboardingDraftRepository(store).draft
            assertEquals(InitiationCheckpoint.AUTHENTICATION, after.checkpoint)
            assertEquals("Asha", after.displayName)
            assertEquals(IdentityMode.NONE, after.identityMode)
        }
    }

    @Test fun emailCodeExpiredInvalidAndMalformed() = runTest {
        val repo = repoAtAuth()
        assertTrue(InitiationAuthCoordinator(repo, email = FakeEmail(AuthOutcome.CodeExpired)).verifyEmailCode("c1", "123456") is DraftResult.Blocked)
        assertTrue(InitiationAuthCoordinator(repo, email = FakeEmail(AuthOutcome.CodeInvalid)).verifyEmailCode("c1", "123456") is DraftResult.Blocked)
        assertTrue(InitiationAuthCoordinator(repo, email = FakeEmail(AuthOutcome.Success(AuthSession("a", "t")))).verifyEmailCode("c1", "12ab") is DraftResult.Blocked)
        assertEquals(InitiationCheckpoint.AUTHENTICATION, repo.draft.checkpoint)
    }

    @Test fun googleAndEmailSuccessAdvance() = runTest {
        val g = repoAtAuth()
        InitiationAuthCoordinator(g, google = FakeGoogle(AuthOutcome.Success(AuthSession("acc-1", "t")))).signInWithGoogle()
        assertEquals(IdentityMode.GOOGLE, g.draft.identityMode); assertEquals("acc-1", g.draft.accountId)
        assertEquals(InitiationCheckpoint.PROPOSITION, g.draft.checkpoint)
        val e = repoAtAuth()
        InitiationAuthCoordinator(e, email = FakeEmail(AuthOutcome.Success(AuthSession("acc-2", "t")))).verifyEmailCode("c1", "123456")
        assertEquals(IdentityMode.EMAIL, e.draft.identityMode)
    }

    @Test fun unconfiguredProvidersFailSafelyAndGuestStillWorks() = runTest {
        val repo = repoAtAuth()
        val c = InitiationAuthCoordinator(repo)
        assertTrue(c.signInWithGoogle() is DraftResult.Blocked)
        assertTrue(c.continueAsGuest() is DraftResult.Ok)
        assertEquals(IdentityMode.GUEST, repo.draft.identityMode)
    }

    private class FakeSink(val failOn: String? = null) : MigrationSink {
        val remote = mutableMapOf<String, LocalRecord>()
        override suspend fun existingClientIds(accountId: String) = remote.keys.toSet()
        override suspend fun upsert(accountId: String, record: LocalRecord) {
            if (record.clientId == failOn) error("network")
            remote[record.clientId] = record
        }
    }

    @Test fun guestMigrationIsIdempotentAndNeverDuplicates() = runTest {
        val recs = listOf(LocalRecord("r1", "routine", "{}"), LocalRecord("w1", "window", "{}"), LocalRecord("w1", "window", "{}"))
        val sink = FakeSink(failOn = "w1")
        val first = GuestMigrator(sink).migrate("acc", recs)
        assertEquals(MigrationReport(1, 0, listOf("w1")), first)
        val sink2 = FakeSink().apply { remote.putAll(sink.remote) }
        val second = GuestMigrator(sink2).migrate("acc", recs) // resume after interruption
        assertEquals(MigrationReport(1, 1, emptyList()), second)
        assertEquals(2, sink2.remote.size)
    }
}
