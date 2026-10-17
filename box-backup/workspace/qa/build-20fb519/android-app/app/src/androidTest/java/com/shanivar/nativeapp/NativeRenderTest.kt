package com.shanivar.nativeapp

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Run on Android to inspect real rendered output; JVM interaction checks do not replace this gate. */
@RunWith(AndroidJUnit4::class)
class NativeRenderTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun prepareAccountlessDevice() {
        context.getSharedPreferences("rhythm", 0).edit().clear().putBoolean("introduced", true).commit()
        if (compose.onAllNodesWithText("English").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("English").performClick()
            compose.onNodeWithText("Explore first").performScrollTo().performClick()
            compose.onNodeWithText("Skip for now").performScrollTo().performClick()
            compose.onNodeWithText("I understand").performScrollTo().performClick()
            compose.onNodeWithText("Build my rhythm").performScrollTo().performClick()
        }
        compose.onNodeWithText("Today", useUnmergedTree = true).performClick()
    }
    private fun capture(name: String) {
        val configured = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val directory = configured?.let { File(it) } ?: checkNotNull(context.getExternalFilesDir("ui-review"))
        directory.mkdirs()
        val output = File(directory, "$name.png")
        output.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun captureDailyFlow() {

        compose.onNodeWithText("Your daily note").assertExists()
        capture("today")
        compose.onNodeWithText("Focus", useUnmergedTree = true).performClick()
        capture("focus")
        compose.onNodeWithText("Practice", useUnmergedTree = true).performClick()
        compose.onAllNodesWithText("Open practice")[0].performScrollTo().performClick()
        compose.onNodeWithText("Begin quiet time").performScrollTo().performClick()
        compose.onNodeWithText("Pause practice").assertIsDisplayed()
        capture("practice")
    }
}
