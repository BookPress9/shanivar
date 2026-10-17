package com.shanivar.nativeapp

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Routine → App protection boundary (decision 0014, R2/R3).
 *
 * [RoutinePlanV1.windows] + [RoutinePlanV1.protectedPackages] are the person's configuration.
 * [FocusScheduler]'s multi-rule registry is what this device actually arms. This file owns the one
 * mapping between them, plus the device-evidence status the Routine surface shows:
 *
 * - [RoutineProtectionSync] — pure plan ⇄ registry mapping, change detection, one-time import of a
 *   pre-V3 schedule.
 * - [ProtectionGateway] — the only seam Routine UI uses to arm, read device evidence, or stop.
 * - [ProtectionSyncingRoutinePlanRepository] — saving the plan syncs the registry **only when the
 *   protection config changed**, so activity / anchor edits can never arm anything.
 * - [RoutineProtectionStatus] — per-window [FocusScheduleStatus] from device evidence (DEV-3: a
 *   save is never Active).
 * - [ProtectionEditorState] — immutable editor state + pure transitions for Compose.
 */

// ── Pure plan ⇄ registry mapping ─────────────────────────────────────────────────────────────

/** The protection-relevant slice of a plan. Anything outside it (anchors, activities) never syncs. */
data class ProtectionConfig(val windows: List<ProtectionWindowV1>, val packages: Set<String>)

object RoutineProtectionSync {
    fun configOf(plan: RoutinePlanV1) = ProtectionConfig(plan.windows, plan.protectedPackages)

    /**
     * Registry rules for a plan. All windows share the plan's package set (V3 handoff). A window
     * with no selected apps cannot protect anything, so it is stored disabled rather than armed.
     */
    fun toRules(plan: RoutinePlanV1): List<FocusScheduler.Rule> = plan.windows.map { w ->
        FocusScheduler.Rule(
            scheduleId = w.id,
            days = w.startDays,
            startTime = w.startTime,
            endTime = w.endTime,
            blockPackages = plan.protectedPackages,
            enabled = w.enabled && plan.protectedPackages.isNotEmpty(),
        )
    }

    /** True when this device's registry does not yet describe [plan] (order-insensitive). */
    fun registryDiffers(registry: List<FocusScheduler.Rule>, plan: RoutinePlanV1): Boolean =
        registry.sortedBy { it.scheduleId } != toRules(plan).sortedBy { it.scheduleId }

    /**
     * One-time, lossless import of rules armed before V3 (the single account-backed schedule from
     * `FocusScheduleCard`, or a migrated legacy rule) into an empty plan. Ids are preserved so the
     * already-armed boundary keeps pointing at the same rule. Returns null when nothing to import.
     */
    fun seedFromRegistry(plan: RoutinePlanV1, registry: List<FocusScheduler.Rule>): RoutinePlanV1? {
        if (plan.windows.isNotEmpty() || plan.protectedPackages.isNotEmpty() || registry.isEmpty()) return null
        val windows = registry.take(RoutinePlanV1.MAX_WINDOWS).map {
            ProtectionWindowV1(it.scheduleId, it.startTime, it.endTime, it.days, it.enabled)
        }
        val packages = registry.flatMapTo(mutableSetOf()) { it.blockPackages }
        val seeded = plan.copy(windows = windows, protectedPackages = packages)
        return seeded.takeIf { RoutinePlanRules.validate(it) is RoutinePlanValidation.Ok }
    }
}

// ── Device seam ──────────────────────────────────────────────────────────────────────────────

sealed class ProtectionSyncResult {
    /** Registry now matches the plan. [armed] null with enabled rules means Android refused the
     * exact alarm — the status layer reports Needs device access / Saved, never Ready. */
    data class Synced(val armed: FocusScheduler.ArmedBoundary?, val enabledRules: Int) : ProtectionSyncResult()
    data class Rejected(val message: String) : ProtectionSyncResult()
}

/** Everything the status layer may look at — all read from this device. */
data class ProtectionDeviceEvidence(
    val registry: List<FocusScheduler.Rule>,
    val armed: FocusScheduler.ArmedBoundary?,
    val hasExactAlarm: Boolean,
    val hasUsageAccess: Boolean,
    val hasOverlay: Boolean,
    val localProtection: LocalProtection?,
)

