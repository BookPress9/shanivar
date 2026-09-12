package com.shanivar.nativeapp

import android.app.TimePickerDialog
import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray

private val Void = Color(0xFF0A0A0C)
private val Bone = Color(0xFFE8E3D8)
private val Gold = Color(0xFFC4AB72)
private val Muted = Color(0xFFB6B3AD)
private val Surface = Color(0xFF1A1B1F)

@Composable
fun ShanivarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Gold, onPrimary = Void, background = Void, onBackground = Bone,
        surface = Surface, onSurface = Bone, onSurfaceVariant = Muted, outline = Color(0xFF686B72)
    ), content = content)
}

private fun loadRhythm(context: Context): Rhythm {
    val prefs = context.getSharedPreferences("rhythm", Context.MODE_PRIVATE)
    return Rhythm(prefs.getString("wake", "07:00")!!, prefs.getString("quiet", "21:30")!!,
        prefs.getInt("focus", 25), prefs.getString("movement", "Scan a QR code")!!,
        prefs.getString("whatsapp", "08:00")!!, prefs.getString("tone", "Direct")!!)
        .takeIf { it.valid() } ?: Rhythm()
}

private fun saveRhythm(context: Context, rhythm: Rhythm) {
    context.getSharedPreferences("rhythm", Context.MODE_PRIVATE).edit()
        .putString("wake", rhythm.wakeTime).putString("quiet", rhythm.quietTime)
        .putInt("focus", rhythm.focusMinutes).putString("movement", rhythm.movement)
        .putString("whatsapp", rhythm.whatsappTime).putString("tone", rhythm.whatsappTone).apply()
}

