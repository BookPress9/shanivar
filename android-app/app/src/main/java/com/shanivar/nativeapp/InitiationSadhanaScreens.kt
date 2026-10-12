package com.shanivar.nativeapp

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Screen group 3: Sadhana entry, the 60-second practice, You Returned.
 * PRACTICE_SAFETY: stationary confirmation first; no pacing; the planet stays still during practice;
 * Pause and End always available; ending early is recorded as ended, never completed.
 */

@Composable fun SadhanaEntryScreen(c: InitiationController, reduceMotion: Boolean, run: IniRun) {
    var stationary by rememberSaveable { mutableStateOf(false) }
    LivingOrbitView(c.orbitView(), still = false, reduceMotion = reduceMotion)
    IniHeader("SADHANA", "One deliberate\nminute.", "Notice the breath already happening. Nothing to deepen, hold or control.")
    IniNotice("The planet moves. Your breath does not follow it. Pause or end whenever you need.")
    Row(Modifier.fillMaxWidth().toggleable(stationary, role = Role.Checkbox) { stationary = it }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(stationary, null, colors = CheckboxDefaults.colors(checkedColor = Ini.Bronze, checkmarkColor = Ini.Ink, uncheckedColor = Ini.Muted))
        Text("I’m stationary and in a suitable place.", fontSize = 12.sp, color = Ini.Muted, modifier = Modifier.padding(start = 10.dp))
    }
    IniSpacer()
    IniPrimary("Begin my minute →", { run { c.begin(stationary) } })
    IniText("Back to my day") { run { c.go(InitiationScreen.ROUTINE) } }
}

@Composable fun PracticeScreen(c: InitiationController, refreshKey: Int, run: IniRun) {
    var remaining by remember { mutableIntStateOf(c.tick()) }
    LaunchedEffect(c.practicePaused, refreshKey) {
        while (!c.practicePaused && c.ui.screen == InitiationScreen.PRACTICE) {
            delay(250)
            val r = c.tick()
            if (c.ui.screen != InitiationScreen.PRACTICE) { run { }; break }
            remaining = r
        }
    }
    IniHeader("SADHANA", if (c.practicePaused) "A moment to pause." else "Return to what is here.")
    LivingOrbitView(c.orbitView(), still = true, reduceMotion = true) // stationary planet: never a pacer
    Text("0:%02d".format(remaining), fontFamily = Ini.Serif, fontSize = 50.sp, color = Ini.Paper, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
    Text(InitiationController.guidance(remaining), style = Ini.Body,
        modifier = Modifier.padding(top = 10.dp, bottom = 20.dp).semantics { liveRegion = LiveRegionMode.Polite })
    IniSubtle("Natural breath. No pacing. No forcing.")
    IniSpacer()
    IniPrimary(if (c.practicePaused) "Resume" else "Pause", { run { c.togglePause() } })
    IniSecondary("End now") { run { c.endPractice() } }
}

@Composable fun ReturnedScreen(c: InitiationController, reduceMotion: Boolean, run: IniRun) {
    LivingOrbitView(c.orbitView(), still = false, reduceMotion = reduceMotion)
    if (c.lastCompleted) IniHeader("YOU RETURNED", "One deliberate\nminute.", "Not a transformation. One deliberate minute. Tomorrow, return again.")
    else IniHeader("YOU RETURNED", "You chose\nyour own ending.", "An attempt belongs in your record. Ending early carries no penalty.")
    IniSpacer()
    IniPrimary("Back to my day →", { run { c.go(InitiationScreen.ROUTINE) } })
    IniText("See my Living Orbit") { run { c.go(InitiationScreen.ORBIT) } }
}
