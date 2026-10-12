package com.shanivar.nativeapp

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InitiationUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val ports = FakeInitiationPorts()
    private lateinit var c: InitiationController

    private fun launch() {
        c = InitiationController(OnboardingDraftRepository(MemDraftStore()), MemDraftStore(), ports)
        compose.setContent { InitiationApp(controllerFactory = { c }, reduceMotionOverride = true) }
    }

    /** Optional review screenshots: set -Dinitiation.shots=<dir>. */
    private fun shot(name: String) {
        val dir = System.getProperty("initiation.shots") ?: return
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(dir).mkdirs(); File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun guestJourneyArrivalToRoutineAndPractice() {
        launch()
        compose.onNodeWithText("Discipline is freedom.").assertExists()
        shot("01-arrival")
        compose.onNodeWithText("Take command →").performClick()
        compose.onNodeWithText("Your name. Your beginning.").assertExists()
        compose.onNodeWithText("Start without an account").performScrollTo().performClick()
        compose.onNodeWithText("Not through motivation. Through what you choose to do, every day.").assertExists()
        shot("02-proposition")
        compose.onNodeWithText("I’m ready →").performScrollTo().performClick()
        compose.onNodeWithText("07:00").assertExists()
        shot("03-anchors")
        compose.onNodeWithText("Set my rhythm →").performScrollTo().performClick()
        compose.onNodeWithText("Make your chosen wake time your alarm?").assertExists()
        shot("04-alarm")
        compose.onNodeWithText("Enable wake alarm →").performScrollTo().performClick()
        compose.onNodeWithText("Continue →").performScrollTo().performClick()
        compose.onNodeWithText("Some things\ncan wait.").assertExists()
        compose.onNodeWithText("Instagram").performClick()
        shot("05-apps")
        compose.onNodeWithText("Continue →").performScrollTo().performClick()
        compose.onNodeWithText("Create this boundary →").performScrollTo().performClick()
        compose.onNodeWithText("Give your boundary\nthe tools it needs.").assertExists()
        shot("05-access")
        compose.onNodeWithText("Continue →").performScrollTo().performClick()
        compose.onNodeWithText("It begins here.").assertExists()
        shot("06-foundation")
        compose.onNodeWithText("Enter Shanivar →").performScrollTo().performClick()
        compose.onNodeWithText("◌ Sadhana").assertExists()
        shot("07-routine")
        compose.onNodeWithText("Begin Sadhana →").performScrollTo().performClick()
        compose.onNodeWithText("I’m stationary and in a suitable place.").performScrollTo().performClick()
        shot("08-sadhana")
        compose.onNodeWithText("Begin my minute →").performScrollTo().performClick()
        compose.onNodeWithText("Natural breath. No pacing. No forcing.").assertExists()
        compose.onNodeWithText("Pause").performScrollTo().performClick()
        shot("09-practice")
        compose.onNodeWithText("End now").performScrollTo().performClick()
        compose.onNodeWithText("You chose\nyour own ending.").assertExists()
        compose.onNodeWithText("See my Living Orbit").performScrollTo().performClick()
        compose.onNodeWithText("Continuity, not a streak.").assertExists()
        shot("10-orbit")
        assertEquals(AttemptOutcome.ENDED, c.practice.attempt!!.outcome)
    }

    @Test fun signInShowsNotAvailableYet() {
        launch()
        compose.onNodeWithText("Take command →").performClick()
        compose.onNodeWithText("Continue →").performClick()
        compose.onNodeWithText("Enter a name, or start without an account.").assertExists()
        compose.onNodeWithText("Display name").performTextInput("Asha")
        compose.onNodeWithText("Continue →").performClick()
        compose.onNodeWithText("Continue with Google").performScrollTo().performClick()
        compose.onNodeWithText("Google sign-in is not available yet. Continue on this device instead.").assertExists()
        shot("00-auth")
    }
}
