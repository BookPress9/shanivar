package com.shanivar.nativeapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Properties
import java.util.UUID

/*
 * Initiation V1 screens state machine (Jira DEV-26). Mirrors android-app/wireframes/initiation-v1/app.js
 * screen by screen, but every consequence is real: draft persistence (DEV-24 step 1), the Routine plan
 * repository (step 2), device truth for alarm and protection (step 3), practice attempt records (step 4)
 * and the Living Orbit (step 5). Pure Kotlin; Android effects come through [InitiationPorts].
 */

enum class InitiationScreen(val title: String) {
    ARRIVAL("The Arrival"), IDENTITY("Your Identity"), AUTH("Authentication sheet"), PROPOSITION("The Proposition"),
    ANCHORS("Two Anchors"), ALARM("Your First Commitment"), APPS("Choose What You Protect"), WINDOW("Your First Boundary"),
    ACCESS("Device Access"), FOUNDATION("The First Foundation"), ROUTINE("Your Routine"), EDIT("Edit Routine"),
    SADHANA("First Sadhana"), PRACTICE("Practice player"), RETURNED("You Returned"), ORBIT("The Living Orbit"), WEEK("Your First Seven Days");

    val onboardingIndex: Int get() = ordinal
    val inOnboarding: Boolean get() = ordinal in 1..9
    val hasTabs: Boolean get() = ordinal >= 10
}

/** Wireframe repeat labels mapped to day sets (0=Sun … 6=Sat). */
object InitiationRepeat {
    val OPTIONS = listOf("Weekdays", "Every day", "Weekends")
    fun days(label: String): Set<Int> = when (label) {
        "Every day" -> (0..6).toSet(); "Weekends" -> setOf(0, 6); else -> (1..5).toSet()
    }
}

data class InitiationActivity(val id: String, val name: String, val time: LocalTime, val duration: Int, val repeat: String)

data class InitiationWindow(
    val start: LocalTime = LocalTime.of(9, 0), val end: LocalTime = LocalTime.of(10, 0),
    val repeat: String = "Weekdays", val saved: Boolean = false, val enabled: Boolean = false,
)

/** Wireframe alarm values: off | scheduled | permission_required | failed. */
enum class InitiationAlarm { OFF, SCHEDULED, PERMISSION_REQUIRED, FAILED }

data class InitiationUi(
    val screen: InitiationScreen = InitiationScreen.ARRIVAL,
    val wake: LocalTime = LocalTime.of(7, 0),
    val sleep: LocalTime = LocalTime.of(23, 0),
    val practiceTime: LocalTime = LocalTime.of(7, 5),
    val routineSaved: Boolean = false,
    val alarm: InitiationAlarm = InitiationAlarm.OFF,
    val selected: Set<String> = emptySet(),
    val window: InitiationWindow = InitiationWindow(),
    val activities: List<InitiationActivity> = emptyList(),
    val firstDate: LocalDate? = null,
    val message: String? = null,
    val emailStage: Boolean = false,
)

interface InitiationPorts {
    suspend fun saveRoutine(plan: RoutinePlanV1): Result<Unit>
    fun currentPlan(): RoutinePlanV1
    /** Schedules the wake alarm with the existing engine; returns the device's answer. */
    fun scheduleWakeAlarm(wake: LocalTime, days: Set<Int>): InitiationAlarm
    fun cancelWakeAlarm()
    fun access(): AccessResume.Access
    /** Device-resolved state for the saved windows, or null if unknown. */
    fun protectionState(): FocusRuleState?
    fun stopProtectionNow()
    fun installedApps(): List<ProtectionAppOption>
    fun today(): LocalDate
    fun wallNow(): Long
    fun mono(): Long
}

