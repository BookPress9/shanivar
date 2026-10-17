package com.shanivar.nativeapp

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReflectionDeviceTest {
    @get:Rule val compose=createComposeRule()
    private val state=ReflectionState()
    private val api=FakeReflectionRemote()
    private fun capture(name:String) {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val dir=InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: context.getExternalFilesDir("ui-review")!!
        dir.mkdirs()
        File(dir,"reflection-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
    private fun screen(scale:Float=1f) {
        state.bind("fixture",api,MemoryReflectionStore())
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,scale)) {
                ShanivarTheme { Surface {
                    ReflectionScreen(state,ReflectionEntryPoint.BLOCKED_APP_SURFACE,
                        protection=ReflectionProtectionReader {DisciplineStatus(ProtectionState.ACTIVE,true)},
                        onExit={},onPractices={})
                } }
            }
        }
        compose.onNodeWithText("Begin reflecting").performScrollTo().performClick()
        compose.waitForIdle()
    }
    @After fun cleanup() {compose.runOnIdle {state.clear()}}
    @Test fun responseGateHistoryAndFiveCardStoppingPoint() {
        screen()
        compose.onNodeWithText("What do you want them to understand?").performScrollTo()
        capture("card")
        compose.onNodeWithText("Continue · or swipe up").assertDoesNotExist()
        compose.onNodeWithText("What do you want them to understand?").performTouchInput {swipeUp()}
        compose.runOnIdle {assertEquals(0,state.session!!.answered)}
        repeat(5) { i ->
            compose.onNodeWithText("Prefer not to share").performScrollTo().performClick()
            compose.onNodeWithText("Send response").performScrollTo().performClick()
            compose.waitForIdle()
            if(i<4) compose.onNodeWithText("Continue · or swipe up").performClick()
        }
        compose.onNodeWithText("A place to pause.").performScrollTo().assertIsDisplayed()
        capture("complete")
        compose.onNodeWithText("Read answered messages").performScrollTo().performClick()
        compose.onNodeWithText("Message 1: What do you want them to understand?").performScrollTo().performClick()
        compose.onNodeWithText("Response saved").performScrollTo().assertExists()
        assertEquals(5,api.calls.count {it.first.endsWith("/responses")})
    }
    @Test fun largeTextFallbackAndDraftRemainReachable() {
        api.value=reflectionFixture(provenance="general_fallback",date="2000-01-01")
        screen(1.6f)
        compose.onNodeWithText("Start fresh for today").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("General reflection · service fallback").performScrollTo().assertIsDisplayed()
        capture("large-text")
        compose.onNodeWithText("Your response (optional)").performScrollTo().performTextInput("I want to be clear without making the reply sharper.")
        compose.onNodeWithText("Send response").performScrollTo().assertIsEnabled()
        capture("keyboard")
        compose.onNodeWithText("Back",useUnmergedTree=true).assertIsDisplayed()
    }
    @Test fun encryptedCacheSurvivesNewStoreAndIsolatesAccounts() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val store=EncryptedReflectionStore(context)
        store.clear()
        store.write("A",JSONObject().put("draft","private words"))
        val bytes=File(context.noBackupFilesDir,"reflection-session.enc").readBytes()
        assertFalse(String(bytes).contains("private words"))
        assertEquals("private words",EncryptedReflectionStore(context).read("A")!!.getString("draft"))
        assertNull(EncryptedReflectionStore(context).read("B"))
        assertFalse(File(context.noBackupFilesDir,"reflection-session.enc").exists())
    }
}