interface ProtectionGateway {
    fun registryRules(): List<FocusScheduler.Rule>
    /** Replace the registry with [plan]'s windows and arm the global next boundary. */
    fun sync(plan: RoutinePlanV1, now: ZonedDateTime = ZonedDateTime.now()): ProtectionSyncResult
    /** Re-arm from the existing registry without changing rules (e.g. exact-alarm access returned). */
    fun rearm(now: ZonedDateTime = ZonedDateTime.now()): FocusScheduler.ArmedBoundary?
    fun evidence(): ProtectionDeviceEvidence
    /** Immediate, local, offline stop of whatever protection is in force. Never gated. */
    fun stopNow()
}

class AndroidProtectionGateway(context: Context) : ProtectionGateway {
    private val app = context.applicationContext
    private val scheduler = FocusScheduler(app)
    private val permissions = AndroidDisciplinePermissions(app)

    override fun registryRules(): List<FocusScheduler.Rule> = scheduler.rules()

    override fun sync(plan: RoutinePlanV1, now: ZonedDateTime): ProtectionSyncResult {
        val rules = RoutineProtectionSync.toRules(plan)
        return scheduler.replaceAll(rules, now).fold(
            onSuccess = { ProtectionSyncResult.Synced(it, rules.count { r -> r.enabled }) },
            onFailure = { ProtectionSyncResult.Rejected(it.message ?: "Could not schedule these windows.") },
        )
    }

    override fun rearm(now: ZonedDateTime): FocusScheduler.ArmedBoundary? = scheduler.rearm(now)

    override fun evidence() = ProtectionDeviceEvidence(
        registry = scheduler.rules(),
        armed = scheduler.pendingArmed(),
        hasExactAlarm = permissions.hasExactAlarmPermission(),
        hasUsageAccess = permissions.hasUsageAccess(),
        hasOverlay = permissions.hasOverlayPermission(),
        localProtection = AndroidProtectionStateStore(app).read(),
    )

    override fun stopNow() = LocalProtectionController.create(app).stopNow()
}

// ── Saving syncs; nothing else does ──────────────────────────────────────────────────────────

/**
 * Wraps the plan store so every save path (Edit Routine, App Protection) keeps the device registry
 * in step — but only when [ProtectionConfig] changed. An anchor or personal-activity edit leaves
 * the registry and the armed alarm untouched (activities must never arm alarms or Focus).
 *
 * Local plan save is the commitment; registry sync follows. If sync is rejected the plan stays
 * saved and [lastSync] carries the reason so the UI can say "Saved, not scheduled here".
 */
class ProtectionSyncingRoutinePlanRepository(
    private val delegate: RoutinePlanRepository,
    private val gateway: ProtectionGateway,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now() },
) : RoutinePlanRepository {
    @Volatile var lastSync: ProtectionSyncResult? = null
        private set

    override fun observe(): Flow<RoutinePlanV1> = delegate.observe()
    override fun current(): RoutinePlanV1 = delegate.current()
    override fun decodeHealthy(): Boolean = delegate.decodeHealthy()

    override suspend fun save(plan: RoutinePlanV1): Result<Unit> {
        val before = delegate.current()
        val saved = delegate.save(plan)
        if (saved.isFailure) return saved
        val changed = RoutineProtectionSync.configOf(before) != RoutineProtectionSync.configOf(plan)
        // A repaired (previously undecodable) plan is not force-synced here: its fallback windows are
        // empty and would wipe the last armed rules. [reconcile] re-imports them instead.
        lastSync = if (changed) gateway.sync(plan, clock()) else null
        return saved
    }

    /**
     * Startup / resume reconciliation; local only.
     * 1. Never act on an undecodable plan — an empty fallback must not wipe armed protection.
     * 2. Empty plan + existing registry → import it once (lossless V3 migration).
     * 3. Registry differs from the saved plan → sync (the plan is the authority under V3).
     * 4. Same rules but nothing armed (e.g. exact-alarm access just granted) → re-arm.
     */
    suspend fun reconcile(): ReconcileOutcome {
        if (!delegate.decodeHealthy()) return ReconcileOutcome.SKIPPED_UNHEALTHY
        val plan = delegate.current()
        val registry = gateway.registryRules()
        if (plan.windows.isEmpty() && plan.protectedPackages.isEmpty() && registry.isNotEmpty()) {
            // Import or leave alone — never sync an empty plan over rules this device already holds.
            val seeded = RoutineProtectionSync.seedFromRegistry(plan, registry) ?: return ReconcileOutcome.UNCHANGED
            return if (delegate.save(seeded).isSuccess) ReconcileOutcome.IMPORTED else ReconcileOutcome.UNCHANGED
        }
        if (RoutineProtectionSync.registryDiffers(registry, plan)) {
            lastSync = gateway.sync(plan, clock())
            return ReconcileOutcome.SYNCED
        }
        if (registry.any { it.enabled } && gateway.evidence().armed == null) {
            return if (gateway.rearm(clock()) != null) ReconcileOutcome.REARMED else ReconcileOutcome.UNCHANGED
        }
        return ReconcileOutcome.UNCHANGED
    }

    enum class ReconcileOutcome { SKIPPED_UNHEALTHY, IMPORTED, SYNCED, REARMED, UNCHANGED }
}

