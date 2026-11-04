package com.shanivar.nativeapp

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.shanivar.nativeapp.sync.AppSync
import com.shanivar.nativeapp.sync.DIAG_TAG
import com.shanivar.nativeapp.sync.STATUS_TAG
import com.shanivar.nativeapp.sync.SyncStatusText
import com.shanivar.nativeapp.sync.SyncUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/** DEV-47 AC1, AC4, AC5, AC7 on the Sadhana Record view. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class SyncStatusUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val reset = AppSyncReset()
    private val c = SadhanaController(MemDraftStore(), FakeSadhanaClock(), allowUnreviewed = true)
    private val statusStrings = listOf("Saved on this device", "Will sync when online", "Synced")


    private fun launchRecord() {
        c.go(SadhanaView.RECORD)
        compose.setContent { SadhanaApp(controllerFactory = { c }, reduceMotionOverride = true, hydrateDispatcher = kotlinx.coroutines.Dispatchers.Unconfined) }
    }
    private fun status() = compose.onNodeWithTag(STATUS_TAG).performScrollTo()
    private fun textsUnder(tag: String): List<String> =
        compose.onAllNodes(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher("has text") { n -> n.config.getOrNull(SemanticsProperties.Text) != null }, useUnmergedTree = true)
            .fetchSemanticsNodes().flatMap { n -> n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }

    @Test fun syncOffRecordViewIsIdentical() {
        c.go(SadhanaView.RECORD)
        val app: @androidx.compose.runtime.Composable (AppSync) -> Unit = { s ->
            SadhanaApp(controllerFactory = { c }, reduceMotionOverride = true, hydrateDispatcher = kotlinx.coroutines.Dispatchers.Unconfined, sync = s)
        }
        compose.setContent {
            androidx.compose.foundation.layout.Row {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f).testTag("off")) { app(AppSync.DISABLED) }
                androidx.compose.foundation.layout.Box(Modifier.weight(1f).testTag("on")) { app(enabledSync()) }
            }
        }
        val off = textsUnder("off"); val on = textsUnder("on")
        assertTrue(off.isNotEmpty())
        statusStrings.forEach { s -> assertTrue(off.none { it.startsWith(s) }) }
        compose.onAllNodes(hasAnyAncestor(hasTestTag("off")) and hasTestTag(STATUS_TAG)).assertCountEquals(0)
        assertEquals(off.size + 1, on.size)
        assertEquals(listOf("Saved on this device"), on - off.toSet())
    }

    @Test fun releaseLikeFlagOffShowsNoLine() {
        AppSync.install(AppSync.create(false, "https://example.invalid", "anon") { FakeSyncEngine() })
        launchRecord()
        compose.onAllNodesWithTag(STATUS_TAG).assertCountEquals(0)
    }

    @Test fun offlineShowsWillSyncWhenOnline() {
        AppSync.install(enabledSync(FakeSyncEngine(status = FakeSyncStatus(SyncUiState.WaitingForNetwork))))
        launchRecord()
        status().assertTextEquals("Will sync when online")
    }

    @Test fun statusFollowsTheEngineStateExactlyThreeStrings() {
        val e = FakeSyncEngine(); AppSync.install(enabledSync(e))
        launchRecord()
        status().assertTextEquals("Saved on this device")
        e.status.state.value = SyncUiState.Synced(Instant.now()); compose.waitForIdle()
        val synced = compose.onNodeWithTag(STATUS_TAG).fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        assertTrue(synced, synced.startsWith("Synced "))
        e.status.state.value = SyncUiState.WaitingForNetwork; compose.waitForIdle()
        status().assertTextEquals("Will sync when online")
    }

    @Test fun deadLetterCountOnlyInDebugDiagnostics() {
        AppSync.install(enabledSync(FakeSyncEngine(status = FakeSyncStatus(dead = 3))))
        launchRecord()
        compose.onAllNodesWithTag(DIAG_TAG, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("Dead-letter", substring = true).assertCountEquals(0)
        assertTrue(BuildConfig.DEBUG) // unit tests run the debug variant; release never composes the dialog
        status().performTouchInput { longClick() }
        compose.onNodeWithTag(DIAG_TAG, useUnmergedTree = true).assertTextContains("Dead-letter items: 3", substring = true)
    }

    @Test fun syncedTextFormatting() {
        val z = ZoneId.of("Europe/Berlin"); val now = Instant.parse("2026-10-17T20:00:00Z")
        assertTrue(SyncStatusText.of(SyncUiState.Synced(Instant.parse("2026-10-17T19:04:00Z")), now, z).startsWith("Synced "))
        assertEquals("Saved on this device", SyncStatusText.of(SyncUiState.LocalOnly, now, z))
        assertEquals("Will sync when online", SyncStatusText.of(SyncUiState.WaitingForNetwork, now, z))
    }
}
