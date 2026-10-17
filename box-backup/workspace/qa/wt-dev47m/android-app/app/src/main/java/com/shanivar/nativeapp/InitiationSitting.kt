package com.shanivar.nativeapp

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/*
 * "Your first sitting" - SIX-STEP BODY-SCAN version, INTERNAL TEST BUILD ONLY (founder's daily baseline).
 * Original design: docs/strategy/shanivar-sankalp-and-sittings-initiation-proposal.md. Entry:
 * BreathingScreen.kt PracticeSelection (HOME only), reached from V1Home and the V3 Sadhana tab.
 * Steps: 1 Stop, 2 Settle, 3 Scan, 4 Stay (2-4 are one 180 s UNPACED timed session, counted as practised
 * under the unchanged 90 s rule), 5 Ask (Sankalp line, SankalpStore), 6 Close. No streaks, scores, counts.
 * No breath is ever directed, counted or held. No effect claims. Wording adapted from an unattributed
 * Instagram post (see shanivar-vbt/content/drafts/DRAFT-body-scan-NOT-REVIEWED-internal-only.md): a
 * qualified human must review it before ANY public release (research/deep-foundation/PRACTICE_SAFETY.md).
 * The body-scan stage lives in code only; it is NOT in the reviewed breathing catalogue.
 */

const val INITIATION_PRACTICE_ID = "body_scan_sitting"

/** Reuses the Sadhana default so the sitting counts as practised at the end of the timer
 * (`BreathingSessionOwner.kt:16`, `MIN_PRACTISED_SECONDS = 90` in BreathingSessionState.kt:198 untouched). */
const val INITIATION_PRACTICE_SECONDS = SADHANA_DEFAULT_SECONDS

/** Elapsed active seconds at which the timed middle moves on: Settle 0-30, Scan 30-120, Stay 120-180. */
const val SCAN_STARTS_AT_SECONDS = 30
const val STAY_STARTS_AT_SECONDS = 120

enum class SittingStep(val key: String) {
    STOP("stop"), SETTLE("settle"), SCAN("scan"), STAY("stay"),
    ASK("ask"), CLOSE("close"),
    /** The early-end screen (End pressed or interrupted during the timed middle). Not a step of the six. */
    END("end"),
}

/** Approved copy. */
data class SittingFrame(
    val title: String,
    val lines: List<String>,
    val primary: String,
    /** Label of the End action on this screen (step 1 keeps the approved "Not now"). */
    val endLabel: String = "End",
)

object InitiationSittingCopy {
    val stop = SittingFrame(
        "Stop.",
        listOf("Find a safe place for three minutes.", "You can end this at any time."),
        "Begin", endLabel = "Not now",
    )
    val settle = SittingFrame(
        "Settle.",
        listOf("Close your eyes if that's comfortable.", "Notice how your body is held up."),
        "Continue",
    )
    val scan = SittingFrame(
        "Scan, top to bottom.",
        listOf("Head, face, jaw, neck, shoulders, chest, stomach, hands, legs, feet.",
            "Notice where there's sensation, and where there's none."),
        "Continue",
    )
    val stay = SittingFrame(
        "Stay with one spot.",
        listOf("Pick the strongest sensation.", "No need to fix or explain it. Let it be there."),
        "Continue",
    )
    /** Optional, skippable; never required for anything. */
    const val STAY_OPTIONAL = "Optional: on the exhale, you may quietly say \"I allow\". Or leave this out."
    const val ASK_TITLE = "Where do you want to reach?"
    const val ASK_HELP = "Your own words. One line, up to 60 characters."
    const val LINE_HINT = "your words"
    const val LINE_PRIVACY = "It stays on this phone. Nobody scores it. Edit or clear it whenever you like."
    const val CLOSE_TITLE = "The time has passed."
    const val SAFE_SETTING_NOTE = "Please don't use this while driving, in water, or doing anything that needs your attention."
    const val SAFE_CHIP = "I'm somewhere safe and still"
    const val ENTRY_TITLE = "Your first sitting"
    const val ENTRY_BODY = "A six-step body-scan sitting, about five minutes: three minutes of attention, then one line about where you want to reach."
    const val READY_LEAD = "Where you want to reach:"