// ── Status from device evidence ──────────────────────────────────────────────────────────────

data class ProtectionWindowStatus(val windowId: String, val status: FocusScheduleStatus)

data class ProtectionOverview(
    val windows: List<ProtectionWindowStatus>,
    /** One state for the Routine summary; worst-first, ACTIVE only from a guard-confirmed window. */
    val summary: FocusScheduleStatus,
    val missingAccess: Set<FocusDeviceAccess>,
    /** Guard record live right now (any owner: window, manual focus, reboot catch-up). */
    val protectionRunning: LocalProtection?,
    /** Plan has enabled windows but no apps selected. */
    val needsApps: Boolean,
) {
    fun statusFor(windowId: String): FocusScheduleStatus? = windows.firstOrNull { it.windowId == windowId }?.status
}

object RoutineProtectionStatus {
    /**
     * Per-window state via [FocusScheduleStatusResolver] — never a parallel truth table.
     *
     * Multi-rule adaptation: the registry arms one global next boundary and re-arms after every
     * delivery, so a window counts as armed when (a) this device's registry holds exactly this
     * rule and (b) the global chain is armed. Its "next change" is its own next boundary. If the
     * global trigger itself was missed, every enabled window reports that missed instant so the
     * resolver says Needs attention rather than Ready.
     */
    fun resolve(plan: RoutinePlanV1, evidence: ProtectionDeviceEvidence, now: ZonedDateTime): ProtectionOverview {
        val nowMillis = now.toInstant().toEpochMilli()
        val chainArmed = evidence.armed != null
        val chainMissed = evidence.armed?.let {
            it.triggerAtEpochMillis + FocusScheduleStatusResolver.MISSED_BOUNDARY_GRACE_MS < nowMillis
        } ?: false
        val planRules = RoutineProtectionSync.toRules(plan)
        val statuses = plan.windows.mapIndexed { index, window ->
            val rule = planRules[index]
            val registryRule = evidence.registry.firstOrNull { it.scheduleId == rule.scheduleId }
            val registered = registryRule == rule
            val nextTrigger: Long? = when {
                !chainArmed || !registered -> null
                chainMissed -> evidence.armed!!.triggerAtEpochMillis
                else -> FocusWindowRecurrence.nextBoundary(rule.days, rule.startTime, rule.endTime, now)
                    ?.first?.toInstant()?.toEpochMilli()
            }
            val status = FocusScheduleStatusResolver.resolve(
                FocusScheduleInputs(
                    now = now,
                    savedRule = rule,
                    enabled = window.enabled,
                    armedRule = if (chainArmed && registered) registryRule else null,
                    nextTriggerAtEpochMillis = nextTrigger,
                    hasExactAlarm = evidence.hasExactAlarm,
                    hasUsageAccess = evidence.hasUsageAccess,
                    hasOverlay = evidence.hasOverlay,
                    localProtection = evidence.localProtection,
                ),
            )
            ProtectionWindowStatus(window.id, status)
        }
        val missing = buildSet {
            if (!evidence.hasExactAlarm) add(FocusDeviceAccess.EXACT_ALARM)
            if (!evidence.hasUsageAccess) add(FocusDeviceAccess.USAGE_ACCESS)
            if (!evidence.hasOverlay) add(FocusDeviceAccess.OVERLAY)
        }
        val running = evidence.localProtection?.takeIf { it.isActiveAt(nowMillis) }
        val needsApps = plan.windows.any { it.enabled } && plan.protectedPackages.isEmpty()
        return ProtectionOverview(statuses, summarize(statuses.map { it.status }, needsApps), missing, running, needsApps)
    }

