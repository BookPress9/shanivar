package com.shanivar.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A read-only view of enforcement state, for display only.
 *
 * This type exists so the breathing screens can say something true about protection without being
 * handed a [DisciplineGateway]. Passing the gateway would make "the player cannot release focus" a
 * promise about code nobody re-reads; passing this makes it a fact about types (handoff section 6).
 */
data class BreathingProtectionSnapshot(val protection: ProtectionState, val endsAtEpochMillis: Long?)

/**
 * The "Breathe & reset" surface: choose, practise, stop.
 *
 * [onExit] belongs to the host, and each host means something different by it - Home returns to Home,
 * the block overlay goes to the device's home screen and never to the blocked app, the post-alarm
 * screen returns to the normal app. The player never decides where exiting goes, which is why there
 * is no navigation logic in this file beyond calling it.
 */
@Composable
fun BreathingHost(
    entry: BreathingEntry,
    protection: BreathingProtectionSnapshot? = null,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val owner = BreathingSessionOwner
    LaunchedEffect(entry) { owner.load(context, entry) }

    val session = owner.session
    val practice = owner.practice

    // Leaving mid-practice ends the session first. Back is not a pause and not a bypass.
    BackHandler {
        if (session != null && !session.phase.terminal) owner.end(context)
        owner.leave()
        onExit()
    }
    // Losing the foreground stops cues and pauses ordinary practice. A sleep practice already
    // resting keeps resting: putting the phone down is the instruction there, not an interruption.
    OnAppPause { owner.onHostBackgrounded(context) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            owner.contentProblems.isNotEmpty() -> ContentUnavailable(owner.contentProblems) {
                owner.leave(); onExit()
            }
            owner.catalogue == null -> Text("Loading practices…")
            session != null && practice != null && session.phase != BreathingPhase.READY ->
                PracticeStage(owner, practice, session, protection, onExit)
            else -> PracticeSelection(owner, protection, onExit)
        }
    }
}

@Composable
private fun ContentUnavailable(problems: List<String>, onExit: () -> Unit) {
    Heading("Practices are\nunavailable.")
    Text("The practice content on this device could not be read, so nothing is offered rather than guessed.")
    // Surfaced verbatim: a content mistake is a build problem, and hiding it helps nobody.
    problems.take(3).forEach { Text("· $it", style = MaterialTheme.typography.bodySmall) }
    PrimaryAction("Exit", onExit)
}

