package com.shanivar.nativeapp

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class RoutineShellTest {
    @get:Rule val compose = createComposeRule()

    private fun shell(initial: RoutineDestination = RoutineDestination.ROUTINE) {
        compose.setContent {
            ShanivarTheme {
                RoutineShell(
                    routine = { Text("ROUTINE CONTENT") },
                    sadhana = { _, _ -> Text("SADHANA CONTENT") },
                    settings = { Text("SETTINGS CONTENT") },
                    initialDestination = initial,
                )
            }
        }
    }

    @Test fun exactlyTwoPrimaryDestinations() {
        shell()
        assertEquals(2, RoutineDestination.values().size)
        compose.onNodeWithText("Routine").assertExists()
        compose.onNodeWithText("Sadhana").assertExists()
        compose.onAllNodesWithText("Dashboard").assertCountEquals(0)
    }

    @Test fun returningLaunchLandsOnRoutine() {
        shell()
        compose.onNodeWithText("ROUTINE CONTENT").assertExists()
    }

    @Test fun sadhanaHoldsExistingSteadyEntryWithoutGamificationCopy() {
        shell()
        compose.onNodeWithText("Sadhana").performClick()
        compose.onNodeWithText("SADHANA CONTENT").assertExists()
        listOf("streak", "XP", "rank", "level", "badge").forEach {
            compose.onAllNodesWithText(it, substring = true, ignoreCase = true).assertCountEquals(0)
        }
    }

    @Test fun settingsIsTopBarNotAThirdTab() {
        shell()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("SETTINGS CONTENT").assertExists()
        assertEquals(2, RoutineDestination.values().size)
    }
}
