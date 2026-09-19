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

/**
 * MVP streamline task (2026-09-18): the flow is now LANGUAGE -> WELCOME -> PLAN -> ACCOUNT ->
 * ARRIVAL (HELPER_GUIDE branches off WELCOME but isn't exercised by these two paths). No birth
 * intake, no capabilities tour - see OnboardingFlow.kt's own header comment.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi", manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class OnboardingFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun accountCreationAdvancesOnlyAfterBackendSuccess() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"token":"new-session"}"""))
            server.enqueue(MockResponse().setBody("""{"id":"new-user","display_name":"Asha","streak_days":0}"""))
            val api = ShanivarApi(server.url("/api").toString())
            var connected: Account? = null
            // Exercise the production form without a second Android window: Robolectric's
            // legacy Windows renderer cannot idle a platform Dialog window.
            compose.setContent { ShanivarTheme { AccountDialog(null, api, {}, { connected = it }, {}, inDialog = false) } }

            compose.onNodeWithText("Create an account").performClick()
            compose.onNodeWithText("Name").performTextInput("Asha")
            compose.onNodeWithText("Email").performTextInput("asha@example.com")
            compose.onNodeWithText("Password").performTextInput("strong-password")
            compose.onNodeWithText("Create account").performScrollTo().performClick()
            compose.waitUntil(10_000) { connected != null }
            assertEquals("Asha", connected?.name)

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
        var complete = false
        compose.setContent { ShanivarTheme { Surface { OnboardingFlow(api, null, "en", {}, {}, {}, { complete = true }) } } }
        compose.onNodeWithText("हिंदी").performClick()
        compose.onNodeWithText("यह मेरे लिए है").performClick()
        assertTrue(complete)
    }

    @Test fun oneMinutePauseIsReachableWithoutSigningInOrAccount() {
        val api = ShanivarApi("http://127.0.0.1:1/api")
        compose.setContent { ShanivarTheme { Surface { OnboardingFlow(api, null, "en", {}, {}, {}, {}) } } }
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("Try a one-minute pause first").performClick()
        compose.onNodeWithText("This pause does not block other apps.").assertExists()
        compose.onNodeWithText("Not now").performClick()
        // Exiting the preview returns to the same onboarding page, not forward into the flow.
        compose.onNodeWithText("Make time for what matters.").assertExists()
    }
}
