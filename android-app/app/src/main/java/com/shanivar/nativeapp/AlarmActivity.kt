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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    private val scheduler by lazy { AlarmScheduler(this) }
    private var dismissed = false
    // Approved V1 wireframe's "alarm-done" screen (v1.js): a verified dismissal shows a brief
    // confirmation - sound already stopped, next occurrence if this alarm repeats - rather than
    // the screen vanishing the instant the ringer goes quiet. Backed by class-level Compose state
    // (not a local var in onCreate) so dismissAlarm() can flip it after setContent has already run.
    private var showDoneScreen by mutableStateOf(false)

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
            // Advance the recurrence AFTER the ringer/service teardown above, and wrapped so a
            // scheduling failure can never affect the (already-complete) stop-ringing path - see
            // the ordering note on dismissAlarm() below.
            runCatching { scheduler.advanceToNextOccurrence() }
            finish()
        })
        // Back must not dismiss the alarm. BlockOverlayActivity already did this for the app-lock
        // overlay; the alarm screen - the surface actually described to users as hard to dismiss -
        // did not, so a single Back press silently defeated it. Swallowing the event is the whole
        // callback: dismissal happens only through a verified result below.
        onBackPressedDispatcher.addCallback(this) { }
        setContent {
            ShanivarTheme {
                if (showDoneScreen) {
                    AlarmDoneScreen(
                        nextOccurrenceEpochMillis = scheduler.pendingTriggerAtEpochMillis(),
                        onOpenShanivar = ::openShanivar,
                        onDeviceHome = ::goToDeviceHome,
                    )
                } else {
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
    }

    private fun openShanivar() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun goToDeviceHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
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
        // Recurrence advance is local-only (SharedPreferences + AlarmManager, no network) and
        // strictly after the ring has already been silenced - it must never gate or delay that.
        // A repeat alarm that fails to re-arm here would silently stop recurring, which is why
        // this is wrapped rather than left to propagate into the Activity teardown below.
        runCatching { scheduler.advanceToNextOccurrence() }
        // Show the done screen rather than finish()ing immediately - the sound is already fully
        // stopped above, so this is presentation only, never a delay on silencing the alarm.
        showDoneScreen = true
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

/**
 * The approved V1 wireframe's "alarm-done" screen. Shown only after [AlarmActivity.dismissAlarm]'s
 * verified path - never after the ringer's own hard-cap giveup, which is a materially different,
 * abandoned outcome that must not read as a completed one.
 */
@Composable
private fun AlarmDoneScreen(nextOccurrenceEpochMillis: Long?, onOpenShanivar: () -> Unit, onDeviceHome: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text("✓", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text("Sound stopped. Begin your day.", style = MaterialTheme.typography.headlineLarge)
            Text("You completed your chosen dismissal. No need to stay here.")
            SectionCard("Next alarm") {
                Text(
                    nextOccurrenceEpochMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))
                    } ?: "This alarm does not repeat.",
                )
            }
            OutlinedButton(onClick = onDeviceHome, modifier = Modifier.fillMaxWidth()) { Text("Return to my day") }
            TextButton(onClick = onOpenShanivar, modifier = Modifier.fillMaxWidth()) { Text("Open Shanivar") }
        }
    }
}
