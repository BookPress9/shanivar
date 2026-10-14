package com.shanivar.nativeapp

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* Sadhana screens A (DEV-31): home, Living Spine, Path. Copy verbatim from app.js unless listed in 0017 honesty edits. */

@Composable fun SadhanaHomeScreen(c: SadhanaController, reduceMotion: Boolean = true) {
    val p = c.practice; val r = c.record
    STop("SADHANA", "", if (c.minimal) "GUIDANCE: MINIMAL" else "TODAY · ${c.nowLabel()}")
    SpineMap(c.ui.centre, compact = true, level = SadhanaSpine.stageLevel(c.record), reduceMotion = reduceMotion, selfCheck = c.ui.selfCheck) { c.selectCentre(it) }
    val d = c.ui.duration
    SHead("TODAY’S PRACTICE", "Give this\n$d honest minute${if (d == 1) "" else "s"}.", "Notice what happens. Then return to your day.")
    SCard(light = true) {
        SMeta("${p.capability?.label?.uppercase()} · ${p.effort.uppercase()}", light = true)
        SCardTitle(p.title, light = true); SCardBody(p.prompt, light = true)
        Row(Modifier.padding(vertical = 8.dp)) { STag("$d min"); STag("End anytime") }
        SPrimary("Start →", { c.go(SadhanaView.PRACTICE) })
    }
    SStats(r.attemptCount, r.activeMinutes, r.independentCount, listOf("ATTEMPTS", "ACTIVE MIN", "WITHOUT APP"))
    SText("Open the Living Spine") { c.go(SadhanaView.SPINE) }
    SText("Stage 1 · The base →") { c.chooseStage1() }
}

@Composable fun SpineScreen(c: SadhanaController, reduceMotion: Boolean = true) {
    val r = c.record; val centre = SadhanaSpine.byId(c.ui.centre)
    STop("←", "THE LIVING SPINE", "STUDY + PRACTICE", onLeft = { c.go(SadhanaView.HOME) })
    SHead("SEVEN-CENTRE STUDY MAP", "See the map.\nTest the practice.",
        "The chakras come from subtle-body traditions. The green-gold nerve is an illustration. Neither is a reading of your body.")
    SelfCheckCard(c.ui.selfCheck) { c.setSelfCheck(it) }
    val level = SadhanaSpine.stageLevel(r)
    SpineMap(c.ui.centre, compact = false, level = level, reduceMotion = reduceMotion, selfCheck = c.ui.selfCheck) { c.selectCentre(it) }
    val next = SadhanaSpine.nextCentre(level)
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, Sad.Line).padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("PRACTICE CURRENT", fontSize = 9.sp, letterSpacing = 1.5.sp, color = Sad.Bronze)
            Text(next?.let { val n = SadhanaSpine.threshold(it.id) - level; "$n segment${if (n == 1) "" else "s"} to ${it.plainLabel} context" } ?: "All context visible", fontSize = 10.sp, color = Sad.Ash)
        }
        Text("%02d / 33".format(level), fontFamily = Sad.Serif, fontSize = 28.sp, color = Sad.Ivory)
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp).border(1.dp, Sad.Line).background(androidx.compose.ui.graphics.Color(0xFF10110F)).padding(15.dp)) {
        val th = SadhanaSpine.threshold(centre.id); val revealed = th <= level
        Text("${centre.plainLabel.uppercase()} · THRESHOLD $th", fontSize = 11.sp, color = Sad.Ash)
        Text(centre.plainLabel, style = Sad.H3, modifier = Modifier.padding(vertical = 8.dp))
        Text(centre.plainNote, fontSize = 11.sp, lineHeight = 17.sp, color = Sad.Ash)
        Row(Modifier.padding(top = 8.dp)) { STag("SUBTLE-BODY MODEL"); STag("SOURCE CONTEXT") }
        HorizontalDivider(color = androidx.compose.ui.graphics.Color(0xFF3B342A), modifier = Modifier.padding(top = 15.dp, bottom = 12.dp))
        Text(if (revealed) "CONTEXT REVEALED" else "OPEN THE CONTEXT NOW", fontSize = 9.sp, letterSpacing = 1.4.sp, color = Sad.Bronze)
        Text("This area shows the source, its interpretation, Shanivar’s adaptation and disagreements. Practice history changes the presentation. It does not prove energy reached this centre.",
            fontSize = 11.sp, lineHeight = 17.sp, color = Sad.Ash, modifier = Modifier.padding(top = 8.dp))
        SText("Read the full context in Deep study") { c.go(SadhanaView.STUDY) }
    }
    SCard {
        SMeta("WHAT MOVES THE CURRENT"); SMeter(level * 100 / 33)
        SCardBody("One completed practice moves one segment. A practice you record without the app does the same. Early endings stay in your record. Missed days remove nothing.")
    }
    SSpacer()
    SPrimary("Choose a practice →", { c.go(SadhanaView.PRACTICE) })
    SText("Read the sources") { c.go(SadhanaView.STUDY) }
}

@Composable fun PathScreen(c: SadhanaController) {
    val inv = c.invitation
    STop("←", "CAPABILITY PATH", "", onLeft = { c.go(SadhanaView.HOME) })
    SHead("FROM GUIDANCE TO OWNERSHIP", "What can you do\nwith less prompting?", "Your history shows activity. It does not assign a spiritual level.")
    Column(Modifier.padding(vertical = 12.dp)) {
        SadhanaCapability.entries.forEach { cap ->
            val done = c.hasCapability(cap); val active = cap == inv && !done
            val state = when { done -> "recorded"; active -> "current invitation"; else -> "open" }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) { stateDescription = state }) {
                Box(Modifier.width(40.dp)) {
                    Box(Modifier.padding(start = 14.dp, top = 7.dp).size(11.dp)
                        .then(if (done || active) Modifier.shadow(6.dp, CircleShape, spotColor = Sad.Bronze) else Modifier)
                        .background(if (done || active) Sad.Bronze else Sad.Void, CircleShape).border(1.dp, Sad.Bronze, CircleShape))
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(cap.label, fontFamily = Sad.Serif, fontSize = 18.sp, color = if (active) Sad.Bronze else Sad.Ivory)
                    Text(cap.line, fontSize = 10.sp, color = Sad.Ash)
                }
            }
        }
    }
    SNotice("Current invitation: ${inv.invitation}. Produced by: ${CapabilityCriteria.VERSION}; any earlier practice stays open.")
    SText(if (c.minimal) "Use full guidance" else "Use fewer prompts") { c.fewerPrompts(!c.minimal) }
    SSpacer()
    SPrimary("Choose a practice →", { c.go(SadhanaView.PRACTICE) })
    SText("Record practice without Shanivar") { c.go(SadhanaView.INDEPENDENT) }
}

@Suppress("unused") private val unusedBold = FontWeight.Bold
