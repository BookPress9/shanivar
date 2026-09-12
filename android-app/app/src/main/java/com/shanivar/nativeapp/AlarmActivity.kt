package com.shanivar.nativeapp

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
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
    private val ringer by lazy { AlarmRinger(this) }
    private var dismissed = false

    private val movementLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data?.getBooleanExtra(DismissalContract.EXTRA_VERIFIED, false) == true) {
            dismissAlarm()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
        showOverLockScreen()
        AlarmForegroundService.start(this)
        ringer.start(onHardCapReached = {
            // The alarm gave up rather than being completed. Close the surface so the user isn't
            // left staring at a dead alarm screen, and let it be recorded as abandoned - that is a
            // materially different outcome from a dismissal and must not look like one in the data.
            dismissed = true
            AlarmForegroundService.stop(this)
            finish()
        })
        // Back must not dismiss the alarm. BlockOverlayActivity already did this for the app-lock
        // overlay; the alarm screen - the surface actually described to users as hard to dismiss -
        // did not, so a single Back press silently defeated it. Swallowing the event is the whole
        // callback: dismissal happens only through a verified result below.
        onBackPressedDispatcher.addCallback(this) { }
        setContent {
            ShanivarTheme {
                AlarmScreen(
                    alarmMuted = !ringer.alarmStreamIsAudible(),
                    onMove = {
                        movementLauncher.launch(Intent(this, MovementDismissalActivity::class.java)
                            .putExtra(EXTRA_SESSION_ID, sessionId))
                    },
                )
            }
        }
    }

    /**
     * Back is swallowed above, but Home is not interceptable on modern Android - the only lawful
     * answer is to come straight back. Relaunching from the background is normally blocked since
     * Android 10; holding SYSTEM_ALERT_WINDOW is what exempts us, the same mechanism FocusGuardService
     * relies on to raise the block screen. Guarded by [dismissed] so a completed alarm never
     * reappears, which would be the "won't stop after the action is done" failure.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (dismissed) return
        startActivity(
            Intent(this, AlarmActivity::class.java)
                .putExtra(EXTRA_SESSION_ID, sessionId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    override fun onDestroy() {
        // Belt and braces: dismissAlarm() already stops the ringer, but the process can also be
        // torn down by the OS mid-alarm, and a MediaPlayer looping on the alarm stream that
        // outlives its Activity would keep ringing with no way for the user to stop it.
        ringer.stop()
        super.onDestroy()
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
        // Order matters: silence first, unconditionally, before service teardown or anything that
        // could throw. Nothing about stopping the noise may depend on work that can fail.
        dismissed = true
        ringer.stop()
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
private fun AlarmScreen(alarmMuted: Boolean, onMove: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text("Time to wake up.", style = MaterialTheme.typography.headlineLarge)
            if (alarmMuted) {
                Text("Your alarm volume is turned down, so this alarm was silent. Raise the alarm volume before tonight.")
            }
            Text("Your chosen destination is waiting. The alarm ends after your movement is verified.")
            Button(onClick = onMove, modifier = Modifier.fillMaxWidth()) { Text("Start moving") }
        }
    }
}