@Composable
private fun PracticeSelection(
    owner: BreathingSessionOwner,
    protection: BreathingProtectionSnapshot?,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { BreathingPreferences(context) }
    var soundConsent by remember { mutableStateOf(prefs.soundConsent) }
    var priorComfort by remember { mutableStateOf(prefs.priorBreathComfort) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val draft = owner.draft
    // B2: read once each time the list is shown - it re-enters composition after every practice.
    val today = owner.wallClock()
    val lastPractised = remember { lastPractisedDates(prefs.journal(), today.zone) }
    fun lastLabel(option: BreathingOption) = lastPractisedLabel(lastPractised[option.practice.id], today.toLocalDate())

    Heading("Breathe & reset")
    Text("A short practice for this moment.")
    protection?.let { ProtectionNote(it) }

    if (owner.interruptedEarlier) {
        Text("A practice was interrupted earlier. Nothing was resumed; start fresh when you want to.")
        TextButton(onClick = { owner.acknowledgeInterruption() }) { Text("Got it") }
    }

    SectionCard("Somewhere safe?") {
        Text("Please don't use this while driving, in water, or doing anything that needs your attention.")
        FilterChip(
            selected = draft.safeSettingConfirmed,
            onClick = { owner.draft = draft.copy(safeSettingConfirmed = !draft.safeSettingConfirmed) },
            label = { Text("I'm somewhere safe and still") },
        )
        if (!draft.safeSettingConfirmed) {
            Text("Nothing starts until you confirm this.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onExit) { Text("Come back later") }
        }
    }

    SectionCard("How does right now feel?") {
        Text("Your own words for it. This isn't a diagnosis and nothing is saved.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        BreathingFeeling.values().forEach { feeling ->
            FilterChip(
                selected = draft.feeling == feeling,
                onClick = { owner.draft = draft.copy(feeling = if (draft.feeling == feeling) null else feeling) },
                label = { Text(feeling.label) },
            )
        }
    }

    SectionCard("Where in your day?") {
        Text(scheduleExplanation(prefs), color = MaterialTheme.colorScheme.onSurfaceVariant)
        BreathingContext.values().forEach { option ->
            FilterChip(
                selected = draft.context == option,
                onClick = { owner.draft = draft.copy(context = if (draft.context == option) null else option) },
                label = { Text(option.label) },
            )
        }
        if (draft.context == BreathingContext.GOING_TO_SLEEP) {
            // Both answers are required for the sleep practice, and both are explicit. The clock is
            // never allowed to answer either one.
            FilterChip(
                selected = draft.sleepIntent,
                onClick = { owner.draft = draft.copy(sleepIntent = !draft.sleepIntent) },
                label = { Text("I'm going to sleep now") },
            )
            FilterChip(
                selected = draft.sleepyNow,
                onClick = { owner.draft = draft.copy(sleepyNow = !draft.sleepyNow) },
                label = { Text("I feel sleepy now") },
            )
        }
    }

    SectionCard("How long?") {
        listOf(60 to "1 min", 180 to "3 min", 300 to "5 min").forEach { (seconds, label) ->
            FilterChip(
                selected = draft.budgetSeconds == seconds,
                onClick = { owner.draft = draft.copy(budgetSeconds = seconds) },
                label = { Text(label) },
            )
        }
    }

    SectionCard("What would you rather notice?") {
        listOf(
            true to "Breathing is comfortable",
            false to "Breath attention feels uncomfortable",
        ).forEach { (comfortable, label) ->
            FilterChip(
                selected = draft.breathComfort == comfortable,
                onClick = {
                    owner.draft = draft.copy(breathComfort = if (draft.breathComfort == comfortable) null else comfortable)
                },
                label = { Text(label) },
            )
        }
        Text("Leave both unchosen and we'll offer options that don't involve breathing at all.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilterChip(
            selected = soundConsent,
            onClick = { soundConsent = !soundConsent; prefs.soundConsent = soundConsent },
            label = { Text("Allow a quiet sound") },
        )
        if (draft.breathComfort == true) {
            FilterChip(
                selected = priorComfort,
                onClick = { priorComfort = !priorComfort; prefs.priorBreathComfort = priorComfort },
                label = { Text("Comfortable with breath awareness") },
            )
        }
    }

    when (val suggestion = owner.suggestion(context)) {
        null -> Text("Loading practices…")
        is BreathingSuggestion.SafeSettingRequired ->
            Text("Confirm you're somewhere safe and still, and the practices will appear.")
        is BreathingSuggestion.Unavailable -> {
            Text(suggestion.explanation)
            TextButton(onClick = onExit) { Text("Exit") }
        }
        is BreathingSuggestion.Offered -> {
            Text(suggestion.reason, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            suggestion.primary?.let { PracticeOptionCard(owner, it, suggested = true, lastLabel(it)) }
                ?: Text("Nothing is suggested automatically right now. Choose from the list if you want to.")
            suggestion.alternatives.forEach { PracticeOptionCard(owner, it, suggested = false, lastLabel(it)) }
            TextButton(onClick = { showAll = !showAll }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showAll) "Hide other options" else "All suitable options")
            }
            if (showAll) {
                val shown = listOfNotNull(suggestion.primary) + suggestion.alternatives
                suggestion.allSuitable.filterNot { option -> shown.any { it.practice.id == option.practice.id } }
                    .forEach { PracticeOptionCard(owner, it, suggested = false, lastLabel(it)) }
            }
        }
    }
    TextButton(onClick = { owner.leave(); onExit() }, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
}

@Composable
private fun PracticeOptionCard(
    owner: BreathingSessionOwner,
    option: BreathingOption,
    suggested: Boolean,
    lastPractised: String? = null,
) {
    val context = LocalContext.current
    var sourceOpen by rememberSaveable(option.practice.id) { mutableStateOf(false) }
    val practice = option.practice
    SectionCard(practice.title) {
        if (suggested) Text("Suggested", color = MaterialTheme.colorScheme.primary)
        Text(practice.originLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // A quiet fact, never a prompt: nothing at all is shown for a practice never dated.
        lastPractised?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(practice.stages[BreathingStageId.INSTRUCTION].orEmpty())
        when (val duration = option.duration) {
            is BreathingDuration.Ready -> {
                Text("${duration.seconds / 60} min" + if (duration.shorterThanBudget) " · the longest this practice has" else "")
                PrimaryAction("Start") { owner.choose(practice, duration.seconds); owner.start(context) }
            }
            is BreathingDuration.NeedsLongerConfirmation -> {
                // Never silently lengthen someone's chosen time; ask instead.
                Text("This practice is ${duration.seconds / 60} minute(s) long - longer than the time you chose.")
                OutlinedButton(
                    onClick = { owner.choose(practice, duration.seconds); owner.start(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start ${duration.seconds / 60} min instead") }
            }
        }
        TextButton(onClick = { sourceOpen = !sourceOpen }) { Text("Origin & adaptation") }
        if (sourceOpen) SourceDetail(practice)
    }
}

@Composable
private fun SourceDetail(practice: BreathingPractice) {
    val source = practice.source
    if (source == null) {
        Text("A modern attention exercise written for this app. It is not from the Vijñāna-bhairava-tantra.")
        return
    }
    Text("Work: ${source.work}")
    Text("Verse: ${source.verseLabels.joinToString(", ")}")
    Text("Edition: ${source.edition}")
    Text(source.url, style = MaterialTheme.typography.bodySmall)
    Text("Adaptation: ${source.adaptationNote}")
    Text("Checked: ${source.verification.replace('_', ' ')}")
    Text("This is our adaptation. The cited authors have no involvement with this app.",
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PracticeStage(
    owner: BreathingSessionOwner,
    practice: BreathingPractice,
    session: BreathingSession,
    protection: BreathingProtectionSnapshot?,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    var sourceOpen by rememberSaveable { mutableStateOf(false) }
    // Reading the source pauses the practice; coming back needs an explicit Resume rather than
    // silently resuming a countdown the person stopped watching.
    LaunchedEffect(sourceOpen) { if (sourceOpen) owner.pause(context) }

    if (session.phase.terminal) {
        PracticeEnded(owner, practice, session, onExit)
        return
    }

    // The session object itself does not change while time passes - elapsed time is derived from the
    // monotonic clock - so the countdown needs its own state to recompose against.
    var remaining by remember(session.sessionId) { mutableStateOf(session.remainingMillis(owner.monotonicMillis())) }
    LaunchedEffect(session.sessionId, session.phase) {
        while (owner.session?.phase?.counting == true) {
            owner.tick(context)
            remaining = owner.session?.remainingMillis(owner.monotonicMillis()) ?: 0L
            delay(250)
        }
        remaining = owner.session?.remainingMillis(owner.monotonicMillis()) ?: 0L
    }
    val stage = session.currentStage(practice, owner.monotonicMillis())
    Heading(practice.title)
    Text(practice.originLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    protection?.let { ProtectionNote(it) }

    if (session.phase == BreathingPhase.RESTING_SILENTLY) {
        // No countdown, no keep-screen-on, nothing to respond to.
        Text(practice.stages[BreathingStageId.PRACTICE].orEmpty(), style = MaterialTheme.typography.titleMedium)
        Text("Nothing more is needed. You can put the screen down.")
    } else {
        val seconds = (remaining + 999) / 1000
        // Not a live region: an announcement every second would make the practice unusable with
        // TalkBack. The stage text below is the polite announcement instead.
        Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayMedium)
        Text(
            stage?.text.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    if (owner.pausedByHost) Text("Paused when the app went to the background. Nothing was missed.")

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (session.phase != BreathingPhase.RESTING_SILENTLY) {
            OutlinedButton(onClick = {
                if (session.phase == BreathingPhase.PAUSED) owner.resume() else owner.pause(context)
            }) { Text(if (session.phase == BreathingPhase.PAUSED) "Resume" else "Pause") }
        }
        TextButton(onClick = { owner.end(context) }) { Text("End") }
    }
    Text(BREATHING_SAFETY_NOTE, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = { sourceOpen = !sourceOpen }) { Text("Origin & adaptation") }
    if (sourceOpen) SourceDetail(practice)
}

@Composable
private fun PracticeEnded(
    owner: BreathingSessionOwner,
    practice: BreathingPractice,
    session: BreathingSession,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    var answered by rememberSaveable(session.sessionId) { mutableStateOf(false) }
    // A bedtime ending stays quiet: no bright completion screen, no survey, no celebration.
    if (practice.isSleepPractice) {
        Text("The time you chose has passed.")
        TextButton(onClick = { owner.clearSession(); owner.leave(); onExit() }) { Text("Close") }
        return
    }
    Heading(
        when (session.phase) {
            BreathingPhase.ENDED_USER -> "You stopped there."
            BreathingPhase.INTERRUPTED -> "That was interrupted."
            else -> "The time has passed."
        }
    )
    // "The timer ended" is all that is claimed. Not "well done", not "practice complete".
    Text("However that felt, it's information. Nothing to fix.")
    if (practice.presentation.showCompletionQuestions && !answered) {
        Text("Was that useful?")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                BreathingFeedback.HELPED to "Helped",
                BreathingFeedback.NEUTRAL to "Neither",
                BreathingFeedback.FELT_WORSE to "Felt worse",
            ).forEach { (feedback, label) ->
                OutlinedButton(onClick = { owner.recordFeedback(context, feedback); answered = true }) { Text(label) }
            }
        }
    }
    if (answered) Text("Noted on this device only. We won't suggest that one again today.")
    PrimaryAction("Done") { owner.clearSession(); owner.leave(); onExit() }
    TextButton(onClick = { owner.clearSession() }, modifier = Modifier.fillMaxWidth()) { Text("Choose another practice") }
}

/** Shows protection state only when the engine actually confirmed it. An unavailable status says so
 * rather than claiming "off" - a wrong "off" would invite someone to open the app they just blocked. */
@Composable
private fun ProtectionNote(snapshot: BreathingProtectionSnapshot) {
    val text = when (snapshot.protection) {
        ProtectionState.ACTIVE -> "Focus protection is on. This practice doesn't change it."
        ProtectionState.OFF -> "Focus protection is off right now."
        ProtectionState.NEEDS_PERMISSION, ProtectionState.UNAVAILABLE -> "Protection status is unavailable."
    }
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * The reviewed grounding instruction from the catalogue, or null when content is unavailable.
 *
 * [LocalPauseScreen] uses this instead of the hardcoded prompts it used to pick from at random. That
 * matters beyond tidiness: one of those lines gave breath-pacing advice that no reviewer had seen,
 * and the pause is shown before sign-in and inside the block overlay - the two places where content
 * reaches people who have agreed to nothing.
 */
@Composable
fun rememberGroundingPrompt(): String? {
    val context = LocalContext.current
    return remember {
        (BreathingCatalog.load(context) as? BreathingCatalogResult.Valid)
            ?.catalogue?.practice("external_grounding")?.stages?.get(BreathingStageId.INSTRUCTION)
    }
}

private fun scheduleExplanation(prefs: BreathingPreferences): String =
    if (prefs.schedule().awakeSpanMinutes == null) "Optional. Add a wake and sleep time in Settings and we can suggest from it."
    else "Optional. Based on the schedule you chose."

const val BREATHING_SAFETY_NOTE =
    "Keep breathing comfortable. Stop if you feel dizzy, short of breath or more distressed."
