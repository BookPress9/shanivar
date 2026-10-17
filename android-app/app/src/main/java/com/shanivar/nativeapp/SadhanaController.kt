package com.shanivar.nativeapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Properties

/*
 * Sadhana V1 screen controller (wireframes/sadhana-v1/app.js flow) over the S1-S4 domain.
 * Pure Kotlin with Compose-observable state. Persists to a DraftStore as java.util.Properties.
 */

enum class SadhanaView(val title: String) {
    HOME("Sadhana"), SPINE("Living Spine"), PATH("Path"), PRACTICE("Practice"), PREPARE("Prepare"),
    PLAYER("Player"), COMPLETE("Return"), RECORD("Record"), INDEPENDENT("Without app"), STUDY("Study")
}

interface SadhanaClock {
    fun wall(): Long
    fun mono(): Long
    fun zone(): ZoneId
    fun today(): LocalDate = Instant.ofEpochMilli(wall()).atZone(zone()).toLocalDate()
}

data class SadhanaUi(
    val view: SadhanaView = SadhanaView.HOME,
    val selected: String = "natural",
    val centre: String = "muladhara",
    val duration: Int = 1,
    val anchor: SadhanaAnchor = SadhanaAnchor.BREATH,
    val stationary: Boolean = false,
    val lastOutcome: SadhanaOutcome? = null,
    val lastSeconds: Long = 0,
    val lastAttemptId: String? = null,
    val ownMinutes: Int = 3,
    /** Local-only reflective self-check; never sent, never evidence. Null = not answered. */
    val selfCheck: SelfCheck? = null,
    /** Default focus lock during practice; voluntary, can be turned off. */
    val focusLockDefault: Boolean = true,
    val message: String? = null,
)

