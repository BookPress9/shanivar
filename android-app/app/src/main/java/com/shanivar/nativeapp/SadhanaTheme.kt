package com.shanivar.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tokens from wireframes/sadhana-v1/style.css :root and HANDOFF "Aura and motion contract". */
object Sad {
    val Void = Color(0xFF080908)
    val Ink = Color(0xFF111210)
    val Ash = Color(0xFFA7A39A)
    val Ivory = Color(0xFFF0ECE1)
    val Bronze = Color(0xFFC99B62)
    val Line = Color(0xFF302D28)
    val Fine = Color(0xFF87837B)
    val Tab = Color(0xFF817D75)
    val OnBronze = Color(0xFF16120D)
    val LightBody = Color(0xFF625E56)
    val Serif = FontFamily.Serif
    val H1 = TextStyle(fontFamily = Serif, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp, color = Ivory)
    val H3 = TextStyle(fontFamily = Serif, fontSize = 22.sp, lineHeight = 28.sp, color = Ivory)
    val Body = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, color = Ash)
    val Eyebrow = TextStyle(fontSize = 10.sp, letterSpacing = 2.4.sp, fontWeight = FontWeight.Bold, color = Bronze)
    val Meta = TextStyle(fontSize = 10.sp, letterSpacing = 1.5.sp, color = Bronze)
}

@Composable fun STop(left: String, center: String, right: String, onLeft: (() -> Unit)? = null, onRight: (() -> Unit)? = null) =
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        @Composable fun cell(t: String, on: (() -> Unit)?, label: String) =
            if (on != null) TextButton(onClick = on, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).semantics { contentDescription = label }) {
                Text(t, color = Sad.Ash, fontSize = 11.sp, letterSpacing = 1.5.sp)
            } else Text(t, color = Sad.Ash, fontSize = 10.sp, letterSpacing = if (t == "SADHANA") 4.sp else 1.5.sp)
        cell(left, onLeft, if (left == "←") "Back" else left)
        Text(center, color = Sad.Ash, fontSize = 10.sp, letterSpacing = 1.5.sp)
        cell(right, onRight, if (right == "END") "End practice" else right)
    }

@Composable fun SHead(eye: String, title: String, body: String? = null) {
    Text(eye, style = Sad.Eyebrow, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
    Text(title, style = Sad.H1, modifier = Modifier.padding(bottom = 12.dp).semantics { heading() })
    if (body != null) Text(body, style = Sad.Body, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable fun SPrimary(label: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) = Button(
    onClick = onClick, shape = RoundedCornerShape(4.dp), colors = ButtonDefaults.buttonColors(containerColor = Sad.Bronze, contentColor = Sad.OnBronze),
    modifier = modifier.heightIn(min = 50.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
) { Text(label, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }

@Composable fun SSecondary(label: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth().padding(top = 10.dp), selectedState: Boolean? = null) = OutlinedButton(
    onClick = onClick, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, if (selectedState == true) Sad.Bronze else Sad.Line),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (selectedState == true) Sad.Bronze else Sad.Ivory),
    modifier = modifier.heightIn(min = 48.dp).then(if (selectedState != null) Modifier.semantics { selected = selectedState } else Modifier),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
) { Text(label, textAlign = TextAlign.Center) }

@Composable fun SChoice(label: String, on: Boolean, onClick: () -> Unit, modifier: Modifier) =
    if (on) SPrimary(label, onClick, modifier.semantics { selected = true }) else SSecondary(label, onClick, modifier, selectedState = false)

@Composable fun SText(label: String, onClick: () -> Unit) = TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
    Text(label, color = Sad.Ash, fontSize = 12.sp, textAlign = TextAlign.Center)
}

@Composable fun SCard(light: Boolean = false, locked: Boolean = false, content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxWidth().padding(vertical = 10.dp)
        .then(if (light) Modifier.background(Sad.Ivory, RoundedCornerShape(8.dp))
        else Modifier.background(Brush.linearGradient(listOf(Color(0xFF131411), Color(0xFF0C0D0C))), RoundedCornerShape(8.dp)).border(1.dp, Sad.Line, RoundedCornerShape(8.dp)))
        .padding(16.dp).then(if (locked) Modifier.semantics { contentDescription = "Locked: research or qualified setting only" } else Modifier),
    content = content,
)

@Composable fun SMeta(t: String, light: Boolean = false) = Text(t, style = Sad.Meta.copy(color = if (light) Color(0xFF7A5A33) else Sad.Bronze), modifier = Modifier.padding(bottom = 8.dp))
@Composable fun SCardTitle(t: String, light: Boolean = false, alpha: Float = 1f) = Text(t, style = Sad.H3.copy(color = (if (light) Color(0xFF161511) else Sad.Ivory).copy(alpha = alpha)), modifier = Modifier.padding(bottom = 6.dp))
@Composable fun SCardBody(t: String, light: Boolean = false, alpha: Float = 1f) = Text(t, style = Sad.Body.copy(color = (if (light) Sad.LightBody else Sad.Ash).copy(alpha = alpha), fontSize = 13.sp))

@Composable fun STag(t: String) = Text(t, fontSize = 9.sp, color = Sad.Bronze, modifier = Modifier.padding(2.dp)
    .border(1.dp, Color(0xFF5E4D39), RoundedCornerShape(30.dp)).padding(horizontal = 8.dp, vertical = 3.dp))

@Composable fun SNotice(t: String) = Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).background(Color(0x0AC99B62))
    .drawBehind { drawRect(Sad.Bronze, size = Size(2.dp.toPx(), size.height)) }.padding(horizontal = 12.dp, vertical = 10.dp)) {
    Text(t, fontSize = 11.sp, lineHeight = 17.sp, color = Color(0xFFCBC6BB))
}