    private val PRIORITY = listOf(
        FocusRuleState.ACTIVE,
        FocusRuleState.NEEDS_DEVICE_ACCESS,
        FocusRuleState.NEEDS_ATTENTION,
        FocusRuleState.SAVED_LOCAL,
        FocusRuleState.READY,
        FocusRuleState.OFF,
        FocusRuleState.DRAFT,
    )

    internal fun summarize(statuses: List<FocusScheduleStatus>, needsApps: Boolean): FocusScheduleStatus {
        if (statuses.isEmpty()) return FocusScheduleStatus(FocusRuleState.DRAFT)
        if (needsApps) return FocusScheduleStatus(FocusRuleState.NEEDS_ATTENTION)
        val worst = statuses.minByOrNull { PRIORITY.indexOf(it.state) }!!
        // For READY, surface the soonest change across windows rather than the first window's.
        if (worst.state == FocusRuleState.READY) {
            val soonest = statuses.filter { it.state == FocusRuleState.READY }.minByOrNull { it.nextChangeEpochMillis ?: Long.MAX_VALUE }
            return soonest ?: worst
        }
        return worst
    }
}

/** Short chip label for timeline rows (never "Active" unless the resolver said so). */
internal fun FocusScheduleStatus.chip(formatTime: (Long) -> String): String = when (state) {
    FocusRuleState.ACTIVE -> "Active until ${formatTime(activeUntilEpochMillis!!)}"
    FocusRuleState.READY -> "Ready"
    else -> label
}

/** Routine-surface copy for the summary card; extends [describe] with the multi-window cases. */
internal fun ProtectionOverview.summaryLine(formatTime: (Long) -> String): String = when {
    windows.isEmpty() -> "Not configured. Choose distracting apps once. Create the windows that fit your day."
    needsApps -> "Needs attention. Choose at least one app, then save, so these windows can protect something."
    else -> summary.describeOnRoutine(formatTime)
}

/** [describe] with repair copy pointed at the Routine surface rather than the old single card. */
internal fun FocusScheduleStatus.describeOnRoutine(formatTime: (Long) -> String): String =
    describe(formatTime)
        .replace("start focus above to protect now", "start a focus session to protect now")
        .replace("Edit and save the schedule", "Open App protection and save")

private fun formatClock(t: LocalTime) = t.toString().take(5)

// ── Editor state ─────────────────────────────────────────────────────────────────────────────

/** A row of the app picker; built by the host from a curated list + PackageManager, never Compose. */
data class ProtectionAppOption(val packageName: String, val label: String, val category: String, val installed: Boolean)

/**
 * Immutable App Protection editor state. Compose renders it and calls these pure transitions;
 * [validate] is the same rule set the registry enforces, with copy that names windows by position.
 */
