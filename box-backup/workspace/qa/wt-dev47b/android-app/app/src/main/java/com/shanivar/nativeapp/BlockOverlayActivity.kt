package com.shanivar.nativeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.ZonedDateTime

/**
 * The "block screen" launched by `FocusGuardService` when it detects a protected package in the
 * foreground during an active session. This Activity is what `SYSTEM_ALERT_WINDOW` permission
 * actually buys us here: without it, Android 10+'s background-activity-start restriction would
 * block a Service from calling startActivity() on an app the user isn't currently inside of.
 *
 * MVP streamline task (package 5, "a door, not a wall"): someone hitting this screen is already
 * holding the phone and already wants something - spending that moment on a bare refusal gets
 * nothing back. The primary action is now the local one-minute pause (see [LocalPauseScreen]),
 * offered inline without leaving this Activity; "Return to my day" is secondary and goes to the
 * device's own home screen, not back into Shanivar, matching "device home rather than the blocked
 * app" from the task spec. There is still no snooze/bypass affordance: ending a session early
 * remains a `DisciplineGateway.releaseFocus()` call from inside the app itself, not from here.
 */
class BlockOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()
        val rule = FocusScheduler(this).pendingRule()
        val reminder = if (rule != null && FocusWindowRecurrence.isActiveAt(rule.days, rule.startTime, rule.endTime, ZonedDateTime.now()))
            FocusCommitmentCache.read(this) else Triple("", "", "")
        // The whole point of the overlay is that a casual back-press doesn't return you to
        // Instagram - only an explicit action does.
        onBackPressedDispatcher.addCallback(this) { goHome() }
        setContent {
            ShanivarTheme {
                var inline by remember { mutableStateOf("block") }
                when (inline) {
                    "pause" -> LocalPauseScreen(isPreview = false, prompt = rememberGroundingPrompt(), onExit = { goHome() })
                    // This host owns the exit: finishing, ending or backing out of a practice goes to
                    // the device home screen, never back into the app that was paused. The player has
                    // no way to release protection, so there is nothing to bypass here.
                    "breathe" -> BreathingHost(BreathingEntry.BLOCK_OVERLAY, rememberProtectionSnapshot(), onExit = ::goHome)
                    else -> BlockOverlayScreen(
                        blockedPackage, reminder,
                        onPause = { inline = "pause" },
                        onBreathe = { inline = "breathe" },
                        onReturn = ::goHome,
                    )
                }
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_ENDS_AT = "ends_at"
    }
}

/**
 * Reads the focus engine's own local record of when protection ends. Read-only by construction - this
 * is a SharedPreferences get, not a gateway - and it re-reads on resume so a window that expired while
 * someone was practising is reported as expired rather than silently claimed as still protecting. An
 * absent record means unknown, which is shown as unknown: a wrong "off" would invite someone straight
 * back into the app they just paused.
 */
@Composable
private fun rememberProtectionSnapshot(): BreathingProtectionSnapshot {
    val context = LocalContext.current
    var resumeCount by remember { mutableIntStateOf(0) }
    OnAppResume { resumeCount++ }
    val endsAt = remember(resumeCount) { context.getSharedPreferences("focus_guard", 0).getLong("endsAt", 0L) }
    return when {
        endsAt <= 0L -> BreathingProtectionSnapshot(ProtectionState.UNAVAILABLE, null)
        endsAt > System.currentTimeMillis() -> BreathingProtectionSnapshot(ProtectionState.ACTIVE, endsAt)
        else -> BreathingProtectionSnapshot(ProtectionState.OFF, null)
    }
}

@Composable
private fun BlockOverlayScreen(
    blockedPackage: String,
    reminder: Triple<String, String, String>,
    onPause: () -> Unit,
    onBreathe: () -> Unit,
    onReturn: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text(reminder.first.ifBlank { "Your focus window is protected." }, style = MaterialTheme.typography.headlineSmall)
            if (reminder.second.isNotBlank()) Text(reminder.second)
            if (reminder.third.isNotBlank()) Text(reminder.third)
            Text(
                if (blockedPackage.isNotBlank()) "$blockedPackage is paused until your session ends."
                else "This app is paused until your session ends.",
            )
            Text("A brief flash of the app before this screen appears is expected - detection isn't instant.")
            PrimaryAction("Return to my day", onReturn)
            OutlinedButton(onClick = onBreathe, modifier = Modifier.fillMaxWidth()) { Text("Choose a short practice") }
            OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth()) { Text("Take a one-minute pause") }
        }
    }
}