    fun frame(step: SittingStep): SittingFrame? = when (step) {
        SittingStep.STOP -> stop
        SittingStep.SETTLE -> settle
        SittingStep.SCAN -> scan
        SittingStep.STAY -> stay
        else -> null
    }

    /** Every user-visible string of the shipped steps, for the forbidden-words test. */
    fun allText(): List<String> = listOf(stop, settle, scan, stay).flatMap { listOf(it.title) + it.lines + it.primary + it.endLabel } +
        listOf(STAY_OPTIONAL, ASK_TITLE, ASK_HELP, LINE_HINT, LINE_PRIVACY, CLOSE_TITLE, SAFE_SETTING_NOTE, SAFE_CHIP,
            ENTRY_TITLE, ENTRY_BODY, READY_LEAD)
}

/** Pure flow rules, no Android. Skip always means "go to the next step"; End always leaves. */
object SittingFlow {
    fun next(step: SittingStep): SittingStep = when (step) {
        SittingStep.STOP -> SittingStep.SETTLE
        SittingStep.SETTLE -> SittingStep.SCAN
        SittingStep.SCAN -> SittingStep.STAY
        SittingStep.STAY -> SittingStep.ASK
        SittingStep.ASK -> SittingStep.CLOSE
        SittingStep.CLOSE -> SittingStep.CLOSE
        SittingStep.END -> SittingStep.END
    }

    val timedSteps = listOf(SittingStep.SETTLE, SittingStep.SCAN, SittingStep.STAY)

    /** The timed middle step the clock alone points at. */
    fun stepAtElapsed(elapsedSeconds: Long): SittingStep = when {
        elapsedSeconds >= STAY_STARTS_AT_SECONDS -> SittingStep.STAY
        elapsedSeconds >= SCAN_STARTS_AT_SECONDS -> SittingStep.SCAN
        else -> SittingStep.SETTLE
    }

    /** The step to show: the clock moves it on, a Skip can move it on further, never back. */
    fun shownStep(current: SittingStep, elapsedSeconds: Long): SittingStep {
        val byClock = stepAtElapsed(elapsedSeconds)
        return if (timedSteps.indexOf(byClock) > timedSteps.indexOf(current)) byClock else current
    }

    /** After the timed session reaches a terminal phase: a finished timer goes on to Ask; the person
     * pressing End (or an interruption) goes straight to the End screen, with no more asked. */
    fun afterPractice(phase: BreathingPhase): SittingStep =
        if (phase == BreathingPhase.ENDED_TIMER) SittingStep.ASK else SittingStep.END

    val forbiddenWords = listOf("higher self", "deepest", "manifest", "transform", "unlock", "journey",
        "streak", "reward", "badge", "awaken")

    /** Phrases that must never appear in the shipped step copy: directed breath and effect claims. */
    val bannedPhrases = listOf("deep breath", "breathe in", "breathe out", "breathe into", "hold your breath",
        "inhale", "count", "heal", "blood flow", "circulation", "nervous system", "pain relief", "relieve",
        "cure", "therapy", "treat", "release tension", "energy", "don't run", "do not run")
}

/** The in-code body-scan stage the session timer runs on. Not in the catalogue; presentation has no tone,
 * no completion questions, no sleep handling. Stage texts are unused (the sitting shows the step copy). */
fun bodyScanPractice(): BreathingPractice = BreathingPractice(
    id = INITIATION_PRACTICE_ID, title = "Body scan sitting", kind = BreathingKind.ATTENTION,
    contentVersion = 1, status = "draft_internal", source = null,
    allowedDurationsSeconds = listOf(INITIATION_PRACTICE_SECONDS), defaultDurationSeconds = INITIATION_PRACTICE_SECONDS,
    suitableFeelings = emptySet(), suitableContexts = emptySet(),
    requiresBreathComfort = false, requiresSoundConsent = false, requiresPriorComfort = false,
    requiresSleepIntent = false, requiresSleepyNow = false, safeStationarySettingRequired = true,
    stages = emptyMap(),
    presentation = BreathingPresentation(eyesOpenAllowed = true, keepScreenOn = true, closingBell = false,
        showCompletionQuestions = false, toneAsset = null),
)

