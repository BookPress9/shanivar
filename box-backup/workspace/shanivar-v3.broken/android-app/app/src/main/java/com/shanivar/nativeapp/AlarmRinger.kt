package com.shanivar.nativeapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Makes the alarm audible.
 *
 * Until 2026-09-12 this app had no audio path at all: firing an alarm posted a notification and
 * relied on whatever tone that channel happened to carry, which plays on the NOTIFICATION stream
 * at notification volume and is silenced outright by Do Not Disturb. An alarm that Do Not Disturb
 * can silence cannot wake anybody, which made the product's core feature non-functional on a real
 * phone regardless of how correct the scheduling was.
 *
 * The distinction that matters is the audio stream, not the volume: [AudioAttributes.USAGE_ALARM]
 * routes to the alarm stream, which is exempt from ringer mute and from Do Not Disturb on stock
 * Android, and which users generally keep loud precisely because alarms use it.
 */
class AlarmRinger(private val context: Context) {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var ringingSince = 0L
    private var stopped = false

    /**
     * @param onHardCapReached invoked if the alarm rang for [HARD_CAP_MILLIS] without being
     * dismissed. The alarm stops regardless; this is the caller's chance to record the occurrence
     * as abandoned rather than dismissed.
     */
    fun start(onHardCapReached: () -> Unit = {}) {
        if (player != null) return // already ringing; never stack two players
        stopped = false
        ringingSince = System.currentTimeMillis()
        acquireWakeLock()
        startAudio()
        startVibration()
        scheduleEscalation()
        // Independent of every other code path on purpose. If the dismissal state machine has a
        // bug and never calls stop(), this still silences the device. An escalating alarm that
        // cannot be stopped is the single most harmful failure this product can ship, so the
        // guarantee that it ends does not depend on the logic that normally ends it.
        handler.postDelayed({
            stop()
            runCatching { onHardCapReached() }
        }, HARD_CAP_MILLIS)
    }

    /** Idempotent and synchronous. Safe to call from any teardown path, more than once. */
    fun stop() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    /**
     * Steps the playback volume up on a fixed cadence rather than fading smoothly: a perceptible
     * increase is what creates urgency, while a slow fade reads as a device malfunction. This
     * scales our own playback level inside the alarm stream; it never changes the user's system
     * alarm-volume setting, which is a separate setup-time consent.
     */
    private fun scheduleEscalation() {
        val step = object : Runnable {
            override fun run() {
                if (stopped) return
                val elapsed = System.currentTimeMillis() - ringingSince
                val volume = escalationVolumeAt(elapsed)
                runCatching { player?.setVolume(volume, volume) }
                if (volume < 1f) handler.postDelayed(this, ESCALATION_STEP_MILLIS)
            }
        }
        runCatching { player?.setVolume(START_VOLUME, START_VOLUME) }
        handler.postDelayed(step, ESCALATION_STEP_MILLIS)
    }

    /**
     * The screen is turned on by AlarmActivity's setTurnScreenOn; this only guarantees the CPU
     * stays up long enough for that Activity to actually appear, since Doze is otherwise free to
     * defer work between the alarm broadcast and the UI. Timeout-bounded so a crash between
     * start() and stop() cannot pin the CPU awake indefinitely and flatten the battery.
     */
    private fun acquireWakeLock() {
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = runCatching {
            power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "shanivar:alarm").apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MILLIS)
            }
        }.getOrNull()
    }

    private fun startAudio() {
        val uri = alarmUri() ?: return
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    /**
     * A device with no alarm tone configured still has to ring, so fall back through the other
     * stock tones rather than returning silence. Whatever is chosen still plays on the alarm
     * stream via the AudioAttributes above.
     */
    private fun alarmUri(): Uri? =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun startVibration() {
        vibrator = resolveVibrator()?.also { v ->
            runCatching {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                v.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0), attributes)
            }
        }
    }

    private fun resolveVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /**
     * Reports whether the alarm stream is actually audible. The UI should warn when this is false:
     * a user who muted their alarm volume will otherwise be woken by nothing and conclude the app
     * is broken, which is the same outcome as the bug this class was written to fix.
     */
    fun alarmStreamIsAudible(): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
        return audio.getStreamVolume(AudioManager.STREAM_ALARM) > 0
    }

    companion object {
        /** Longer than the hard cap, so a stuck alarm can never outlive its own wake lock. */
        const val WAKE_LOCK_TIMEOUT_MILLIS = 20 * 60 * 1000L

        /**
         * The alarm gives up after this long and records the occurrence as abandoned. A siren with
         * no end is a hazard (and a Play Store removal risk), not stronger discipline.
         */
        const val HARD_CAP_MILLIS = 15 * 60 * 1000L

        const val ESCALATION_STEP_MILLIS = 20_000L
        const val START_VOLUME = 0.4f
        private const val VOLUME_STEP = 0.15f

        // Half-second buzz, half-second gap, repeating: distinct from a notification's single tap.
        val VIBRATION_PATTERN = longArrayOf(0, 500, 500)

        /**
         * Playback level for a given time spent ringing: starts audible but not shocking, steps up
         * every [ESCALATION_STEP_MILLIS], reaches full at roughly 100 seconds and stays there.
         * Pure so the curve itself can be tested without a device or a MediaPlayer.
         */
        fun escalationVolumeAt(elapsedMillis: Long): Float {
            if (elapsedMillis <= 0L) return START_VOLUME
            val steps = elapsedMillis / ESCALATION_STEP_MILLIS
            return (START_VOLUME + steps * VOLUME_STEP).coerceAtMost(1f)
        }
    }
}
