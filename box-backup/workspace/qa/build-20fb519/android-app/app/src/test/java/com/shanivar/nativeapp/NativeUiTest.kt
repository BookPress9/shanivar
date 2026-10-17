package com.shanivar.nativeapp

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.Assert.*

/**
 * MVP streamline task (2026-09-18): rewritten for the single-Home surface
 * (`BuildConfig.STREAMLINED_LAUNCH = true` by default - see build.gradle.kts). The four-tab
 * `LegacyTabShell` this file previously exercised is still present but compiled out of the default
 * build, so its own reachability is no longer covered by an automated test here; see the delivery
 * report's known-gaps note.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class NativeUiTest {
    @get:org.junit.Rule val outboxReset = AppSyncOutboxReset()
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun resetLocalData() {
        context.getSharedPreferences("rhythm", 0).edit().clear().commit()
        context.getSharedPreferences("reflections", 0).edit().clear().commit()
    }

    private fun completeGuestOnboarding() {
        compose.setContent { ShanivarApp(initiation = false) }
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("This is for me").performClick()
    }

    private fun openReturningUser() {
        context.getSharedPreferences("rhythm", 0).edit().putBoolean("introduced", true).commit()
        compose.setContent { ShanivarApp(initiation = false) }
    }

    @Test fun onboardingReachesHomeWithBothToolsAndNoPermissionWall() {
        completeGuestOnboarding()
        compose.onNodeWithText("Start focus").assertExists()
        compose.onNodeWithText("Set alarm").assertExists()
        compose.onNodeWithText("Reading", substring = true).assertExists() // default intention preset
    }

    @Test fun focusCardNeverClaimsUnconnectedProtection() {
        openReturningUser()
        compose.onNodeWithText("Start focus").performClick()
        compose.onNodeWithText("Sign in to start protection", substring = true).assertExists()
        compose.onNodeWithText("Focus protected", substring = true).assertDoesNotExist()
    }

    @Test fun alarmCardNeverClaimsUnconnectedScheduling() {
        openReturningUser()
        compose.onNodeWithText("Set alarm").performClick()
        compose.onNodeWithText("Sign in to set a wake-up alarm.", substring = true).assertExists()
        compose.onNodeWithText("Scheduled on this device", substring = true).assertDoesNotExist()
    }

    @Test fun focusCardPresetChoiceUpdatesIntentionAndPersistsLocally() {
        openReturningUser()
        compose.onNodeWithText("Start focus").performClick()
        compose.onNodeWithText("Family time", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("← Home").performScrollTo().performClick()
        assertEquals("Family time", context.getSharedPreferences("rhythm", 0).getString("intentionLabel", null))
    }
}
