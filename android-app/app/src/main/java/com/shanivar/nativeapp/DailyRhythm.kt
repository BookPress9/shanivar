package com.shanivar.nativeapp

import java.time.LocalTime

enum class DayMoment(val title: String, val action: String) {
    MORNING("Make room for yourself.", "Begin your practice"),
    FOCUS("Give one thing your attention.", "Plan a focus window"),
    EVENING("Let the day settle.", "Take a quiet moment"),
    NIGHT("Make space for rest.", "Set your next wake-up")
}

data class Rhythm(
    val wakeTime: String = "07:00",
    val quietTime: String = "21:30",
    val focusMinutes: Int = 25,
    val movement: String = "Scan a QR code",
    val whatsappTime: String = "08:00",
    val whatsappTone: String = "Direct",
) {
    fun moment(now: LocalTime): DayMoment {
        val wake = LocalTime.parse(wakeTime)
        val elapsed = (now.toSecondOfDay() - wake.toSecondOfDay() + 86400) % 86400
        val awake = (LocalTime.parse(quietTime).toSecondOfDay() - wake.toSecondOfDay() + 86400) % 86400
        return when {
            elapsed >= awake -> DayMoment.NIGHT
            elapsed < 3 * 3600 -> DayMoment.MORNING
            elapsed >= (awake - 3 * 3600).coerceAtLeast(3 * 3600) -> DayMoment.EVENING
            else -> DayMoment.FOCUS
        }
    }
    fun valid(): Boolean = runCatching {
        val awake = (LocalTime.parse(quietTime).toSecondOfDay() - LocalTime.parse(wakeTime).toSecondOfDay() + 86400) % 86400
        LocalTime.parse(whatsappTime)
        awake in 6 * 3600..20 * 3600 && focusMinutes in 5..120
    }.getOrDefault(false)
}

// UI seams for Claude's native engine. Only engine-confirmed states may claim protection.
enum class ProtectionState { UNAVAILABLE, NEEDS_PERMISSION, OFF, ACTIVE }

// focusEndsAtEpochMillis/protectedPackages are populated only when protection == ACTIVE; NATIVE_HANDOFF.md
// asks the UI to be able to show "Focus protected . Xm remaining" and which app(s) are covered, not just a
// boolean. Defaults keep this source-compatible with any existing UNAVAILABLE-style construction.
data class DisciplineStatus(
    val protection: ProtectionState,
    val alarmScheduled: Boolean,
    val focusEndsAtEpochMillis: Long? = null,
    val protectedPackages: Set<String> = emptySet(),
)
interface DisciplineGateway {
    suspend fun status(): DisciplineStatus
    suspend fun startFocus(minutes: Int, packageNames: Set<String>): DisciplineStatus
    suspend fun releaseFocus(): DisciplineStatus
}

/** v1 ships one blocking target (Instagram) but the set is plain data, not hardcoded through the
 * service - a second target is a config-list change, not a code change. See platform-research.md's
 * "First blocking target: Instagram" justification and discipline_engine_squad.md's locked scope. */
object DisciplineTargets {
    val DEFAULT_BLOCKED_PACKAGES: Set<String> = setOf("com.instagram.android")
}
