package com.shanivar.nativeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The "block screen" launched by `FocusGuardService` when it detects a protected package in the
 * foreground during an active session. This Activity is what `SYSTEM_ALERT_WINDOW` permission
 * actually buys us here: without it, Android 10+'s background-activity-start restriction would
 * block a Service from calling startActivity() on an app the user isn't currently inside of.
 *
 * Deliberately minimal: no snooze/bypass affordance beyond "back to Shanivar" - ending a session
 * early is a `DisciplineGateway.releaseFocus()` call from inside the app itself, not from here.
 */
class BlockOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()
        // The whole point of the overlay is that a casual back-press doesn't return you to
        // Instagram - only the explicit "Back to Shanivar" button does.
        onBackPressedDispatcher.addCallback(this) { returnToShanivar() }
        setContent { ShanivarTheme { BlockOverlayScreen(blockedPackage, onReturn = ::returnToShanivar, onReflect = {
            ReflectionNavigation.fromBlockedSurface(); returnToShanivar()
        }) } }
    }

    private fun returnToShanivar() {
        val home = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(home)
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        const val EXTRA_ENDS_AT = "ends_at"
    }
}

@Composable
private fun BlockOverlayScreen(blockedPackage: String, onReturn: () -> Unit, onReflect: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text("Your focus window is protected.", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (blockedPackage.isNotBlank()) "$blockedPackage is paused until your session ends."
                else "This app is paused until your session ends.",
            )
            Text("A brief flash of the app before this screen appears is expected - detection isn't instant.")
            OutlinedButton(onClick = onReflect, modifier = Modifier.fillMaxWidth()) { Text("Reflect with Shanivar") }
            Button(onClick = onReturn, modifier = Modifier.fillMaxWidth()) { Text("Back to Shanivar") }
        }
    }
}