/**
 * Process-level position in the sitting, like [BreathingSessionOwner]: the Sadhana host can be
 * recomposed or the tab switched mid-sitting, and the one practice session lives in the owner anyway.
 */
object InitiationSitting {
    /** null = not in a sitting. */
    var step by mutableStateOf<SittingStep?>(null)
        private set

    /** Where a recorded guiding voice plugs in. Default: nothing plays (text only). See [SittingGuideAudio]. */
    var guide: SittingGuideAudio = SittingGuideAudio.None

    fun begin() { step = SittingStep.STOP; guide.onStep(SittingStep.STOP) }
    fun skip() { advanceTo(step?.let(SittingFlow::next) ?: return) }
    fun goTo(next: SittingStep) = advanceTo(next)
    fun end() { step = null; guide.onLeave() }
    fun resetForTest() { step = null; guide = SittingGuideAudio.None }

    private fun advanceTo(next: SittingStep) { step = next; guide.onStep(next) }
}

/**
 * Slot for the founder's own recorded voice (no audio is shipped, no TTS, no generated persona).
 * To add it later: drop one file per screen into `app/src/main/res/raw/` named
 * `sitting_guide_<key>` (`stop`, `settle`, `scan`, `stay`, `ask`, `close`, `end`), implement
 * this interface with a player, and set `InitiationSitting.guide`. Playback must sit behind the
 * existing sound-consent gate (`BreathingPreferences.soundConsent`), stop on End/background, and
 * never loop. Until then [None] is used and nothing is played.
 */
interface SittingGuideAudio {
    fun onStep(step: SittingStep)
    fun onLeave()

    object None : SittingGuideAudio {
        override fun onStep(step: SittingStep) {}
        override fun onLeave() {}
    }

    companion object {
        /** The raw resource name a recording for [step] must use. */
        fun resourceName(step: SittingStep) = "sitting_guide_${step.key}"

        /** The raw resource id of the recording for [step], or null when none is bundled (today: always). */
        fun clipResId(context: Context, step: SittingStep): Int? =
            context.resources.getIdentifier(resourceName(step), "raw", context.packageName).takeIf { it != 0 }
    }
}

/** The "Your first sitting" card shown on the Sadhana selection. Always reachable in the test build
 * (no hiding, no "used" state: nothing is counted or remembered about use). */
@Composable
fun FirstSittingEntryCard() {
    SectionCard(InitiationSittingCopy.ENTRY_TITLE) {
        Text(InitiationSittingCopy.ENTRY_BODY)
        PrimaryAction("Start your first sitting") {
            // A finished session from an earlier visit must not be mistaken for this sitting's practice.
            BreathingSessionOwner.clearSession()
            InitiationSitting.begin()
        }
    }
}

/** Steps 1 and 5-6 (the timed middle is [SittingTimedStep]). */
@Composable
fun InitiationSittingFrames(step: SittingStep, owner: BreathingSessionOwner, onExit: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SankalpStore(context) }
    val endAndLeave = { owner.leave(); InitiationSitting.end(); onExit() }
    when (step) {
        SittingStep.STOP -> {
            var safe by remember { mutableStateOf(owner.draft.safeSettingConfirmed) }
            val frame = InitiationSittingCopy.stop
            Heading(frame.title)
            frame.lines.forEach { Text(it) }
            Text(InitiationSittingCopy.SAFE_SETTING_NOTE, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilterChip(
                selected = safe,
                onClick = { safe = !safe; owner.draft = owner.draft.copy(safeSettingConfirmed = safe) },
                label = { Text(InitiationSittingCopy.SAFE_CHIP) },
            )
            // Begin and Skip both start the 3-minute unpaced session; both wait for the safe chip.
            val begin = { startSittingPractice(context, owner); InitiationSitting.skip() }
            PrimaryActionEnabled(frame.primary, enabled = safe) { begin() }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { begin() }, enabled = safe) { Text("Skip") }
                TextButton(onClick = endAndLeave) { Text(frame.endLabel) }
            }
        }
        SittingStep.ASK -> SittingAsk(store, endAndLeave)
        SittingStep.CLOSE -> SittingClose(store) { owner.clearSession(); owner.leave(); InitiationSitting.end(); onExit() }
        else -> Unit
    }
}

