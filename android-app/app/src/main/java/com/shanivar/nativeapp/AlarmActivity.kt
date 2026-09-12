package com.shanivar.nativeapp

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The dedicated full-screen "session alarm" Activity from platform-research.md's recommended
 * architecture: launched by `AlarmFireReceiver`'s full-screen intent, stays on top over the lock
 * screen (via `setShowWhenLocked`/`setTurnScreenOn`, the current non-deprecated replacement for
 * the old `FLAG_SHOW_WHEN_LOCKED`/`FLAG_TURN_SCREEN_ON` window flags on API 27+, with those flags
 * kept as a pre-27 fallback), and refuses to finish() until a dismissal method reports a verified
 * success. `AlarmForegroundService` keeps the process alive alongside it.
 *
 * Per the locked founder decision, true non-dismissibility does not exist on Android (force-stop,
 * disabling notifications, or revoking permissions all bypass this) - this Activity implements
 * "maximally inconvenient to bypass," not "impossible," matching platform-research.md's framing.
 */
class AlarmActivity : ComponentActivity() {
    private var sessionId: String = ""

    private val qrLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data?.getBooleanExtra(DismissalContract.EXTRA_VERIFIED, false) == true) {
            dismissAlarm()
        }
    }
    private val photoMotionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data?.getBooleanExtra(DismissalContract.EXTRA_VERIFIED, false) == true) {
            dismissAlarm()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
        showOverLockScreen()
        AlarmForegroundService.start(this)
        setContent {
            ShanivarTheme {
                AlarmScreen(
                    onScanQr = { qrLauncher.launch(Intent(this, QrDismissalActivity::class.java)) },
                    onPhotoMotion = { photoMotionLauncher.launch(Intent(this, PhotoMotionDismissalActivity::class.java)) },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
    }

    private fun dismissAlarm() {
        AlarmForegroundService.stop(this)
        finish()
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }
}

/** Shared result contract between AlarmActivity and both dismissal Activities. */
object DismissalContract {
    const val EXTRA_VERIFIED = "verified"
}

@Composable
private fun AlarmScreen(onScanQr: () -> Unit, onPhotoMotion: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text("Time to wake up.", style = MaterialTheme.typography.headlineLarge)
            Text("Choose how you'll dismiss this alarm. Both require leaving your bed.")
            Button(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) { Text("Scan your QR code") }
            Button(onClick = onPhotoMotion, modifier = Modifier.fillMaxWidth()) { Text("Verify motion + take a photo") }
        }
    }
}
