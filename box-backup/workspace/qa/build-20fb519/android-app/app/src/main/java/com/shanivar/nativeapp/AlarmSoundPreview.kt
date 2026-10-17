package com.shanivar.nativeapp

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper

/**
 * Plays a few seconds of the actual alarm tone/stream so "sound preview" at alarm setup means
 * something - a user finding out at 6am that their alarm stream is muted is the exact failure
 * `AlarmRinger.alarmStreamIsAudible()` exists to warn about ahead of time. Deliberately a separate,
 * standalone player rather than reusing `AlarmRinger` itself: this has none of that class's
 * escalation/hard-cap/wake-lock machinery, and must not be able to interact with a real ringing
 * alarm's state. A bounded auto-stop (not looping, capped duration) keeps this from ever becoming
 * an un-stoppable sound by its own separate bug.
 */
object AlarmSoundPreview {
    private const val PREVIEW_DURATION_MILLIS = 3_000L
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    fun play(context: Context) {
        stop() // never stack two previews
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = false
                prepare()
                start()
            }
        }.getOrNull() ?: return
        handler.postDelayed({ stop() }, PREVIEW_DURATION_MILLIS)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
