package com.shanivar.nativeapp

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val EVERY_DAY = (0..6).toSet()
private val WEEKDAYS = (1..5).toSet()
private val SCHEDULE_DAYS = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

internal data class FocusScheduleDraft(
    val mode: String = "days",
    val days: Set<Int> = WEEKDAYS,
    val start: String = "00:00",
    val end: String = "23:59",
    val packages: Set<String> = emptySet(),
    val intention: String = "",
    val trigger: String = "",
    val action: String = "",
) {
    fun preset(next: String): FocusScheduleDraft = when (next) {
        "all_day" -> copy(mode = next, days = EVERY_DAY, start = "00:00", end = "23:59")
        "days" -> copy(mode = next, days = if (days == EVERY_DAY) WEEKDAYS else days, start = "00:00", end = "23:59")
        "custom" -> copy(mode = next, days = if (days == EVERY_DAY) WEEKDAYS else days, start = "09:00", end = "17:00")
        else -> this
    }

    fun problem(): String? = when {
        days.isEmpty() -> "Choose at least one day."
        packages.isEmpty() -> "Choose at least one installed app to pause."
        intention.isBlank() -> "Name the activity you want this time for."
        start >= end -> "End time must be later than start time. Overnight windows are not available yet."
        else -> null
    }
}

private fun RemoteFocusSchedule.toDraft() = FocusScheduleDraft(
    mode, daysOfWeek, startTime, endTime, blockedPackages,
    intentionLabel, commitmentTrigger, commitmentAction,
)

