package com.shanivar.nativeapp

import java.time.LocalTime
import java.util.UUID

/**
 * Routine V3 first-run flow (decision 0014 / handoff R3 onboarding).
 *
 * Wireframe path: Welcome → Your day (wake + intended sleep) → Optional guardrails
 * (wake alarm, first protection window + apps, honest permissions) → Routine.
 *
 * Pure page machine and plan-write helpers live here so unit tests never need Compose.
 * Activities do not appear in onboarding and never arm alarms or Focus. Saving protection
 * is not Active — device evidence decides status later.
 */
enum class RoutineOnboardingPage {
    WELCOME,
    RHYTHM,
    SETUP,
    ALARM,
    PROTECTION,
    PERMISSIONS,
    DONE,
}

data class RoutineOnboardingDraft(
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val intendedSleepTime: LocalTime = LocalTime.of(23, 0),
    val alarmEnabledChoice: Boolean = false,
    val alarmDays: Set<Int> = WindowRepeat.EVERY_DAY_DAYS,
    val alarmArmedOnDevice: Boolean = false,
    val packages: Set<String> = emptySet(),
    val window: ProtectionWindowV1? = null,
    val protectionSaved: Boolean = false,
    val error: String? = null,
)

data class RoutineOnboardingState(
    val page: RoutineOnboardingPage = RoutineOnboardingPage.WELCOME,
    val draft: RoutineOnboardingDraft = RoutineOnboardingDraft(),
    /** True once anchors were accepted into the plan (Use these times or Set this later with defaults). */
    val anchorsCommitted: Boolean = false,
)

sealed class RoutineOnboardingEvent {
    data object StartBuilding : RoutineOnboardingEvent()
    data class SetWake(val time: LocalTime) : RoutineOnboardingEvent()
    data class SetSleep(val time: LocalTime) : RoutineOnboardingEvent()
    data object UseTheseTimes : RoutineOnboardingEvent()
    data object SetRhythmLater : RoutineOnboardingEvent()
    data object OpenAlarm : RoutineOnboardingEvent()
    data object OpenProtection : RoutineOnboardingEvent()
    data object OpenPermissions : RoutineOnboardingEvent()
    data class SetAlarmEnabledChoice(val enabled: Boolean) : RoutineOnboardingEvent()
    data class SetAlarmDays(val days: Set<Int>) : RoutineOnboardingEvent()
    data class AlarmArmed(val armed: Boolean, val message: String? = null) : RoutineOnboardingEvent()
    data class TogglePackage(val packageName: String) : RoutineOnboardingEvent()
    data class SetWindowStart(val time: LocalTime) : RoutineOnboardingEvent()
    data class SetWindowEnd(val time: LocalTime) : RoutineOnboardingEvent()
    data class EnsureDefaultWindow(val id: String = UUID.randomUUID().toString()) : RoutineOnboardingEvent()
    data class ProtectionSaved(val ok: Boolean, val message: String? = null) : RoutineOnboardingEvent()
    data object BackToSetup : RoutineOnboardingEvent()
    data object GoToRoutine : RoutineOnboardingEvent()
    data object Back : RoutineOnboardingEvent()
    data class ClearError(val keep: Boolean = false) : RoutineOnboardingEvent()
}

