package com.shanivar.nativeapp

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Navigation stays local; only protection/account/alarm operations use the network. */
@Composable
fun V1Home(
    intention: String, minutes: Int, accountName: String?, openAccount: () -> Unit,
    navigationEnabled: Boolean = true,
    focus: @Composable () -> Unit, alarm: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rhythm", 0) }
    var route by rememberSaveable { mutableStateOf("home") }
    var shani by rememberSaveable { mutableStateOf(prefs.getBoolean("shaniFraming", false)) }
    var resumeCount by remember { mutableIntStateOf(0) }
    OnAppResume { resumeCount++ }
    val focusEnd = remember(route, resumeCount) { context.getSharedPreferences("focus_guard", 0).getLong("endsAt", 0L) }
    BackHandler(route != "home") { if (navigationEnabled) route = "home" }
    Crossfade(targetState = route, animationSpec = tween(180), label = "v1ScreenFade") { screen ->
        if (screen == "pause") {
            LocalPauseScreen(isPreview = true, onExit = { route = "home" })
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (screen != "home") TextButton(onClick = { route = "home" }, enabled = navigationEnabled) { Text("← Home") }
                    else Text("SHANIVAR", letterSpacing = 3.sp, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { route = "settings" }, enabled = navigationEnabled) { Text("Settings") }
                }
                when (screen) {
                    "home" -> {
                        Text(if (shani) "A daily practice, inspired by Shani" else "Your time, your choice",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Heading("What matters\ntoday?")
                        Text("Begin with one thing you want to do.")
                        SectionCard("Time for one thing.") {
                            Text(if (focusEnd > System.currentTimeMillis()) "$intention · focus in progress" else "$intention · $minutes minutes")
                            Text("Protect time from distracting apps.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PrimaryAction("Start focus") { route = "focus" }
                        }
                        SectionCard("Begin when you choose.") {
                            val next = AlarmScheduler(context).pendingTriggerAtEpochMillis()
                            Text(next?.let { "Next: " + java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm")) }
                                ?: "A wake-up action you choose.")
                            OutlinedButton(onClick = { route = "alarm" }, modifier = Modifier.fillMaxWidth()) { Text("Set alarm") }
                        }
                        TextButton(onClick = { route = "pause" }, modifier = Modifier.fillMaxWidth()) { Text("Take a one-minute pause") }
                    }
                    "focus" -> focus()
                    "alarm" -> alarm()
                    "settings" -> {
                        Heading("Your choices.")
                        PrimaryAction(if (accountName == null) "Sign in or create account" else "Account · $accountName", openAccount)
                        SectionCard("Choose your wording") {
                            Text("The same tools. A context that feels right to you.")
                            listOf(false to "Plain", true to "Inspired by Shani").forEach { (value, label) ->
                                FilterChip(selected = shani == value, onClick = {
                                    shani = value; prefs.edit().putBoolean("shaniFraming", value).apply()
                                }, label = { Text(label) })
                            }
                            Text("Inspired by Shani: choose your intention and act on it with patience. No belief is required.")
                        }
                        TextButton(onClick = { route = "helper" }) { Text("Help a parent get started") }
                    }
                    "helper" -> {
                        Heading("Help them begin.\nLet them choose.")
                        Text("Ask what they want more time for.")
                        Text("Explain the alarm action or whole-app lock.")
                        Text("Help only where requested, then hand the phone back.")
                        Text("Their choices and private activity stay theirs.")
                        PrimaryAction("Hand over & get started") { route = "home" }
                    }
                }
                if (screen == "focus") TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }, modifier = Modifier.fillMaxWidth()) { Text("Return to my day") }
            }
        }
    }
}
