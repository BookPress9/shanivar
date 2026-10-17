package com.shanivar.nativeapp

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The three-tab shell (founder direction 2026-09-29).
 *
 * Tested directly rather than through `BuildConfig.THREE_TAB_NAV`, which is false: a scaffold that
 * only compiles is not a scaffold anyone can trust when the flag flips. These render the real
 * composable with stub tab contents, so what is under test is the navigation contract - not the cards.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ShanivarShellTest {
    @get:Rule val compose = createComposeRule()
    private val now = 1_800_000_000_000L

    private fun shell(
        snapshot: HomeSnapshot = HomeSnapshot(nowEpochMillis = now, nextAlarmEpochMillis = now + 99_999_999L, hasFocusSchedule = true),
        onReturnToDay: () -> Unit = {},
        initialTab: ShanivarTab = ShanivarTab.DASHBOARD,
    ) {
        compose.setContent {
            ShanivarTheme {
                ShanivarShell(
                    snapshot = snapshot,
                    accountName = null,
                    onOpenAccount = {},
                    onReturnToDay = onReturnToDay,
                    routineAlarm = { Text("ALARM CARD") },
                    routineFocus = { Text("FOCUS CARD") },
                    sadhana = { _, _ -> Text("SADHANA CONTENT") },
                    settings = { Text("SETTINGS CONTENT") },
                    initialTab = initialTab,
                )
            }
        }
    }

    @Test fun allThreeDestinationsAreReachableAndCarryTheirOwnContent() {
        shell()
        ShanivarTab.values().forEach { compose.onNodeWithText(it.label).assertExists() }

        compose.onNodeWithText("Routine").performClick()
        compose.onNodeWithText("ALARM CARD").assertExists()
        compose.onNodeWithText("FOCUS CARD").assertExists()

        compose.onNodeWithText("Sadhana").performClick()
        compose.onNodeWithText("SADHANA CONTENT").assertExists()

        compose.onNodeWithText("Dashboard").performClick()
        compose.onNodeWithText("One thing, protected.").assertExists()
    }

    @Test fun theRoutineTabHoldsBothOneTimeConfigurationsTogether() {
        // The founder's framing: the alarm and the app lock are one setup decision, not two features
        // in different places.
        shell(initialTab = ShanivarTab.ROUTINE)
        compose.onNodeWithText("ALARM CARD").assertExists()
        compose.onNodeWithText("FOCUS CARD").assertExists()
    }

    @Test fun settingsIsReachableFromTheTopBarWithoutBecomingAFourthTab() {
        shell()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("SETTINGS CONTENT").assertExists()
        assertEquals(3, ShanivarTab.values().size)
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("SETTINGS CONTENT").assertDoesNotExist()
    }

    @Test fun anActiveFocusWindowOffersLeavingAndTheDashboardActuallyLeaves() {
        var left = false
        shell(
            snapshot = HomeSnapshot(nowEpochMillis = now, focusEndsAtEpochMillis = now + 15 * 60_000L, hasFocusSchedule = true),
            onReturnToDay = { left = true },
        )
        compose.onNodeWithText("Your time is protected.").assertExists()
        compose.onNodeWithText("Return to my day").performScrollTo().performClick()
        assertTrue("the dashboard must actually exit rather than navigate inward", left)
    }

    @Test fun anUnconfiguredInstallLandsOnSetUpRoutine() {
        shell(snapshot = HomeSnapshot(nowEpochMillis = now))
        compose.onNodeWithText("Set your day once.").assertExists()
        compose.onNodeWithText("Set up routine").performScrollTo().performClick()
        compose.onNodeWithText("ALARM CARD").assertExists()
    }

    @Test fun theDashboardShowsNoStreakRankOrScoreAnywhere() {
        // Decision 0009 permits a factual recent-choices card "without streaks, ranks or scores".
        shell(snapshot = HomeSnapshot(nowEpochMillis = now, nextAlarmEpochMillis = now + 99_999_999L, hasFocusSchedule = true))
        listOf("streak", "level", "points", "score", "XP", "badge", "rank").forEach {
            compose.onAllNodesWithText(it, substring = true, ignoreCase = true).assertCountEquals(0)
        }
    }

    @Test fun disabledNavigationCannotChangeTab() {
        compose.setContent {
            ShanivarTheme {
                ShanivarShell(
                    snapshot = HomeSnapshot(nowEpochMillis = now, nextAlarmEpochMillis = now + 99_999_999L, hasFocusSchedule = true),
                    accountName = null, onOpenAccount = {}, onReturnToDay = {},
                    routineAlarm = { Text("ALARM CARD") }, routineFocus = { Text("FOCUS CARD") },
                    sadhana = { _, _ -> Text("SADHANA CONTENT") }, settings = { Text("SETTINGS CONTENT") },
                    navigationEnabled = false,
                )
            }
        }
        compose.onNodeWithText("Routine").performClick()
        // A network operation is in flight; moving tabs mid-operation is how a focus start gets
        // orphaned, so the bar refuses rather than half-navigating.
        compose.onNodeWithText("ALARM CARD").assertDoesNotExist()
    }
}
