package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.GuestResult
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test

/** DEV-45 AC1: guest once per install, encrypted tokens that survive restarts, refresh, never a second guest. */
class SyncSessionTest {
    @Test fun guestSignsUpOncePerInstallAndSurvivesRestart() = MockWebServer().use { server ->
        server.enqueue(authReply())
        val rig = SyncRig(server, signedInAs = null)
        assertEquals(GuestResult.Ok(UID_A), rig.session.ensureGuestBlocking())
        server.takeRequest().let {
            assertEquals("/auth/v1/signup", it.path); assertEquals("{}", it.body.readUtf8())
            assertEquals("anon", it.getHeader("apikey")); assertEquals("Bearer anon", it.getHeader("Authorization"))
        }
        assertEquals(GuestResult.Ok(UID_A), rig.session.ensureGuestBlocking())
        assertEquals(GuestResult.Ok(UID_A), runBlocking { rig.session.ensureGuest() })
        // restart: a new session object over the same encrypted file, no network
        val restarted = SupabaseSession(rig.http, EncryptedSessionStore(rig.sessionStore, XorCipher()))
        assertEquals(UID_A, restarted.userId)
        assertEquals(GuestResult.Ok(UID_A), restarted.ensureGuestBlocking())
        assertEquals(TokenResult.Valid("acc-1"), restarted.accessToken())
        assertEquals("one sign-up only", 1, server.requestCount)
    }

    @Test fun tokensAreStoredEncrypted() = MockWebServer().use { server ->
        server.enqueue(authReply(access = "secret-access-xyz", refresh = "secret-refresh-xyz"))
        val rig = SyncRig(server, signedInAs = null)
        rig.session.ensureGuestBlocking()
        val atRest = rig.sessionStore.value!!
        assertFalse(atRest.contains("secret-access-xyz")); assertFalse(atRest.contains("secret-refresh-xyz")); assertFalse(atRest.contains(UID_A))
        assertEquals("secret-refresh-xyz", rig.encrypted.load()!!.refreshToken)
    }

    @Test fun refreshesBeforeExpiryAndStoresRotatedTokens() = MockWebServer().use { server ->
        val clock = FakeNow()
        server.enqueue(authReply(access = "acc-2", refresh = "ref-2", expiresAtSec = clock.ms / 1000 + 3600))
        val rig = SyncRig(server.url("/").toString(), signedInAs = UID_A, clock = clock, accessExpiresAtMs = clock.ms + 30_000)
        assertEquals(TokenResult.Valid("acc-2"), rig.session.accessToken())
        server.takeRequest().let {
            assertEquals("/auth/v1/token?grant_type=refresh_token", it.path)
            assertEquals("""{"refresh_token":"ref-1"}""", it.body.readUtf8())
        }
        assertEquals("ref-2", rig.encrypted.load()!!.refreshToken)
        assertEquals(TokenResult.Valid("acc-2"), rig.session.accessToken())
        assertEquals("no second refresh while valid", 1, server.requestCount)
    }

    @Test fun refreshAfter401IsOncePerToken() = MockWebServer().use { server ->
        server.enqueue(authReply(access = "acc-2", refresh = "ref-2"))
        val rig = SyncRig(server)
        assertEquals(TokenResult.Valid("acc-2"), rig.session.refreshAfter401("acc-1"))
        assertEquals("another caller already refreshed", TokenResult.Valid("acc-2"), rig.session.refreshAfter401("acc-1"))
        assertEquals(1, server.requestCount)
    }

    @Test fun revokedRefreshTokenNeverCreatesANewGuest() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"code":400,"error_code":"refresh_token_not_found"}"""))
        val rig = SyncRig(server.url("/").toString(), signedInAs = UID_A, accessExpiresAtMs = 0)
        assertEquals(TokenResult.Lost, rig.session.accessToken())
        assertNull(rig.session.userId); assertTrue(rig.session.sessionLost); assertTrue(rig.session.hasRefreshToken)
        assertEquals(GuestResult.Failed("session_lost"), rig.session.ensureGuestBlocking())
        val restarted = SupabaseSession(rig.http, rig.encrypted)
        assertEquals(GuestResult.Failed("session_lost"), restarted.ensureGuestBlocking())
        assertEquals("only the refresh call, never /signup", 1, server.requestCount)
        assertEquals("/auth/v1/token?grant_type=refresh_token", server.takeRequest().path)
    }

    @Test fun offlineSignUpStoresNothingAndRetriesLater() {
        val store = MemDraftStore()
        val dead = MockWebServer().apply { start() }; val deadUrl = dead.url("/").toString(); dead.shutdown()
        val offline = SyncRig(deadUrl, signedInAs = null, sessionStore = store)
        assertEquals("no network", GuestResult.Offline, offline.session.ensureGuestBlocking())
        assertNull(store.value)
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503)); server.enqueue(authReply())
            val rig = SyncRig(server.url("/").toString(), signedInAs = null, sessionStore = store)
            assertEquals("server busy", GuestResult.Offline, rig.session.ensureGuestBlocking())
            assertNull(store.value)
            assertEquals(GuestResult.Ok(UID_A), rig.session.ensureGuestBlocking())
            assertEquals(2, server.requestCount)
        }
    }

    @Test fun disabledAndRejected() = MockWebServer().use { server ->
        val off = SupabaseSession(SupabaseHttp(server.url("/").toString(), "anon"), EncryptedSessionStore(MemDraftStore(), XorCipher()), enabled = false)
        assertEquals(GuestResult.Disabled, off.ensureGuestBlocking())
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error_code":"anonymous_provider_disabled"}"""))
        assertEquals(GuestResult.Failed("http_422"), SyncRig(server, signedInAs = null).session.ensureGuestBlocking())
        assertEquals(1, server.requestCount)
    }

    @Test fun signOutClearsTokensAndNotifies() = MockWebServer().use { server ->
        val rig = SyncRig(server)
        var signedOut = 0; rig.session.onSignedOut = { signedOut++ }
        runBlocking { rig.session.signOut() }
        assertNull(rig.session.userId); assertNull(rig.sessionStore.value); assertFalse(rig.session.hasRefreshToken)
        assertEquals(1, signedOut)
        assertEquals(TokenResult.NoSession, rig.session.accessToken())
    }
}