@Composable
private fun PrimaryActionEnabled(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled) { Text(label) }
}

/** Starts the one 180 s unpaced session regardless of the draft's chosen length. */
fun startSittingPractice(context: Context, owner: BreathingSessionOwner) {
    owner.clearSession()
    owner.choose(bodyScanPractice(), INITIATION_PRACTICE_SECONDS)
    owner.start(context)
}

/** Steps 2-4 while the session runs: the step shown follows the clock; Skip moves on, End ends the session. */
@Composable
fun SittingTimedStep(owner: BreathingSessionOwner, session: BreathingSession, step: SittingStep) {
    val context = LocalContext.current
    var elapsed by remember(session.sessionId) { mutableStateOf(session.elapsedActiveMillis(owner.monotonicMillis()) / 1000) }
    var remaining by remember(session.sessionId) { mutableStateOf(session.remainingMillis(owner.monotonicMillis())) }
    LaunchedEffect(session.sessionId, session.phase) {
        while (owner.session?.phase?.counting == true) {
            owner.tick(context)
            val live = owner.session ?: break
            elapsed = live.elapsedActiveMillis(owner.monotonicMillis()) / 1000
            remaining = live.remainingMillis(owner.monotonicMillis())
            val shown = SittingFlow.shownStep(InitiationSitting.step ?: step, elapsed)
            if (shown != InitiationSitting.step && !live.phase.terminal) InitiationSitting.goTo(shown)
            delay(250)
        }
    }
    val frame = InitiationSittingCopy.frame(step) ?: return
    Heading(frame.title)
    frame.lines.forEach { Text(it) }
    if (step == SittingStep.STAY) Text(InitiationSittingCopy.STAY_OPTIONAL, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val seconds = (remaining + 999) / 1000
    Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.titleMedium)
    if (owner.pausedByHost) Text("Paused when the app went to the background. Nothing was missed.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { if (session.phase == BreathingPhase.PAUSED) owner.resume() else owner.pause(context) }) {
            Text(if (session.phase == BreathingPhase.PAUSED) "Resume" else "Pause")
        }
        TextButton(onClick = {
            // Skip on Settle/Scan moves the text on; the timer keeps running. Skip on Stay ends the timer the normal way.
            if (step == SittingStep.STAY) { owner.end(context); InitiationSitting.goTo(SittingStep.ASK) } else InitiationSitting.skip()
        }) { Text("Skip") }
        TextButton(onClick = { owner.end(context) }) { Text("End") }
    }
}

@Composable
private fun SittingAsk(store: SankalpStore, onEnd: () -> Unit) {
    var text by remember { mutableStateOf(store.line.orEmpty()) }
    Heading(InitiationSittingCopy.ASK_TITLE)
    Text(InitiationSittingCopy.ASK_HELP)
    OutlinedTextField(
        value = text,
        onValueChange = { text = SankalpStore.limitInput(it) },
        label = { Text(InitiationSittingCopy.LINE_HINT) },
        singleLine = true,
        supportingText = { Text("${text.codePointCount(0, text.length)}/${SankalpStore.MAX_LENGTH}") },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(InitiationSittingCopy.LINE_PRIVACY, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PrimaryActionEnabled("Keep", enabled = SankalpStore.isAcceptable(text)) {
        if (store.save(text)) InitiationSitting.skip()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { store.clear(); text = "" }) { Text("Clear") }
        TextButton(onClick = { InitiationSitting.skip() }) { Text("Skip") }
        TextButton(onClick = onEnd) { Text("End") }
    }
}

@Composable
private fun SittingClose(store: SankalpStore, onDone: () -> Unit) {
    Heading(InitiationSittingCopy.CLOSE_TITLE)
    store.line?.let { SankalpEcho(it) }
    PrimaryAction("Done", onDone)
}