/** Persists the screen state that is not part of the draft or the Routine plan. */
object InitiationUiCodec {
    fun encode(u: InitiationUi, orbit: List<OrbitEvent>, attempt: PracticeAttempt?): String {
        val p = Properties()
        p["v"] = "1"; p["screen"] = u.screen.name; p["wake"] = u.wake.toString(); p["sleep"] = u.sleep.toString()
        p["practice"] = u.practiceTime.toString(); p["routineSaved"] = u.routineSaved.toString(); p["alarm"] = u.alarm.name
        p["selected"] = u.selected.joinToString(",")
        with(u.window) { p["win"] = "$start|$end|$repeat|$saved|$enabled" }
        p["activities"] = u.activities.joinToString("\n") { "${it.id}|${it.name.replace("|", "/")}|${it.time}|${it.duration}|${it.repeat}" }
        u.firstDate?.let { p["firstDate"] = it.toString() }
        p["orbit"] = orbit.joinToString("\n") { "${it.id}|${it.type}|${it.occurrenceDate}|${it.source}" }
        attempt?.let { p["attempt"] = "${it.attemptId}|${it.contentVersion}|${it.startedAtMillis}|${it.activeMillis}|${it.outcome}|${it.endedAtMillis ?: ""}" }
        return StringWriter().also { p.store(it, null) }.toString()
    }

    data class Decoded(val ui: InitiationUi, val orbit: List<OrbitEvent>, val attempt: PracticeAttempt?)

    fun decode(raw: String?): Decoded? = runCatching {
        if (raw.isNullOrBlank()) return null
        val p = Properties().apply { load(StringReader(raw)) }
        if (p.getProperty("v") != "1") return null
        fun lines(k: String) = p.getProperty(k, "").split("\n").filter { it.isNotBlank() }
        val w = p.getProperty("win").split("|")
        Decoded(
            InitiationUi(
                screen = InitiationScreen.valueOf(p.getProperty("screen")),
                wake = LocalTime.parse(p.getProperty("wake")), sleep = LocalTime.parse(p.getProperty("sleep")),
                practiceTime = LocalTime.parse(p.getProperty("practice")),
                routineSaved = p.getProperty("routineSaved").toBoolean(),
                alarm = InitiationAlarm.valueOf(p.getProperty("alarm")),
                selected = p.getProperty("selected", "").split(",").filter { it.isNotBlank() }.toSet(),
                window = InitiationWindow(LocalTime.parse(w[0]), LocalTime.parse(w[1]), w[2], w[3].toBoolean(), w[4].toBoolean()),
                activities = lines("activities").map { val a = it.split("|"); InitiationActivity(a[0], a[1], LocalTime.parse(a[2]), a[3].toInt(), a[4]) },
                firstDate = p.getProperty("firstDate")?.let(LocalDate::parse),
            ),
            lines("orbit").map { val o = it.split("|"); OrbitEvent(o[0], OrbitEventType.valueOf(o[1]), LocalDate.parse(o[2]), OrbitSource.valueOf(o[3])) },
            p.getProperty("attempt")?.split("|")?.let { a ->
                PracticeAttempt(a[0], a[1], a[2].toLong(), a[3].toLong(), AttemptOutcome.valueOf(a[4]), a[5].takeIf { it.isNotEmpty() }?.toLong())
            },
        )
    }.getOrNull()
}

