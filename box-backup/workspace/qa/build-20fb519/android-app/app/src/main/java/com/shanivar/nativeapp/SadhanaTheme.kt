package com.shanivar.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.stateDescription
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
 * The Living Spine (founder update 13 Oct): 33 vertebra-like stages from base (1) to crown (33), with the seven
 * centre waypoints placed at their founder-defined stages. Plain labels only (naming rule). Clean and minimal:
 * thin bronze outlines, soft glow on reached stages, and a shimmer that travels up the lit trace at irregular,
 * randomised intervals and speeds (never periodic, never breath-paced). Off under reduced motion.
 * Selection is navigation; nothing here is an energy reading.
 */
@Composable fun SpineMap(selected: String, compact: Boolean, level: Int = 1, reduceMotion: Boolean = true, selfCheck: SelfCheck? = null, onSelect: (String) -> Unit) {
    // Vagal layer (founder approved 13 Oct): fireflies flicker independently at random times; never periodic,
    // never paced to breath, still under reduced motion. The nerve dash drifts one way at constant speed.
    val flies = remember { List(SadhanaSpine.fireflies.size) { androidx.compose.animation.core.Animatable(if (reduceMotion) 0.75f else 0.18f) } }
    if (!reduceMotion) flies.forEachIndexed { i, a ->
        LaunchedEffect(i) {
            val rnd = java.util.Random()
            while (true) {
                kotlinx.coroutines.delay(SadhanaSpine.fireflyGapMs(rnd))
                val glow = SadhanaSpine.fireflyGlowMs(rnd)
                a.animateTo(1f, androidx.compose.animation.core.tween(glow / 2)); a.animateTo(0.18f, androidx.compose.animation.core.tween(glow / 2))
            }
        }
    }
    val dash = if (reduceMotion) 0f else {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "nerve")
        val v by t.animateFloat(0f, -120f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(10_000, easing = androidx.compose.animation.core.LinearEasing)), label = "dash")
        v
    }
    val h = if (compact) 255.dp else 430.dp
    val shimmer = remember { androidx.compose.animation.core.Animatable(-1f) }
    if (!reduceMotion) LaunchedEffect(Unit) {
        val rnd = java.util.Random()
        while (true) {
            kotlinx.coroutines.delay(4_000L + rnd.nextInt(9_000))          // irregular gap 4-13 s
            shimmer.snapTo(0f)
            shimmer.animateTo(1f, androidx.compose.animation.core.tween(5_000 + rnd.nextInt(6_000), easing = androidx.compose.animation.core.FastOutSlowInEasing)) // 5-11 s pass
            shimmer.snapTo(-1f)
        }
    }
    // Root-branch tip pulse (founder 13 Oct): grows out and fades at random intervals 3-9 s, random durations.
    // Never periodic, never breath-paced. Under reduced motion the branches are static.
    val rootGrow = remember { androidx.compose.animation.core.Animatable(if (reduceMotion) 1f else 0f) }
    val rootAlpha = remember { androidx.compose.animation.core.Animatable(if (reduceMotion) 0.45f else 0f) }
    var rootSeed by remember { androidx.compose.runtime.mutableLongStateOf(7L) }
    if (!reduceMotion) LaunchedEffect(Unit) {
        val rnd = java.util.Random()
        while (true) {
            kotlinx.coroutines.delay(3_000L + rnd.nextInt(6_000))          // irregular gap 3-9 s
            rootSeed = rnd.nextLong()
            rootGrow.snapTo(0f); rootAlpha.snapTo(0.85f)
            rootGrow.animateTo(1f, androidx.compose.animation.core.tween(1_400 + rnd.nextInt(2_600)))   // grow 1.4-4 s
            rootAlpha.animateTo(0f, androidx.compose.animation.core.tween(1_800 + rnd.nextInt(3_200)))  // fade 1.8-5 s
        }
    }
    val tipRoots = remember(rootSeed) { SadhanaSpine.roots(rootSeed, if (compact) 22f else 34f) }
    val stageRoots = remember(rootSeed, level) {
        val rr = java.util.Random(rootSeed xor 0x5EED)
        if (level < 4) emptyList() else List(2) { val st = 2 + rr.nextInt(level - 2); st to SadhanaSpine.roots(rootSeed + st, if (compact) 12f else 20f, 2) }
    }
    val padTop = if (compact) 18.dp else 30.dp; val padBottom = if (compact) 18.dp else 34.dp
    Box(Modifier.fillMaxWidth().height(h).semantics { contentDescription = "Living Spine: 33 stages, seven centre waypoints and a symbolic vagal illustration. Practice stage $level of 33. A study map, not anatomy, not a measurement or energy reading." }) {
        Canvas(Modifier.matchParentSize()) {
            val cx = size.width / 2
            val top = padTop.toPx(); val bottom = size.height - padBottom.toPx()
            fun yOf(stage: Int) = bottom - (stage - 1) / 32f * (bottom - top)
            drawOval(Brush.radialGradient(listOf(Color(0x16D6A766), Color(0x08A4773B), Color.Transparent), Offset(cx, size.height / 2), size.width * 0.32f),
                Offset(size.width * 0.18f, size.height * 0.02f), Size(size.width * 0.64f, size.height * 0.96f))
            // Vagal orientation layer in the founder's 260x430 viewBox, centred on the trace.
            val vb = (bottom - top + 40.dp.toPx()) / 430f; val ox = cx - 130f * vb; val oy = top - 20.dp.toPx()
            fun P(p: Pair<Float, Float>) = Offset(ox + p.first * vb, oy + p.second * vb)
            val layerAlpha = if (compact) 0.55f else 1f
            SadhanaSpine.bodyLines.forEach { pts ->
                val path = androidx.compose.ui.graphics.Path().apply { moveTo(P(pts[0]).x, P(pts[0]).y); for (k in 1 until pts.size step 2) quadraticTo(P(pts[k]).x, P(pts[k]).y, P(pts[minOf(k + 1, pts.size - 1)]).x, P(pts[minOf(k + 1, pts.size - 1)]).y) }
                drawPath(path, Color(0x25D3B17E), style = Stroke(1.dp.toPx()))
            }
            val glow = selfCheck?.glow ?: 0.68f
            val dashOn = if (selfCheck == SelfCheck.STEADY) 2f else 5f; val dashOff = if (selfCheck == SelfCheck.STEADY) 3f else 7f
            SadhanaSpine.vagusPaths.forEach { pts ->
                val path = androidx.compose.ui.graphics.Path().apply { moveTo(P(pts[0]).x, P(pts[0]).y); var k = 1; while (k + 2 < pts.size + 0 && k + 2 <= pts.size - 1) { cubicTo(P(pts[k]).x, P(pts[k]).y, P(pts[k + 1]).x, P(pts[k + 1]).y, P(pts[k + 2]).x, P(pts[k + 2]).y); k += 3 } }
                drawPath(path, Color(0xFF9FD1A1).copy(alpha = glow * 0.25f * layerAlpha), style = Stroke(3.5.dp.toPx() * vb))
                drawPath(path, Color(0xFF9FD1A1).copy(alpha = glow * layerAlpha), style = Stroke(1.25f * vb, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(dashOn * vb, dashOff * vb), dash * vb)))
            }
            SadhanaSpine.fireflies.forEachIndexed { i, (fx, fy, fr) ->
                val a = flies[i].value * (selfCheck?.glow ?: 0.8f) * layerAlpha
                drawCircle(Color(0xFFF5D778).copy(alpha = a * 0.35f), fr * vb * 2.6f, P(fx to fy))
                drawCircle(Color(0xFFF5D778).copy(alpha = a), fr * vb, P(fx to fy))
            }
            val scale = if (compact) 0.7f else 1f
            for (s in 1..SadhanaSpine.STAGES) {
                val w = when (SadhanaSpine.region(s)) {
                    SadhanaSpine.Region.COCCYGEAL -> 16f; SadhanaSpine.Region.SACRAL -> 25f; SadhanaSpine.Region.LUMBAR -> 29f
                    SadhanaSpine.Region.THORACIC -> 23f; SadhanaSpine.Region.CERVICAL -> 19f
                }.dp.toPx() * scale
                val y = yOf(s); val hgt = (if (compact) 3.5f else 6f).dp.toPx()
                val lit = s <= level
                if (lit) drawRoundRect(Color(0x33C99B62), Offset(cx - w / 2 - 3, y - hgt / 2 - 3), Size(w + 6, hgt + 6), androidx.compose.ui.geometry.CornerRadius(hgt))
                drawRoundRect(if (lit) Color(0xCCD9AE72) else Color(0x40C99B62), Offset(cx - w / 2, y - hgt / 2), Size(w, hgt),
                    androidx.compose.ui.geometry.CornerRadius(hgt / 2), style = Stroke(1.dp.toPx()))
            }
            // Practice trace: thin line from base to the current stage.
            val yl = yOf(level)
            drawLine(Brush.verticalGradient(listOf(Color(0xFFFFF4C1), Color(0xFFEFBB67), Color(0xFFD15F45)), yl, bottom), Offset(cx, bottom), Offset(cx, yl), 1.5.dp.toPx())
            drawLine(Color(0x22C99B62), Offset(cx, yl), Offset(cx, top), 1.dp.toPx())
            // Root branches: bronze/amber to pink, thin, fading with depth.
            fun drawRoots(segs: List<SadhanaSpine.RootSegment>, ox: Float, oy: Float, strength: Float) {
                val g = rootGrow.value; val a = rootAlpha.value * strength
                if (a <= 0.01f) return
                segs.forEach { sg ->
                    val local = ((g - sg.order) / 0.25f).coerceIn(0f, 1f)
                    if (local <= 0f) return@forEach
                    val col = androidx.compose.ui.graphics.lerp(Color(0xFFE2B377), Color(0xFFE58AA8), (sg.depth / 3f).coerceAtMost(1f))
                    val x0 = ox + sg.x0.dp.toPx(); val y0 = oy + sg.y0.dp.toPx()
                    val x1 = x0 + (sg.x1 - sg.x0).dp.toPx() * local; val y1 = y0 + (sg.y1 - sg.y0).dp.toPx() * local
                    drawLine(col.copy(alpha = a * (1f - sg.depth * 0.2f)), Offset(x0, y0), Offset(x1, y1), (1.1f - sg.depth * 0.22f).dp.toPx().coerceAtLeast(0.4f))
                }
            }
            drawRoots(tipRoots, cx, yl, 1f)
            stageRoots.forEach { (st, segs) -> drawRoots(segs, cx, yOf(st), 0.35f) }
            val p = shimmer.value
            if (p in 0f..1f) {
                val ys = bottom - p * (bottom - yl)
                drawCircle(Brush.radialGradient(listOf(Color(0x99FFF2B2), Color.Transparent), Offset(cx, ys), 14.dp.toPx()), 14.dp.toPx(), Offset(cx, ys))
            }
        }
        // Centre waypoints at their stages.
        BoxWithConstraints(Modifier.matchParentSize()) {
            val topPx = padTop; val usable = maxHeight - padTop - padBottom
            SadhanaSpine.centres.forEach { c ->
                val stage = SadhanaSpine.threshold(c.id)
                val y = topPx + usable * ((33 - stage) / 32f)
                val sel = c.id == selected; val tone = Color(c.toneArgb); val reached = stage <= level
                val rowH = if (compact) 24.dp else 40.dp
                Box(Modifier.align(Alignment.TopCenter).offset(y = y - rowH / 2).width(190.dp).height(rowH)
                    .semantics { this.selected = sel; stateDescription = "stage $stage" }
                    .clickable(role = Role.Button, onClickLabel = "Open ${c.plainLabel}") { onSelect(c.id) }) {
                    Canvas(Modifier.size(if (compact) 12.dp else 16.dp).align(Alignment.Center)) {
                        val r = size.minDimension / 2
                        if (sel || reached) drawCircle(tone.copy(alpha = if (sel) 0.45f else 0.25f), r * 2.2f)
                        drawCircle(if (reached) tone else Sad.Void, r)
                        drawCircle(tone, r, style = Stroke(1.dp.toPx()))
                    }
                    if (!compact) Text(c.plainLabel.uppercase(), fontSize = 9.sp, letterSpacing = 1.5.sp, color = if (sel) Sad.Ivory else Sad.Ash,
                        modifier = Modifier.align(Alignment.CenterEnd).width(70.dp))
                }
            }
        }
    }
}

