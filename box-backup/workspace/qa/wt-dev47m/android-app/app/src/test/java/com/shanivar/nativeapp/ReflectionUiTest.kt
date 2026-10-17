package com.shanivar.nativeapp

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], qualifiers="w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ReflectionUiTest {
    @get:Rule val compose=createComposeRule()
    private val state=ReflectionState()
    private val api=FakeReflectionRemote()
    @Before fun setup() { state.bind("A",api,MemoryReflectionStore()) }
    @After fun cleanup() {compose.runOnIdle {state.clear()}}
    private fun open(exit:()->Unit={}) {
        compose.setContent { ShanivarTheme { Surface { ReflectionScreen(state,ReflectionEntryPoint.TODAY,onExit=exit,onPractices=exit) } } }
        compose.onNodeWithText("Begin reflecting").performScrollTo().performClick()
        compose.waitForIdle()
    }
    @Test fun swipeCannotBypassUnansweredAndNonDisclosingChoiceUnlocksContinue() {
        open()
        compose.onNodeWithText("Continue · or swipe up").assertDoesNotExist()
        compose.onNodeWithText("What do you want them to understand?").performTouchInput {swipeUp()}
        compose.runOnIdle {assertEquals(0,state.session!!.answered)}
        compose.onNodeWithText("Send response").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Prefer not to share").performScrollTo().performClick()
        compose.onNodeWithText("Send response").performScrollTo().performClick()
        compose.onNodeWithText("Continue · or swipe up").assertIsDisplayed().performClick()
        compose.runOnIdle {assertEquals("card-1",state.displayedId)}
    }
    @Test fun exitNeverRequiresResponseAndProtectionInterfaceOnlyReads() {
        var exits=0
        open { exits++ }
        compose.onNodeWithText("Back",useUnmergedTree=true).performClick()
        assertEquals(1,exits)
        compose.runOnIdle { assertFalse(api.calls.any {it.first.endsWith("/responses")}) }
    }
    @Test fun oversizedDraftIsRetainedAndSendDisabled() {
        open()
        compose.onNodeWithText("Your response (optional)").performScrollTo().performTextInput("x".repeat(1001))
        compose.onNodeWithText("Send response").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle {assertEquals(1001,state.draft.length)}
    }
}

