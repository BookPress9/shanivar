package com.shanivar.nativeapp

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ShanivarApiTest {
    @Test fun loginUsesExistingContractAndFallbackNotesStayGeneral() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"token":"test-session","user":{"username":"Observer"}}"""))
            server.enqueue(MockResponse().setBody("""{"id":"user-1","display_name":"Observer","streak_days":8}"""))
            server.enqueue(MockResponse().setBody("""{"insight":{"id":"00000000-0000-0000-0000-000000000000","insight_text":"Pause.","action_text":"Make space.","reflection_text":"What matters?","insight_date":"2026-09-09"}}"""))
            val api = ShanivarApi(server.url("/api").toString())
            assertEquals(8, api.login("observer@example.com", "test-password").streak)
            assertTrue(api.today().general)
            val login = server.takeRequest()
            assertEquals("/api/auth/login", login.path)
            assertEquals("POST", login.method)
            val body = JSONObject(login.body.readUtf8())
            assertEquals("observer@example.com", body.getString("email"))
            assertEquals("test-password", body.getString("password"))
            assertNull(login.getHeader("Authorization"))
            assertEquals("Bearer test-session", server.takeRequest().getHeader("Authorization"))
            assertEquals("Bearer test-session", server.takeRequest().getHeader("Authorization"))
            api.logout()
        }
    }

    @Test fun failedProfileReadClearsThePartiallyCreatedSession() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"token":"partial-session","user":{}}"""))
            server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"Profile unavailable"}"""))
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Invalid credentials"}"""))
            val api = ShanivarApi(server.url("/api").toString())
            val first = runCatching { api.login("observer@example.com", "password") }.exceptionOrNull()
            assertTrue(first is ApiFailure)
            assertEquals("Profile unavailable", first?.message)
            runCatching { api.login("observer@example.com", "wrong-password") }
            server.takeRequest()
            assertEquals("Bearer partial-session", server.takeRequest().getHeader("Authorization"))
            assertNull(server.takeRequest().getHeader("Authorization"))
        }
    }
}