class SadhanaController(
    private val store: DraftStore,
    private val clock: SadhanaClock,
    val allowUnreviewed: Boolean,
    private val routineBoundaryLinked: () -> Boolean = { false },
    /** B6: the finished onboarding 60 s attempt, imported once into the record as app-observed. */
    private val initiationAttempt: () -> PracticeAttempt? = { null },
    private val focusLock: SadhanaFocusLockPort? = null,
    private val outbox: SyncOutbox? = null,
) {
    /** Draft of the Stage 1 self-check for the last attempt (null when the last attempt was not Stage 1). */
    var stage1Check: Stage1Check? by mutableStateOf(null)
        private set
    val stage1Saved: Boolean get() = stage1Check?.let { d -> record.stage1Checks.any { it == d } } ?: false
    private fun editStage1(f: (Stage1Check) -> Stage1Check) { stage1Check = stage1Check?.let(f) }
    fun stage1Reach(r: Stage1Reach) = editStage1 { it.copy(reach = if (it.reach == r) null else r) }
    fun stage1Drifts(delta: Int) = editStage1 { it.copy(drifts = ((it.drifts ?: 0) + delta).coerceIn(0, Stage1.MAX_DRIFTS)) }
    fun stage1Reason(r: DriftReason) = editStage1 { it.copy(reasons = if (r in it.reasons) it.reasons - r else it.reasons + r) }
    fun stage1Note(t: String) = editStage1 { it.copy(note = t.take(500).ifBlank { null }) }
    fun saveStage1Check() {
        val chk = stage1Check?.takeIf { it.answered } ?: return
        recordState = record.withStage1Check(chk)
        outbox?.enqueue("self_report", chk.id, Stage1.payload(chk))
        set(ui)
    }
    var focusStatus: FocusLockStatus? by mutableStateOf(null)
        private set
    private var uiState by mutableStateOf(SadhanaUi(), neverEqualPolicy())
    private var recordState by mutableStateOf(SadhanaRecord(), neverEqualPolicy())
    private var remainingState by mutableStateOf(0L)
    val ui: SadhanaUi get() = uiState
    val record: SadhanaRecord get() = recordState
    val remainingMillis: Long get() = remainingState
    private val tracker = SadhanaAttemptTracker { saveCurrent(it) }
    val practiceInProgress: Boolean get() { uiState; return tracker.inProgress }
    val running: Boolean get() { uiState; remainingState; return tracker.isRunning }

    init {
        val p = Properties().apply { store.read()?.let { load(it.reader()) } }
        recordState = SadhanaCodec.record(p)
        val ui0 = SadhanaCodec.ui(p)
        val current = SadhanaCodec.current(p)
        if (current != null && current.outcome == SadhanaOutcome.IN_PROGRESS) {
            tracker.restore(current, clock.wall()) // process death -> INTERRUPTED, never completed
            recordState = recordState.withAttempt(tracker.attempt!!)
            tracker.clear()
        }
        runCatching { initiationAttempt() }.getOrNull()?.let { InitiationImport.toSadhana(it, clock.zone()) }
            ?.takeIf { a -> recordState.attempts.none { it.id == a.id } }
            ?.let { a -> recordState = recordState.withAttempt(a)
                if (recordState.attempts.any { it.id == a.id }) outbox?.enqueue("attempt", a.id, SyncPayloads.attempt(a)) }
        runCatching { focusLock?.release() } // never left on after process death
        uiState = ui0.copy(view = if (ui0.view == SadhanaView.PLAYER) SadhanaView.HOME else ui0.view, message = null, stationary = false)
        persist(null)
    }

    val practice: SadhanaPracticeDefinition get() = SadhanaCatalog.byId(ui.selected) ?: SadhanaCatalog.practices.first()
    val invitation: SadhanaCapability get() = CapabilityCriteria.invitation(record, routineBoundaryLinked())
    fun hasCapability(c: SadhanaCapability) = CapabilityCriteria.has(record, routineBoundaryLinked(), c)
    val minimal: Boolean get() = record.guidance.detail != GuidanceDetail.FULL
    fun orbitView(): OrbitView = LivingOrbit.derive(record.orbitEvents(clock.zone()))
    fun today(): LocalDate = clock.today()
    fun nowLabel(): String = Instant.ofEpochMilli(clock.wall()).atZone(clock.zone()).toLocalTime().let { "%02d:%02d".format(it.hour, it.minute) }

    fun go(v: SadhanaView) {
        if (tracker.inProgress) return // End is the only way out of an active practice
        set(ui.copy(view = v, message = null))
    }
    fun selectCentre(id: String) = set(ui.copy(centre = id))
    fun setDuration(m: Int) { if (SadhanaCatalog.validDuration(practice, m)) set(ui.copy(duration = m)) }
    fun anotherPractice() { val n = SadhanaCatalog.next(ui.selected); set(ui.copy(selected = n.id, duration = n.minutes.first(), message = null)) }
    fun chooseStage1() = set(ui.copy(selected = Stage1.ID, duration = 1, view = SadhanaView.PRACTICE, message = null))
    fun setAnchor(a: SadhanaAnchor) = set(ui.copy(anchor = a))
    fun setStationary(v: Boolean) = set(ui.copy(stationary = v))

    fun begin() {
        if (!ui.stationary) return set(ui.copy(message = "Confirm you are stationary, or return without beginning."))
        val ok = tracker.start(practice, ui.duration, ui.anchor, true, allowUnreviewed, clock.wall(), clock.mono(), clock.zone().id)
        if (!ok) return set(ui.copy(message = "This practice is not available in this build."))
        remainingState = ui.duration * 60_000L
        focusStatus = if (ui.focusLockDefault) runCatching { focusLock?.engage(clock.wall() + remainingState + 60_000L) }.getOrNull() else null
        set(ui.copy(view = SadhanaView.PLAYER, message = null))
    }

    fun tick() {
        if (!tracker.inProgress) return
        remainingState = tracker.tick(clock.mono(), clock.wall())
        if (tracker.attempt?.outcome == SadhanaOutcome.COMPLETED) finish()
    }

    fun togglePause() {
        if (tracker.isRunning) tracker.pause(PauseReason.USER, clock.mono(), clock.wall()) else tracker.resume(clock.mono(), clock.wall())
        set(ui.copy(message = null))
    }

    fun background() {
        if (tracker.isRunning) { tracker.pause(PauseReason.BACKGROUND, clock.mono(), clock.wall()); set(ui) }
    }

    fun changeAnchor() {
        val to = tracker.changeAnchor(clock.mono(), clock.wall()) ?: return
        set(ui.copy(anchor = to, message = "Anchor changed to ${to.label}. Resume when ready."))
    }

    fun end() { tracker.end(clock.mono(), clock.wall()); finish() }

    private fun finish() {
        runCatching { focusLock?.release() }; focusStatus = null
        val a = tracker.attempt ?: return
        recordState = record.withAttempt(a)
        tracker.clear()
        stage1Check = if (a.practiceId == Stage1.ID) Stage1Check("s1-" + a.id, a.id, clock.today()) else null
        outbox?.enqueue("attempt", a.id, SyncPayloads.attempt(a))
        set(ui.copy(view = SadhanaView.COMPLETE, lastOutcome = a.outcome, lastSeconds = a.activeSeconds, lastAttemptId = a.id, message = null))
    }

    fun reflect(u: Usefulness) {
        val id = ui.lastAttemptId ?: return
        val current = record.reflections[id]?.usefulness
        recordState = record.reflect(id, if (current == u) null else u, clock.wall())
        set(ui.copy(message = SadhanaGuidance.afterReflection(record.reflections[id]?.usefulness)))
    }
    fun reflection(): Usefulness? = ui.lastAttemptId?.let { record.reflections[it]?.usefulness }

    fun setFocusLockDefault(on: Boolean) = set(ui.copy(focusLockDefault = on))
    fun setSelfCheck(v: SelfCheck) = set(ui.copy(selfCheck = if (ui.selfCheck == v) null else v))
    fun setOwnMinutes(m: Int) = set(ui.copy(ownMinutes = m))
    /** "I'll practise now · hide guidance": chooses minimal guidance; records nothing (nothing happened yet). */
    fun leaveToPractise() { recordState = record.setGuidance(GuidanceDetail.MINIMAL); set(ui.copy(view = SadhanaView.HOME, message = null)) }
    fun recordOwn() {
        recordState = record.addIndependent(clock.today(), ui.ownMinutes)
        queueOwn(record.independent.last().id)
        set(ui.copy(view = SadhanaView.RECORD, message = null))
    }
    private fun queueOwn(id: String) { record.independent.firstOrNull { it.id == id }?.let { outbox?.enqueue("self_report", it.id, SyncPayloads.selfReport(it)) } }
    /** B7: edit actual minutes of a self-report (1..180). Stays a self-report, never app-observed. */
    fun adjustOwnActual(id: String, delta: Int) {
        val o = record.independent.firstOrNull { it.id == id } ?: return
        val now = (o.actualMinutes ?: o.intendedMinutes ?: 0) + delta
        recordState = record.editIndependent(id, now.coerceIn(1, 180)); queueOwn(id); set(ui)
    }
    fun deleteOwn(id: String) {
        if (record.independent.none { it.id == id }) return
        recordState = record.deleteIndependent(id)
        outbox?.enqueue("self_report", id, SyncPayloads.tombstone(id))
        set(ui)
    }
    fun fewerPrompts(on: Boolean) { recordState = record.setGuidance(if (on) GuidanceDetail.MINIMAL else GuidanceDetail.FULL); set(ui) }
    fun inspectSource(text: String? = null) = set(ui.copy(message = text ?: "The source reader is not available yet. No unreviewed text is shown."))

    fun guidanceLine(): String {
        val total = ui.duration * 60L
        return SadhanaGuidance.line(practice, record.guidance.detail, false, total, total - remainingMillis / 1000)
    }

    private fun set(next: SadhanaUi) { uiState = next; persist(tracker.attempt) }
    private fun saveCurrent(a: SadhanaAttempt) = persist(a)
    private fun persist(current: SadhanaAttempt?) {
        val p = SadhanaCodec.encode(uiState, recordState, current?.takeIf { it.outcome == SadhanaOutcome.IN_PROGRESS })
        val w = java.io.StringWriter(); p.store(w, null); store.write(w.toString())
    }
}

