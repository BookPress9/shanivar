package com.shanivar.nativeapp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tokens from android-app/wireframes/initiation-v1/style.css (HANDOFF "Visual implementation contract"). */
object Ini {
    val Ink = Color(0xFF0D0E0F)
    val Paper = Color(0xFFEEE9DE)
    val Bronze = Color(0xFFC49A69)
    val Muted = Color(0xFFAAA59C)
    val Line = Color(0xFF35322D)
    val Field = Color(0xFF191A1B)
    val PaperMuted = Color(0xFF5E5A52)
    val PaperEyebrow = Color(0xFF70502E)
    val OnBronze = Color(0xFF141210)
    /** Georgia in the wireframe; system serif keeps the app offline and font-free. */
    val Serif = FontFamily.Serif
    val H2 = TextStyle(fontFamily = Serif, fontSize = 37.sp, lineHeight = 42.sp, letterSpacing = (-1).sp, color = Paper)
    val H3 = TextStyle(fontFamily = Serif, fontSize = 23.sp, lineHeight = 29.sp)
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = Muted)
    val Eyebrow = TextStyle(fontSize = 10.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.SemiBold, color = Bronze)
    val Subtle = TextStyle(fontSize = 12.sp, lineHeight = 19.sp, color = Muted)
}

@Composable fun IniEyebrow(text: String, color: Color = Ini.Bronze) = Text(text, style = Ini.Eyebrow.copy(color = color), modifier = Modifier.padding(bottom = 10.dp))

/** app.js header(eyebrow, title, body). Titles keep the wireframe's line breaks ("\n"). */
@Composable fun IniHeader(eyebrow: String, title: String, body: String? = null, titleSize: Int = 37) {
    IniEyebrow(eyebrow)
    Text(title, style = Ini.H2.copy(fontSize = titleSize.sp, lineHeight = (titleSize * 1.13).sp), modifier = Modifier.padding(top = 12.dp, bottom = 18.dp))
    if (body != null) Text(body, style = Ini.Body, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))
}

@Composable fun IniPrimary(label: String, onClick: () -> Unit, enabled: Boolean = true) = Button(
    onClick = onClick, enabled = enabled, shape = RoundedCornerShape(4.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Ini.Bronze, contentColor = Ini.OnBronze),
    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
) { Text(label, fontWeight = FontWeight.SemiBold) }

@Composable fun IniSecondary(label: String, onClick: () -> Unit) = OutlinedButton(
    onClick = onClick, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Ini.Line),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ini.Paper),
    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 50.dp),
) { Text(label) }

@Composable fun IniText(label: String, onClick: () -> Unit) = TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
    Text(label, color = Ini.Muted, fontSize = 12.sp)
}

@Composable fun IniCard(paper: Boolean = false, content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxWidth().padding(vertical = 12.dp)
        .then(if (paper) Modifier.background(Ini.Paper, RoundedCornerShape(7.dp)) else Modifier.border(1.dp, Ini.Line, RoundedCornerShape(7.dp)))
        .padding(18.dp), content = content,
)

@Composable fun IniPaperTitle(text: String) = Text(text, style = Ini.H3.copy(color = Ini.Ink), modifier = Modifier.padding(vertical = 12.dp))
@Composable fun IniPaperBody(text: String) = Text(text, style = Ini.Body.copy(color = Ini.PaperMuted))

@Composable fun IniNotice(text: String, alert: Boolean = false) = Box(
    Modifier.fillMaxWidth().padding(vertical = 15.dp).background(Color(0x09C49A69))
        .drawBehind { drawRect(Ini.Bronze, size = Size(2.dp.toPx(), size.height)) }
        .padding(12.dp)
        .then(if (alert) Modifier.semantics { contentDescription = text } else Modifier),
) { Text(text, fontSize = 12.sp, color = Ini.Paper) }

@Composable fun IniSubtle(text: String) = Text(text, style = Ini.Subtle, modifier = Modifier.padding(vertical = 10.dp))