@Composable
fun ShanivarApp() {
    val context = LocalContext.current
    var rhythm by remember { mutableStateOf(loadRhythm(context)) }
    var tab by rememberSaveable { mutableStateOf("Today") }
    var welcome by rememberSaveable { mutableStateOf(!context.getSharedPreferences("rhythm", 0).getBoolean("introduced", false)) }
    var accountDialog by rememberSaveable { mutableStateOf(false) }
    val session: AccountState = viewModel()
    val reflections: ReflectionState = viewModel()
    val reflectionStore = remember { EncryptedReflectionStore(context) }
    var reflectionOpen by rememberSaveable { mutableStateOf(false) }
    var reflectionEntry by rememberSaveable { mutableStateOf(ReflectionEntryPoint.TODAY) }
    val reflectionProtection = remember(session.api) {
        DisciplineEngine.create(context, session.api, AlarmScheduler(context)).reflectionReader()
    }
    val account = session.account
    LaunchedEffect(account?.id) {
        account?.let { reflections.bind(it.id, ReflectionApi(session.api), reflectionStore) }
    }
    LaunchedEffect(ReflectionNavigation.requested, account?.id) {
        ReflectionNavigation.requested?.let { entry ->
            if (account == null) accountDialog = true
            else { reflectionEntry = entry; reflectionOpen = true; ReflectionNavigation.consume() }
        }
    }
    val note = session.note
    val noteBusy = session.noteBusy
    val noteError = session.noteError
    var now by remember { mutableStateOf(LocalTime.now()) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var sessionId by rememberSaveable { mutableStateOf<String?>(null) }
    val techniques = remember { loadTechniques(context) }
    fun refreshNote() = session.refreshNote()
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            date = LocalDate.now()
            delay(30_000)
        }
    }
    LaunchedEffect(account?.id, date) { if (account != null) refreshNote() }
    ShanivarTheme {
        Surface(Modifier.fillMaxSize(), color = Void, contentColor = Bone) {
        if (welcome) {
            WelcomeScreen {
                context.getSharedPreferences("rhythm", 0).edit().putBoolean("introduced", true).apply()
                welcome = false
                tab = "Rhythm"
            }
        } else if (reflectionOpen && account != null && reflections.owner == account.id) {
            ReflectionScreen(reflections, reflectionEntry, protection = reflectionProtection,
                onExit = { reflectionOpen = false },
                onPractices = { reflectionOpen = false; tab = "Practice" })
        } else if (sessionId != null) {
            val technique = techniques.firstOrNull { it.id == sessionId }
            if (technique != null) PracticePlayer(technique) { sessionId = null }
        } else {
            Scaffold(containerColor = Void, bottomBar = {
                NavigationBar(containerColor = Void) {
                    listOf("Today" to "01", "Focus" to "02", "Practice" to "03", "Rhythm" to "04").forEach { (name, number) ->
                        NavigationBarItem(selected = tab == name, onClick = { tab = name },
                            icon = { Text(number, modifier = Modifier.clearAndSetSemantics { }) }, label = { Text(name) })
                    }
                }
            }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("SHANIVAR", color = Gold, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                            Text("BY THE HYBRID MONK", fontSize = 10.sp, color = Muted, letterSpacing = 1.sp)
                        }
                        TextButton(onClick = { accountDialog = true }) { Text(if (account == null) "Sign in" else "Account") }
                    }
                    when (tab) {
                        "Today" -> {
                            Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), color = Muted)
                            Heading(rhythm.moment(now).title)
                            Text(if (account == null) "A little structure. More room to live." else "Welcome, ${account!!.name}. Make this day your own.", color = Muted)
                            PrimaryAction(rhythm.moment(now).action) {
                                when (rhythm.moment(now)) {
                                    DayMoment.MORNING, DayMoment.EVENING -> sessionId = techniques.first().id
                                    DayMoment.FOCUS -> tab = "Focus"
                                    DayMoment.NIGHT -> tab = "Rhythm"
                                }
                            }
                            NoteCard(note, noteBusy, noteError, account != null, date, { accountDialog = true }, { refreshNote() })
                            SectionCard("Messages from Shanivar") {
                                Text("One observation. Your response. A little room for the next question.")
                                Text("AI-guided reflection ? sharing is optional", color = Muted, fontSize = 13.sp)
                                PrimaryAction("Reflect with Shanivar") {
                                    if (account == null) accountDialog = true else { reflectionEntry = ReflectionEntryPoint.TODAY; reflectionOpen = true }
                                }
                            }
                            SectionCard("Your rhythm") {
                                RhythmLine("Rise", rhythm.wakeTime, rhythm.movement)
                                HorizontalDivider()
                                RhythmLine("Protect", "${rhythm.focusMinutes} min", "Focus window · lock not connected")
                                HorizontalDivider()
                                RhythmLine("Unwind", rhythm.quietTime, "Make space before sleep")
                                TextButton(onClick = { tab = "Rhythm" }) { Text("Adjust your day") }
                            }
                            SectionCard("Keep returning") {
                                Text(account?.let { "${it.streak} day streak" } ?: "Your next practice is a beginning.", fontSize = 24.sp)
                                Text("A missed day is an invitation to return. Start where you are.", color = Muted)
                                if (account == null) Text("Sign in to see your saved streak.", color = Muted)
                            }
                        }
                        "Focus" -> FocusScreen(rhythm) { tab = "Rhythm" }
                        "Practice" -> {
                            Heading("Room to be.")
                            Text("Choose a quiet practice. Read at your pace, then sit with it.", color = Muted)
                            Text("Text sessions · local practice · no audio narration yet", color = Gold, fontSize = 12.sp)
                            techniques.forEach { technique ->
                                SectionCard(technique.title) {
                                    Text(technique.duration, color = Muted)
                                    Text(technique.essence, maxLines = 3)
                                    PrimaryAction("Open practice") { sessionId = technique.id }
                                }
                            }
                        }
                        "Rhythm" -> RhythmScreen(rhythm) { updated ->
                            saveRhythm(context, updated)
                            rhythm = updated
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        if (accountDialog) AccountDialog(account, session.api, onClose = { accountDialog = false },
            onConnected = { session.connect(it); accountDialog = false },
            onLogout = { ReflectionNavigation.consume(); reflections.clear(); reflectionStore.clear(); reflectionOpen = false; session.logout(); accountDialog = false })
        }
    }
}

@Composable
private fun WelcomeScreen(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("SHANIVAR / THE HYBRID MONK", color = Gold, letterSpacing = 2.sp)
        SaturnRing()
        Heading("Your attention.\nBack in your hands.")
        Text("A daily rhythm for less scrolling, a morning that gets you moving, and space to know yourself.", color = Muted, fontSize = 18.sp)
        SectionCard("A day with intention") {
            Text("01  Wake with a physical action")
            Text("02  Protect a window of attention")
            Text("03  Practice, reflect, return")
        }
        Text("You'll choose your rhythm first. App locks and alarms ask for Android access only when you enable them. Practice stays available either way.", color = Muted)
        PrimaryAction("Find my rhythm", onContinue)
    }
}