object SadhanaCodec {
    fun encode(ui: SadhanaUi, r: SadhanaRecord, current: SadhanaAttempt?): Properties = Properties().apply {
        setProperty("view", ui.view.name); setProperty("selected", ui.selected); setProperty("centre", ui.centre)
        setProperty("duration", ui.duration.toString()); setProperty("anchor", ui.anchor.name); setProperty("ownMinutes", ui.ownMinutes.toString()); ui.selfCheck?.let { setProperty("selfCheck", it.name) }; setProperty("focusLock", ui.focusLockDefault.toString())
        ui.lastOutcome?.let { setProperty("lastOutcome", it.name) }; setProperty("lastSeconds", ui.lastSeconds.toString())
        ui.lastAttemptId?.let { setProperty("lastAttemptId", it) }
        setProperty("guidance", r.guidance.detail.name); setProperty("guidanceBy", r.guidance.changedBy.name)
        setProperty("attempts", r.attempts.joinToString("\n") { att(it) })
        setProperty("own", r.independent.joinToString("\n") { "${it.id}|${it.localDate}|${it.intendedMinutes ?: ""}|${it.actualMinutes ?: ""}" })
        setProperty("reflections", r.reflections.values.joinToString("\n") { "${it.attemptId}|${it.usefulness?.name ?: ""}|${it.createdAtUtc}" })
        current?.let { setProperty("current", att(it)) }
        setProperty("stage1", r.stage1Checks.joinToString("\n") { Stage1.encode(it) })
    }

