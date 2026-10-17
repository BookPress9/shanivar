package com.shanivar.nativeapp

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

/** Screen group 1: Arrival → Foundation (wireframe app.js screens 1-10). Copy is the founder's, verbatim. */

typealias IniRun = (suspend () -> Unit) -> Unit
typealias IniPickTime = (LocalTime, (LocalTime) -> Unit) -> Unit

@Composable internal fun iniFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ini.Paper, unfocusedTextColor = Ini.Paper, focusedContainerColor = Ini.Field, unfocusedContainerColor = Ini.Field,
    focusedBorderColor = Ini.Bronze, unfocusedBorderColor = Ini.Line, cursorColor = Ini.Bronze, focusedLabelColor = Ini.Muted, unfocusedLabelColor = Ini.Muted,
)

@Composable fun ArrivalScreen(c: InitiationController, reduceMotion: Boolean, run: IniRun) {
    Text("SHANIVAR", letterSpacing = 5.sp, fontSize = 15.sp, color = Ini.Paper)
    LivingOrbitView(c.orbitView(), still = false, reduceMotion = reduceMotion)
    IniHeader("BY THE HYBRID MONK", "Your life.\nYour command.", "You’ve spent enough time responding to everything around you. Now make room for what matters to you.", titleSize = 47)
    IniSpacer()
    IniPrimary("Take command →", { run { c.takeCommand() } })
    IniMotto()
}

@Composable fun IdentityScreen(c: InitiationController, run: IniRun) {
    var name by rememberSaveable { mutableStateOf(c.draft.displayName) }
    IniHeader("THE BEGINNING", "What should\nwe call you?", "Your name. Your beginning.")
    OutlinedTextField(name, { if (it.length <= 60) name = it }, label = { Text("Display name") }, placeholder = { Text("Enter your name") },
        singleLine = true, colors = iniFieldColors(), modifier = Modifier.fillMaxWidth())
    IniSpacer()
    IniPrimary("Continue →", { run { c.submitName(name) } })
    IniText("Start without an account") { run { c.continueAsGuest(name) } }
}

