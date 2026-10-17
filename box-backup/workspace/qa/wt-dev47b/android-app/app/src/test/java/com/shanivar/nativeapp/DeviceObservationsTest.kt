package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceObservationsTest {
    private fun obs(rev: Int, at: Long, state: FocusRuleState) =
        DeviceObservation("d1", rev, true, true, true, state, at)

    @Test fun unknownUntilDeviceReports() {
        assertNull(ObservationLedger(1, true).effectiveState())
    }

    @Test fun staleAndOutOfOrderIgnored() {
        val l = ObservationLedger(1, true)
        assertTrue(l.accept(obs(1, 100, FocusRuleState.ACTIVE)))
        assertFalse(l.accept(obs(1, 90, FocusRuleState.NEEDS_ATTENTION)))
        assertFalse(l.accept(obs(1, 100, FocusRuleState.OFF)))
        assertEquals(FocusRuleState.ACTIVE, l.effectiveState())
    }

    @Test fun oldConfigRevisionNeverApplies() {
        val l = ObservationLedger(1, true)
        l.accept(obs(1, 100, FocusRuleState.ACTIVE))
        l.configChanged(2, true)
        assertNull(l.effectiveState())
        assertFalse(l.accept(obs(1, 200, FocusRuleState.ACTIVE)))
        assertTrue(l.accept(obs(2, 150, FocusRuleState.READY)))
    }

    @Test fun userDisabledStaysOffWhateverObservationSays() {
        val l = ObservationLedger(3, true)
        l.configChanged(4, false)
        l.accept(obs(4, 500, FocusRuleState.ACTIVE))
        assertEquals(FocusRuleState.OFF, l.effectiveState())
    }

    @Test fun alarmObservationStates() {
        assertEquals(AlarmObservedStatus.OFF, DeviceObservationMapper.alarm(1, false, true, 5, null, 0).status)
        assertEquals(AlarmObservedStatus.PERMISSION_REQUIRED, DeviceObservationMapper.alarm(1, true, false, 5, null, 0).status)
        assertEquals(AlarmObservedStatus.FAILED, DeviceObservationMapper.alarm(1, true, true, null, "boom", 0).status)
        val s = DeviceObservationMapper.alarm(1, true, true, 99, null, 0)
        assertEquals(AlarmObservedStatus.SCHEDULED, s.status); assertEquals(99L, s.nextFireAtMillis)
        assertEquals(AlarmObservedStatus.CONFIGURED, DeviceObservationMapper.alarm(1, true, true, null, null, 0).status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun scheduledWithoutAcceptedTimeRejected() {
        AlarmObservation(1, AlarmObservedStatus.SCHEDULED, null, null, 0)
    }

    @Test fun accessIsReadOnResume() {
        assertEquals(AccessResume.Result.GRANTED, AccessResume.classify(AccessResume.Access(true, true)))
        assertEquals(AccessResume.Result.PARTIAL, AccessResume.classify(AccessResume.Access(true, false)))
        assertEquals(AccessResume.Result.DENIED, AccessResume.classify(AccessResume.Access(false, false)))
    }
}
