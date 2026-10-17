package com.shanivar.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Two-destination shell from decision 0014: **Routine · Sadhana**.
 *
 * Gated by [BuildConfig.ROUTINE_V3] (default false) so V1Home / THREE_TAB_NAV remain the
 * launch path until physical-device gates pass. Returning users land on Routine; Settings stays
 * a top-bar route. Sadhana hosts existing Steady — no XP, ranks or streaks.
 */
enum class RoutineDestination(val label: String) {
    ROUTINE("Routine"),
    SADHANA("Sadhana"),
}

@Composable
fun RoutineShell(
    routine: @Composable () -> Unit,
    sadhana: @Composable (BreathingEntry, () -> Unit) -> Unit,
    settings: @Composable () -> Unit,
    navigationEnabled: Boolean = true,
    initialDestination: RoutineDestination = RoutineDestination.ROUTINE,
) {
    var tab by rememberSaveable { mutableStateOf(initialDestination.name) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val current = runCatching { RoutineDestination.valueOf(tab) }.getOrDefault(RoutineDestination.ROUTINE)

    BackHandler(settingsOpen || current != RoutineDestination.ROUTINE) {
        if (!navigationEnabled) return@BackHandler
        if (settingsOpen) settingsOpen = false else tab = RoutineDestination.ROUTINE.name
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                RoutineDestination.values().forEach { destination ->
                    NavigationBarItem(
                        selected = current == destination && !settingsOpen,
                        onClick = {
                            if (navigationEnabled) {
                                settingsOpen = false
                                tab = destination.name
                            }
                        },
                        enabled = navigationEnabled,
                        icon = {},
                        label = { Text(destination.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SHANIVAR", letterSpacing = 3.sp, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { settingsOpen = !settingsOpen }, enabled = navigationEnabled) {
                    Text(if (settingsOpen) "Close" else "Settings")
                }
            }
            Crossfade(
                targetState = if (settingsOpen) "settings" else current.name,
                animationSpec = tween(180),
                label = "routineShellFade",
            ) { screen ->
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    when (screen) {
                        "settings" -> settings()
                        RoutineDestination.ROUTINE.name -> routine()
                        RoutineDestination.SADHANA.name -> sadhana(BreathingEntry.HOME) {}
                    }
                }
            }
        }
    }
}