@Composable fun AuthScreen(c: InitiationController, run: IniRun) {
    var email by rememberSaveable { mutableStateOf("") }
    IniHeader("YOUR IDENTITY", "Keep your\nstructure with you.", "Choose a simple way in. You can also begin on this device.")
    // The wireframe's "Demo authentication only" notice becomes the honest state of this build (DEV-25).
    IniNotice("Google and email sign-in are not available yet. Continue on this device.")
    IniSecondary("Continue with Google") { run { c.google() } }
    IniSecondary("Continue with email") { run { c.openEmail() } }
    if (c.ui.emailStage) {
        OutlinedTextField(email, { email = it }, label = { Text("Email address") }, placeholder = { Text("you@example.com") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = iniFieldColors(), modifier = Modifier.fillMaxWidth().padding(top = 15.dp))
        IniSecondary("Send code →") { run { c.requestEmailCode(email) } }
    }
    IniSpacer()
    IniPrimary("Continue on this device", { run { c.continueAsGuest() } })
}

@Composable fun PropositionScreen(c: InitiationController, reduceMotion: Boolean, run: IniRun) {
    LivingOrbitView(c.orbitView(), still = false, reduceMotion = reduceMotion)
    IniHeader("THE PROPOSITION", "Let’s find out\nwhat you’re capable of.", "Not through motivation. Through what you choose to do, every day.")
    IniCard(paper = true) { IniPaperTitle("You bring the will."); IniPaperBody("Shanivar helps you build the structure.") }
    IniSpacer()
    IniPrimary("I’m ready →", { run { c.ready() } })
    IniSubtle("Start with a few minutes. Make it yours as you go.")
}

@Composable fun AnchorsScreen(c: InitiationController, run: IniRun, pick: IniPickTime) {
    var wake by rememberSaveable { mutableStateOf(c.ui.wake) }
    var sleep by rememberSaveable { mutableStateOf(c.ui.sleep) }
    IniHeader("YOUR RHYTHM", "Give your day\na beginning\nand an end.")
    IniTimeField("When do you usually wake up?", wake) { pick(wake) { wake = it } }
    IniTimeField("When do you make room for sleep?", sleep) { pick(sleep) { sleep = it } }
    IniSpacer()
    IniPrimary("Set my rhythm →", { run { c.saveAnchors(wake, sleep) } })
    IniSubtle("Starting points, not rules about how everyone should live.")
}

@Composable fun AlarmScreen(c: InitiationController, run: IniRun) {
    IniHeader("THE FIRST BOUNDARY", "Tomorrow begins\nwith a decision tonight.", "Make your chosen wake time your alarm?")
    IniCard(paper = true) {
        Text(c.ui.wake.toString(), style = Ini.H2.copy(color = Ini.Ink))
        IniPaperBody(c.alarmLabel())
    }
    if (c.ui.alarm == InitiationAlarm.FAILED || c.ui.alarm == InitiationAlarm.PERMISSION_REQUIRED)
        IniNotice("Keep a backup alarm. Review device settings and retry; configuration alone does not ensure a ring.")
    IniSpacer()
    if (c.ui.alarm == InitiationAlarm.SCHEDULED) IniPrimary("Continue →", { run { c.alarmContinue() } })
    else IniPrimary("Enable wake alarm →", { run { c.enableAlarm() } })
    IniText("I’ll use my existing alarm") { run { c.skipAlarm() } }
}

private val APP_GLYPHS = mapOf("com.instagram.android" to "◎", "com.google.android.youtube" to "▷", "com.facebook.katana" to "f",
    "com.reddit.frontpage" to "r", "com.zhiliaoapp.musically" to "♪", "com.twitter.android" to "X", "com.android.chrome" to "◉")

@Composable fun AppsScreen(c: InitiationController, apps: List<ProtectionAppOption>, run: IniRun) {
    IniHeader("RECLAIM YOUR ATTENTION", "Some things\ncan wait.", "Choose apps to make less accessible during your protected time.")
    Column {
        apps.forEach { app ->
            val checked = app.packageName in c.ui.selected
            Row(Modifier.fillMaxWidth().heightIn(min = 58.dp)
                .toggleable(checked, role = Role.Checkbox) { run { c.toggleApp(app.packageName) } }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(34.dp).border(1.dp, androidx.compose.ui.graphics.Color(0xFF635541), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                    Text(APP_GLYPHS[app.packageName] ?: app.label.take(1), color = Ini.Bronze, fontWeight = FontWeight.Bold)
                }
                Text(app.label + if (app.installed) "" else " · unavailable", color = Ini.Paper, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Checkbox(checked, null, colors = CheckboxDefaults.colors(checkedColor = Ini.Bronze, checkmarkColor = Ini.Ink, uncheckedColor = Ini.Muted))
            }
            HorizontalDivider(color = Ini.Line)
        }
    }
    IniSubtle("Essential and emergency apps remain accessible.")
    IniSpacer()
    IniPrimary("Continue →", { run { c.saveApps() } })
    IniText("Not now") { run { c.skipProtection() } }
}

@Composable fun WindowScreen(c: InitiationController, apps: List<ProtectionAppOption>, run: IniRun, pick: IniPickTime) {
    val w = c.ui.window
    IniHeader("YOUR FIRST BOUNDARY", "Make room for one\nuninterrupted hour.", "A starting suggestion. Change it to fit your day.")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) { IniTimeField("From", w.start) { pick(w.start) { t -> run { c.editWindow(start = t) } } } }
        Column(Modifier.weight(1f)) { IniTimeField("Until", w.end) { pick(w.end) { t -> run { c.editWindow(end = t) } } } }
    }
    Text("Repeat", fontSize = 12.sp, color = Ini.Muted, modifier = Modifier.padding(top = 15.dp, bottom = 8.dp))
    IniRepeatChoice(w.repeat) { r -> run { c.editWindow(repeat = r) } }
    IniSubtle(apps.filter { it.packageName in c.ui.selected }.joinToString(" · ") { it.label }.ifEmpty { "No apps selected" })
    IniSpacer()
    IniPrimary("Create this boundary →", { run { c.saveWindow() } })
    IniText("Change selected apps") { run { c.go(InitiationScreen.APPS) } }
}

