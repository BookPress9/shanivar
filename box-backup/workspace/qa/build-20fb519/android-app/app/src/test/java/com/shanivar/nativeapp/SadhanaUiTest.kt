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
        compose.onNodeWithText("Give this\n1 honest minute.").assertExists()
        noSanskritShown(); shot("01-sadhana-home")
        click("Open the Living Spine")
        compose.onNodeWithText("See the map.\nTest the practice.").assertExists()
        compose.onNodeWithContentDescription("Living Spine: 33", substring = true).performScrollTo()
        compose.onNode(SemanticsMatcher("click label") { it.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsActions.OnClick) { null }?.label == "Open Crown" }).performClick()
        compose.onNodeWithText("CROWN · THRESHOLD 33").assertExists()
        noSanskritShown(); shot("02-living-spine")
        compose.onNodeWithText("CROWN · THRESHOLD 33").performScrollTo(); shot("02b-living-spine-readout")
        click("Choose a practice →")
        compose.onNodeWithText("Return to one ordinary breath").assertExists()
        noSanskritShown(); shot("03-practice-detail")
        click("Prepare · 1 min →")
        click("Begin →")
        compose.onNodeWithText("Confirm you are stationary, or return without beginning.").assertExists()
        click("I am stationary and in a suitable place.")
        shot("04-prepare")
        click("Begin →")
        compose.onNodeWithText("ACTIVE PRACTICE").assertExists()
        compose.onNodeWithText("◌ Sadhana").assertDoesNotExist()
        compose.onNodeWithContentDescription("End practice").assertExists()
        shot("05-player")
        click("Change anchor")
        compose.onNodeWithText("Anchor changed to the room. Resume when ready.").assertExists()
        click("Resume")
        compose.runOnIdle { clock.advance(60_000); c.tick() }
        compose.onNodeWithText("1 active minute.").assertExists()
        click("Uncomfortable")
        shot("06-return")
        click("View the record")
        compose.onNodeWithText("Return to one ordinary breath").assertExists()
        shot("07-record")
        click("Practise without the player")
        click("Record an earlier practice")
        assertEquals(1, c.record.independentCount)
        click("See capability path")
        compose.onNodeWithText("What can you do\nwith less prompting?").assertExists()
        shot("08-path")
        compose.onNodeWithText("◇ Study").performClick()
        compose.onNodeWithText("Read the source.\nKeep the differences.").assertExists()
        compose.onNodeWithText("Crown · Sahasrāra").assertExists() // names live only in deep study
        shot("09-deep-study")
        compose.onNodeWithText("Intensive methods").performScrollTo(); shot("09b-deep-study-research-boundary")
    }

    @Test fun endImmediatelyRecordsEndedAndWithoutApp() {
        launch()
        click("Start →"); click("Prepare · 1 min →")
        click("I am stationary and in a suitable place."); click("Begin →")
        compose.runOnIdle { clock.advance(5_000); c.tick() }
        compose.onNodeWithContentDescription("End practice").performClick()
        compose.onNodeWithText("You ended the practice.").assertExists()
        assertEquals(SadhanaOutcome.ENDED, c.record.attempts.single().outcome)
        click("Return to my day →")
        compose.onNodeWithText("⌁ Path").performClick()
        click("Record practice without Shanivar")
        shot("10-without-app")
        click("Hide guidance")
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

    @Test fun saturnLoadingWithSpinningRings() {
        compose.mainClock.autoAdvance = false
        compose.setContent { SadhanaLoadingSurface(LoadState.Visible("Preparing Sadhana"), reduceMotion = false, onRetry = {}, onDismiss = {}) }
        compose.mainClock.advanceTimeBy(9_000)
        compose.onNodeWithText(SadhanaLoadingMotion.CREDIT).assertExists()
        shot("13-saturn-loading")
    }

    @Test fun spineShowsThirtyThreeStagesWithPlainWaypoints() {
        repeat(6) { c.recordOwn() }
        c.go(SadhanaView.SPINE)
        launch()
        compose.onNodeWithText("07 / 33").performScrollTo()
        compose.onNodeWithText("4 segments to Mid torso context").assertExists()
        noSanskritShown()
        compose.onNodeWithContentDescription("Living Spine", substring = true).performScrollTo()
        shot("14-spine-33")
    }

    @Test fun spineRootsScreenshot() {
        repeat(9) { c.recordOwn() }
        c.go(SadhanaView.SPINE)
        compose.setContent { SadhanaApp(controllerFactory = { c }, reduceMotionOverride = true, hydrateDispatcher = kotlinx.coroutines.Dispatchers.Unconfined) }
        compose.onNodeWithContentDescription("Living Spine: 33", substring = true).performScrollTo()
        shot("15-spine-roots")
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
        compose.onAllNodesWithText("Open →")[0].performScrollTo().performClick()
        compose.onNodeWithText("The source reader is not available yet. No unreviewed text is shown.").assertExists()
        compose.onAllNodesWithContentDescription("Shanivar is loading", substring = true).assertCountEquals(0)
    }

    @Test fun selfCheckIsLocalReflectiveAndVagalLayerIsLabelledIllustration() {
        c.go(SadhanaView.SPINE); launch()
        compose.onNodeWithText("What is true right now?").assertExists()
        compose.onNodeWithText("The lights follow your selection. Shanivar is not measuring your nervous system.").assertExists()
        compose.onNodeWithText("NOW · NOT ANSWERED").assertExists()
        click("On edge")
        compose.onNodeWithText("NOW · ON EDGE").assertExists()
        assertEquals(SelfCheck.ACTIVATED, c.ui.selfCheck)
        assertEquals("self-check is not evidence", 0, CapabilityCriteria.evidence(c.record, true).size)
        compose.onNodeWithContentDescription("symbolic vagal illustration", substring = true).assertExists()
        noSanskritShown()
        shot("16-self-check-vagal")
        compose.onNodeWithContentDescription("Living Spine: 33", substring = true).performScrollTo()
        shot("16b-spine-vagal-fireflies")
        compose.onNodeWithText("CONTEXT REVEALED").performScrollTo(); shot("16c-spine-context")
    }

    @Test fun revisedVoiceCopyHome() {
        launch()
        compose.onNodeWithText("Notice what happens. Then return to your day.").assertExists()
        compose.onNodeWithText("End anytime").assertExists()
        shot("16d-home-voice")
    }

    @Test fun postureGuidanceInDetailAndPrepare() {
        launch()
        click("Start →")
        compose.onNodeWithText(SadhanaPosture.BRIEF).performScrollTo().assertExists()
        click("Prepare · 1 min →")
        compose.onNodeWithText(SadhanaPosture.PREPARE).performScrollTo().assertExists()
        compose.onNodeWithText(SadhanaPosture.SAFETY).assertExists()
        shot("17-prepare-posture")
    }

    @Test fun stage1PracticeAndSelfCheck() {
        launch()
        click("Stage 1 · The base →")
        compose.onNodeWithText(Stage1.PROMPT).assertExists()
        shot("18-stage1-detail")
        click("Prepare · 1 min →")
        click("I am stationary and in a suitable place.")
        click("Begin →")
        compose.onNodeWithContentDescription("End practice").assertExists()
        clock.monoMs += 61_000; clock.wallMs += 61_000
        compose.runOnIdle { c.tick() }
        compose.onNodeWithText("Where did your attention go?").performScrollTo().assertExists()
        click("Partly"); click("+"); click("+"); click("Thoughts")
        compose.onNodeWithText("Where did your attention go?").performScrollTo()
        shot("18b-stage1-self-check")
        click("Save my report")
        compose.onNodeWithText("Saved").assertExists()
        assertEquals(2, c.record.stage1Checks.single().drifts)
    }
}
