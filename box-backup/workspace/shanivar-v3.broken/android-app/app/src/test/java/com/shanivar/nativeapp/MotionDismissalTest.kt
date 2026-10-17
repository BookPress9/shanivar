package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test

class GenuineMotionTrackerTest {
    private val gravity = GenuineMotionTracker.GRAVITY_M_S2

    @Test fun stillPhoneNeverSatisfies() {
        val tracker = GenuineMotionTracker()
        var t = 0L
        repeat(20) {
            tracker.onSample(0f, 0f, gravity, t) // resting flat, magnitude == gravity, dynamic == 0
            t += 100
        }
        assertFalse(tracker.isSatisfied())
    }

    @Test fun briefPickupJoltDoesNotSatisfy() {
        val tracker = GenuineMotionTracker(magnitudeThresholdMetersPerSecondSquared = 2.5f, requiredDurationMillis = 1500)
        // A single short spike (e.g. picking the phone up) crosses the threshold but decays well
        // under the required sustained duration.
        tracker.onSample(0f, 0f, gravity, 0)
        tracker.onSample(0f, 0f, gravity + 6f, 100) // spike: dynamic = 6, above threshold
        tracker.onSample(0f, 0f, gravity + 6f, 300) // still within the spike, only 300ms elapsed
        tracker.onSample(0f, 0f, gravity, 400) // settles back to still
        tracker.onSample(0f, 0f, gravity, 2000)
        assertFalse(tracker.isSatisfied())
    }

    @Test fun sustainedWalkingLikeMotionSatisfies() {
        val tracker = GenuineMotionTracker(magnitudeThresholdMetersPerSecondSquared = 2.5f, requiredDurationMillis = 1500)
        // Oscillating dynamic acceleration around 3 m/s^2 above gravity, sustained continuously -
        // representative of a walking gait signal per the reasoning documented on the class.
        var t = 0L
        var satisfiedEarly = false
        repeat(20) { i ->
            val dynamic = if (i % 2 == 0) 3f else -3f
            tracker.onSample(0f, 0f, gravity + dynamic, t)
            if (t < 1500 && tracker.isSatisfied()) satisfiedEarly = true
            t += 100
        }
        assertFalse("should not satisfy before the required duration elapses", satisfiedEarly)
        assertTrue(tracker.isSatisfied())
    }

    @Test fun dropBelowThresholdResetsTheSustainedStreak() {
        val tracker = GenuineMotionTracker(magnitudeThresholdMetersPerSecondSquared = 2.5f, requiredDurationMillis = 1000)
        tracker.onSample(0f, 0f, gravity + 4f, 0)
        tracker.onSample(0f, 0f, gravity + 4f, 500)
        tracker.onSample(0f, 0f, gravity, 600) // drop back to still - resets the streak
        tracker.onSample(0f, 0f, gravity + 4f, 700)
        tracker.onSample(0f, 0f, gravity + 4f, 1200) // only 500ms since the reset at t=700
        assertFalse(tracker.isSatisfied())
        tracker.onSample(0f, 0f, gravity + 4f, 1800) // now 1100ms sustained since t=700
        assertTrue(tracker.isSatisfied())
    }

    @Test fun onceSatisfiedStaysSatisfiedAndIgnoresLaterSamples() {
        val tracker = GenuineMotionTracker(magnitudeThresholdMetersPerSecondSquared = 2.5f, requiredDurationMillis = 500)
        tracker.onSample(0f, 0f, gravity + 4f, 0)
        tracker.onSample(0f, 0f, gravity + 4f, 600)
        assertTrue(tracker.isSatisfied())
        tracker.onSample(0f, 0f, gravity, 700) // going still afterwards must not un-satisfy it
        assertTrue(tracker.isSatisfied())
    }

    @Test fun resetClearsSatisfaction() {
        val tracker = GenuineMotionTracker(magnitudeThresholdMetersPerSecondSquared = 2.5f, requiredDurationMillis = 500)
        tracker.onSample(0f, 0f, gravity + 4f, 0)
        tracker.onSample(0f, 0f, gravity + 4f, 600)
        assertTrue(tracker.isSatisfied())
        tracker.reset()
        assertFalse(tracker.isSatisfied())
    }
}

class PhotoMotionDismissalStateTest {
    @Test fun requiresBothMotionAndPhotoBeforeDismissal() {
        val state = PhotoMotionDismissalState(GenuineMotionTracker(2.5f, 500))
        assertFalse(state.canDismiss())

        state.onPhotoCaptured()
        assertFalse("photo alone (no motion) must not be enough", state.canDismiss())

        state.reset()
        val gravity = GenuineMotionTracker.GRAVITY_M_S2
        state.onAccelerometerSample(0f, 0f, gravity + 4f, 0)
        state.onAccelerometerSample(0f, 0f, gravity + 4f, 600)
        assertTrue(state.motionSatisfied())
        assertFalse("motion alone (no photo) must not be enough", state.canDismiss())

        state.onPhotoCaptured()
        assertTrue(state.canDismiss())
    }
}