@Composable fun IniRepeatChoice(value: String, onPick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InitiationRepeat.OPTIONS.forEach { opt ->
            val on = opt == value
            Box(Modifier.weight(1f).heightIn(min = 48.dp).border(1.dp, if (on) Ini.Bronze else Ini.Line, RoundedCornerShape(4.dp))
                .toggleable(on, role = Role.RadioButton) { onPick(opt) }, contentAlignment = Alignment.Center) {
                Text(opt, fontSize = 13.sp, color = if (on) Ini.Bronze else Ini.Paper)
            }
        }
    }
}

@Composable fun AccessScreen(c: InitiationController, access: AccessResume.Access, run: IniRun, openUsage: () -> Unit, openOverlay: () -> Unit) {
    IniHeader("ONE LAST DEVICE STEP", "Give your boundary\nthe tools it needs.", "Each access has a specific job. You can continue without it.")
    IniCard {
        Text("Usage access", style = Ini.H3.copy(color = Ini.Paper))
        IniSubtle("Recognize when a selected app is foregrounded.")
        IniPill(if (access.usage) "Granted" else "Access still needed")
        IniSecondary("Open usage settings ↗", openUsage)
    }
    IniCard {
        Text("Display over other apps", style = Ini.H3.copy(color = Ini.Paper))
        IniSubtle("Display the protection surface. Essential access and local exit stay available.")
        IniPill(if (access.overlay) "Granted" else "Access still needed")
        IniSecondary("Open overlay settings ↗", openOverlay)
    }
    IniNotice(c.boundaryLabel())
    IniSpacer()
    IniPrimary("Continue →", { run { c.accessContinue() } })
}

@Composable fun FoundationScreen(c: InitiationController, run: IniRun) {
    IniHeader("YOUR FOUNDATION", "It begins here.", "${c.draft.displayName.ifEmpty { "Your" }} day has a starting structure.")
    IniTimeline(c)
    IniCard(paper = true) { IniPaperTitle("You decide what happens\ninside these moments."); IniPaperBody("No score. No performance. A place to begin.") }
    IniSpacer()
    IniPrimary("Enter Shanivar →", { run { c.enter() } })
}

/** app.js timeline(): wake, Sadhana, protected time, activities, sleep. */
@Composable fun IniTimeline(c: InitiationController) {
    val u = c.ui
    Column(Modifier.padding(vertical = 20.dp)) {
        IniEvent(u.wake.toString(), "Wake", c.alarmLabel())
        IniEvent(u.practiceTime.toString(), "Sadhana · 1 min", "One deliberate return")
        if (u.window.saved) IniEvent(u.window.start.toString(), "Protected time", "Until ${u.window.end} · ${u.window.repeat}\n${c.boundaryLabel()}")
        u.activities.forEach { IniEvent(it.time.toString(), it.name, "${it.duration} min · ${it.repeat}") }
        IniEvent(u.sleep.toString(), "Make room for sleep", "Your chosen closing anchor")
    }
}

@Composable fun IniEvent(time: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(time, color = Ini.Bronze, fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp).then(Modifier.fillMaxWidth(0.2f)))
        Box(Modifier.padding(top = 6.dp).size(7.dp).border(4.dp, Ini.Bronze, RoundedCornerShape(50)))
        Column { Text(title, fontSize = 14.sp, color = Ini.Paper); Text(detail, fontSize = 10.sp, color = Ini.Muted, modifier = Modifier.padding(top = 3.dp)) }
    }
}
