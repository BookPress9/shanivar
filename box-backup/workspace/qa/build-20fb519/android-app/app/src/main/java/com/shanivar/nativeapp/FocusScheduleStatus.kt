package com.shanivar.nativeapp

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * The states a recurring focus rule moves through (decision 0014, "Reliability and authority").
 *
 * `Saved`, `Ready` and `Active` are deliberately distinct conditions (DEV-3 / FOCUS-6): a saved form
 * is not proof that protection is armed, and an armed `PendingIntent` is not proof that protection
 * is on. Only [ACTIVE] claims enforcement, and only [FocusScheduleStatusResolver] may produce it.
 */
enum class FocusRuleState { DRAFT, SAVED_LOCAL, NEEDS_DEVICE_ACCESS, READY, ACTIVE, NEEDS_ATTENTION, OFF }

/** Which Android access is missing, so the repair step can name the exact setting. */
enum class FocusDeviceAccess { EXACT_ALARM, USAGE_ACCESS, OVERLAY }

/**
 * Everything the resolver may look at. All of it is read from this device, never from a server:
 * the account schedule only says what the person asked for, not what the phone is doing.
 */
data class FocusScheduleInputs(
    val now: ZonedDateTime,
    /** The person's saved rule (account copy), or null when nothing has been saved yet. */
    val savedRule: FocusScheduler.Rule?,
    val enabled: Boolean,
    /** The rule [FocusScheduler] persisted when it last armed a native trigger, or null. */
    val armedRule: FocusScheduler.Rule?,
    /** The instant of the native trigger [FocusScheduler] last armed, or null. */
    val nextTriggerAtEpochMillis: Long?,
    val hasExactAlarm: Boolean,
    val hasUsageAccess: Boolean,
    val hasOverlay: Boolean,
    /** The guard's own record ([LocalProtectionController.active]): the engine confirmation. */
    val localProtection: LocalProtection?,
)

data class FocusScheduleStatus(
    val state: FocusRuleState,
    /** Next boundary this device armed; set for [FocusRuleState.READY] (and kept when known). */
    val nextChangeEpochMillis: Long? = null,
    /** End of protection the guard confirms; set only for [FocusRuleState.ACTIVE]. */
    val activeUntilEpochMillis: Long? = null,
    val missingAccess: Set<FocusDeviceAccess> = emptySet(),
    val problem: Problem? = null,
) {
    enum class Problem {
        /** Saved, but no native trigger for this rule exists on this device. */
        NOT_ARMED,
        /** The armed trigger's instant passed without advancing - the boundary was missed. */
        MISSED_BOUNDARY,
        /** Inside the window, but the guard is not protecting the chosen apps. */
        WINDOW_OPEN_NOT_PROTECTING,
    }

    /** Status word for the card. Never green for anything but ACTIVE / READY. */
    val label: String get() = when (state) {
        FocusRuleState.DRAFT -> "Not configured"
        FocusRuleState.SAVED_LOCAL -> "Saved"
        FocusRuleState.NEEDS_DEVICE_ACCESS -> "Needs device access"
        FocusRuleState.READY -> "Ready"
        FocusRuleState.ACTIVE -> "Active"
        FocusRuleState.NEEDS_ATTENTION -> "Needs attention"
        FocusRuleState.OFF -> "Off"
    }
}

/**
 * Derives a focus rule's displayed state from device evidence only (DEV-3 / FOCUS-6).
 *
 * Order matters and is the whole point:
 * 1. Nothing saved → DRAFT. Disabled → OFF.
 * 2. Missing exact-alarm, Usage access or overlay → NEEDS_DEVICE_ACCESS, even if a trigger is armed
 *    or a guard record exists: without them the boundary cannot fire, or the guard cannot block.
 * 3. ACTIVE only when the guard's own record ([LocalProtection]) is live *and* covers this rule's
 *    apps *while* one of its occurrences is open. A saved rule, an armed trigger or a server
 *    response is never enough.
 * 4. Inside the window without that confirmation → NEEDS_ATTENTION, never ACTIVE.
 * 5. No trigger armed for this rule on this device → SAVED_LOCAL ("saved, not scheduled here").
 * 6. Armed trigger already in the past → NEEDS_ATTENTION (missed boundary).
 * 7. Otherwise READY, with the armed next-change instant.
 */
object FocusScheduleStatusResolver {
    /** An exact alarm can be delivered a little late; only call it missed after this. */
    const val MISSED_BOUNDARY_GRACE_MS = 5 * 60_000L

