package com.shanivar.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/* Sadhana screens B (DEV-32): practice detail, prepare, player, return. */

@Composable fun PracticeDetailScreen(c: SadhanaController, reduceMotion: Boolean) {
    val p = c.practice; val cap = p.capability?.label.orEmpty()
    STop("←", "PRACTICE", cap, onLeft = { c.go(SadhanaView.HOME) })
    SRitual(c.orbitView(), still = false, reduceMotion = reduceMotion)
    SHead(cap.uppercase(), p.title, p.prompt)
    SCard {
        SMeta("WHAT THIS ASKS")
        SCardBody("${p.effort}. You may change anchor, end early or return to a simpler practice.")
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            p.minutes.forEach { m -> SChoice("$m min", c.ui.duration == m, { c.setDuration(m) }, Modifier.weight(1f)) }
        }
    }
    HorizontalDivider(color = Sad.Line, modifier = Modifier.padding(top = 6.dp))
    Text("${p.sourcePlain}\n\n${p.boundary}", fontSize = 11.sp, lineHeight = 17.sp, color = Sad.Ash, modifier = Modifier.padding(top = 12.dp))
    SSpacer()
    SPrimary("Prepare · ${c.ui.duration} min →", { c.go(SadhanaView.PREPARE) })
    SText("Choose another practice") { c.anotherPractice() }
}

@Composable fun PrepareScreen(c: SadhanaController) {
    STop("←", "BEFORE YOU BEGIN", "", onLeft = { c.go(SadhanaView.PRACTICE) })
    SHead("YOUR CHOICE", "Set the conditions.\nKeep authority.", "Practise only while stationary and somewhere suitable. There is nothing to endure.")
    Row(Modifier.fillMaxWidth().toggleable(c.ui.stationary, role = Role.Checkbox) { c.setStationary(it) }.padding(vertical = 13.dp),
        verticalAlignment = Alignment.Top) {
        Checkbox(c.ui.stationary, null, colors = CheckboxDefaults.colors(checkedColor = Sad.Bronze, uncheckedColor = Sad.Ash, checkmarkColor = Sad.OnBronze))
        Column(Modifier.padding(start = 10.dp)) {
            Text("I am stationary and in a suitable place.", fontWeight = FontWeight.SemiBold, color = Sad.Ivory, fontSize = 14.sp)
            SFine("This is your confirmation; Shanivar does not infer safety.")
        }
    }
    HorizontalDivider(color = Sad.Line)
    Text("CHOOSE AN ANCHOR", style = Sad.Eyebrow, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        SChoice("Natural breath", c.ui.anchor == SadhanaAnchor.BREATH, { c.setAnchor(SadhanaAnchor.BREATH) }, Modifier.weight(1f))
        SChoice("The room", c.ui.anchor == SadhanaAnchor.ROOM, { c.setAnchor(SadhanaAnchor.ROOM) }, Modifier.weight(1f))
    }
    SNotice("If discomfort appears: stop, look around, change position or choose the room. End is always available.")
    SSpacer()
    SPrimary("Begin Sadhana →", { c.begin() })
}

/** Player: the planet stays still (PRACTICE_SAFETY); guidance is text only, never breath-timed; End top-right. */
@Composable fun PlayerScreen(c: SadhanaController) {
    val p = c.practice
    LaunchedEffect(c.running) { while (c.running) { c.tick(); delay(250) } }
    val secs = (c.remainingMillis + 999) / 1000
    STop(p.capability?.label.orEmpty().uppercase(), "ACTIVE PRACTICE", "END", onRight = { c.end() })
    SRitual(c.orbitView(), still = true, reduceMotion = true)
    Text("${secs / 60}:${"%02d".format(secs % 60)}", fontFamily = Sad.Serif, fontSize = 54.sp, color = Sad.Ivory, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth())
    Text(c.guidanceLine(), fontFamily = Sad.Serif, fontSize = 19.sp, lineHeight = 26.sp, color = Sad.Ivory, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().heightIn(min = 55.dp).padding(vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
    SBreathNote("The orbit stays still. Your breath does not follow anything.")
    SSpacer()
    SPrimary(if (c.running) "Pause" else "Resume", { c.togglePause() })
    SText("Change anchor") { c.changeAnchor() }
}

@Composable fun ReturnScreen(c: SadhanaController, reduceMotion: Boolean) {
    val completed = c.ui.lastOutcome == SadhanaOutcome.COMPLETED
    STop("SADHANA", if (completed) "COMPLETE" else "ENDED", "")
    SRitual(c.orbitView(), still = false, reduceMotion = reduceMotion)
    SHead("RETURN", if (completed) "You stayed.\nNow leave it here." else "You chose\nwhen to end.",
        if (completed) "${Math.round(c.ui.lastSeconds / 60.0)} active minutes. No score. No claim about what happened inside."
        else "The attempt remains true. Ending carries no penalty.")
    Text("WHAT WAS TRUE?", style = Sad.Eyebrow, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Usefulness.entries.forEach { u -> SSecondary(u.label, { c.reflect(u) }, Modifier.weight(1f), selectedState = c.reflection() == u) }
    }
    SNotice("No response is also valid. This reflection does not decide what you are ready for.")
    SSpacer()
    SPrimary("Return to my day →", { c.go(SadhanaView.HOME) })
    SText("View practice record") { c.go(SadhanaView.RECORD) }
}