/** Overlay reads this account-owned reminder locally because it must not wait for a network call. */
internal object FocusCommitmentCache {
    private const val NAME = "focus_commitment"
    fun store(context: Context, schedule: RemoteFocusSchedule) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("schedule_id", schedule.id)
            .putString("intention", schedule.intentionLabel)
            .putString("trigger", schedule.commitmentTrigger)
            .putString("action", schedule.commitmentAction)
            .apply()
    }
    fun clear(context: Context) { context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().clear().apply() }
    fun read(context: Context): Triple<String, String, String> {
        val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return Triple(prefs.getString("intention", "").orEmpty(), prefs.getString("trigger", "").orEmpty(), prefs.getString("action", "").orEmpty())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FocusScheduleCard(
    connected: Boolean, api: ShanivarApi, signIn: () -> Unit, onBusy: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheduler = remember(context) { FocusScheduler(context) }
    val permissions = remember(context) { AndroidDisciplinePermissions(context) }
    var remote by remember { mutableStateOf<RemoteFocusSchedule?>(null) }
    var draft by remember { mutableStateOf(FocusScheduleDraft()) }
    var editing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    var permissionRevision by remember { mutableIntStateOf(0) }
    OnAppResume { permissionRevision++ }
    SideEffect { onBusy(busy) }
    DisposableEffect(Unit) { onDispose { onBusy(false) } }

    LaunchedEffect(connected, api) {
        if (!connected) { remote = null; loaded = false; editing = false; return@LaunchedEffect }
        try {
            val found = RoutineApi(api).listFocusSchedules().firstOrNull()
            remote = found
            if (found != null) {
                draft = found.toDraft()
                if (found.enabled) FocusCommitmentCache.store(context, found)
                else FocusCommitmentCache.clear(context)
                val local = scheduler.pendingRule()
                if (local != null && (local.scheduleId != found.id || !found.enabled)) scheduler.cancel()
            } else {
                scheduler.cancel()
                FocusCommitmentCache.clear(context)
            }
            loaded = true
            error = null
        } catch (e: Exception) { error = e.message ?: "Could not load your focus schedule." }
    }

    fun pickTime(current: String, update: (String) -> Unit) {
        val time = LocalTime.parse(current)
        TimePickerDialog(context, { _, hour, minute -> update("%02d:%02d".format(hour, minute)) }, time.hour, time.minute, true).show()
    }

    val installed = remember(permissionRevision) {
        KnownDistractingApps.ALL.filter {
            runCatching { context.packageManager.getPackageInfo(it.packageName, 0) }.isSuccess
        }.map { it.packageName }.toSet()
    }
    val next = scheduler.pendingTriggerAtEpochMillis()
    SectionCard("Focus on your routine") {
        Text("Protect time on the days you choose.")
        if (!connected) {
            Text("Sign in to keep a recurring focus schedule.")
            PrimaryAction("Sign in to schedule", signIn)
            return@SectionCard
        }
        if (!loaded) {
            Text("Checking your schedule…")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = { scope.launch {
                try { remote = RoutineApi(api).listFocusSchedules().firstOrNull(); remote?.let { draft = it.toDraft() }; loaded = true; error = null }
                catch (e: Exception) { error = e.message ?: "Could not load your focus schedule." }
            } }) { Text("Try again") }
            return@SectionCard
        }
        if (!editing && remote != null) {
            val saved = remote!!
            Text(saved.intentionLabel, style = MaterialTheme.typography.titleLarge)
            Text(when (saved.mode) { "all_day" -> "All day"; "days" -> "A few days a week"; else -> "Custom times" })
            Text(if (saved.daysOfWeek == EVERY_DAY) "Every day" else SCHEDULE_DAYS.filterIndexed { i, _ -> i in saved.daysOfWeek }.joinToString(", "))
            Text("${saved.startTime}–${saved.endTime}")
            if (saved.commitmentTrigger.isNotBlank()) Text(saved.commitmentTrigger)
            if (saved.commitmentAction.isNotBlank()) Text(saved.commitmentAction)
            Text(if (saved.enabled && next != null) "Next change: " + Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))
                else "Not scheduled on this device.", color = if (saved.enabled && next != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = saved.enabled && next != null, enabled = !busy, onCheckedChange = { enabled ->
                    if (enabled) {
                        editing = true
                        result = "Review the schedule, then save it on this device."
                    } else {
                        scheduler.cancel(); FocusCommitmentCache.clear(context)
                        busy = true
                        scope.launch {
                            try { remote = RoutineApi(api).setFocusScheduleEnabled(saved.id, false); result = "Schedule off on this device." }
                            catch (_: Exception) { remote = saved.copy(enabled = false); error = "Off on this device. Account sync failed; reconnect before using another device." }
                            finally { busy = false }
                        }
                    }
                })
                TextButton(onClick = { draft = saved.toDraft(); editing = true }) { Text("Edit") }
                TextButton(onClick = {
                    scheduler.cancel(); FocusCommitmentCache.clear(context)
                    busy = true
                    scope.launch {
                        try { RoutineApi(api).deleteFocusSchedule(saved.id); remote = null; editing = false; result = "Schedule deleted." }
                        catch (_: Exception) { error = "Stopped on this device. Could not delete the account schedule." }
                        finally { busy = false }
                    }
                }) { Text("Delete") }
            }
        } else {
            Text("When should these apps be paused?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all_day" to "All day", "days" to "A few days a week", "custom" to "Custom times").forEach { (mode, label) ->
                    FilterChip(selected = draft.mode == mode, onClick = { draft = draft.preset(mode) }, label = { Text(label) })
                }
            }
            if (draft.mode != "all_day") {
                Text("Repeat on")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SCHEDULE_DAYS.forEachIndexed { day, label -> FilterChip(selected = day in draft.days,
                        onClick = { draft = draft.copy(days = if (day in draft.days) draft.days - day else draft.days + day) },
                        label = { Text(label) }) }
                }
            }
            if (draft.mode == "custom") {
                OutlinedButton(onClick = { pickTime(draft.start) { draft = draft.copy(start = it) } }, modifier = Modifier.fillMaxWidth()) { Text("Start · ${draft.start}") }
                OutlinedButton(onClick = { pickTime(draft.end) { draft = draft.copy(end = it) } }, modifier = Modifier.fillMaxWidth()) { Text("End · ${draft.end}") }
                Text("The end must be later on the same day.")
            }
            OutlinedTextField(draft.intention, { draft = draft.copy(intention = it.take(60)) }, label = { Text("Your chosen activity") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Apps to pause")
            KnownDistractingApps.ALL.forEach { app -> FilterChip(selected = app.packageName in draft.packages,
                enabled = app.packageName in installed, onClick = { draft = draft.copy(packages = if (app.packageName in draft.packages) draft.packages - app.packageName else draft.packages + app.packageName) },
                label = { Text(app.label + if (app.packageName !in installed) " · not installed" else "") }) }
            Text("The whole selected app is paused, including its messages.")
            Text("Optional: write your own reminder for when this gets difficult.")
            OutlinedTextField(draft.trigger, { draft = draft.copy(trigger = it.take(120)) }, label = { Text("When…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.action, { draft = draft.copy(action = it.take(120)) }, label = { Text("I will…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (!permissions.hasExactAlarmPermission()) {
                Text("Allow Alarms & reminders to schedule this focus window.", color = MaterialTheme.colorScheme.error)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }) { Text("Open access settings") }
            }
            Button(onClick = {
                error = draft.problem()
                if (error != null) return@Button
                if (!permissions.hasExactAlarmPermission()) { error = "Allow Alarms & reminders, then return and save."; return@Button }
                busy = true; error = null; result = null
                scope.launch {
                    var armedThisAttempt = false
                    try {
                        val routine = RoutineApi(api)
                        val saved = remote?.let { routine.editFocusSchedule(it.id, draft.mode, draft.days, draft.start, draft.end, draft.packages, draft.intention, draft.trigger, draft.action) }
                            ?: routine.createFocusSchedule(draft.mode, draft.days, draft.start, draft.end, draft.packages, draft.intention, draft.trigger, draft.action)
                        remote = saved
                        val armed = scheduler.scheduleNext(FocusScheduler.Rule(saved.id, saved.daysOfWeek, LocalTime.parse(saved.startTime), LocalTime.parse(saved.endTime), saved.blockedPackages))
                        check(armed != null) { "Android did not schedule this focus window. Check Alarms & reminders access." }
                        armedThisAttempt = true
                        if (!saved.enabled) remote = routine.setFocusScheduleEnabled(saved.id, true)
                        FocusCommitmentCache.store(context, remote!!)
                        editing = false
                        result = "Scheduled on this device."
                    } catch (e: Exception) {
                        if (armedThisAttempt) { scheduler.cancel(); FocusCommitmentCache.clear(context) }
                        error = e.message ?: "Could not save this focus schedule."
                    }
                    finally { busy = false }
                }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Saving…" else "Save focus schedule") }
            if (remote != null) TextButton(onClick = { editing = false; draft = remote!!.toDraft() }) { Text("Cancel edit") }
        }
        if (!editing && remote == null) TextButton(onClick = { editing = true }) { Text("Set a focus schedule") }
        result?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    }
}
