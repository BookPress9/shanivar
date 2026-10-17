package com.shanivar.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun ReflectionScreen(state: ReflectionState, entry: ReflectionEntryPoint,
    protection: ReflectionProtectionReader = UnavailableReflectionProtection,
    onExit: () -> Unit, onPractices: () -> Unit) {
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current
    var protectionText by remember { mutableStateOf("Checking protection…") }
    var showContext by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showEarlierDraft by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    val s = state.session
    val card = state.displayed
    val scroll = rememberScrollState()
    var currentDate by remember { mutableStateOf(LocalDate.now().toString()) }
    LaunchedEffect(s?.timezone) {
        while(true) {
            currentDate = runCatching { LocalDate.now(ZoneId.of(s?.timezone ?: "UTC")).toString() }.getOrDefault("")
            delay(30_000)
        }
    }
    val advanceAtBottom = remember(state.displayedId, state.canContinue) {
        object : NestedScrollConnection {
            var distance = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if(state.canContinue && source == NestedScrollSource.UserInput && available.y < 0) {
                    distance -= available.y
                    if(distance > 72f) { distance=0f; state.next() }
                } else if(available.y > 0) distance=0f
                return Offset.Zero
            }
        }
    }
    fun readProtection() { scope.launch {
        protectionText = try {
            when(protection.status().protection) {
                ProtectionState.ACTIVE -> "Focus protected"
                ProtectionState.OFF -> "App lock off"
                ProtectionState.NEEDS_PERMISSION -> "Protection needs Android permission"
                ProtectionState.UNAVAILABLE -> "App lock not connected"
            }
        } catch (_: Exception) { "Protection status unavailable" }
    } }
    DisposableEffect(lifecycle, protection) {
        val observer = LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_RESUME) { readProtection(); if(state.consent) state.refresh() }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { if(state.consent) state.refresh() }
    LaunchedEffect(protection) { while(true) { readProtection(); delay(15_000) } }
    LaunchedEffect(card?.id) { scroll.scrollTo(0); showContext=false }
    BackHandler(onBack = onExit)
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp), horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick=onExit) { Text("Back") }
            TextButton(onClick=onPractices) { Text("Offline practices") }
        }
        Column(Modifier.weight(1f).nestedScroll(advanceAtBottom).verticalScroll(scroll).padding(horizontal=24.dp, vertical=12.dp),
            verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text("MESSAGES FROM SHANIVAR", color=MaterialTheme.colorScheme.primary, fontSize=12.sp,
                modifier=Modifier.semantics { heading() })
            Text("AI-guided reflection", style=MaterialTheme.typography.labelLarge)
            Text(protectionText, color=MaterialTheme.colorScheme.onSurfaceVariant, fontSize=13.sp)
            if(entry==ReflectionEntryPoint.BLOCKED_APP_SURFACE)
                Text("Reflecting does not unlock the other app or dismiss an alarm.", fontSize=13.sp)
            if(!state.consent) {
                Text("A little room to hear yourself.", fontSize=30.sp, lineHeight=36.sp)
                Text("Read a message, respond in your own way, and discover the next question. You can leave whenever you want.")
                Text("These messages are generated by AI. Relevant responses and available chart context are processed by our AI provider to shape this session. Astrology is an interpretive framework, not a diagnosis or prediction.")
                Text("Your responses are private. They stay with your session until you delete it or your account. An encrypted copy and drafts are saved on this device. Deleting here does not promise immediate deletion from the provider's systems.")
                Text("You can always choose “Prefer not to share.”")
                Button(onClick={state.acceptConsent(entry)}, enabled=!state.busy, modifier=Modifier.fillMaxWidth()) { Text("Begin reflecting") }
            } else {
                if(state.offline) Text("Saved on this device · current server state unconfirmed", color=MaterialTheme.colorScheme.primary)
                state.error?.let {
                    Text(it, color=MaterialTheme.colorScheme.error, modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
                    TextButton(onClick=state::refresh, enabled=!state.busy) { Text("Check connection and recover") }
                }
                if(state.earlierDraft.isNotEmpty()) {
                    TextButton(onClick={showEarlierDraft=!showEarlierDraft}) { Text("Earlier local wording") }
                    if(showEarlierDraft) {
                        Text("The session changed while you were writing. This wording is kept separately; read history for the response the server saved.")
                        Text(state.earlierQuestion, fontSize=14.sp)
                        Text(state.earlierDraft)
                    }
                }
                if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(s==null) {
                    if(state.pending!=null) {
                        Text("A request is waiting for confirmation. Recover it before starting another.")
                        Button(onClick=state::refresh, enabled=!state.busy) { Text("Recover session") }
                    } else Button(onClick={state.start(entry)},enabled=!state.busy) { Text("Start a reflection") }
                } else {
                    Text("${s.answered} of ${s.limit} responses saved", fontSize=13.sp)
                    Text("Session context · ${s.localDate.take(10)} · ${s.timezone}", fontSize=12.sp)
                    if(s.localDate.take(10) != currentDate) {
                        Text("Saved session from an earlier day. Its context stays pinned to that date.")
                        TextButton(onClick={state.start(entry)}, enabled=!state.busy && state.pending==null) { Text("Start fresh for today") }
                    }
                    if(s.history.isNotEmpty()) {
                        TextButton(onClick={showHistory=!showHistory}) { Text(if(showHistory) "Hide answered messages" else "Read answered messages") }
                        if(showHistory) {
                            s.history.forEach { item ->
                                TextButton(onClick={state.showCard(item.card.id); showHistory=false}) { Text("Message ${item.card.index+1}: ${item.card.question}") }
                            }
                            s.current?.let { current ->
                                TextButton(onClick={state.showCard(current.id); showHistory=false}) { Text("Current message") }
                            }
                        }
                    }
                    if(card!=null) {
                        Text(card.sender, color=MaterialTheme.colorScheme.primary)
                        // Render only plain text: no URL, HTML or model-provided action execution.
                        Text(card.message, fontSize=22.sp, lineHeight=32.sp)
                        Text(card.question, fontSize=26.sp, lineHeight=34.sp, modifier=Modifier.semantics { heading() })
                        Text(card.contextLabel(), fontSize=13.sp, color=MaterialTheme.colorScheme.primary)
                        TextButton(onClick={showContext=!showContext}) { Text("Why this reflection?") }
                        if(showContext) {
                            Text("Context as of ${s.asOf}. Session mode: ${s.mode.replace('_',' ')}.", fontSize=13.sp)
                            Text(if(card.provenance=="general_fallback") "This card is a general fallback; it is not a personalized chart reading."
                                else if(card.provenance=="general") "No personal chart facts are used for this reflection."
                                else "Chart references support a reflective interpretation. You decide what fits.", fontSize=13.sp)
                            card.context.forEach { Text(it, fontSize=13.sp) }
                            if(card.context.isEmpty()) Text("No chart references attached.", fontSize=13.sp)
                        }
                        state.answered?.let { answer ->
                            val responseText = if(answer.kind=="option") card.options.find { it.id==answer.value }?.label ?: answer.value else answer.value
                            Text("Response saved", modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
                            Text(responseText)
                        }
                        if(state.answered==null && s.current?.id==card.id && s.status=="awaiting_response") {
                            Text("Choose a response, or write your own. Sharing personal details is optional.", fontSize=14.sp)
                            card.options.forEach { option ->
                                OutlinedButton(onClick={state.choose(option.id)}, enabled=state.canAnswer,
                                    modifier=Modifier.fillMaxWidth().semantics { selected=state.selected==option.id }) {
                                    Text(if(state.selected==option.id) "Selected: ${option.label}" else option.label)
                                }
                            }
                            OutlinedTextField(value=state.draft,onValueChange=state::edit,
                                enabled=state.canAnswer, label={Text("Your response (optional)")},
                                modifier=Modifier.fillMaxWidth(), minLines=3,
                                isError=state.draft.trim().length>1000,
                                supportingText={Text("${state.draft.trim().length}/1000 characters · unsent until confirmed")})
                            if(state.pending!=null) Text("Sending or awaiting confirmation. Your response is kept for recovery.")
                            Button(onClick=state::submit, enabled=state.canAnswer && state.validInput,
                                modifier=Modifier.fillMaxWidth()) { Text("Send response") }
                            Text("The next message opens after your response is saved.", fontSize=13.sp)
                        }
                    }
                    when(s.status) {
                        "generating" -> {
                            Text("Your response is saved. Preparing the next message…", modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
                            TextButton(onClick=state::retryGeneration,enabled=!state.busy) { Text("Retry generation") }
                        }
                        "generation_failed" -> {
                            Text("The next reflection could not be prepared. Your saved responses are safe.")
                            Button(onClick=state::retryGeneration,enabled=!state.busy) { Text("Retry generation") }
                        }
                        "completed","closed" -> {
                            HorizontalDivider()
                            Text(if(s.status=="completed") "A place to pause." else "This session is closed.", fontSize=28.sp)
                            Text("${s.answered} responses saved. Take one thought back into your day.")
                            Button(onClick=onExit) { Text("Return to my day") }
                            OutlinedButton(onClick={state.start(entry)},enabled=!state.busy && state.pending==null) { Text("Continue reflecting") }
                        }
                    }
                    if(s.status !in listOf("closed","completed"))
                        TextButton(onClick=state::closeSession,enabled=!state.busy && state.pending==null) { Text("End this session") }
                    TextButton(onClick={deleteConfirm=true},enabled=!state.busy) { Text("Delete this session") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if(state.canContinue) {
            var drag by remember { mutableFloatStateOf(0f) }
            Box(Modifier.fillMaxWidth().padding(16.dp).pointerInput(state.displayedId) {
                detectVerticalDragGestures(onDragStart={drag=0f},onVerticalDrag={change,amount -> change.consume(); drag+=amount},
                    onDragEnd={if(drag < -60f) state.next()})
            }) {
                Button(onClick=state::next,modifier=Modifier.fillMaxWidth()) { Text("Continue · or swipe up") }
            }
        }
    }
    if(deleteConfirm) AlertDialog(onDismissRequest={deleteConfirm=false},
        title={Text("Delete this reflection session?")},
        text={Text("This removes the session and responses from your account and this device after confirmation. It does not change your app locks or alarms.")},
        confirmButton={TextButton(onClick={deleteConfirm=false;state.deleteSession()}){Text("Delete")}},
        dismissButton={TextButton(onClick={deleteConfirm=false}){Text("Keep session")}})
}

