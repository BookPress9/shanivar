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
 *
 * DEV-89: the block screen no longer opens the breathing practice list ([BreathingHost]). The only
 * practice offered here is the non-paced one-minute pause, whose prompt is not a breath instruction.
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
                    // This host owns the exit: finishing, ending or backing out of the pause goes to the
                    // device home screen, never back into the app that was paused.
                    "pause" -> LocalPauseScreen(isPreview = false, prompt = rememberGroundingPrompt(), onExit = { goHome() })
                    else -> BlockOverlayScreen(
                        blockedPackage, reminder,
                        onPause = { inline = "pause" },
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

@Composable
internal fun BlockOverlayScreen(
    blockedPackage: String,
    reminder: Triple<String, String, String>,
    onPause: () -> Unit,
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
            OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth()) { Text("Take a one-minute pause") }
        }
    }
}
