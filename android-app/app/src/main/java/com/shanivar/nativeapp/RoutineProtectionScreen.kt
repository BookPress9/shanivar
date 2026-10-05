package com.shanivar.nativeapp

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Routine → App protection (decision 0014 final wireframe: `ROUTINE / APP PROTECTION`).
 *
 * Compose here renders [ProtectionEditorState] / [ProtectionOverview] and emits events to
 * [RoutineProtectionController]. It never reads SharedPreferences, computes recurrence, inspects
 * permissions or touches [FocusScheduler] itself.
 */
class RoutineProtectionController(
    val repository: ProtectionSyncingRoutinePlanRepository,
    private val gateway: ProtectionGateway,
    /** Best-effort account mirror after a local stop; never awaited before the device stops. */
    private val mirrorRelease: suspend () -> Unit = {},
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    fun overview(): ProtectionOverview =
        RoutineProtectionStatus.resolve(repository.current(), gateway.evidence(), clock())

    suspend fun reconcile() = repository.reconcile()

    /** Saves locally, syncs the registry, and returns honest copy. Never says "Active". */
    suspend fun save(editor: ProtectionEditorState): Result<String> {
        editor.validate()?.let { return Result.failure(IllegalArgumentException(it)) }
        val plan = editor.applyTo(repository.current())
        val saved = repository.save(plan)
        if (saved.isFailure) return Result.failure(saved.exceptionOrNull() ?: IllegalStateException("Could not save."))
        return Result.success(saveMessage(repository.lastSync, overview()))
    }

    /** Immediate local stop first; the account mirror is secondary and may fail silently. */
    suspend fun stopNow() {
        gateway.stopNow()
        runCatching { mirrorRelease() }
    }

    companion object {
        internal fun saveMessage(sync: ProtectionSyncResult?, overview: ProtectionOverview): String {
            val base = when (sync) {
                is ProtectionSyncResult.Rejected -> "Saved on this device, but not scheduled: ${sync.message}"
                is ProtectionSyncResult.Synced -> when {
                    sync.enabledRules == 0 -> "Saved. No window is on, so nothing is scheduled."
                    sync.armed == null -> "Saved on this device, but Android did not schedule it. Allow Alarms & reminders."
                    else -> "Saved on this device. Status below shows what is actually armed."
                }
                null -> "Saved. Nothing about protection changed."
            }
            val running = overview.protectionRunning
            return if (running != null) "$base Protection already running keeps going until you stop it or it ends." else base
        }
    }
}

private val statusTimeFormat = DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm")
private fun fmt(epochMillis: Long) = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(statusTimeFormat)

/** Re-reads device evidence on resume and on a slow tick; the engines write outside composition. */
@Composable
fun rememberProtectionOverview(controller: RoutineProtectionController, revision: Int): ProtectionOverview {
    var resume by remember { mutableIntStateOf(0) }
    var tick by remember { mutableLongStateOf(0L) }
    var reconciled by remember { mutableIntStateOf(0) }
    OnAppResume { resume++ }
    LaunchedEffect(Unit) { while (true) { delay(30_000L); tick++ } }
    // Local reconcile (import / sync / re-arm) on entry and every resume, then re-read evidence.
    LaunchedEffect(resume) { runCatching { controller.reconcile() }; reconciled++ }
    return remember(resume, tick, revision, reconciled) { controller.overview() }
}

/** Routine summary card + stop + repair. */
@Composable
fun RoutineProtectionSummary(
    plan: RoutinePlanV1,
    overview: ProtectionOverview,
    onEdit: () -> Unit,
    onStop: () -> Unit,
) {
    SectionCard("App protection") {
        Text("Choose what loses access to you.", style = MaterialTheme.typography.titleMedium)
        val count = plan.windows.size
        Text(
            if (count == 0) "Choose distracting apps once. Create the windows that fit your day."
            else "$count protection window${if (count == 1) "" else "s"} · ${plan.protectedPackages.size} app${if (plan.protectedPackages.size == 1) "" else "s"}",
        )
        StatusLine(overview.summary.state, overview.summaryLine(::fmt))
        ProtectionRepairActions(overview.missingAccess)
        StopProtection(overview.protectionRunning, onStop)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (count == 0) "Set up" else "Edit")
            }
        }
    }
}

