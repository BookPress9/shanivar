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
    val focusMinutes: Int = 30,
    val movementDestination: String = "morning_light",
    val stepTarget: Int = 18,
    val sceneConfirmation: Boolean = false,
    val accessibleMovement: Boolean = false,
    val whatsappTime: String = "08:00",
    val whatsappTone: String = "Direct",
    val dailyNoteEnabled: Boolean = true,
    val blockedPackages: Set<String> = DisciplineTargets.DEFAULT_BLOCKED_PACKAGES,
    // MVP streamline task, package 2: the chosen-activity preset from onboarding's PLAN page.
    // Deliberately local-only (not part of the /discipline/preferences PUT body) - it is display
    // copy for "what is this focus window for," not a behavior the backend needs to know, so
    // adding it required zero backend schema change.
    val intentionLabel: String = FocusPresets.DEFAULT.label,
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
        awake in 6 * 3600..20 * 3600 && focusMinutes in 5..120 && stepTarget in 1..200 &&
            movementDestination.isNotBlank() && blockedPackages.size <= 50
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

/** A curated toggle list for the Focus card's app picker (MVP streamline task, package 3:
 * "selected app set... exclude system/essential communication defaults"). Deliberately a fixed,
 * known-distracting-apps allowlist rather than a full installed-app picker: listing every
 * installed package would also surface system and communication apps that must then be actively
 * excluded, which is a materially larger feature than a v1 app-lock target list needs. Mirrors
 * backend/src/routes/discipline.js's DEFAULT_LOCK_TARGETS package ids - keep both lists in sync if
 * either changes. */
data class DistractingApp(val packageName: String, val label: String)
object KnownDistractingApps {
    val ALL = listOf(
        DistractingApp("com.instagram.android", "Instagram"),
        DistractingApp("com.google.android.youtube", "YouTube"),
        DistractingApp("com.zhiliaoapp.musically", "TikTok"),
        DistractingApp("com.facebook.katana", "Facebook"),
        DistractingApp("com.twitter.android", "X (Twitter)"),
        DistractingApp("com.snapchat.android", "Snapchat"),
    )
}

/** Editable presets offered on onboarding's PLAN page and the Focus card (MVP streamline task,
 * package 2/3: "editable reading, family time, one task and quiet-time presets"). A preset only
 * suggests an activity label and a starting duration - per the task spec, presets differ in
 * suggested duration, never in enforcement strength; every preset uses the same focus engine and
 * the same package-list contract. */
data class FocusPreset(val label: String, val suggestedMinutes: Int)
object FocusPresets {
    val ALL = listOf(
        FocusPreset("Reading", 30),
        FocusPreset("Family time", 45),
        FocusPreset("One task", 25),
        FocusPreset("Quiet time", 20),
    )
    val DEFAULT = ALL.first()
}
