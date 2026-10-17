package com.shanivar.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The three-tab shell from the founder's 2026-09-29 direction: **Dashboard · Routine · Sadhana**.
 *
 * Scaffold, not a rewrite. Every tab renders composables that already exist and are already tested -
 * the alarm card, the focus/app-lock schedule card, the Steady player. Nothing about the alarm engine,
 * the focus engine, the recurrence maths or local storage changes when `THREE_TAB_NAV` is flipped;
 * only which container presents them. That is deliberate: a navigation change should never be able to
 * break enforcement, and the cheapest way to guarantee that is to give it no access to enforcement.
 *
 * Gated off by default (`BuildConfig.THREE_TAB_NAV = false`) because the visual contract for these
 * tabs does not exist yet. When GPT's wireframe lands, the tab *contents* get fitted to it; this
 * structure and [HomeState] should survive that.
 *
 * Settings is reached from the top bar rather than being a fourth tab, because the direction specifies
 * three.
 */
@Composable
fun ShanivarShell(
    snapshot: HomeSnapshot,
    accountName: String?,
    onOpenAccount: () -> Unit,
    onReturnToDay: () -> Unit,
    routineAlarm: @Composable () -> Unit,
    routineFocus: @Composable () -> Unit,
    sadhana: @Composable (BreathingEntry, () -> Unit) -> Unit,
    settings: @Composable () -> Unit,
    navigationEnabled: Boolean = true,
    initialTab: ShanivarTab = ShanivarTab.DASHBOARD,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab.name) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val current = runCatching { ShanivarTab.valueOf(tab) }.getOrDefault(ShanivarTab.DASHBOARD)

    // Back returns to the Dashboard before it leaves the app - the tabs are peers, not a stack.
    BackHandler(settingsOpen || current != ShanivarTab.DASHBOARD) {
        if (!navigationEnabled) return@BackHandler
        if (settingsOpen) settingsOpen = false else tab = ShanivarTab.DASHBOARD.name
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                ShanivarTab.values().forEach { destination ->
                    NavigationBarItem(
                        selected = current == destination && !settingsOpen,
                        onClick = { if (navigationEnabled) { settingsOpen = false; tab = destination.name } },
                        enabled = navigationEnabled,
                        // No icon set exists, and inventing one is not this change's job. The label
                        // carries the meaning and NavigationBarItem still exposes selected state to
                        // TalkBack, which a row of TextButtons would not.
                        icon = {},
                        label = { Text(destination.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SHANIVAR", letterSpacing = 3.sp, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { settingsOpen = !settingsOpen }, enabled = navigationEnabled) {
                    Text(if (settingsOpen) "Close" else "Settings")
                }
            }
            Crossfade(targetState = if (settingsOpen) "settings" else current.name,
                animationSpec = tween(180), label = "shellFade") { screen ->
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    when (screen) {
                        "settings" -> settings()
                        ShanivarTab.DASHBOARD.name -> DashboardTab(
                            snapshot = snapshot,
                            accountName = accountName,
                            onOpenAccount = onOpenAccount,
                            onReturnToDay = onReturnToDay,
                            onGoTo = { destination -> tab = destination.name },
                        )
                        ShanivarTab.ROUTINE.name -> RoutineTab(routineAlarm, routineFocus)
                        ShanivarTab.SADHANA.name -> SadhanaTab(sadhana)
                    }
                }
            }
        }
    }
}

/**
 * One relevant next action, resolved locally by [HomeState]. No model call, no network, no wait.
 *
 * When protection is already running this offers leaving rather than anything to do here. That is the
 * mission stated as code: the app's best move mid-focus is to be finished with you.
 */
@Composable
private fun DashboardTab(
    snapshot: HomeSnapshot,
    accountName: String?,
    onOpenAccount: () -> Unit,
    onReturnToDay: () -> Unit,
    onGoTo: (ShanivarTab) -> Unit,
) {
    val next = remember(snapshot) { HomeState.resolve(snapshot) }
    Heading(next.headline)
    Text(next.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PrimaryAction(next.actionLabel) {
        if (next.kind == NextActionKind.FOCUS_ACTIVE) onReturnToDay() else onGoTo(next.tab)
    }
    // Deliberately factual and deliberately not a scoreboard: decision 0009 allows a recent-choices
    // card "without streaks, ranks or scores", and that limit is the reason this shows a state rather
    // than a count.
    SectionCard("Today") {
        Text(if (snapshot.nextAlarmEpochMillis != null) "Alarm set." else "No alarm set.")
        Text(if (snapshot.hasFocusSchedule) "App lock configured." else "App lock not configured yet.")
        Text(if (snapshot.practisedToday) "You practised today." else "No practice yet today.")
    }
    TextButton(onClick = onOpenAccount, modifier = Modifier.fillMaxWidth()) {
        Text(accountName?.let { "Account · $it" } ?: "Sign in or create account")
    }
}

/**
 * The day's agenda: the alarm and the app lock, both configured once and then left alone.
 *
 * Both cards are passed in rather than constructed here, so this tab has no reference to
 * `DisciplineGateway`, `AlarmScheduler` or `FocusScheduler` - the same containment the Steady player
 * has. A navigation container cannot release protection if it cannot name it.
 */
@Composable
private fun RoutineTab(alarm: @Composable () -> Unit, focus: @Composable () -> Unit) {
    Heading("Your day,\nset once.")
    Text("Two decisions that hold until you change them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    alarm()
    focus()
}

/**
 * Practice. Today this is Steady (the eight reviewed VBT-derived practices).
 *
 * What is *not* here yet, and should not be added without an explicit founder decision: any measured
 * progression, level, score or streak. Decision 0009 bans gamification for V1 and stance ST-005
 * rejects depth earned through status. The founder's 2026-09-29 direction asks for a measurable,
 * gamified sadhana, which means 0009 has to be revisited on the record first - see the note in
 * `docs/decisions/` before building it.
 */
@Composable
private fun SadhanaTab(sadhana: @Composable (BreathingEntry, () -> Unit) -> Unit) {
    sadhana(BreathingEntry.HOME) {}
}
