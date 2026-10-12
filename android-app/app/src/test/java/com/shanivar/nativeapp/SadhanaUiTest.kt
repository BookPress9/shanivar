package com.shanivar.nativeapp

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
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
class SadhanaUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val clock = FakeSadhanaClock()
    private val c = SadhanaController(MemDraftStore(), clock, allowUnreviewed = true)
    private val sanskrit = SadhanaSpine.centres.map { it.deepStudyName }

    private fun launch() = compose.setContent { SadhanaApp(controllerFactory = { c }, reduceMotionOverride = true, hydrateDispatcher = kotlinx.coroutines.Dispatchers.Unconfined) }
    private fun click(t: String) = compose.onNodeWithText(t).performScrollTo().performClick()
    private fun noSanskritShown() = sanskrit.forEach { compose.onAllNodesWithText(it, substring = true).assertCountEquals(0) }
    private fun shot(name: String) {
        val dir = System.getProperty("sadhana.shots") ?: return
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(dir).mkdirs(); File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun fullJourneyHomeToRecord() {
        launch()
        compose.onNodeWithText("The path lives\nwithin you.").assertExists()
        noSanskritShown(); shot("01-sadhana-home")
        click("Enter the Living Spine")
        compose.onNodeWithText("One axis.\nSeven doorways.").assertExists()
        compose.onNodeWithContentDescription("Seven-centre subtle-body study map").performScrollTo()
        compose.onNode(SemanticsMatcher("click label") { it.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsActions.OnClick) { null }?.label == "Open Crown" }).performClick()
        compose.onNodeWithText("SELECTED WAYPOINT · CROWN").assertExists()
        noSanskritShown(); shot("02-living-spine")
        compose.onNodeWithText("SELECTED WAYPOINT · CROWN").performScrollTo(); shot("02b-living-spine-readout")
        click("Practise from this foundation →")
        compose.onNodeWithText("Return to one ordinary breath").assertExists()
        noSanskritShown(); shot("03-practice-detail")
        click("Prepare · 1 min →")
        click("Begin Sadhana →")
        compose.onNodeWithText("Confirm you are stationary, or return without beginning.").assertExists()
        click("I am stationary and in a suitable place.")
        shot("04-prepare")
        click("Begin Sadhana →")
        compose.onNodeWithText("ACTIVE PRACTICE").assertExists()
        compose.onNodeWithText("◌ Sadhana").assertDoesNotExist()
        compose.onNodeWithContentDescription("End practice").assertExists()
        shot("05-player")
        click("Change anchor")
        compose.onNodeWithText("Anchor changed to the room. Resume when ready.").assertExists()
        click("Resume")
        compose.runOnIdle { clock.advance(60_000); c.tick() }
        compose.onNodeWithText("You stayed.\nNow leave it here.").assertExists()
        click("Uncomfortable")
        shot("06-return")
        click("View practice record")
        compose.onNodeWithText("Return to one ordinary breath").assertExists()
        shot("07-record")
        click("Practise without the player")
        click("I already practised · record it")
        assertEquals(1, c.record.independentCount)
        click("See capability path")
        compose.onNodeWithText("What can you do\nwith less prompting?").assertExists()
        shot("08-path")
        compose.onNodeWithText("◇ Study").performClick()
        compose.onNodeWithText("Depth was never\nsomething to earn.").assertExists()
        compose.onNodeWithText("Crown · Sahasrāra").assertExists() // names live only in deep study
        shot("09-deep-study")
        compose.onNodeWithText("Intensive methods").performScrollTo(); shot("09b-deep-study-research-boundary")
    }

    @Test fun endImmediatelyRecordsEndedAndWithoutApp() {
        launch()
        click("Enter practice →"); click("Prepare · 1 min →")
        click("I am stationary and in a suitable place."); click("Begin Sadhana →")
        compose.runOnIdle { clock.advance(5_000); c.tick() }
        compose.onNodeWithContentDescription("End practice").performClick()
        compose.onNodeWithText("You chose\nwhen to end.").assertExists()
        assertEquals(SadhanaOutcome.ENDED, c.record.attempts.single().outcome)
        click("Return to my day →")
        compose.onNodeWithText("⌁ Path").performClick()
        click("Record practice without Shanivar")
        shot("10-without-app")
        click("I’ll practise now · hide guidance")
        assertEquals(0, c.record.independentCount)
        compose.onNodeWithText("GUIDANCE: MINIMAL").assertExists()
        shot("11-home-minimal-guidance")
    }

    @Test fun loadingSurfaceIsBusyForTalkBackAndStaticUnderReducedMotion() {
        compose.setContent { SadhanaLoadingSurface(LoadState.Visible("Preparing Sadhana"), reduceMotion = true, onRetry = {}, onDismiss = {}) }
        compose.onNodeWithContentDescription("Shanivar is loading. Preparing Sadhana")
            .assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
                androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate))
        compose.onNodeWithText("PREPARING SADHANA").assertExists()
        shot("12-loading")
    }

    @Test fun loadingErrorOffersRetryAndContinue() {
        var retried = false
        compose.setContent { SadhanaLoadingSurface(LoadState.Failed("Opening source context", "This is taking longer than expected. Try again, or continue without it."), true, { retried = true }, {}) }
        compose.onNodeWithText("Continue without it").assertExists()
        compose.onNodeWithText("Try again").performClick(); assertTrue(retried)
    }

    @Test fun instantSourceRetrievalShowsNoLoader() {
        compose.setContent { SadhanaApp(controllerFactory = { c }, reduceMotionOverride = true, hydrateDispatcher = kotlinx.coroutines.Dispatchers.Unconfined, sourcePort = { null }) }
        compose.onNodeWithText("◇ Study").performClick()
        compose.onAllNodesWithText("Inspect lane →")[0].performScrollTo().performClick()
        compose.onNodeWithText("The source reader is not available yet. No unreviewed text is shown.").assertExists()
        compose.onAllNodesWithContentDescription("Shanivar is loading", substring = true).assertCountEquals(0)
    }
}