@Composable
private fun StatusLine(state: FocusRuleState, text: String) {
    Text(
        text,
        color = when (state) {
            FocusRuleState.ACTIVE -> MaterialTheme.colorScheme.primary
            FocusRuleState.NEEDS_DEVICE_ACCESS, FocusRuleState.NEEDS_ATTENTION -> MaterialTheme.colorScheme.error
            FocusRuleState.READY -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Exact repair steps for whatever access is missing — re-read on resume by the caller. */
@Composable
private fun ProtectionRepairActions(missing: Set<FocusDeviceAccess>) {
    val context = LocalContext.current
    if (FocusDeviceAccess.EXACT_ALARM in missing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Allow Alarms & reminders") }
    }
    if (FocusDeviceAccess.USAGE_ACCESS in missing) {
        OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Grant Usage access") }
    }
    if (FocusDeviceAccess.OVERLAY in missing) {
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Allow display over other apps") }
    }
}

/** Essential access: one tap, local, no server, no timer, no practice. */
@Composable
private fun StopProtection(running: LocalProtection?, onStop: () -> Unit) {
    if (running == null) return
    Text("Protection is on until ${fmt(running.endsAtEpochMillis)}.", color = MaterialTheme.colorScheme.primary)
    Button(
        onClick = onStop,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "Stop protection now" },
    ) { Text("Stop protection now") }
}

/**
 * Full App Protection editor: shared app set, window list, bottom-right `+`, save & status.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppProtectionEditor(
    controller: RoutineProtectionController,
    apps: List<ProtectionAppOption>,
    overview: ProtectionOverview,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var editor by remember { mutableStateOf(ProtectionEditorState.from(controller.repository.current())) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val saved = controller.repository.current()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onClose, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("← Routine") }
        Text("ROUTINE / APP PROTECTION", letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() })
        Heading("Choose what\ngets cut off.")
        Text("You already know which apps take more than they give. Select them once. Shanivar handles the windows.")

        // Shared app set — one selection for every window.
        apps.groupBy { it.category }.forEach { (category, options) ->
            Text(category.uppercase(), letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { app ->
                    val selected = app.packageName in editor.packages
                    FilterChip(
                        selected = selected,
                        // A gone app stays deselectable so it can be removed; never newly selectable.
                        enabled = !busy && (app.installed || selected),
                        onClick = { editor = editor.togglePackage(app.packageName) },
                        label = { Text(app.label + if (!app.installed) " · not installed" else "") },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        if (apps.none { it.installed }) Text("No supported distracting apps are installed on this device.")
        Text("The whole selected app is paused, including its messages. Calls and system apps are never selected.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("Protection windows", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Morning. Work. Night. Add the boundaries your 24-hour cycle needs.")
        if (editor.windows.isEmpty()) Text("No windows yet. Use + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        editor.windows.forEachIndexed { index, window ->
            val persisted = saved.windows.firstOrNull { it.id == window.id }
            val status = if (persisted == window && editor.packages == saved.protectedPackages) overview.statusFor(window.id) else null
            ProtectionWindowCard(
                index = index,
                window = window,
                status = status,
                busy = busy,
                onEditor = { transform -> editor = transform(editor) },
            )
        }

        // Bottom-right add control, after the list (final wireframe fidelity).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (!editor.canAdd) Text("Eight windows is the limit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { editor = editor.add() },
                enabled = !busy && editor.canAdd,
                modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp)
                    .semantics { contentDescription = "Add another protection window" },
            ) { Text("+", fontSize = 24.sp) }
        }

        Text("Essential access stays available. Saving schedules windows on this device; it does not switch protection on.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProtectionRepairActions(overview.missingAccess)
        StopProtection(overview.protectionRunning) {
            scope.launch { controller.stopNow(); onRefresh() }
        }

        editor.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
        }
        result?.let {
            Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Button(
            onClick = {
                val checked = editor.withValidation()
                editor = checked
                if (checked.error != null) return@Button
                busy = true; result = null
                scope.launch {
                    controller.save(checked)
                        .onSuccess { result = it; onRefresh() }
                        .onFailure { editor = editor.copy(error = it.message ?: "Could not save.") }
                    busy = false
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(if (busy) "Saving…" else "Save & check device access") }
        if (editor.windows.any { it.enabled }) {
            TextButton(onClick = { editor = editor.allOff() }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Turn all windows off (then save)")
            }
        }
        TextButton(onClick = onClose, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (editor.isDirtyAgainst(controller.repository.current())) "Discard changes" else "Done")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProtectionWindowCard(
    index: Int,
    window: ProtectionWindowV1,
    status: FocusScheduleStatus?,
    busy: Boolean,
    onEditor: ((ProtectionEditorState) -> ProtectionEditorState) -> Unit,
) {
    val context = LocalContext.current
    fun pick(current: LocalTime, update: (LocalTime) -> Unit) {
        TimePickerDialog(context, { _, h, m -> update(LocalTime.of(h, m)) }, current.hour, current.minute, true).show()
    }
    SectionCard("Protection window ${index + 1}") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(ProtectionWindowGeometry.describe(window), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(
                checked = window.enabled,
                enabled = !busy,
                onCheckedChange = { on -> onEditor { it.setEnabled(window.id, on) } },
                modifier = Modifier.semantics {
                    contentDescription = "Window ${index + 1} on"
                    stateDescription = if (window.enabled) "On" else "Off"
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pick(window.startTime) { t -> onEditor { it.setStart(window.id, t) } } },
                enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("From · ${window.startTime.toString().take(5)}") }
            OutlinedButton(onClick = { pick(window.endTime) { t -> onEditor { it.setEnd(window.id, t) } } },
                enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Until · ${window.endTime.toString().take(5)}") }
        }
        when {
            ProtectionWindowGeometry.isFullCalendarDay(window) -> Text("Midnight to midnight: the whole calendar day.")
            ProtectionWindowGeometry.isOvernight(window) -> Text("Overnight: starts on the chosen day, ends the next morning.")
        }
        TextButton(onClick = { onEditor { it.setFullCalendarDay(window.id) } }, enabled = !busy && !ProtectionWindowGeometry.isFullCalendarDay(window),
            modifier = Modifier.heightIn(min = 48.dp)) { Text("Full calendar day") }
        Text("Repeat")
        val repeat = WindowRepeat.kindOf(window.startDays)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WindowRepeat.values().forEach { kind ->
                FilterChip(selected = repeat == kind, enabled = !busy,
                    onClick = { onEditor { it.setRepeat(window.id, kind) } },
                    label = { Text(kind.label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        if (repeat == WindowRepeat.CUSTOM) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 2, 3, 4, 5, 6, 0).forEach { day ->
                    FilterChip(selected = day in window.startDays, enabled = !busy,
                        onClick = { onEditor { it.toggleDay(window.id, day) } },
                        label = { Text(RoutineActivityOccurrence.weekdayName(day)) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        if (status != null) StatusLine(status.state, status.describeOnRoutine(::fmt))
        else Text("Not saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onEditor { it.remove(window.id) } }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
