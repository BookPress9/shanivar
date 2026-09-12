package com.shanivar.nativeapp

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi", manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class OnboardingFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun accountCreationAdvancesOnlyAfterBackendSuccess() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"token":"new-session"}"""))
            server.enqueue(MockResponse().setBody("""{"id":"new-user","display_name":"Asha","streak_days":0}"""))
            val api = ShanivarApi(server.url("/api").toString())
            var connected: Account? = null
            compose.setContent { ShanivarTheme { Surface { OnboardingFlow(api, null, "en", {}, { connected = it }, {}) } } }

            compose.onNodeWithText("English").performClick()
            compose.onNodeWithText("Create my account").performScrollTo().performClick()
            compose.onNodeWithText("Name").performTextInput("Asha")
            compose.onNodeWithText("Email").performTextInput("asha@example.com")
            compose.onNodeWithText("Password").performTextInput("strong-password")
            compose.onNodeWithText("Create account").performScrollTo().performClick()
            compose.waitUntil(10_000) { connected != null }
            compose.onNodeWithText("A little more context.").assertExists()

            val register = server.takeRequest()
            assertEquals("/api/auth/register", register.path)
            val body = JSONObject(register.body.readUtf8())
            assertEquals("Asha", body.getString("display_name"))
            assertEquals("en", body.getString("preferred_language"))
            assertFalse(body.has("phone"))
            assertEquals("Bearer new-session", server.takeRequest().getHeader("Authorization"))
        }
    }

    @Test fun hindiGuestPathKeepsTheSameSetupOrder() {
        val api = ShanivarApi("http://127.0.0.1:1/api")
        compose.setContent { ShanivarTheme { Surface { OnboardingFlow(api, null, "en", {}, {}, {}) } } }
        compose.onNodeWithText("हिंदी").performClick()
        compose.onNodeWithText("पहले देखें").performScrollTo().performClick()
        compose.onNodeWithText("थोड़ा और संदर्भ।").assertExists()
        compose.onNodeWithText("अभी छोड़ें").performScrollTo().performClick()
        compose.onNodeWithText("नियंत्रण आपके हाथ में।").assertExists()
    }
}
