package com.shanivar.nativeapp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * The single optional sound in this feature: one short, original, fading tone for the
 * `sound_listen` practice.
 *
 * Synthesised rather than shipped as an audio file. That keeps it unambiguously original (no sample
 * licence to track), keeps the APK smaller, and means "the asset is missing or corrupt" reduces to
 * "the device would not give us an AudioTrack" - a condition [available] can actually answer, which
 * is what the recommender needs in order to withhold the practice rather than start a silent one.
 *
 * What this deliberately is not: a frequency treatment, a binaural claim, or a breath pacer. It is a
 * sound that starts and fades so there is something to notice ending. It plays once per practice
 * stage, never on a loop, and [stop] silences it immediately.
 *
 * Played on the media usage at low volume. An alarm uses its own stream and its own configuration,
 * so this can never quieten one - which matters, because a muted alarm is the project's worst bug.
 */
class BreathingTonePlayer {
    private var track: AudioTrack? = null
    private var availability: Boolean? = null

    /** Cached: the answer cannot change without the process restarting, and the check itself
     * allocates the tone buffer - not something to repeat during composition. */
    fun available(): Boolean = availability
        ?: runCatching { ensureTrack() != null }.getOrDefault(false).also { availability = it }

    /** Plays the tone from the beginning, once. Calling it again while it sounds restarts it rather
     * than layering a second voice. */
    fun playOnce() {
        runCatching {
            val audio = ensureTrack() ?: return
            audio.stop()
            audio.reloadStaticData()
            audio.setVolume(VOLUME)
            audio.play()
        }
    }

    fun stop() {
        runCatching { track?.pause(); track?.flush() }
    }

    fun release() {
        runCatching { track?.stop(); track?.release() }
        track = null
    }

    private fun ensureTrack(): AudioTrack? {
        track?.let { return it }
        val samples = renderFadingTone()
        val created = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        if (created.state != AudioTrack.STATE_INITIALIZED) {
            created.release()
            return null
        }
        created.write(samples, 0, samples.size)
        track = created
        return created
    }

    /** A plain sine with an exponential decay to silence - the "fading" the practice text describes,
     * and nothing more. */
    private fun renderFadingTone(): ShortArray {
        val total = SAMPLE_RATE * DURATION_SECONDS
        return ShortArray(total) { index ->
            val seconds = index.toDouble() / SAMPLE_RATE
            val envelope = exp(-seconds / DECAY_SECONDS)
            (sin(2.0 * PI * FREQUENCY_HZ * seconds) * envelope * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val DURATION_SECONDS = 8
        const val DECAY_SECONDS = 2.5
        const val FREQUENCY_HZ = 220.0
        const val VOLUME = 0.25f
    }
}