@Composable fun SFine(t: String) = Text(t, fontSize = 11.sp, color = Sad.Fine, modifier = Modifier.padding(vertical = 8.dp))

@Composable fun SStats(a: Any, b: Any, c: Any, labels: List<String>) = Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
    listOf(a, b, c).forEachIndexed { i, v ->
        Column(Modifier.weight(1f).border(1.dp, Sad.Line).padding(vertical = 12.dp, horizontal = 8.dp).semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(v.toString(), fontFamily = Sad.Serif, fontSize = 24.sp, color = Sad.Bronze)
            Text(labels[i], fontSize = 9.sp, color = Sad.Ash, letterSpacing = 1.sp)
        }
    }
}

@Composable fun SSpacer() = Spacer(Modifier.height(20.dp))

/** The wireframe's "ritual": planet + orbit, drawn from Orbit evidence. Still when [still] or reduced motion. */
@Composable fun SRitual(view: OrbitView, still: Boolean, reduceMotion: Boolean) = LivingOrbitView(view, still, reduceMotion)

/**
 * The Living Spine map (spine.css). Waypoints carry plain labels only (naming rule, 0017 as amended).
 * Selection is navigation; nothing here is an energy reading.
 */
@Composable fun SpineMap(selected: String, compact: Boolean, onSelect: (String) -> Unit) {
    val h = if (compact) 255.dp else 390.dp
    Box(Modifier.fillMaxWidth().height(h).semantics { contentDescription = "Seven-centre subtle-body study map" }) {
        Canvas(Modifier.matchParentSize()) {
            val cx = size.width / 2
            drawOval(Brush.radialGradient(listOf(Color(0x12C99B62), Color(0x08A4773B), Color.Transparent), Offset(cx, size.height / 2), size.width * 0.3f),
                Offset(size.width * 0.2f, size.height * 0.04f), Size(size.width * 0.6f, size.height * 0.92f))
            val w = 36.dp.toPx(); val top = size.height * 0.05f; val bottom = size.height * 0.95f
            drawOval(Color(0x38C99B62), Offset(cx - w / 2, top), Size(w, bottom - top), style = Stroke(1.dp.toPx()))
            val lt = top + (bottom - top) * 0.03f; val lb = bottom - (bottom - top) * 0.03f
            drawRect(Brush.verticalGradient(listOf(Color(0xFFD7CCFF), Color(0xFF76B5CF), Color(0xFF83B884), Color(0xFFD8B66D), Color(0xFFD88A58), Color(0xFFB85F54)), lt, lb),
                Offset(cx - 1.dp.toPx(), lt), Size(2.dp.toPx(), lb - lt))
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceAround, horizontalAlignment = Alignment.CenterHorizontally) {
            SadhanaSpine.centres.forEach { c ->
                val sel = c.id == selected; val tone = Color(c.toneArgb)
                Box(Modifier.width(170.dp).height(if (compact) 28.dp else 44.dp)
                    .semantics { this.selected = sel }
                    .clickable(role = Role.Button, onClickLabel = "Open ${c.plainLabel}") { onSelect(c.id) }) {
                    Canvas(Modifier.size(13.dp).offset(x = 66.dp - 6.5.dp + if (compact) 10.dp else 0.dp).align(Alignment.CenterStart)) {
                        rotate(45f) {
                            if (sel) drawCircle(tone.copy(alpha = 0.35f), size.width * 1.4f)
                            drawRect(if (sel) tone else Sad.Void); drawRect(tone, style = Stroke(1.dp.toPx()))
                        }
                    }
                    if (!compact) Text(c.plainLabel.uppercase(), fontSize = 9.sp, letterSpacing = 1.5.sp, color = if (sel) Sad.Ivory else Sad.Ash,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 84.dp))
                }
            }
        }
    }
}

@Composable fun SMeter(percent: Int) = Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(2.dp).background(Color(0xFF292722))) {
    Box(Modifier.fillMaxWidth(percent / 100f).fillMaxHeight().background(Sad.Bronze))
}

@Composable fun SBreathNote(t: String) = Text(t, fontFamily = Sad.Serif, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = Sad.Bronze,
    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
