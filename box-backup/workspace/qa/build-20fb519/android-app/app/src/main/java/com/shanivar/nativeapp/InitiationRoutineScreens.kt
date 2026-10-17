package com.shanivar.nativeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

/** Screen group 2: Routine (normal destination) and Edit Routine. */

@Composable fun RoutineHomeScreen(c: InitiationController, run: IniRun) {
    val name = c.draft.displayName
    IniHeader("ROUTINE", "${name.ifEmpty { "Your" }}${if (name.isNotEmpty()) "’s" else ""} day.\nYour decisions.")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("TODAY", fontSize = 10.sp, letterSpacing = 2.sp, color = Ini.Muted)
        TextButton(onClick = { run { c.go(InitiationScreen.EDIT) } }) { Text("Edit Routine ↗", color = Ini.Muted, fontSize = 11.sp) }
    }
    IniTimeline(c)
    IniCard(paper = true) {
        IniEyebrow("YOUR FIRST CHALLENGE", Ini.PaperEyebrow)
        IniPaperTitle("Give yourself one minute.")
        IniPaperBody("Before trying to change your thoughts, notice your natural breath.")
        Column(Modifier.padding(top = 12.dp)) { IniPrimary("Begin Sadhana →", { run { c.go(InitiationScreen.SADHANA) } }) }
    }
    if (c.ui.window.saved) IniSecondary(if (c.ui.window.enabled) "Turn protection off locally" else "Enable saved boundary") { run { c.toggleProtection() } }
    else IniSecondary("Choose a boundary") { run { c.go(InitiationScreen.APPS) } }
    IniText("View your Living Orbit") { run { c.go(InitiationScreen.ORBIT) } }
    IniText("First-week invitation") { run { c.go(InitiationScreen.WEEK) } }
}

@Composable fun EditRoutineScreen(c: InitiationController, run: IniRun, pick: IniPickTime) {
    var wake by rememberSaveable { mutableStateOf(c.ui.wake) }
    var practice by rememberSaveable { mutableStateOf(c.ui.practiceTime) }
    var sleep by rememberSaveable { mutableStateOf(c.ui.sleep) }
    var activity by rememberSaveable { mutableStateOf("") }
    var activityTime by rememberSaveable { mutableStateOf(LocalTime.of(18, 0)) }
    var minutes by rememberSaveable { mutableStateOf("20") }
    var repeat by rememberSaveable { mutableStateOf("Every day") }
    IniHeader("MAKE IT YOURS", "Structure can change.")
    IniTimeField("Wake anchor", wake) { pick(wake) { wake = it } }
    IniTimeField("Sadhana placement", practice) { pick(practice) { practice = it } }
    IniTimeField("Sleep anchor", sleep) { pick(sleep) { sleep = it } }
    OutlinedTextField(activity, { if (it.length <= 80) activity = it }, label = { Text("Add personal activity (optional)") },
        placeholder = { Text("Read, walk, train…") }, singleLine = true, colors = iniFieldColors(), modifier = Modifier.fillMaxWidth().padding(top = 15.dp))
    IniTimeField("Activity start", activityTime) { pick(activityTime) { activityTime = it } }
    OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(4) }, label = { Text("Minutes") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = iniFieldColors(), modifier = Modifier.fillMaxWidth().padding(top = 15.dp))
    Text("Repeat", fontSize = 12.sp, color = Ini.Muted, modifier = Modifier.padding(top = 15.dp, bottom = 8.dp))
    IniRepeatChoice(repeat) { repeat = it }
    c.ui.activities.forEach { a ->
        IniNotice("${a.name} · ${a.time}")
        IniText("Remove") { run { c.removeActivity(a.id) } }
    }
    IniSpacer()
    IniPrimary("Save my Routine →", { run { c.saveEdit(wake, practice, sleep, activity, activityTime, minutes.toIntOrNull(), repeat) } })
    IniText("Cancel") { run { c.go(InitiationScreen.ROUTINE) } }
}