    fun resolve(input: FocusScheduleInputs): FocusScheduleStatus {
        val rule = input.savedRule ?: return FocusScheduleStatus(FocusRuleState.DRAFT)
        if (!input.enabled) return FocusScheduleStatus(FocusRuleState.OFF)

        val nowMillis = input.now.toInstant().toEpochMilli()
        val armed = input.armedRule?.takeIf { it.sameRuleAs(rule) } != null
        val nextTrigger = input.nextTriggerAtEpochMillis.takeIf { armed }

        val missing = buildSet {
            if (!input.hasExactAlarm) add(FocusDeviceAccess.EXACT_ALARM)
            if (!input.hasUsageAccess) add(FocusDeviceAccess.USAGE_ACCESS)
            if (!input.hasOverlay) add(FocusDeviceAccess.OVERLAY)
        }
        if (missing.isNotEmpty()) {
            return FocusScheduleStatus(FocusRuleState.NEEDS_DEVICE_ACCESS, nextTrigger, missingAccess = missing)
        }

        val windowEnd = FocusWindowRecurrence.activeWindowEnd(rule.days, rule.startTime, rule.endTime, input.now)
        if (windowEnd != null) {
            val guard = input.localProtection?.takeIf {
                it.isActiveAt(nowMillis) && it.packages.containsAll(rule.blockPackages)
            }
            return if (guard != null) {
                FocusScheduleStatus(FocusRuleState.ACTIVE, nextTrigger, activeUntilEpochMillis = guard.endsAtEpochMillis)
            } else {
                FocusScheduleStatus(FocusRuleState.NEEDS_ATTENTION, nextTrigger,
                    problem = FocusScheduleStatus.Problem.WINDOW_OPEN_NOT_PROTECTING)
            }
        }

        if (nextTrigger == null) {
            return FocusScheduleStatus(FocusRuleState.SAVED_LOCAL, problem = FocusScheduleStatus.Problem.NOT_ARMED)
        }
        if (nextTrigger + MISSED_BOUNDARY_GRACE_MS < nowMillis) {
            return FocusScheduleStatus(FocusRuleState.NEEDS_ATTENTION, nextTrigger,
                problem = FocusScheduleStatus.Problem.MISSED_BOUNDARY)
        }
        return FocusScheduleStatus(FocusRuleState.READY, nextTrigger)
    }

    /** The armed copy must be the same rule, not just the same id: an edit that failed to re-arm
     * leaves the old times armed, and calling that "Ready" would describe the wrong window. */
    private fun FocusScheduler.Rule.sameRuleAs(other: FocusScheduler.Rule): Boolean =
        scheduleId == other.scheduleId && days == other.days && startTime == other.startTime &&
            endTime == other.endTime && blockPackages == other.blockPackages
}

/** Builds the resolver's rule from the account copy; null when its times do not parse. */
internal fun RemoteFocusSchedule.toSchedulerRule(): FocusScheduler.Rule? = runCatching {
    FocusScheduler.Rule(id, daysOfWeek, LocalTime.parse(startTime), LocalTime.parse(endTime), blockedPackages)
}.getOrNull()

/**
 * One honest sentence for the card: state word first, then the next step. [formatTime] renders an
 * epoch-millis instant in the device's zone. Kept out of Compose so the wording is unit-testable.
 */
internal fun FocusScheduleStatus.describe(formatTime: (Long) -> String): String = when (state) {
    FocusRuleState.DRAFT -> "Not configured. Set a focus schedule to protect time on this device."
    FocusRuleState.OFF -> "Off on this device."
    FocusRuleState.ACTIVE -> "Active · protecting your chosen apps until ${formatTime(activeUntilEpochMillis!!)}."
    FocusRuleState.READY -> "Ready on this device · next change ${formatTime(nextChangeEpochMillis!!)}. Protection is not on yet."
    FocusRuleState.SAVED_LOCAL -> "Saved, but not scheduled on this device. Turn it on and save to schedule it here."
    FocusRuleState.NEEDS_DEVICE_ACCESS -> "Needs device access. Allow " + missingAccess.sortedBy { it.ordinal }.joinToString(", ") {
        when (it) {
            FocusDeviceAccess.EXACT_ALARM -> "Alarms & reminders"
            FocusDeviceAccess.USAGE_ACCESS -> "Usage access"
            FocusDeviceAccess.OVERLAY -> "Display over other apps"
        }
    } + " so this schedule can protect your apps."
    FocusRuleState.NEEDS_ATTENTION -> when (problem) {
        FocusScheduleStatus.Problem.WINDOW_OPEN_NOT_PROTECTING ->
            "Needs attention. This window is open, but this device is not protecting it. It starts at the next window, or start focus above to protect now."
        FocusScheduleStatus.Problem.MISSED_BOUNDARY ->
            "Needs attention. A scheduled change was missed on this device. Edit and save the schedule to re-arm it."
        else -> "Needs attention. Edit and save the schedule to set it up on this device again."
    }
}
