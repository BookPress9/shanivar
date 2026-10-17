package com.shanivar.nativeapp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** B8: End must be visible without scrolling at 200% font and in landscape; navigation stays present. */
abstract class SadhanaA11yBase {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val c = SadhanaController(MemDraftStore(), FakeSadhanaClock(), allowUnreviewed = true)
    private fun click(t: String) = compose.onNodeWithText(t).performScrollTo().performClick()

    fun endReachableAndNavPresent() {
        compose.setContent { SadhanaApp(controllerFactory = { c }, reduceMotionOverride = true, hydrateDispatcher = kotlinx.coroutines.Dispatchers.Unconfined) }
        compose.onNodeWithText("◌ Sadhana").assertExists()
        click("Start →")
        compose.onAllNodesWithText("Prepare · 1 min →").fetchSemanticsNodes().takeIf { it.isNotEmpty() }?.let { click("Prepare · 1 min →") }
        click("Begin →")
        click("I am stationary and in a suitable place.")
        click("Begin →")
        compose.onNodeWithContentDescription("End practice").assertIsDisplayed().performClick()
        compose.onNodeWithText("◌ Sadhana").assertExists()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SadhanaLargeFontTest : SadhanaA11yBase() {
    @Test fun at200PercentFont() { RuntimeEnvironment.setFontScale(2f); endReachableAndNavPresent() }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SadhanaLandscapeTest : SadhanaA11yBase() {
    @Test fun inLandscape() = endReachableAndNavPresent()
}