@Composable fun IniPill(text: String) = Text(text, fontSize = 10.sp, color = Ini.Bronze,
    modifier = Modifier.border(1.dp, Color(0xFF675439), RoundedCornerShape(30.dp)).padding(horizontal = 8.dp, vertical = 3.dp))

@Composable fun IniMotto() = Text("Discipline is freedom.", fontFamily = Ini.Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp,
    color = Ini.Bronze, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))

/** Time field: big bronze-on-field value; tap opens the platform 24h time picker. */
@Composable fun IniTimeField(label: String, value: java.time.LocalTime, onPick: () -> Unit) {
    Text(label, fontSize = 12.sp, color = Ini.Muted, modifier = Modifier.padding(top = 15.dp, bottom = 8.dp))
    Box(Modifier.fillMaxWidth().heightIn(min = 50.dp).background(Ini.Field, RoundedCornerShape(4.dp)).border(1.dp, Ini.Line, RoundedCornerShape(4.dp))
        .clickable(onClickLabel = "Change $label", onClick = onPick).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(value.toString(), fontSize = 25.sp, color = Ini.Paper)
    }
}

/**
 * The Living Orbit (style.css .orbit): dim sphere, rings after routine creation, three points, thicker ring
 * when mature. Ring drift is slow and ornamental; [still] freezes it (practice screen: the planet stays still,
 * PRACTICE_SAFETY) and so does reduced motion. Meaning is always in the content description.
 */
@Composable fun LivingOrbitView(view: OrbitView, still: Boolean, reduceMotion: Boolean) {
    val animate = !still && !reduceMotion
    val drift = if (animate) {
        val t = rememberInfiniteTransition(label = "drift")
        val v by t.animateFloat(-27f, -17f, infiniteRepeatable(tween(35_000, easing = LinearEasing), RepeatMode.Reverse), label = "ring")
        v
    } else -27f
    Box(Modifier.fillMaxWidth().height(215.dp).padding(top = 4.dp, bottom = 18.dp).semantics { contentDescription = "Living Orbit: " + view.description() },
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(260.dp).height(215.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = 60.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(Color(0xFFB19168), Color(0xFF554632), Color(0xFF18191A)),
                center = Offset(c.x - r * 0.36f, c.y - r * 0.5f), radius = r * 1.6f), r, c)
            if (view.ring) rotate(drift, c) {
                val w = if (view.secondPresence) 2.dp.toPx() else 1.dp.toPx()
                drawOval(Color(0x90C49A69), Offset(c.x - 125.dp.toPx(), c.y - 44.dp.toPx()), Size(250.dp.toPx(), 88.dp.toPx()), style = Stroke(w))
                drawOval(Color(0x30C49A69), Offset(c.x - 110.dp.toPx(), c.y - 37.5.dp.toPx()), Size(220.dp.toPx(), 75.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
            val pts = listOf(Offset(48.dp.toPx(), 136.dp.toPx()) to view.alarmPoint, Offset(size.width - 48.dp.toPx(), 67.dp.toPx()) to view.boundaryPoint,
                Offset(size.width - 117.dp.toPx(), size.height - 29.dp.toPx()) to view.practicePoint)
            pts.forEach { (o, lit) ->
                if (lit) drawCircle(Ini.Bronze.copy(alpha = 0.25f), 9.dp.toPx(), o)
                drawCircle(if (lit) Ini.Bronze else Color(0xFF514433), 3.5.dp.toPx(), o)
            }
        }
    }
}

@Composable fun IniProgress(index: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 44.dp)) { Text("← Back", color = Ini.Muted, fontSize = 11.sp) }
        Text("THE INITIATION", fontSize = 10.sp, letterSpacing = 2.sp, color = Ini.Muted)
        Text("${minOf(index, 9)} / 9", fontSize = 10.sp, letterSpacing = 2.sp, color = Ini.Muted)
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 27.dp).semantics { contentDescription = "Step ${minOf(index, 9)} of 9" }, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(9) { i -> Box(Modifier.weight(1f).height(2.dp).background(if (i < index) Ini.Bronze else Ini.Line)) }
    }
}

@Composable fun IniSpacer() = Spacer(Modifier.height(22.dp))
