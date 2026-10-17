package com.shanivar.nativeapp

import kotlin.math.abs
import kotlin.math.sqrt

/** Counts hardware step-detector events and provides a conservative accelerometer fallback for
 * devices without that sensor. The fallback counts separated acceleration peaks, never a single
 * pickup jolt as the whole movement requirement. */
class MovementProgressTracker(val target: Int) {
    var steps: Int = 0
        private set
    private var aboveThreshold = false
    private var lastFallbackStepAt: Long? = null

    fun onHardwareStep(value: Float = 1f) {
        if (value > 0f && steps < target) steps++
    }

    fun onAccelerometer(x: Float, y: Float, z: Float, atMillis: Long) {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val dynamic = abs(magnitude - GenuineMotionTracker.GRAVITY_M_S2)
        val isPeak = dynamic >= 2.2f
        if (isPeak && !aboveThreshold && (lastFallbackStepAt == null || atMillis - lastFallbackStepAt!! >= 280L)) {
            if (steps < target) steps++
            lastFallbackStepAt = atMillis
        }
        aboveThreshold = isPeak
    }

    fun complete() = steps >= target
}
