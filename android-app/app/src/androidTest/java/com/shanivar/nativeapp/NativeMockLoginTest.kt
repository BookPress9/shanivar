package com.shanivar.nativeapp

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith

/** Opt in with -PshanivarApiUrl=http://10.0.2.2:5101/api and the local fixture server. */
@RunWith(AndroidJUnit4::class)
class NativeMockLoginTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun prepare() {
        Assume.assumeTrue(com.shanivar.nativeapp.BuildConfig.API_URL in listOf("http://10.0.2.2:5101/api", "http://10.0.2.2:5102/api"))
        ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("rhythm", 0)
            .edit().clear().putBoolean("introduced", true).commit()
        if (compose.onAllNodesWithText("Find my rhythm").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Find my rhythm").performScrollTo().performClick()
        }
        compose.onNodeWithText("Today", useUnmergedTree = true).performClick()
    }
    @Test fun rejectedLoginThenProfileAndNoteLoad() {
        compose.onNodeWithText("Sign in").performClick()
        compose.onNodeWithText("Email").performTextInput("observer@example.com")
        compose.onNodeWithText("Password").performTextInput("wrong")
        compose.onNode(hasText("Sign in") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Invalid credentials").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Password").performTextReplacement("local-practice")
        compose.onNode(hasText("Sign in") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Account").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Local mock note: make room for one quiet moment.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Reflect with Shanivar").performScrollTo().performClick()
        compose.onNodeWithText("Begin reflecting").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Prefer not to share").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Prefer not to share").performScrollTo().performClick()
        compose.onNodeWithText("Send response").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Continue \u00b7 or swipe up").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Continue \u00b7 or swipe up").performClick()
        compose.onNodeWithText("Back", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Account").performScrollTo().performClick()
        compose.onNodeWithText("Local Observer").assertExists()
        compose.onNode(hasText("8 day streak") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNodeWithText("Sign out").performClick()
        compose.onNodeWithText("Sign in").assertExists()
        compose.onNodeWithText("Local mock note: make room for one quiet moment.").assertDoesNotExist()
    }
}