data class ProtectionEditorState(
    val windows: List<ProtectionWindowV1>,
    val packages: Set<String>,
    val error: String? = null,
) {
    val canAdd: Boolean get() = windows.size < RoutinePlanV1.MAX_WINDOWS

    fun add(id: String = UUID.randomUUID().toString()): ProtectionEditorState {
        if (!canAdd) return copy(error = "Eight protection windows is the limit.")
        return copy(windows = windows + suggestNewWindow(windows, id), error = null)
    }

    fun remove(id: String) = copy(windows = windows.filterNot { it.id == id }, error = null)

    fun update(id: String, transform: (ProtectionWindowV1) -> ProtectionWindowV1) =
        copy(windows = windows.map { if (it.id == id) transform(it) else it }, error = null)

    fun setStart(id: String, time: LocalTime) = update(id) { it.copy(startTime = time) }
    fun setEnd(id: String, time: LocalTime) = update(id) { it.copy(endTime = time) }
    fun setEnabled(id: String, enabled: Boolean) = update(id) { it.copy(enabled = enabled) }
    fun setRepeat(id: String, repeat: WindowRepeat) = update(id) { it.copy(startDays = WindowRepeat.daysFor(repeat, it.startDays)) }
    fun toggleDay(id: String, day: Int) = update(id) {
        it.copy(startDays = if (day in it.startDays) it.startDays - day else it.startDays + day)
    }
    fun setFullCalendarDay(id: String) = update(id) { it.copy(startTime = LocalTime.MIDNIGHT, endTime = LocalTime.MIDNIGHT) }
    fun togglePackage(pkg: String) = copy(packages = if (pkg in packages) packages - pkg else packages + pkg, error = null)
    fun allOff() = copy(windows = windows.map { it.copy(enabled = false) }, error = null)

    /** Null when savable. */
    fun validate(): String? {
        if (windows.size > RoutinePlanV1.MAX_WINDOWS) return "Eight protection windows is the limit."
        windows.forEachIndexed { i, w ->
            val name = "Window ${i + 1}"
            if (!FocusWindowRecurrence.isValidShape(w.startTime, w.endTime)) {
                return "$name starts and ends at the same time. For overnight, end in the morning; for a whole day, choose Full calendar day."
            }
            if (w.startDays.isEmpty()) return "$name needs at least one day."
        }
        if (windows.any { it.enabled } && packages.isEmpty()) {
            return "Choose at least one app to protect your attention from."
        }
        RoutinePlanRules.findEnabledOverlap(windows)?.let { (a, b) ->
            val ia = windows.indexOf(a) + 1
            val ib = windows.indexOf(b) + 1
            return "Window $ia (${ProtectionWindowGeometry.describe(a)}) overlaps window $ib (${ProtectionWindowGeometry.describe(b)}). Back-to-back is fine; overlap is not."
        }
        return null
    }

    fun withValidation(): ProtectionEditorState = copy(error = validate())

    fun applyTo(plan: RoutinePlanV1): RoutinePlanV1 = plan.copy(windows = windows, protectedPackages = packages)

    fun isDirtyAgainst(plan: RoutinePlanV1): Boolean =
        windows != plan.windows || packages != plan.protectedPackages

    companion object {
        fun from(plan: RoutinePlanV1) = ProtectionEditorState(plan.windows, plan.protectedPackages)

        /** Wireframe default first, then other common slots; first one that overlaps nothing wins. */
        private val CANDIDATES = listOf(
            LocalTime.of(18, 0) to LocalTime.of(19, 0),
            LocalTime.of(9, 0) to LocalTime.of(10, 30),
            LocalTime.of(6, 0) to LocalTime.of(7, 0),
            LocalTime.of(21, 30) to LocalTime.of(22, 30),
            LocalTime.of(13, 0) to LocalTime.of(14, 0),
            LocalTime.of(15, 0) to LocalTime.of(16, 0),
            LocalTime.of(11, 0) to LocalTime.of(12, 0),
            LocalTime.of(20, 0) to LocalTime.of(21, 0),
        )

        internal fun suggestNewWindow(existing: List<ProtectionWindowV1>, id: String): ProtectionWindowV1 {
            val enabled = existing.filter { it.enabled }
            for ((s, e) in CANDIDATES) {
                val candidate = ProtectionWindowV1(id, s, e, WindowRepeat.EVERY_DAY_DAYS, true)
                if (enabled.none { RoutinePlanRules.windowsOverlap(it, candidate) }) return candidate
            }
            // Nothing free every day: add it switched off so the person picks a time before it counts.
            return ProtectionWindowV1(id, CANDIDATES.first().first, CANDIDATES.first().second, WindowRepeat.EVERY_DAY_DAYS, false)
        }
    }
}

/** Curated V3 catalogue (presentation categories only; not a mental-health taxonomy). */
object ProtectionAppCatalog {
    data class Entry(val packageName: String, val label: String, val category: String)

    private val CATEGORY = mapOf(
        "com.instagram.android" to "Social",
        "com.facebook.katana" to "Social",
        "com.snapchat.android" to "Social",
        "com.twitter.android" to "Social",
        "com.google.android.youtube" to "Feeds & video",
        "com.zhiliaoapp.musically" to "Feeds & video",
    )

    /** Same reviewed list the Focus card uses ([KnownDistractingApps]); categories are labels only. */
    val ENTRIES: List<Entry> = KnownDistractingApps.ALL.map {
        Entry(it.packageName, it.label, CATEGORY[it.packageName] ?: "Easy escape routes")
    }

    /** Known apps present on this device, plus any selected package that is gone (shown as unavailable). */
    fun options(selected: Set<String>, isInstalled: (String) -> Boolean): List<ProtectionAppOption> {
        val known = ENTRIES.map { ProtectionAppOption(it.packageName, it.label, it.category, isInstalled(it.packageName)) }
        val unknownSelected = (selected - ENTRIES.map { it.packageName }.toSet()).map {
            ProtectionAppOption(it, it, "Other", isInstalled(it))
        }
        return known + unknownSelected
    }
}

internal fun epochToClock(epochMillis: Long, now: ZonedDateTime): String =
    Instant.ofEpochMilli(epochMillis).atZone(now.zone).toLocalTime().let(::formatClock)
