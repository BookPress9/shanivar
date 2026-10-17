package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the escalation curve and the safety ceilings. The audio/vibration plumbing itself needs a
 * real device, but the curve and the caps are pure arithmetic and are exactly the parts where a
 * quiet mistake would either fail to wake someone or refuse to stop.
 */
class AlarmRingerTest {

    @Test fun startsAudibleButNotAtFullVolume() {
        // Opening at full blast is startling and unnecessary; opening too quietly fails the one job.
        val start = AlarmRinger.escalationVolumeAt(0)
        assertEquals(AlarmRinger.START_VOLUME, start, 0.001f)
        assertTrue("should be clearly audible from the first second", start >= 0.3f)
        assertTrue("should leave headroom to escalate into", start < 1f)
    }

    @Test fun volumeIncreasesWithEachStep() {
        val first = AlarmRinger.escalationVolumeAt(AlarmRinger.ESCALATION_STEP_MILLIS)
        val second = AlarmRinger.escalationVolumeAt(AlarmRinger.ESCALATION_STEP_MILLIS * 2)
        assertTrue("each step must be perceptibly louder than the last", second > first)
        assertTrue(first > AlarmRinger.escalationVolumeAt(0))
    }

    @Test fun reachesFullVolumeWithinTwoMinutes() {
        // Someone genuinely asleep has to be woken; an escalation that takes too long to arrive is
        // indistinguishable from an alarm that failed.
        assertEquals(1f, AlarmRinger.escalationVolumeAt(120_000), 0.001f)
    }

    @Test fun neverExceedsFullVolume() {
        // Guards against a runaway multiplier being handed to MediaPlayer.setVolume.
        assertEquals(1f, AlarmRinger.escalationVolumeAt(60 * 60 * 1000), 0.001f)
        assertTrue(AlarmRinger.escalationVolumeAt(Long.MAX_VALUE / 2) <= 1f)
    }

    @Test fun handlesNonsensicalElapsedTimeWithoutGoingSilent() {
        // A clock adjustment mid-alarm must not produce a negative volume and mute the alarm.
        assertEquals(AlarmRinger.START_VOLUME, AlarmRinger.escalationVolumeAt(-5_000), 0.001f)
    }

    @Test fun hardCapEndsTheAlarmAndOutlastsNothing() {
        // The cap has to be long enough to be a real alarm, short enough not to be a siege.
        assertTrue(AlarmRinger.HARD_CAP_MILLIS >= 10 * 60 * 1000L)
        assertTrue(AlarmRinger.HARD_CAP_MILLIS <= 20 * 60 * 1000L)
    }

    @Test fun wakeLockOutlivesTheHardCap() {
        // If the wake lock expired first, the device could sleep mid-alarm and the cap would never
        // fire - leaving the ringing state un-torn-down.
        assertTrue(
            "wake lock must not expire before the alarm's own deadline",
            AlarmRinger.WAKE_LOCK_TIMEOUT_MILLIS > AlarmRinger.HARD_CAP_MILLIS,
        )
    }
}
