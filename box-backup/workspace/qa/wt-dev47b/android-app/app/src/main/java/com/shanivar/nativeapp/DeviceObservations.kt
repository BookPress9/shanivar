package com.shanivar.nativeapp

/*
 * Initiation V1, build step 3 (HANDOFF.md:101), software part: device-truth observations.
 * The existing resolver ([FocusScheduleStatus], [FocusRuleState]) stays the only source of device state.
 * This file turns its output into the HANDOFF DeviceObservation / AlarmObservation records that the
 * backend may mirror (POST /v1/devices/:id/observations, not built yet), and guards the rules:
 *  - a server copy can never prove device enforcement;
 *  - stale or out-of-order observations are ignored;
 *  - an observation for an old configRevision never changes the current status;
 *  - a user-disabled rule is OFF locally, whatever any observation says.
 * Physical-device checks (denied/partial/revoked access, ring, reboot, DST, offline exit) still need real phones.
 */

enum class AlarmObservedStatus { OFF, CONFIGURED, PERMISSION_REQUIRED, SCHEDULED, FAILED }

data class AlarmObservation(
    val configRevision: Int,
    val status: AlarmObservedStatus,
    val nextFireAtMillis: Long?,
    val reason: String?,
    val observedAtMillis: Long,
) {
    init { require(status != AlarmObservedStatus.SCHEDULED || nextFireAtMillis != null) { "SCHEDULED needs the scheduler's accepted next fire time" } }
}

data class DeviceObservation(
    val deviceId: String,
    val configRevision: Int,
    val usageAccess: Boolean,
    val overlayAccess: Boolean,
    val alarmCapability: Boolean,
    val effectiveProtectionState: FocusRuleState,
    val observedAtMillis: Long,
)

object DeviceObservationMapper {
    /** Uses only device inputs; never infers ACTIVE from a saved or synced rule. */
    fun fromFocus(deviceId: String, configRevision: Int, inputs: FocusScheduleInputs, status: FocusScheduleStatus): DeviceObservation =
        DeviceObservation(
            deviceId = deviceId,
            configRevision = configRevision,
            usageAccess = inputs.hasUsageAccess,
            overlayAccess = inputs.hasOverlay,
            alarmCapability = inputs.hasExactAlarm,
            effectiveProtectionState = if (!inputs.enabled) FocusRuleState.OFF else status.state,
            observedAtMillis = inputs.now.toInstant().toEpochMilli(),
        )

    /** Scheduler acceptance is reported as SCHEDULED, which is not the same as a demonstrated ring. */
    fun alarm(configRevision: Int, desiredEnabled: Boolean, hasExactAlarm: Boolean, acceptedNextFireAt: Long?, error: String?, now: Long): AlarmObservation = when {
        !desiredEnabled -> AlarmObservation(configRevision, AlarmObservedStatus.OFF, null, null, now)
        !hasExactAlarm -> AlarmObservation(configRevision, AlarmObservedStatus.PERMISSION_REQUIRED, null, "Alarms & reminders access is off", now)
        error != null -> AlarmObservation(configRevision, AlarmObservedStatus.FAILED, null, error, now)
        acceptedNextFireAt != null -> AlarmObservation(configRevision, AlarmObservedStatus.SCHEDULED, acceptedNextFireAt, null, now)
        else -> AlarmObservation(configRevision, AlarmObservedStatus.CONFIGURED, null, null, now)
    }
}

/** Holds the latest accepted observation for the current config. */
class ObservationLedger(private var currentRevision: Int, private var userEnabled: Boolean) {
    var latest: DeviceObservation? = null
        private set

    fun configChanged(revision: Int, enabled: Boolean) {
        currentRevision = revision; userEnabled = enabled
        latest = null // old observations say nothing about the new config
    }

    /** Returns true if accepted. */
    fun accept(o: DeviceObservation): Boolean {
        if (o.configRevision != currentRevision) return false
        val prev = latest
        if (prev != null && o.observedAtMillis <= prev.observedAtMillis) return false
        latest = o
        return true
    }

    /** What the UI may show. Unknown until the device has reported; OFF wins when the person turned it off. */
    fun effectiveState(): FocusRuleState? = when {
        !userEnabled -> FocusRuleState.OFF
        else -> latest?.effectiveProtectionState
    }
}

/** HANDOFF step 3: tapping "Open settings" grants nothing; only the re-read on resume counts. */
object AccessResume {
    data class Access(val usage: Boolean, val overlay: Boolean)
    enum class Result { GRANTED, PARTIAL, DENIED }
    fun classify(afterResume: Access): Result = when {
        afterResume.usage && afterResume.overlay -> Result.GRANTED
        afterResume.usage || afterResume.overlay -> Result.PARTIAL
        else -> Result.DENIED
    }
}