    private fun att(a: SadhanaAttempt) = listOf(a.id, a.practiceId, a.contentVersion, a.startedAtUtc, a.zoneId, a.intendedSeconds, a.activeMillis,
        a.outcome.name, a.endedAtUtc ?: "", a.anchor.name, a.anchorChanges,
        a.events.joinToString(";") { "${it.type.name},${it.atUtcMillis},${it.activeSeconds},${it.detail ?: ""}" }).joinToString("|")

    private fun parseAtt(s: String): SadhanaAttempt? = runCatching {
        val f = s.split("|")
        SadhanaAttempt(f[0], f[1], f[2], f[3].toLong(), f[4], f[5].toLong(), f[6].toLong(), SadhanaOutcome.valueOf(f[7]),
            f[8].toLongOrNull(), SadhanaAnchor.valueOf(f[9]), f[10].toInt(),
            f[11].split(";").filter { it.isNotBlank() }.map { e -> e.split(",").let { SadhanaEvent(SadhanaEventType.valueOf(it[0]), it[1].toLong(), it[2].toLong(), it.getOrNull(3)?.ifBlank { null }) } })
    }.getOrNull()

    private fun lines(p: Properties, k: String) = p.getProperty(k).orEmpty().split("\n").filter { it.isNotBlank() }

    fun record(p: Properties) = SadhanaRecord(
        stage1Checks = lines(p, "stage1").mapNotNull(Stage1::decode),
        attempts = lines(p, "attempts").mapNotNull(::parseAtt),
        reflections = lines(p, "reflections").mapNotNull { l -> l.split("|").let { f ->
            f[1].ifBlank { null }?.let { PracticeReflectionRecord(f[0], Usefulness.valueOf(it), null, f[2].toLong()) } } }.associateBy { it.attemptId },
        independent = lines(p, "own").map { l -> l.split("|").let { IndependentPracticeRecord(it[0], LocalDate.parse(it[1]), it[2].toIntOrNull(), it[3].toIntOrNull()) } },
        guidance = GuidancePreference(runCatching { GuidanceDetail.valueOf(p.getProperty("guidance")) }.getOrDefault(GuidanceDetail.FULL),
            runCatching { ChangedBy.valueOf(p.getProperty("guidanceBy")) }.getOrDefault(ChangedBy.USER)),
    )

    fun ui(p: Properties) = SadhanaUi(
        view = runCatching { SadhanaView.valueOf(p.getProperty("view")) }.getOrDefault(SadhanaView.HOME),
        selected = p.getProperty("selected") ?: "natural", centre = p.getProperty("centre") ?: "muladhara",
        duration = p.getProperty("duration")?.toIntOrNull() ?: 1,
        anchor = runCatching { SadhanaAnchor.valueOf(p.getProperty("anchor")) }.getOrDefault(SadhanaAnchor.BREATH),
        ownMinutes = p.getProperty("ownMinutes")?.toIntOrNull() ?: 3,
        selfCheck = runCatching { SelfCheck.valueOf(p.getProperty("selfCheck")) }.getOrNull(),
        focusLockDefault = p.getProperty("focusLock")?.toBoolean() ?: true,
        lastOutcome = runCatching { SadhanaOutcome.valueOf(p.getProperty("lastOutcome")) }.getOrNull(),
        lastSeconds = p.getProperty("lastSeconds")?.toLongOrNull() ?: 0, lastAttemptId = p.getProperty("lastAttemptId"),
    )

    fun current(p: Properties): SadhanaAttempt? = p.getProperty("current")?.let(::parseAtt)
}

/** B6: maps the onboarding attempt to a Sadhana attempt of the "natural" practice. Unfinished attempts are not imported. */
object InitiationImport {
    fun toSadhana(a: PracticeAttempt, zone: ZoneId): SadhanaAttempt? {
        val outcome = when (a.outcome) {
            AttemptOutcome.COMPLETED -> SadhanaOutcome.COMPLETED
            AttemptOutcome.ENDED -> SadhanaOutcome.ENDED
            AttemptOutcome.IN_PROGRESS -> return null
        }
        return SadhanaAttempt(
            id = "initiation-" + a.attemptId, practiceId = "natural", contentVersion = a.contentVersion,
            startedAtUtc = a.startedAtMillis, zoneId = zone.id, intendedSeconds = 60,
            activeMillis = a.activeMillis, outcome = outcome, endedAtUtc = a.endedAtMillis,
        )
    }
}
