package com.shanivar.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Sadhana screens C (DEV-33): Record, Without app, Deep study. */

@Composable private fun RecordRow(time: String, title: String, sub: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp).semantics(mergeDescendants = true) {}) {
        Text(time, fontSize = 11.sp, color = Sad.Bronze, modifier = Modifier.width(58.dp))
        Column(Modifier.padding(start = 13.dp)) {
            Text(title, fontWeight = FontWeight.Medium, color = Sad.Ivory, fontSize = 14.sp)
            Text(sub, fontSize = 10.sp, color = Sad.Ash)
        }
    }
    HorizontalDivider(color = Sad.Line)
}

@Composable fun RecordScreen(c: SadhanaController, today: java.time.LocalDate) {
    val r = c.record
    STop("←", "PRACTICE RECORD", "", onLeft = { c.go(SadhanaView.HOME) })
    SHead("FACTUAL HISTORY", "What you gave\nyour practice.", "Active time, attempts and independent reports stay separate.")
    SStats(r.attemptCount, r.activeMinutes, r.independentCount, listOf("ATTEMPTS", "ACTIVE MIN", "SELF-REPORTED"))
    if (r.attempts.isEmpty()) Text("No app-observed attempts yet.", style = Sad.Body)
    r.attempts.reversed().forEach { a ->
        val d = java.time.Instant.ofEpochMilli(a.startedAtUtc).atZone(java.time.ZoneId.of(a.zoneId)).toLocalDate()
        RecordRow(if (d == today) "Today" else d.toString(), SadhanaCatalog.byId(a.practiceId)?.title ?: a.practiceId,
            "${Math.round(a.activeSeconds / 60.0)} active min · ${a.outcome.name.lowercase()} · app-observed")
    }
    RecordRow("OWN", "Practised without Shanivar", "${r.independentCount} self-reported · private by choice")
    r.independent.reversed().forEach { o ->
        Row(Modifier.fillMaxWidth()) {
            Text("${o.localDate} · ${o.intendedMinutes ?: "—"} min intended · self-reported", fontSize = 10.sp, color = Sad.Ash, modifier = Modifier.weight(1f).padding(top = 14.dp))
            androidx.compose.material3.TextButton(onClick = { c.deleteOwn(o.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Delete", fontSize = 11.sp, color = Sad.Ash) }
        }
    }
    SSpacer()
    SPrimary("Practise without the player", { c.go(SadhanaView.INDEPENDENT) })
    SText("See capability path") { c.go(SadhanaView.PATH) }
}

@Composable fun IndependentScreen(c: SadhanaController, reduceMotion: Boolean) {
    STop("←", "CARRY", "", onLeft = { c.go(SadhanaView.HOME) })
    SRitual(c.orbitView(), still = false, reduceMotion = reduceMotion)
    SHead("WITHOUT THE APP", "The practice\nbelongs to you.", "Set the phone down. Use what you know. Return here only if recording it serves you.")
    SCard(light = true) {
        SMeta("CHOOSE YOUR OWN CONTAINER", light = true)
        SCardTitle("One return. No player.", light = true)
        SCardBody("Choose a familiar anchor. Practise for as long as you intended. Stop when you choose.", light = true)
    }
    Text("Minutes intended (private unless recorded)", fontSize = 11.sp, color = Sad.Ash, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(1, 3, 5, 10, 20).forEach { m -> SChoice("$m", c.ui.ownMinutes == m, { c.setOwnMinutes(m) }, Modifier.weight(1f)) }
    }
    SSpacer()
    SPrimary("I’ll practise now · hide guidance", { c.leaveToPractise() })
    SSecondary("I already practised · record it", { c.recordOwn() })
    SFine("You may leave without recording anything. Missing data remains missing.")
}

@Composable fun StudyScreen(c: SadhanaController, onInspect: (String) -> Unit = { c.inspectSource() }) {
    STop("←", "DEEP STUDY", "OPEN BY CHOICE", onLeft = { c.go(SadhanaView.HOME) })
    SHead("SOURCES, NOT STATUS", "Depth was never\nsomething to earn.", "Read across named traditions without turning them into one ladder.")
    listOf(
        Triple("VBT FOUNDATIONS", "Breath awareness adaptations", "Named passages · adaptation notes · open now"),
        Triple("MANOVIGYANA", "Models of mind kept distinct", "Inquiry · comparison · no diagnosis"),
        Triple("YOGA / BUDDHIST / VEDĀNTA", "Separate study lanes", "Different aims · terms · disagreements"),
    ).forEach { (m, t, b) -> SCard { SMeta(m); SCardTitle(t); SCardBody(b); SSecondary("Inspect lane →", { onInspect(m) }) } }
    // Naming rule (0017 as amended): centre names and attribution appear only here, in deep study.
    SCard {
        SMeta("SEVEN-CENTRE MAP · SOURCES PENDING VERIFICATION")
        SCardTitle("Living Spine names")
        SadhanaSpine.centres.forEach { s ->
            Text("${s.plainLabel} · ${s.deepStudyName}", color = Sad.Ivory, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            Text(s.deepStudyText, fontSize = 11.sp, lineHeight = 16.sp, color = Sad.Ash)
        }
        SFine(SadhanaSpine.centres.first().source.let { "${it.tradition}. ${it.limitations}" })
        Text("Practice sources: " + SadhanaCatalog.practices.joinToString(" · ") { "${it.title}: ${it.sourceDeepStudy}" }, fontSize = 11.sp, color = Sad.Ash)
    }
    SCard(locked = true) {
        SMeta("RESEARCH / QUALIFIED SETTING")
        SCardTitle("Intensive methods", alpha = 0.7f)
        SCardBody("Retention, forceful breathwork, fasting, sleep restriction, kundalini-oriented and altered-state practices are contextual study only. Practice activity never unlocks them.", alpha = 0.7f)
    }
}
