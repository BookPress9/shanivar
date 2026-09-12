package com.shanivar.nativeapp

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Deterministic "did the phone experience genuine ambulatory motion" heuristic, fed raw
 * accelerometer samples (`TYPE_ACCELEROMETER` x/y/z in m/s^2, with a timestamp). Kept as plain
 * Kotlin with no Android Sensor/Context dependency so it's testable with synthetic data, per the
 * task's requirement to unit-test "the motion-threshold detection logic (given synthetic
 * accelerometer readings, does it correctly classify motion vs. no-motion)" without hardware.
 *
 * Reasoning behind the two constants below (documented here so on-device tuning is an informed
 * adjustment against real traces later, not a guess made from scratch):
 * - A phone resting still reads a magnitude of ~9.81 m/s^2 (gravity only). We look at the
 *   *dynamic* component - magnitude minus gravity's baseline - so a stationary phone reads ~0.
 * - Picking a phone up off a nightstand produces a brief single acceleration spike that decays in
 *   well under a second - it crosses most reasonable thresholds momentarily but does not *sustain*
 *   above them.
 * - Walking produces a magnitude signal with repeated dynamic-acceleration peaks in roughly the
 *   2-4 m/s^2 range above gravity (consistent with published pocket/hip-carry gait-accelerometry
 *   figures), sustained continuously step after step.
 * - So instead of a single-sample spike check, we require the dynamic magnitude to stay at or
 *   above [magnitudeThresholdMetersPerSecondSquared] continuously for at least
 *   [requiredDurationMillis] - a threshold-crossing blip resets the streak rather than satisfying
 *   it. This is a heuristic starting point, not a validated model; it MUST be tuned against real
 *   accelerometer traces during on-device verification, which this machine cannot perform (see the
 *   delivery report's unverified-until-real-device-testing list).
 */
class GenuineMotionTracker(
    private val magnitudeThresholdMetersPerSecondSquared: Float = DEFAULT_MAGNITUDE_THRESHOLD_M_S2,
    private val requiredDurationMillis: Long = DEFAULT_REQUIRED_DURATION_MS,
) {
    private var sustainedSinceEpochMillis: Long? = null
    private var satisfiedAtEpochMillis: Long? = null

    fun isSatisfied(): Boolean = satisfiedAtEpochMillis != null

    fun reset() {
        sustainedSinceEpochMillis = null
        satisfiedAtEpochMillis = null
    }

    /** Feed one accelerometer sample. Samples must be delivered in non-decreasing timestamp order. */
    fun onSample(x: Float, y: Float, z: Float, atEpochMillis: Long) {
        if (satisfiedAtMillis()) return
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val dynamic = abs(magnitude - GRAVITY_M_S2)
        if (dynamic >= magnitudeThresholdMetersPerSecondSquared) {
            val since = sustainedSinceEpochMillis ?: atEpochMillis.also { sustainedSinceEpochMillis = it }
            if (atEpochMillis - since >= requiredDurationMillis) satisfiedAtEpochMillis = atEpochMillis
        } else {
            sustainedSinceEpochMillis = null
        }
    }

    private fun satisfiedAtMillis() = satisfiedAtEpochMillis != null

    companion object {
        const val GRAVITY_M_S2 = 9.81f
        const val DEFAULT_MAGNITUDE_THRESHOLD_M_S2 = 2.5f
        const val DEFAULT_REQUIRED_DURATION_MS = 1500L
    }
}

/**
 * Combines the motion gate with a photo-capture gate - the task spec requires both: "a genuine
 * motion threshold to be crossed... AND a photo capture... before allowing dismissal." The photo
 * itself doesn't need to be saved or matched against anything; capturing it is what proves the
 * camera flow was actually exercised, not just a sensor spoof left on a nightstand.
 */
class PhotoMotionDismissalState(private val motion: GenuineMotionTracker = GenuineMotionTracker()) {
    var photoCaptured: Boolean = false
        private set

    fun onAccelerometerSample(x: Float, y: Float, z: Float, atEpochMillis: Long) = motion.onSample(x, y, z, atEpochMillis)
    fun onPhotoCaptured() { photoCaptured = true }
    fun motionSatisfied(): Boolean = motion.isSatisfied()
    fun canDismiss(): Boolean = motion.isSatisfied() && photoCaptured
    fun reset() {
        motion.reset()
        photoCaptured = false
    }
}