object RoutineOnboardingMachine {
    fun reduce(state: RoutineOnboardingState, event: RoutineOnboardingEvent): RoutineOnboardingState = when (event) {
        RoutineOnboardingEvent.StartBuilding -> state.copy(page = RoutineOnboardingPage.RHYTHM, draft = state.draft.copy(error = null))
        is RoutineOnboardingEvent.SetWake -> state.copy(draft = state.draft.copy(wakeTime = event.time, error = null))
        is RoutineOnboardingEvent.SetSleep -> state.copy(draft = state.draft.copy(intendedSleepTime = event.time, error = null))
        RoutineOnboardingEvent.UseTheseTimes -> {
            validateAnchors(state.draft.wakeTime, state.draft.intendedSleepTime)?.let {
                return state.copy(draft = state.draft.copy(error = it))
            }
            state.copy(page = RoutineOnboardingPage.SETUP, anchorsCommitted = true, draft = state.draft.copy(error = null))
        }
        RoutineOnboardingEvent.SetRhythmLater ->
            state.copy(page = RoutineOnboardingPage.SETUP, anchorsCommitted = true, draft = state.draft.copy(error = null))
        RoutineOnboardingEvent.OpenAlarm -> state.copy(page = RoutineOnboardingPage.ALARM, draft = state.draft.copy(error = null))
        RoutineOnboardingEvent.OpenProtection -> {
            val withWindow = if (state.draft.window == null) {
                state.draft.copy(window = defaultFirstWindow(), error = null)
            } else state.draft.copy(error = null)
            state.copy(page = RoutineOnboardingPage.PROTECTION, draft = withWindow)
        }
        RoutineOnboardingEvent.OpenPermissions -> state.copy(page = RoutineOnboardingPage.PERMISSIONS, draft = state.draft.copy(error = null))
        is RoutineOnboardingEvent.SetAlarmEnabledChoice ->
            state.copy(draft = state.draft.copy(alarmEnabledChoice = event.enabled, error = null))
        is RoutineOnboardingEvent.SetAlarmDays ->
            state.copy(draft = state.draft.copy(alarmDays = event.days, error = null))
        is RoutineOnboardingEvent.AlarmArmed ->
            state.copy(
                page = if (event.armed) RoutineOnboardingPage.SETUP else state.page,
                draft = state.draft.copy(
                    alarmArmedOnDevice = event.armed,
                    alarmEnabledChoice = event.armed || state.draft.alarmEnabledChoice,
                    error = event.message,
                ),
            )
        is RoutineOnboardingEvent.TogglePackage -> {
            val pkgs = if (event.packageName in state.draft.packages) state.draft.packages - event.packageName
            else state.draft.packages + event.packageName
            state.copy(draft = state.draft.copy(packages = pkgs, protectionSaved = false, error = null))
        }
        is RoutineOnboardingEvent.SetWindowStart -> state.copy(
            draft = state.draft.copy(
                window = (state.draft.window ?: defaultFirstWindow()).copy(startTime = event.time),
                protectionSaved = false,
                error = null,
            ),
        )
        is RoutineOnboardingEvent.SetWindowEnd -> state.copy(
            draft = state.draft.copy(
                window = (state.draft.window ?: defaultFirstWindow()).copy(endTime = event.time),
                protectionSaved = false,
                error = null,
            ),
        )
        is RoutineOnboardingEvent.EnsureDefaultWindow -> state.copy(
            draft = state.draft.copy(window = state.draft.window ?: defaultFirstWindow(event.id), error = null),
        )
        is RoutineOnboardingEvent.ProtectionSaved ->
            if (event.ok) state.copy(
                page = RoutineOnboardingPage.PERMISSIONS,
                draft = state.draft.copy(protectionSaved = true, error = event.message),
            ) else state.copy(draft = state.draft.copy(error = event.message ?: "Could not save protection."))
        RoutineOnboardingEvent.BackToSetup -> state.copy(page = RoutineOnboardingPage.SETUP, draft = state.draft.copy(error = null))
        RoutineOnboardingEvent.GoToRoutine -> state.copy(page = RoutineOnboardingPage.DONE, draft = state.draft.copy(error = null))
        RoutineOnboardingEvent.Back -> state.copy(
            page = when (state.page) {
                RoutineOnboardingPage.RHYTHM -> RoutineOnboardingPage.WELCOME
                RoutineOnboardingPage.SETUP -> RoutineOnboardingPage.RHYTHM
                RoutineOnboardingPage.ALARM, RoutineOnboardingPage.PROTECTION -> RoutineOnboardingPage.SETUP
                RoutineOnboardingPage.PERMISSIONS -> RoutineOnboardingPage.SETUP
                else -> state.page
            },
            draft = state.draft.copy(error = null),
        )
        is RoutineOnboardingEvent.ClearError -> state.copy(draft = state.draft.copy(error = if (event.keep) state.draft.error else null))
    }

    fun validateAnchors(wake: LocalTime?, sleep: LocalTime?): String? = when {
        wake == null || sleep == null -> "Choose wake and intended sleep times."
        wake == sleep -> "Choose different wake and sleep times."
        else -> null
    }

    /** Anchors only — never touches windows/packages/activities, so never arms Focus. */
    fun planWithAnchors(current: RoutinePlanV1, draft: RoutineOnboardingDraft): RoutinePlanV1 =
        current.copy(wakeTime = draft.wakeTime, intendedSleepTime = draft.intendedSleepTime)

    /**
     * First protection window + shared app set. Returns a validation message when the draft is
     * incomplete; otherwise the plan to save through [ProtectionSyncingRoutinePlanRepository].
     */
    fun protectionPlanOrError(current: RoutinePlanV1, draft: RoutineOnboardingDraft): Pair<RoutinePlanV1?, String?> {
        val window = draft.window ?: return null to "Add a protection window, or skip this step."
        val editor = ProtectionEditorState(windows = listOf(window), packages = draft.packages)
        editor.validate()?.let { return null to it }
        return editor.applyTo(current) to null
    }

    fun defaultFirstWindow(id: String = UUID.randomUUID().toString()): ProtectionWindowV1 =
        ProtectionWindowV1(
            id = id,
            startTime = LocalTime.of(18, 0),
            endTime = LocalTime.of(19, 0),
            startDays = WindowRepeat.EVERY_DAY_DAYS,
            enabled = true,
        )

    const val LOCAL_WAKE_ALARM_ID = "local-wake"
}

/**
 * Whether first-run should use the V3 Routine onboarding.
 * Production path: [BuildConfig.ROUTINE_V3]. Debug path: SharedPreferences `debug_v3_onboarding`
 * on debug builds only, so a gated false default can still exercise the flow locally.
 */
fun shouldUseRoutineV3Onboarding(routineV3: Boolean, debugBuild: Boolean, debugPrefEnabled: Boolean): Boolean =
    routineV3 || (debugBuild && debugPrefEnabled)
