package com.shanivar.nativeapp

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * V3 first-run: set up Routine (actual day), not a skip-to-Home tour.
 *
 * Collects wake + intended sleep into [RoutinePlanV1]. Optional wake alarm uses the existing
 * [AlarmScheduler] locally (no account required to arm). Optional first protection window + apps
 * save through [ProtectionSyncingRoutinePlanRepository] so Focus sync follows only when protection
 * config changes. Permissions are explained honestly; declining leaves rules Saved, never Active.
 */
@Composable
fun RoutineOnboardingFlow(
    repository: RoutinePlanRepository,
    onWakeAlarmArmed: (days: Set<Int>, triggerAtEpochMillis: Long) -> Unit = { _, _ -> },
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(RoutineOnboardingState()) }
    var busy by remember { mutableStateOf(false) }
    var permissionRevision by remember { mutableIntStateOf(0) }
    OnAppResume { permissionRevision++ }
    val permissions = remember(permissionRevision) { AndroidDisciplinePermissions(context) }
    val missingAccess = remember(permissionRevision) {
        buildSet {
            if (!permissions.hasExactAlarmPermission()) add(FocusDeviceAccess.EXACT_ALARM)
            if (!permissions.hasUsageAccess()) add(FocusDeviceAccess.USAGE_ACCESS)
            if (!permissions.hasOverlayPermission()) add(FocusDeviceAccess.OVERLAY)
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRevision++
    }
    val apps = remember(permissionRevision) {
        ProtectionAppCatalog.options(state.draft.packages) { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        }
    }

    fun dispatch(event: RoutineOnboardingEvent) {
        state = RoutineOnboardingMachine.reduce(state, event)
        if (state.page == RoutineOnboardingPage.DONE) onComplete()
    }

    suspend fun commitAnchors(): Boolean {
        val plan = RoutineOnboardingMachine.planWithAnchors(repository.current(), state.draft)
        val result = repository.save(plan)
        if (result.isFailure) {
            dispatch(RoutineOnboardingEvent.ClearError())
            state = state.copy(draft = state.draft.copy(error = result.exceptionOrNull()?.message ?: "Could not save your times."))
            return false
        }
        return true
    }

    if (state.page == RoutineOnboardingPage.DONE) return

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("SHANIVAR", color = MaterialTheme.colorScheme.primary, letterSpacing = 3.sp)
        StepLabel(state.page)
        when (state.page) {
            RoutineOnboardingPage.WELCOME -> WelcomePage(onStart = { dispatch(RoutineOnboardingEvent.StartBuilding) })
            RoutineOnboardingPage.RHYTHM -> RhythmPage(
                draft = state.draft,
                busy = busy,
                onWake = { dispatch(RoutineOnboardingEvent.SetWake(it)) },
                onSleep = { dispatch(RoutineOnboardingEvent.SetSleep(it)) },
                onUse = {
                    val next = RoutineOnboardingMachine.reduce(state, RoutineOnboardingEvent.UseTheseTimes)
                    if (next.draft.error != null) {
                        state = next
                        return@RhythmPage
                    }
                    busy = true
                    scope.launch {
                        if (commitAnchors()) dispatch(RoutineOnboardingEvent.UseTheseTimes)
                        busy = false
                    }
                },
                onLater = {
                    busy = true
                    scope.launch {
                        if (commitAnchors()) dispatch(RoutineOnboardingEvent.SetRhythmLater)
                        busy = false
                    }
                },
                onBack = { dispatch(RoutineOnboardingEvent.Back) },
            )
            RoutineOnboardingPage.SETUP -> SetupPage(
                draft = state.draft,
                onAlarm = { dispatch(RoutineOnboardingEvent.OpenAlarm) },
                onProtection = { dispatch(RoutineOnboardingEvent.OpenProtection) },
                onPermissions = { dispatch(RoutineOnboardingEvent.OpenPermissions) },
                onRoutine = { dispatch(RoutineOnboardingEvent.GoToRoutine) },
                onBack = { dispatch(RoutineOnboardingEvent.Back) },
            )
            RoutineOnboardingPage.ALARM -> AlarmPage(
                draft = state.draft,
                busy = busy,
                hasExactAlarm = permissions.hasExactAlarmPermission(),
                hasNotifications = permissions.hasNotificationPermission(),
                onToggleEnabled = { dispatch(RoutineOnboardingEvent.SetAlarmEnabledChoice(it)) },
                onDays = { dispatch(RoutineOnboardingEvent.SetAlarmDays(it)) },
                onRequestExactAlarm = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                        )
                    }
                },
                onRequestNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onSave = {
                    if (!state.draft.alarmEnabledChoice) {
                        AlarmScheduler(context).cancel()
                        dispatch(RoutineOnboardingEvent.AlarmArmed(false, "Wake alarm left off. Your wake time is still an anchor."))
                        dispatch(RoutineOnboardingEvent.BackToSetup)
                        return@AlarmPage
                    }
                    if (state.draft.alarmDays.isEmpty()) {
                        dispatch(RoutineOnboardingEvent.AlarmArmed(false, "Choose at least one repeat day."))
                        return@AlarmPage
                    }
                    if (!permissions.hasExactAlarmPermission()) {
                        dispatch(RoutineOnboardingEvent.AlarmArmed(false, "Allow Alarms & reminders, then return and try again."))
                        return@AlarmPage
                    }
                    if (!permissions.hasNotificationPermission()) {
                        dispatch(RoutineOnboardingEvent.AlarmArmed(false, "Allow notifications so the wake screen can appear, then try again."))
                        return@AlarmPage
                    }
                    busy = true
                    scope.launch {
                        // Keep anchors coherent before arming; never invent a remote account id.
                        commitAnchors()
                        val trigger = AlarmScheduler(context).scheduleRule(
                            RoutineOnboardingMachine.LOCAL_WAKE_ALARM_ID,
                            state.draft.wakeTime,
                            state.draft.alarmDays,
                        )
                        if (trigger == null) {
                            dispatch(RoutineOnboardingEvent.AlarmArmed(false, "Android did not confirm the alarm. Check Alarms & reminders, then try again."))
                        } else {
                            onWakeAlarmArmed(state.draft.alarmDays, trigger)
                            val whenText = Instant.ofEpochMilli(trigger).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))
                            dispatch(RoutineOnboardingEvent.AlarmArmed(true, "Scheduled on this device · $whenText. Saving is not the same as ringing — the next trigger is what matters."))
                        }
                        busy = false
                    }
                },
                onLeaveOff = {
                    AlarmScheduler(context).cancel()
                    dispatch(RoutineOnboardingEvent.AlarmArmed(false, null))
                    dispatch(RoutineOnboardingEvent.BackToSetup)
                },
                onBack = { dispatch(RoutineOnboardingEvent.Back) },
            )
            RoutineOnboardingPage.PROTECTION -> ProtectionPage(
                draft = state.draft,
                apps = apps,
                busy = busy,
                onTogglePackage = { dispatch(RoutineOnboardingEvent.TogglePackage(it)) },
                onStart = { dispatch(RoutineOnboardingEvent.SetWindowStart(it)) },
                onEnd = { dispatch(RoutineOnboardingEvent.SetWindowEnd(it)) },
                onSave = {
                    val (plan, error) = RoutineOnboardingMachine.protectionPlanOrError(repository.current(), state.draft)
                    if (error != null || plan == null) {
                        dispatch(RoutineOnboardingEvent.ProtectionSaved(false, error))
                        return@ProtectionPage
                    }
                    busy = true
                    scope.launch {
                        // Anchors first so a protection-only save does not drop the day plan.
                        if (!state.anchorsCommitted) commitAnchors()
                        val withAnchors = RoutineOnboardingMachine.planWithAnchors(plan, state.draft)
                        val result = repository.save(withAnchors)
                        if (result.isFailure) {
                            dispatch(RoutineOnboardingEvent.ProtectionSaved(false, result.exceptionOrNull()?.message))
                        } else {
                            dispatch(
                                RoutineOnboardingEvent.ProtectionSaved(
                                    true,
                                    "Saved on this device. A saved window is not Active until the engine confirms it — check device access next.",
                                ),
                            )
                        }
                        busy = false
                    }
                },
                onSkip = { dispatch(RoutineOnboardingEvent.BackToSetup) },
                onBack = { dispatch(RoutineOnboardingEvent.Back) },
            )
            RoutineOnboardingPage.PERMISSIONS -> PermissionsPage(
                missing = missingAccess,
                note = state.draft.error,
                onContinue = { dispatch(RoutineOnboardingEvent.BackToSetup) },
                onRoutine = { dispatch(RoutineOnboardingEvent.GoToRoutine) },
            )
            RoutineOnboardingPage.DONE -> Unit
        }
    }
}