/** Founder selfCheck(): the user's report only. Reflective, not diagnostic; stored locally. */
@Composable fun SelfCheckCard(current: SelfCheck?, onPick: (SelfCheck) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp).border(1.dp, Sad.Line).background(Color(0xFF0E100E)).padding(15.dp)) {
        SMeta("CHECK IN · YOUR REPORT")
        Text("What is true right now?", style = Sad.H3, modifier = Modifier.padding(bottom = 12.dp))
        SelfCheck.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { st -> SSecondary(st.label, { onPick(st) }, Modifier.weight(1f).padding(0.dp), selectedState = current == st) }
            }
        }
        Text("The lights follow your selection. Shanivar is not measuring your nervous system.", fontSize = 10.sp, color = Sad.Ash, modifier = Modifier.padding(top = 4.dp))
        HorizontalDivider(color = Sad.Line, modifier = Modifier.padding(vertical = 11.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("NOW · ${current?.label?.uppercase() ?: "NOT ANSWERED"}", fontSize = 8.sp, letterSpacing = 1.2.sp, color = Sad.Bronze)
            Text("→", fontFamily = Sad.Serif, fontSize = 18.sp, color = Sad.Bronze)
            Text("CHOSEN DIRECTION · STEADIER", fontSize = 8.sp, letterSpacing = 1.2.sp, color = Sad.Bronze)
        }
    }
}

@Composable fun SMeter(percent: Int) = Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(2.dp).background(Color(0xFF292722))) {
    Box(Modifier.fillMaxWidth(percent / 100f).fillMaxHeight().background(Sad.Bronze))
}

@Composable fun SBreathNote(t: String) = Text(t, fontFamily = Sad.Serif, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = Sad.Bronze,
    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