class InitiationController(
    private val draftRepo: OnboardingDraftRepository,
    private val uiStore: DraftStore,
    private val ports: InitiationPorts,
    private val auth: InitiationAuthCoordinator = InitiationAuthCoordinator(draftRepo),
    /** DEV-47: leaving Proposition as a guest starts the silent guest sign-in (AppSync.onGuestChosen). Never blocks. */
    private val onGuestReady: () -> Unit = {},
) {
    /** Compose-observable: every assignment notifies (neverEqualPolicy), so screens recompose after any action. */
    private var uiState by mutableStateOf(InitiationUi(), neverEqualPolicy())
    val ui: InitiationUi get() = uiState
    val draft: OnboardingDraft get() { uiState; return draftRepo.draft }
    val orbit: OrbitLog
    val practice: PracticeAttemptTracker
    /** Last practice was completed (true) or ended early (false). */
    val lastCompleted: Boolean get() = practice.attempt?.outcome == AttemptOutcome.COMPLETED
    var practicePaused: Boolean by mutableStateOf(true)
        private set

    init {
        val decoded = InitiationUiCodec.decode(uiStore.read())
        orbit = OrbitLog(decoded?.orbit ?: emptyList())
        practice = PracticeAttemptTracker(persist = { persist() })
        decoded?.attempt?.let { practice.restore(it) }
        val resumeScreen = when {
            draft.checkpoint == InitiationCheckpoint.COMPLETE -> decoded?.ui?.screen?.takeIf { it.hasTabs } ?: InitiationScreen.ROUTINE
            else -> InitiationScreen.entries[draft.checkpoint.ordinal] // HANDOFF: resume at the valid persisted checkpoint
        }.let { if (it == InitiationScreen.PRACTICE) InitiationScreen.SADHANA else it } // never resume a running practice
        uiState = (decoded?.ui ?: InitiationUi()).copy(screen = resumeScreen, message = null, emailStage = false)
        if (draft.checkpoint == InitiationCheckpoint.COMPLETE) { orbit.recordVisit(ports.today()); persist() }
    }

    private fun persist() { uiStore.write(InitiationUiCodec.encode(ui, orbit.all, practice.attempt)) }
    private fun set(u: InitiationUi) { uiState = u; persist() }
    private fun fail(text: String) { uiState = ui.copy(message = text) }
    private fun blocked(r: DraftResult): Boolean { if (r is DraftResult.Blocked) { fail(r.reason); return true }; return false }

    fun go(screen: InitiationScreen) {
        if (ui.screen == InitiationScreen.PRACTICE && screen != InitiationScreen.PRACTICE) pausePractice()
        set(ui.copy(screen = screen, message = null))
    }

    fun back() { go(InitiationScreen.entries[maxOf(0, ui.screen.ordinal - 1)]) }

    private fun reach(screen: InitiationScreen) {
        if (screen.ordinal <= InitiationCheckpoint.FOUNDATION.ordinal) draftRepo.reach(InitiationCheckpoint.entries[screen.ordinal])
        go(screen)
    }

    // ---- Arrival / identity / auth ----
    fun takeCommand() { draftRepo.reach(InitiationCheckpoint.IDENTITY); go(InitiationScreen.IDENTITY) }

    fun submitName(name: String) {
        if (name.trim().isEmpty()) return fail("Enter a name, or start without an account.")
        if (blocked(draftRepo.setDisplayName(name))) return
        go(InitiationScreen.AUTH)
    }

    fun continueAsGuest(name: String? = null) {
        if (!name.isNullOrBlank()) draftRepo.setDisplayName(name)
        if (blocked(auth.continueAsGuest())) return
        go(InitiationScreen.PROPOSITION)
    }

    suspend fun google() { if (blocked(auth.signInWithGoogle())) return; go(InitiationScreen.PROPOSITION) }

    fun openEmail() { uiState = ui.copy(emailStage = true, message = null) }

    suspend fun requestEmailCode(email: String) {
        if (!email.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) return fail("Enter a valid email.")
        UnconfiguredEmailCodeAuth.requestCode(email).onFailure { fail(it.message ?: "Email sign-in is not available yet.") }
    }

    fun ready() {
        if (draftRepo.draft.identityMode == IdentityMode.GUEST) runCatching { onGuestReady() }
        reach(InitiationScreen.ANCHORS)
    }

    // ---- Anchors ----
    suspend fun saveAnchors(wake: LocalTime?, sleep: LocalTime?) {
        if (wake == null || sleep == null) return fail("Choose both anchors.")
        if (wake == sleep) return fail("Choose different wake and sleep times.")
        val practiceAt = RoutineSeed.practiceFor(wake)!!
        val saved = ports.saveRoutine(ports.currentPlan().copy(wakeTime = wake, intendedSleepTime = sleep))
        if (saved.isFailure) return fail("Could not save your times.")
        val today = ports.today()
        orbit.append(OrbitEvent("routine-created", OrbitEventType.ROUTINE_CREATED, today, OrbitSource.CONFIGURED))
        uiState = ui.copy(wake = wake, sleep = sleep, practiceTime = practiceAt, routineSaved = true, firstDate = ui.firstDate ?: today)
        reach(InitiationScreen.ALARM)
    }

    // ---- Alarm ----
    fun enableAlarm() {
        val result = ports.scheduleWakeAlarm(ui.wake, (0..6).toSet())
        if (result != InitiationAlarm.OFF) orbit.append(OrbitEvent("alarm-configured", OrbitEventType.ALARM_CONFIGURED, ports.today(), OrbitSource.CONFIGURED))
        set(ui.copy(alarm = result, message = null))
    }

    fun skipAlarm() { ports.cancelWakeAlarm(); uiState = ui.copy(alarm = InitiationAlarm.OFF); reach(InitiationScreen.APPS) }
    fun alarmContinue() = reach(InitiationScreen.APPS)

    // ---- Apps / window / access ----
    fun toggleApp(pkg: String) = set(ui.copy(selected = if (pkg in ui.selected) ui.selected - pkg else ui.selected + pkg))

    fun saveApps() { if (ui.selected.isEmpty()) return fail("Select an app or choose Not now."); reach(InitiationScreen.WINDOW) }

    fun skipProtection() { uiState = ui.copy(window = ui.window.copy(saved = false, enabled = false)); reach(InitiationScreen.FOUNDATION) }

    fun editWindow(start: LocalTime? = null, end: LocalTime? = null, repeat: String? = null) =
        set(ui.copy(window = ui.window.copy(start = start ?: ui.window.start, end = end ?: ui.window.end, repeat = repeat ?: ui.window.repeat)))

    suspend fun saveWindow() {
        if (ui.selected.isEmpty()) return fail("Choose at least one app first.")
        val w = ui.window
        if (w.start == w.end) return fail("Choose distinct start and end times. Overnight windows are supported.")
        val window = ProtectionWindowV1("initiation-first", w.start, w.end, InitiationRepeat.days(w.repeat), enabled = true)
        val plan = ports.currentPlan().let { p -> p.copy(protectedPackages = ui.selected, windows = p.windows.filterNot { it.id == window.id } + window) }
        (RoutinePlanRules.validate(plan) as? RoutinePlanValidation.Error)?.let { return fail(it.message) }
        if (ports.saveRoutine(plan).isFailure) return fail("Could not save.")
        orbit.append(OrbitEvent("boundary-saved", OrbitEventType.BOUNDARY_SAVED, ports.today(), OrbitSource.CONFIGURED))
        uiState = ui.copy(window = w.copy(saved = true, enabled = true))
        reach(InitiationScreen.ACCESS)
    }

    fun accessContinue() = reach(InitiationScreen.FOUNDATION)

    fun enter() {
        draftRepo.complete(ports.wallNow())
        orbit.recordEntry(ports.today())
        go(InitiationScreen.ROUTINE)
    }

    // ---- Routine ----
    suspend fun toggleProtection() {
        val enable = !ui.window.enabled
        if (!enable) ports.stopProtectionNow() // local exit first, never gated
        val plan = ports.currentPlan().let { p -> p.copy(windows = p.windows.map { if (it.id == "initiation-first") it.copy(enabled = enable) else it }) }
        ports.saveRoutine(plan)
        set(ui.copy(window = ui.window.copy(enabled = enable)))
    }

    suspend fun saveEdit(wake: LocalTime?, practiceAt: LocalTime?, sleep: LocalTime?, activityName: String, activityTime: LocalTime?, duration: Int?, repeat: String) {
        if (wake == null || practiceAt == null || sleep == null) return fail("Choose times for all three anchors.")
        var activities = ui.activities
        if (activityName.isNotBlank()) {
            if (activityTime == null || duration == null || duration !in 1..1440) return fail("Choose an activity time and a duration from 1 to 1440 minutes.")
            activities = activities + InitiationActivity(UUID.randomUUID().toString(), activityName.trim().take(80), activityTime, duration, repeat)
        }
        val changedWake = wake != ui.wake
        val plan = ports.currentPlan().copy(wakeTime = wake, intendedSleepTime = sleep,
            activities = activities.map { RoutineActivityV1(it.id, it.name, it.time, it.duration, InitiationRepeat.days(it.repeat)) })
        (RoutinePlanRules.validate(plan) as? RoutinePlanValidation.Error)?.let { return fail(it.message) }
        if (ports.saveRoutine(plan).isFailure) return fail("Could not save your Routine.")
        // HANDOFF Editor row: a changed wake invalidates schedule readiness.
        val alarm = if (changedWake && ui.alarm != InitiationAlarm.OFF) InitiationAlarm.PERMISSION_REQUIRED else ui.alarm
        uiState = ui.copy(wake = wake, practiceTime = practiceAt, sleep = sleep, activities = activities, alarm = alarm, routineSaved = true)
        go(InitiationScreen.ROUTINE)
    }

    suspend fun removeActivity(id: String) {
        val activities = ui.activities.filterNot { it.id == id }
        ports.saveRoutine(ports.currentPlan().let { p -> p.copy(activities = p.activities.filterNot { it.id == id }) })
        set(ui.copy(activities = activities))
    }

    // ---- Sadhana ----
    fun begin(stationary: Boolean) {
        if (!stationary) return fail("Confirm you are stationary and in a suitable place. You can return to Routine instead.")
        practice.start(true, "steady-initiation-v1", ports.wallNow(), ports.mono())
        orbit.append(OrbitEvent("practice-attempted", OrbitEventType.PRACTICE_ATTEMPTED, ports.today(), OrbitSource.DEVICE_OBSERVED))
        practicePaused = false
        go(InitiationScreen.PRACTICE)
    }

    fun pausePractice() { if (!practicePaused) { practice.pause(ports.mono()); practicePaused = true } }
    fun togglePause() { if (practicePaused) { practice.resume(ports.mono()); practicePaused = false } else pausePractice() }

    /** Returns remaining whole seconds; moves to Returned on completion. */
    fun tick(): Int {
        val remaining = practice.tick(ports.mono(), ports.wallNow())
        if (practice.attempt?.outcome == AttemptOutcome.COMPLETED && ui.screen == InitiationScreen.PRACTICE) { practicePaused = true; go(InitiationScreen.RETURNED) }
        return ((remaining + 999) / 1000).toInt()
    }

    fun endPractice() { practice.end(ports.mono(), ports.wallNow()); practicePaused = true; go(InitiationScreen.RETURNED) }

    // ---- Orbit / week ----
    fun independent() {
        orbit.append(OrbitEvent("independent-${ports.today()}", OrbitEventType.INDEPENDENT_RECORD, ports.today(), OrbitSource.SELF_REPORTED))
        uiState = uiState
        persist()
    }

    fun orbitView(): OrbitView { uiState; return LivingOrbit.derive(orbit.all) }

    /** Day 1-7 since the first anchor; production uses real days, never a review selector. */
    fun weekDay(): Int = ui.firstDate?.let { (ChronoUnit.DAYS.between(it, ports.today()).toInt() + 1).coerceIn(1, 7) } ?: 1

    // ---- Labels (wireframe app.js, minus the prototype's "simulated" markers) ----
    fun alarmLabel(): String = when (ui.alarm) {
        InitiationAlarm.OFF -> "Using your own alarm"
        InitiationAlarm.SCHEDULED -> "Device accepted"
        InitiationAlarm.PERMISSION_REQUIRED -> "Saved · device access needed"
        InitiationAlarm.FAILED -> "Saved · scheduling failed"
    }

    fun boundaryLabel(): String {
        val w = ui.window
        if (!w.saved) return "Not configured"
        if (!w.enabled) return "Off · local exit available"
        return when (ports.protectionState()) {
            FocusRuleState.ACTIVE -> "Active"
            FocusRuleState.READY -> "Ready"
            FocusRuleState.NEEDS_ATTENTION -> "Needs attention"
            else -> "Saved · device access needed"
        }
    }

    /** Installed apps plus any selected package that is no longer installed (shown as unavailable, never silently dropped). */
    fun installedApps(): List<ProtectionAppOption> {
        val found = runCatching { ports.installedApps() }.getOrDefault(emptyList())
        val known = found.map { it.packageName }.toSet()
        val missing = ui.selected.filter { it !in known }.sorted()
            .map { ProtectionAppOption(it, it.substringAfterLast('.'), "Unavailable", installed = false) }
        return found + missing
    }
    /** Read on every resume; tapping "Open settings" never grants anything by itself. */
    fun readAccess(): AccessResume.Access = ports.access()

    fun clearMessage() { uiState = ui.copy(message = null) }

    companion object {
        val INVITES = listOf(
            "Establish your rhythm. Try one minute.", "Notice one automatic reach for an app.", "Make room for one personal activity.",
            "Try a short period of chosen work.", "Notice a familiar reaction before acting.", "Make room for something beyond work.",
            "What did you choose? What would you change?",
        )

        /** app.js guidance(): changes with remaining seconds; text only, never a breath cue. */
        fun guidance(remaining: Int): String = when {
            remaining > 45 -> "Notice the breath already happening."
            remaining > 30 -> "Attention moved? Return, without forcing."
            remaining > 15 -> "Notice arrival and departure. No control needed."
            else -> "One more return."
        }
    }
}