@Composable
private fun StepLabel(page: RoutineOnboardingPage) {
    val label = when (page) {
        RoutineOnboardingPage.WELCOME -> "01 · Welcome"
        RoutineOnboardingPage.RHYTHM -> "02 · Your day"
        RoutineOnboardingPage.SETUP, RoutineOnboardingPage.ALARM, RoutineOnboardingPage.PROTECTION, RoutineOnboardingPage.PERMISSIONS -> "03 · Guardrails"
        RoutineOnboardingPage.DONE -> ""
    }
    if (label.isNotEmpty()) Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
}

@Composable
private fun WelcomePage(onStart: () -> Unit) {
    Text("DISCIPLINE IS FREEDOM", color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, modifier = Modifier.semantics { heading() })
    Heading("Stop leaving your life\nto chance.")
    Text("Work gets calendars. Deadlines get reminders. Other people get your structure. Shanivar starts giving some of it back to you.")
    SectionCard("ROUTINE") {
        Text("Build the day you intend to live.", style = MaterialTheme.typography.titleMedium)
        Text("Wake. Focus. Protect time. Put your personal work on the calendar.")
    }
    SectionCard("SADHANA") {
        Text("Do the work. Need fewer prompts.", style = MaterialTheme.typography.titleMedium)
        Text("Train attention until the practice can leave the phone and enter your life. Progression waits — Steady is here when you are ready.")
    }
    PrimaryAction("Build my rhythm", onStart)
    Text(
        "Shanivar is not here to keep you busy. It is here to make what matters harder to ignore.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RhythmPage(
    draft: RoutineOnboardingDraft,
    busy: Boolean,
    onWake: (LocalTime) -> Unit,
    onSleep: (LocalTime) -> Unit,
    onUse: () -> Unit,
    onLater: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Text("YOUR DAY", color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, modifier = Modifier.semantics { heading() })
    Heading("Give the day\ntwo fixed points.")
    Text("Not an ideal morning. Your actual life.")
    SectionCard("Day anchors") {
        OutlinedButton(
            onClick = {
                TimePickerDialog(context, { _, h, m -> onWake(LocalTime.of(h, m)) }, draft.wakeTime.hour, draft.wakeTime.minute, true).show()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !busy,
        ) { Text("I usually wake around · ${draft.wakeTime.toString().take(5)}") }
        OutlinedButton(
            onClick = {
                TimePickerDialog(context, { _, h, m -> onSleep(LocalTime.of(h, m)) }, draft.intendedSleepTime.hour, draft.intendedSleepTime.minute, true).show()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !busy,
        ) { Text("I make room for sleep around · ${draft.intendedSleepTime.toString().take(5)}") }
        Text(
            "These organize your calendar. Your wake alarm remains a separate, optional choice.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        "We use your chosen times to place the day you can actually follow. The rest can be built around reality — not someone else's ideal morning.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    draft.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    PrimaryAction("Use these times") { if (!busy) onUse() }
    TextButton(onClick = { if (!busy) onLater() }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) {
        Text("Set this later")
    }
    TextButton(onClick = onBack, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) { Text("Back") }
}

@Composable
private fun SetupPage(
    draft: RoutineOnboardingDraft,
    onAlarm: () -> Unit,
    onProtection: () -> Unit,
    onPermissions: () -> Unit,
    onRoutine: () -> Unit,
    onBack: () -> Unit,
) {
    Text("SET THE GUARDRAILS", color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, modifier = Modifier.semantics { heading() })
    Heading("Set it once.\nThen get on with life.")
    Text("Two tools. Both editable. Neither should need your attention every day.")
    SectionCard("WAKE ALARM") {
        Text("Start when you said you would.", style = MaterialTheme.typography.titleMedium)
        Text(
            if (draft.alarmArmedOnDevice) "Alarm set for ${draft.wakeTime.toString().take(5)} on this device."
            else "Use your ${draft.wakeTime.toString().take(5)} anchor if you want Shanivar to wake you.",
        )
        TextButton(onClick = onAlarm, Modifier.heightIn(min = 48.dp)) {
            Text(if (draft.alarmArmedOnDevice) "Edit" else "Set up")
        }
    }
    SectionCard("APP PROTECTION") {
        Text("Choose what loses access to you.", style = MaterialTheme.typography.titleMedium)
        Text(
            when {
                draft.protectionSaved && draft.window != null ->
                    "1 protection window saved · ${draft.packages.size} app${if (draft.packages.size == 1) "" else "s"}. Saved is not Active."
                else -> "Choose distracting apps once. Create the window that fits your day. You can stop protection anytime."
            },
        )
        TextButton(onClick = onProtection, Modifier.heightIn(min = 48.dp)) {
            Text(if (draft.protectionSaved) "Edit" else "Set up")
        }
    }
    Text(
        "This is not discipline theatre. Remove the decision when you already know what the right decision is.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onPermissions, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Review device access")
    }
    PrimaryAction("Go to Routine", onRoutine)
    TextButton(onClick = onBack, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Back") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmPage(
    draft: RoutineOnboardingDraft,
    busy: Boolean,
    hasExactAlarm: Boolean,
    hasNotifications: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onDays: (Set<Int>) -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestNotifications: () -> Unit,
    onSave: () -> Unit,
    onLeaveOff: () -> Unit,
    onBack: () -> Unit,
) {
    Text("ROUTINE / ALARM", color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, modifier = Modifier.semantics { heading() })
    Heading("Start when\nyou said you would.")
    SectionCard("Wake alarm") {
        Text("Time · ${draft.wakeTime.toString().take(5)} (from your day anchor)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
            Switch(checked = draft.alarmEnabledChoice, onCheckedChange = onToggleEnabled, enabled = !busy)
            Text(if (draft.alarmEnabledChoice) "Enable this alarm" else "Alarm off")
        }
        Text("Repeat on", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEachIndexed { index, label ->
                FilterChip(
                    selected = index in draft.alarmDays,
                    onClick = {
                        val next = if (index in draft.alarmDays) draft.alarmDays - index else draft.alarmDays + index
                        onDays(next)
                    },
                    label = { Text(label) },
                    enabled = !busy,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
    SectionCard("Dismissal stays humane") {
        Text("The wake screen can ask you to stand or move, but an accessible non-physical exit remains available. A hard cap always ends ringing.")
    }
    if (!hasExactAlarm) {
        OutlinedButton(onClick = onRequestExactAlarm, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) {
            Text("Allow Alarms & reminders")
        }
    }
    if (!hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        OutlinedButton(onClick = onRequestNotifications, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) {
            Text("Allow notifications")
        }
    }
    draft.error?.let {
        Text(it, color = if (draft.alarmArmedOnDevice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    PrimaryAction(if (draft.alarmEnabledChoice) "Save alarm" else "Keep alarm off") { if (!busy) onSave() }
    TextButton(onClick = { if (!busy) onLeaveOff() }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) {
        Text("Leave alarm off")
    }
    TextButton(onClick = onBack, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) { Text("Back") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProtectionPage(
    draft: RoutineOnboardingDraft,
    apps: List<ProtectionAppOption>,
    busy: Boolean,
    onTogglePackage: (String) -> Unit,
    onStart: (LocalTime) -> Unit,
    onEnd: (LocalTime) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val window = draft.window ?: RoutineOnboardingMachine.defaultFirstWindow()
    Text("ROUTINE / APP PROTECTION", color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, modifier = Modifier.semantics { heading() })
    Heading("Choose what\ngets cut off.")
    Text("You already know which apps take more than they give. Select them once. Protection is voluntary and stoppable — never unbreakable.")
    apps.groupBy { it.category }.forEach { (category, options) ->
        Text(category.uppercase(), letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { app ->
                val selected = app.packageName in draft.packages
                FilterChip(
                    selected = selected,
                    enabled = !busy && (app.installed || selected),
                    onClick = { onTogglePackage(app.packageName) },
                    label = { Text(app.label + if (!app.installed) " · not installed" else "") },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
    if (apps.none { it.installed }) {
        Text("No supported distracting apps are installed on this device. You can still skip and set this later in Routine.")
    }
    SectionCard("First protection window") {
        OutlinedButton(
            onClick = {
                TimePickerDialog(context, { _, h, m -> onStart(LocalTime.of(h, m)) }, window.startTime.hour, window.startTime.minute, true).show()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !busy,
        ) { Text("Starts · ${window.startTime.toString().take(5)}") }
        OutlinedButton(
            onClick = {
                TimePickerDialog(context, { _, h, m -> onEnd(LocalTime.of(h, m)) }, window.endTime.hour, window.endTime.minute, true).show()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !busy,
        ) { Text("Ends · ${window.endTime.toString().take(5)}") }
        Text("Every day for this first window. Add more later in App protection. Saving schedules on this device; it does not mean Active.")
    }
    draft.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    PrimaryAction("Save & check device access") { if (!busy) onSave() }
    TextButton(onClick = { if (!busy) onSkip() }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) {
        Text("Skip for now")
    }
    TextButton(onClick = onBack, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) { Text("Back") }
}

@Composable
private fun PermissionsPage(
    missing: Set<FocusDeviceAccess>,
    note: String?,
    onContinue: () -> Unit,
    onRoutine: () -> Unit,
) {
    Text("DEVICE ACCESS", color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, modifier = Modifier.semantics { heading() })
    Heading("One system\ncheck remains.")
    Text("These grants are optional until you want protection or a wake alarm to work. Declining never deletes your saved plan. Shanivar will say Needs device access — not pretend you are protected.")
    SectionCard("What each grant is for") {
        Text("Alarms & reminders — schedule the next wake alarm and protection boundary exactly.")
        Text("Usage access — notice when a selected app is open so protection can respond.")
        Text("Display over other apps — show the protection screen without taking over the whole phone permanently.")
    }
    if (missing.isEmpty()) {
        Text("All three are granted on this device right now.", color = MaterialTheme.colorScheme.primary)
    } else {
        Text("Still missing on this device:", color = MaterialTheme.colorScheme.error)
        ProtectionRepairActions(missing)
    }
    note?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    Text(
        "Essential access stays available. Stop protection anytime — no waiting, no payment, no forced practice, no server.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PrimaryAction("Go to Routine", onRoutine)
    TextButton(onClick = onContinue, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Back to guardrails") }
}

/** SharedPreferences key for debug builds to force V3 onboarding while ROUTINE_V3 stays false. */
const val DEBUG_V3_ONBOARDING_PREF = "debug_v3_onboarding"

fun readDebugV3OnboardingPref(context: android.content.Context): Boolean =
    context.getSharedPreferences("rhythm", 0).getBoolean(DEBUG_V3_ONBOARDING_PREF, false)
