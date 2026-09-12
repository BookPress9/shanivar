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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class NativeUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun resetLocalData() {
        context.getSharedPreferences("rhythm", 0).edit().clear().commit()
        context.getSharedPreferences("reflections", 0).edit().clear().commit()
    }
    private fun openReturningUser() {
        context.getSharedPreferences("rhythm", 0).edit().putBoolean("introduced", true).commit()
        compose.setContent { ShanivarApp() }
    }
    @Test fun introductionLeadsToEditableRoutineWithoutPermissionWall() {
        compose.setContent { ShanivarApp() }
        compose.onNodeWithText("Find my rhythm").performScrollTo().performClick()
        compose.onNodeWithText("Rise with a reason").assertExists()
        compose.onNodeWithText("Complete a movement").performScrollTo().performClick()
        compose.onNodeWithText("Gentle").performScrollTo().performClick()
        compose.onNodeWithText("Save my rhythm").performScrollTo().performClick()
        compose.onNodeWithText("Rhythm saved on this device").assertExists()
        val prefs = context.getSharedPreferences("rhythm", 0)
        assertEquals("Complete a movement", prefs.getString("movement", null))
        assertEquals("Gentle", prefs.getString("tone", null))
        assertFalse(prefs.contains("alarmScheduled"))
    }
    @Test fun todayAndFocusNeverClaimUnconnectedProtection() {
        openReturningUser()
        compose.onNodeWithText("Your daily note").assertExists()
        compose.onNodeWithText("Focus", useUnmergedTree = true).performClick()
        compose.onNodeWithText("App lock is not connected in this build.").assertExists()
    }
    @Test fun practiceShowsSourceAndConfirmsEarlyExit() {
        openReturningUser()
        compose.onNodeWithText("Practice", useUnmergedTree = true).performClick()
        compose.onAllNodesWithText("Open practice")[0].performScrollTo().performClick()
        compose.onNodeWithText("Read source & adaptation").performScrollTo().performClick()
        compose.onNodeWithText("Translation cited:", substring = true).assertExists()
        compose.onNodeWithText("Begin quiet time").performScrollTo().performClick()
        compose.onNodeWithText("Find a position where your spine", substring = true).assertExists()
        compose.onNodeWithText("Back to practices").performScrollTo().performClick()
        compose.onNodeWithText("Leave this practice?").assertExists()
        compose.onNodeWithText("Stay").performClick()
        compose.onNodeWithText("Find a position where your spine", substring = true).assertExists()
        compose.onNodeWithText("Back to practices").performScrollTo().performClick()
        compose.onNodeWithText("End practice").performClick()
        compose.onNodeWithText("Room to be.").assertExists()
    }
}
