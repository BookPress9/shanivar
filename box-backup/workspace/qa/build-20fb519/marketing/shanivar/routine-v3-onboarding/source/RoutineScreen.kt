package com.shanivar.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Routine home surface (decision 0014 wireframe copy).
 * Renders stored [RoutinePlanV1] — never wireframe fixture times.
 */
@Composable
fun RoutineScreen(
    repository: RoutinePlanRepository,
    alarm: @Composable () -> Unit = {},
    focus: @Composable () -> Unit = {},
    nextAlarmEpochMillis: Long? = null,
    alarmArmed: Boolean = false,
    protectionStatusLabel: String? = null,
    protectionStatusByWindowId: Map<String, String> = emptyMap(),
    /** App protection summary card; receives the action that opens the editor. */
    protectionSummary: @Composable (plan: RoutinePlanV1, openEditor: () -> Unit) -> Unit = { _, _ -> },
    /** Full-screen App protection editor; null hides the route. */
    protectionEditor: (@Composable (close: () -> Unit) -> Unit)? = null,
) {
    val plan by repository.observe().collectAsState(initial = repository.current())
    var editing by remember { mutableStateOf(false) }
    var editingProtection by remember { mutableStateOf(false) }
    val healthy = repository.decodeHealthy()

    if (editingProtection && protectionEditor != null) {
        BackHandler { editingProtection = false }
        protectionEditor { editingProtection = false }
        return
    }

    if (editing) {
        EditRoutineScreen(
            initial = plan,
            onCancel = { editing = false },
            onSave = { saved ->
                // Caller persists via repository inside the editor.
                editing = false
            },
            repository = repository,
        )
        return
    }

    val snapshot = RoutineSnapshot(
        plan = plan,
        decodeHealthy = healthy,
        nextAlarmEpochMillis = nextAlarmEpochMillis,
        alarmArmed = alarmArmed,
        protectionStatusLabel = protectionStatusLabel,
        protectionStatusByWindowId = protectionStatusByWindowId,
    )
    val timeline = remember(plan, healthy, nextAlarmEpochMillis, alarmArmed, protectionStatusLabel, protectionStatusByWindowId) {
        RoutineSnapshotComposer.compose(snapshot)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "YOUR DAY",
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { contentDescription = "YOUR DAY"; heading() },
        )
        Heading("Not an ideal morning.\nYour actual life.")
        Text(
            "Wake, sleep, and what you put on the calendar — separate from alarms and protection.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val wake = plan.wakeTime?.toString()?.take(5) ?: "—"
            val sleep = plan.intendedSleepTime?.toString()?.take(5) ?: "—"
            Text("Wake $wake · Sleep $sleep", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(
                onClick = { editing = true },
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Edit Routine" },
            ) { Text("Edit Routine") }
        }

        if (!healthy) {
            SectionCard("Needs attention") {
                Text("Routine data could not be read cleanly. Recovery is still available. Re-save your day to repair.")
            }
        }

        SectionCard("Today") {
            if (timeline.isEmpty()) {
                Text("Nothing on the calendar yet. Edit Routine to set wake, sleep, and activities.")
            } else {
                timeline.forEach { item ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text("${item.timeLabel}  ${item.title}", style = MaterialTheme.typography.titleMedium)
                        item.detail?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        item.deviceStatus?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        alarm()
        protectionSummary(plan) { editingProtection = true }
        focus()
    }
}

/**
 * Edits wake/sleep anchors and personal activities in one scrollable flow.
 * Saving activities never arms alarms or Focus — only [RoutinePlanRepository.save].
 */
@Composable
fun EditRoutineScreen(
    initial: RoutinePlanV1,
    repository: RoutinePlanRepository,
    onCancel: () -> Unit,
    onSave: (RoutinePlanV1) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var wake by remember { mutableStateOf(initial.wakeTime?.toString()?.take(5).orEmpty()) }
    var sleep by remember { mutableStateOf(initial.intendedSleepTime?.toString()?.take(5).orEmpty()) }
    var activities by remember { mutableStateOf(initial.activities) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Edit Routine", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text("Anchors and personal activities. This does not arm an alarm or start protection.")

        OutlinedTextField(
            value = wake, onValueChange = { wake = it },
            label = { Text("I usually wake around (HH:mm)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = sleep, onValueChange = { sleep = it },
            label = { Text("I make room for sleep around (HH:mm)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        Text("Personal activities", style = MaterialTheme.typography.titleMedium)
        activities.forEachIndexed { index, activity ->
            ActivityEditor(
                activity = activity,
                onChange = { updated ->
                    activities = activities.toMutableList().also { it[index] = updated }
                },
                onDelete = {
                    activities = activities.filterNot { it.id == activity.id }
                },
            )
        }

        if (activities.size < RoutinePlanV1.MAX_ACTIVITIES) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoutineActivityQuickAdds.ALL.forEach { starter ->
                    TextButton(
                        onClick = {
                            activities = activities + RoutineActivityQuickAdds.create(starter)
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("+ ${starter.title}") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { activities = activities + RoutineActivityQuickAdds.blank() },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Add activity" },
                ) { Text("+") }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCancel, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
            Button(
                onClick = {
                    val wakeTime = parseOptionalTime(wake)
                    val sleepTime = parseOptionalTime(sleep)
                    if (wake.isNotBlank() && wakeTime == null) {
                        error = "Wake time must look like 07:00."; return@Button
                    }
                    if (sleep.isNotBlank() && sleepTime == null) {
                        error = "Sleep time must look like 22:30."; return@Button
                    }
                    val draft = initial.copy(
                        wakeTime = wakeTime,
                        intendedSleepTime = sleepTime,
                        activities = activities,
                    )
                    when (val v = RoutinePlanRules.validate(draft)) {
                        is RoutinePlanValidation.Error -> { error = v.message; return@Button }
                        RoutinePlanValidation.Ok -> Unit
                    }
                    busy = true
                    scope.launch {
                        val result = repository.save(draft)
                        busy = false
                        result.onSuccess {
                            error = null
                            onSave(draft)
                        }.onFailure {
                            error = it.message ?: "Could not save."
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(if (busy) "Saving…" else "Save") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActivityEditor(
    activity: RoutineActivityV1,
    onChange: (RoutineActivityV1) -> Unit,
    onDelete: () -> Unit,
) {
    SectionCard(activity.title.ifBlank { "New activity" }) {
        OutlinedTextField(
            value = activity.title,
            onValueChange = { onChange(activity.copy(title = it)) },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = activity.startTime.toString().take(5),
            onValueChange = { raw ->
                parseOptionalTime(raw)?.let { onChange(activity.copy(startTime = it)) }
            },
            label = { Text("Start (HH:mm)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = activity.durationMinutes.toString(),
            onValueChange = { raw ->
                raw.toIntOrNull()?.let { onChange(activity.copy(durationMinutes = it)) }
            },
            label = { Text("Duration (minutes)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Text("Occurrence")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OccurrenceChip("Every day", RoutineActivityOccurrence.kindOf(activity.days) == ActivityOccurrenceKind.EVERY_DAY) {
                onChange(activity.copy(days = RoutineActivityOccurrence.EVERY_DAY))
            }
            OccurrenceChip("Weekly", RoutineActivityOccurrence.kindOf(activity.days) == ActivityOccurrenceKind.WEEKLY) {
                val day = activity.days.firstOrNull() ?: 1
                onChange(activity.copy(days = setOf(day)))
            }
            OccurrenceChip("Custom", RoutineActivityOccurrence.kindOf(activity.days) == ActivityOccurrenceKind.CUSTOM) {
                if (activity.days.size <= 1) onChange(activity.copy(days = setOf(1, 3, 5)))
            }
        }
        if (RoutineActivityOccurrence.kindOf(activity.days) != ActivityOccurrenceKind.EVERY_DAY) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0..6).forEach { day ->
                    val selected = day in activity.days
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = activity.days.toMutableSet()
                            if (selected) next.remove(day) else next.add(day)
                            onChange(activity.copy(days = next))
                        },
                        label = { Text(RoutineActivityOccurrence.weekdayName(day)) },
                    )
                }
            }
        }
        TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) { Text("Delete activity") }
    }
}

@Composable
private fun OccurrenceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private fun parseOptionalTime(raw: String): LocalTime? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return runCatching { LocalTime.parse(trimmed.take(5)) }.getOrNull()
}
