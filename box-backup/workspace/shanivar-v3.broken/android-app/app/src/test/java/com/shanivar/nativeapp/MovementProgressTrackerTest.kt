package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test

class MovementProgressTrackerTest {
    @Test fun hardwareStepsReachTheChosenTargetExactly() {
        val tracker = MovementProgressTracker(3)
        repeat(2) { tracker.onHardwareStep() }
        assertFalse(tracker.complete())
        tracker.onHardwareStep()
        tracker.onHardwareStep()
        assertTrue(tracker.complete())
        assertEquals(3, tracker.steps)
    }

    @Test fun onePickupJoltCannotSatisfyFallbackMovement() {
        val tracker = MovementProgressTracker(3)
        tracker.onAccelerometer(14f, 0f, 0f, 1_000)
        tracker.onAccelerometer(15f, 0f, 0f, 1_050)
        tracker.onAccelerometer(9.81f, 0f, 0f, 1_100)
        assertEquals(1, tracker.steps)
        assertFalse(tracker.complete())
    }

    @Test fun separatedFallbackPeaksCountAsSteps() {
        val tracker = MovementProgressTracker(3)
        repeat(3) { index ->
            val at = 1_000L + index * 400L
            tracker.onAccelerometer(14f, 0f, 0f, at)
            tracker.onAccelerometer(9.81f, 0f, 0f, at + 100)
        }
        assertTrue(tracker.complete())
    }
}