@Composable
private fun SaturnRing(progress: Float = 0.72f) {
    Canvas(Modifier.fillMaxWidth().height(140.dp).clearAndSetSemantics { }) {
        val radius = size.height * .36f
        drawCircle(Gold.copy(alpha = .28f), radius, style = Stroke(2.dp.toPx()))
        drawArc(Gold, -90f, progress * 360, false, Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2), style = Stroke(3.dp.toPx()))
        drawOval(Gold.copy(alpha = .65f), Offset(center.x - radius * 1.7f, center.y - radius * .32f),
            Size(radius * 3.4f, radius * .64f), style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun Heading(text: String) { Text(text, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() }) }

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(8.dp)) { Text(label) }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = Surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, color = Gold, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun RhythmLine(label: String, time: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Medium); Text(time, color = Gold)
        }
        Text(detail, color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun NoteCard(note: DailyNote?, busy: Boolean, error: String?, connected: Boolean, date: LocalDate, signIn: () -> Unit, retry: () -> Unit) {
    SectionCard("Your daily note") {
        when {
            busy -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Finding your note…") }
            error != null -> {
                Text(error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                TextButton(onClick = retry) { Text("Try again") }
            }
            note != null -> {
                Text(if (note.general) "General reflection" else "Personal reflection", color = Muted, fontSize = 12.sp)
                if (note.date.take(10) != date.toString()) Text("Saved note · ${note.date.take(10)}", color = Gold)
                Text(note.text, fontSize = 23.sp, lineHeight = 31.sp)
                Text("TRY TODAY", color = Gold, fontSize = 11.sp, letterSpacing = 2.sp)
                Text(note.action)
                Text(note.reflection, color = Muted)
                Text("Use this as a prompt for your own choices.", color = Muted, fontSize = 12.sp)
            }
            !connected -> {
                Text("One observation. One useful action. A question to carry with you.", fontSize = 20.sp)
                TextButton(onClick = signIn) { Text("Sign in for your note") }
            }
            else -> TextButton(onClick = retry) { Text("Load today's note") }
        }
    }
}

@Composable
private fun FocusScreen(rhythm: Rhythm, edit: () -> Unit) {
    Heading("Less noise.\nMore of you.")
    SaturnRing(0f)
    SectionCard("Your focus window") {
        Text("${rhythm.focusMinutes} minutes", fontSize = 32.sp)
        Text("Start with Instagram. Keep essential calls and the rest of your phone available.", color = Muted)
        Text("App lock is not connected in this build.", color = Gold)
        Text("Your plan is saved. Protection will need Usage Access and permission to display over other apps when the Android lock service is ready.", color = Muted)
        TextButton(onClick = edit) { Text("Change focus duration") }
    }
    SectionCard("A commitment you choose") {
        Text("Choose your window before you begin. See exactly what is blocked and when it ends.")
        Text("Paid lock options are being defined. No payment or lock is activated here.", color = Muted)
    }
}

@Composable
fun RhythmScreen(initial: Rhythm, onSave: (Rhythm) -> Unit) {
    val context = LocalContext.current
    var wake by rememberSaveable { mutableStateOf(initial.wakeTime) }
    var quiet by rememberSaveable { mutableStateOf(initial.quietTime) }
    var focus by rememberSaveable { mutableStateOf(initial.focusMinutes.toFloat()) }
    var movement by rememberSaveable { mutableStateOf(initial.movement) }
    var whatsapp by rememberSaveable { mutableStateOf(initial.whatsappTime) }
    var tone by rememberSaveable { mutableStateOf(initial.whatsappTone) }
    var saved by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun pickTime(value: String, update: (String) -> Unit) {
        val time = LocalTime.parse(value)
        TimePickerDialog(context, { _, h, m -> update("%02d:%02d".format(h, m)); saved = false }, time.hour, time.minute, true).show()
    }
    Heading("Built around\nyour day.")
    Text("Save a rhythm for this device. Change it whenever life changes.", color = Muted)
    SectionCard("Rise with a reason") {
        OutlinedButton(onClick = { pickTime(wake) { wake = it } }, modifier = Modifier.fillMaxWidth()) { Text("Wake time · $wake") }
        Text("How would you like to get moving?", color = Muted)
        listOf("Scan a QR code", "Complete a movement", "Solve a puzzle").forEach { choice ->
            FilterChip(selected = movement == choice, onClick = { movement = choice; saved = false }, label = { Text(choice) })
        }
        Text("Alarm not scheduled. Saving records your preference only.", color = Gold, fontSize = 13.sp)
    }
    SectionCard("Protect your attention") {
        Text("${focus.toInt()} minutes", fontSize = 28.sp)
        Slider(value = focus, onValueChange = { focus = it; saved = false }, valueRange = 5f..120f, steps = 22,
            modifier = Modifier.semantics { contentDescription = "Focus duration in minutes" })
    }
    SectionCard("One WhatsApp nudge") {
        Text("A message with a useful next step, at a time you choose.", color = Muted)
        OutlinedButton(onClick = { pickTime(whatsapp) { whatsapp = it } }, modifier = Modifier.fillMaxWidth()) { Text("Message time · $whatsapp") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Direct", "Gentle").forEach { choice ->
                FilterChip(selected = tone == choice, onClick = { tone = choice; saved = false }, label = { Text(choice) })
            }
        }
        Text("MESSAGE PREVIEW", color = Gold, fontSize = 11.sp, letterSpacing = 1.sp)
        Text(if (tone == "Direct") "Before the scroll: take four minutes for yourself. Your practice is waiting."
            else "A little room for you today. When you're ready, your practice is here.")
        Text("Not connected. Your number and consent will be requested before messages can start.", color = Muted, fontSize = 13.sp)
    }
    SectionCard("Leave room for rest") {
        OutlinedButton(onClick = { pickTime(quiet) { quiet = it } }, modifier = Modifier.fillMaxWidth()) { Text("Quiet time · $quiet") }
        Text("Your home screen follows this rhythm, including overnight schedules.", color = Muted)
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    PrimaryAction(if (saved) "Rhythm saved on this device" else "Save my rhythm") {
        val next = Rhythm(wake, quiet, focus.toInt(), movement, whatsapp, tone)
        if (!next.valid()) { error = "Choose wake and quiet times 6–20 hours apart."; saved = false }
        else { onSave(next); saved = true; error = null }
    }
}

@Composable
private fun AccountDialog(account: Account?, api: ShanivarApi, onClose: () -> Unit, onConnected: (Account) -> Unit, onLogout: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text(account?.name ?: "Welcome back.") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (account != null) {
                    Text("${account.streak} day streak")
                    Text("Your daily note and streak come from your Shanivar account.")
                } else {
                    Text("Connect your Shanivar account for your daily note and saved streak.")
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    error?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }, confirmButton = {
            if (account != null) TextButton(onClick = onLogout) { Text("Sign out") }
            else TextButton(enabled = !busy && email.isNotBlank() && password.isNotBlank(), onClick = {
                busy = true; error = null
                scope.launch {
                    try { onConnected(api.login(email.trim(), password)); password = "" }
                    catch (e: Exception) { error = if (e is ApiFailure) e.message else "Could not sign in. Check your connection and try again." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Connecting…" else "Sign in") }
        }, dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text("Close") } })
}

data class Technique(val id: String, val title: String, val essence: String, val duration: String, val script: String, val source: String)
private fun loadTechniques(context: Context): List<Technique> {
    val json = JSONArray(context.assets.open("techniques.json").bufferedReader().use { it.readText() })
    return (0 until json.length()).map {
        val item = json.getJSONObject(it)
        Technique(item.getString("id"), item.getString("title"), item.getString("essence"), item.getString("duration"),
            item.getString("script"), item.getString("source"))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticePlayer(technique: Technique, onExit: () -> Unit) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val passages = remember(technique.id) { technique.script.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() } }
    var duration by rememberSaveable(technique.id) { mutableStateOf(4) }
    var deadline by rememberSaveable(technique.id) { mutableStateOf(0L) }
    var remaining by rememberSaveable(technique.id) { mutableStateOf(0L) }
    var started by rememberSaveable(technique.id) { mutableStateOf(false) }
    var paused by rememberSaveable(technique.id) { mutableStateOf(false) }
    var exitConfirm by remember { mutableStateOf(false) }
    var sourceOpen by rememberSaveable { mutableStateOf(false) }
    var reflection by rememberSaveable(technique.id) {
        mutableStateOf(context.getSharedPreferences("reflections", 0)
            .getString("${LocalDate.now()}:${technique.id}", "").orEmpty())
    }
    var reflectionSaved by remember { mutableStateOf(false) }
    val complete = started && remaining <= 0
    val playerScroll = rememberScrollState()
    LaunchedEffect(started, complete) { playerScroll.scrollTo(0) }
    BackHandler { if (started && !complete) exitConfirm = true else onExit() }
    DisposableEffect(started, paused, complete) {
        val wasKeepingScreenOn = view.keepScreenOn
        view.keepScreenOn = started && !paused && !complete
        onDispose { view.keepScreenOn = wasKeepingScreenOn }
    }
    LaunchedEffect(deadline, paused) {
        if (deadline > 0 && !paused) {
            while (true) {
                remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                if (remaining == 0L) break
                delay(250)
            }
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
    Column(Modifier.weight(1f).verticalScroll(playerScroll).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = { if (started && !complete) exitConfirm = true else onExit() }) { Text("Back to practices") }
        Text("PRACTICE / SPACE TO EXPAND", color = Gold, letterSpacing = 2.sp, fontSize = 11.sp)
        Heading(if (complete) "Carry this\ninto your day." else technique.title)
        Text("App lock off", color = Muted)
        if (!started) {
            Text(technique.duration, color = Gold)
            Text("Follow one passage at a time. Pause whenever you want more space.", fontSize = 21.sp, lineHeight = 29.sp)
            Text(technique.essence, color = Muted, lineHeight = 25.sp)
            Text("Your time", color = Gold)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(3, 4, 5, 6).forEach { minutes ->
                    FilterChip(selected = duration == minutes, onClick = { duration = minutes }, label = { Text("$minutes min") })
                }
            }
            PrimaryAction("Begin quiet time") {
                remaining = duration * 60_000L
                deadline = SystemClock.elapsedRealtime() + remaining
                sourceOpen = false
                started = true
            }
            Text("Guided text. Your screen stays awake during practice.", color = Muted, fontSize = 13.sp)
        } else if (!complete) {
            val stage = passageIndex(remaining, duration * 60_000L, passages.size)
            SaturnRing(remaining.toFloat() / (duration * 60000f))
            val seconds = (remaining + 999) / 1000
            Text("%02d:%02d".format(seconds / 60, seconds % 60), fontSize = 56.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(if (paused) "Paused · Passage ${stage + 1} of ${passages.size}" else "Passage ${stage + 1} of ${passages.size}", color = Gold)
            Text(passages[stage], fontSize = 23.sp, lineHeight = 33.sp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        } else {
            Text("You made space. What would you like to take with you?", fontSize = 22.sp)
            OutlinedTextField(reflection, { reflection = it; reflectionSaved = false }, label = { Text("A reflection for yourself") }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = {
                context.getSharedPreferences("reflections", 0).edit()
                    .putString("${LocalDate.now()}:${technique.id}", reflection).apply()
                reflectionSaved = true
            }, enabled = reflection.isNotBlank()) { Text(if (reflectionSaved) "Saved on this device" else "Save reflection") }
            Text("This local session does not update your account streak yet.", color = Muted)
            PrimaryAction("Back to practices", onExit)
        }
        TextButton(onClick = { sourceOpen = !sourceOpen }) { Text(if (sourceOpen) "Hide source" else "Read source & adaptation") }
        if (sourceOpen) Text(technique.source, color = Muted, fontSize = 13.sp)
    }
    if (started && !complete) Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        PrimaryAction(if (paused) "Resume practice" else "Pause practice") {
            if (paused) deadline = SystemClock.elapsedRealtime() + remaining
            else remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            paused = !paused
        }
    }
    }
    if (exitConfirm) AlertDialog(onDismissRequest = { exitConfirm = false }, title = { Text("Leave this practice?") },
        text = { Text("The timer will stop. You can return whenever you're ready.") },
        confirmButton = { TextButton(onClick = onExit) { Text("End practice") } },
        dismissButton = { TextButton(onClick = { exitConfirm = false }) { Text("Stay") } })
}
