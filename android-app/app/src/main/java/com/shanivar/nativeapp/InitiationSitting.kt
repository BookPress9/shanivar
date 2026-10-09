package com.shanivar.nativeapp

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/*
 * "Your first sitting" - Phase A, INTERNAL TEST BUILD ONLY.
 * Design: docs/strategy/shanivar-sankalp-and-sittings-initiation-proposal.md (§1 copy :23-34, §2 :36-48,
 * §4 Phase A :111-122). Entry: BreathingScreen.kt PracticeSelection (HOME only), which both the
 * current V1Home and the V3 RoutineShell Sadhana tab reach through BreathingHost. Hard-coded:
 * Arrive (screens 1-3) -> one practice (external_grounding, 180 s) -> Close (screens 4-5) -> the
 * existing End screen. No streaks, scores, counts, rewards, reminders. Sankalp stays in SankalpStore.
 * Frame text is new script text: a qualified human must review it before public release
 * (research/deep-foundation/PRACTICE_SAFETY.md "Review and testing contract").
 */

const val INITIATION_PRACTICE_ID = "external_grounding"

/** Reuses the Sadhana default so the sitting counts as practised at the end of the timer
 * (`BreathingSessionOwner.kt:16`, `MIN_PRACTISED_SECONDS = 90` in BreathingSessionState.kt:198 untouched). */
const val INITIATION_PRACTICE_SECONDS = SADHANA_DEFAULT_SECONDS

enum class SittingStep(val key: String) {
    STOP("stop"), ARRIVE("arrive"), NOISE("noise"),
    PRACTICE("practice"),
    QUESTION("question"), LINE("line"),
    END("end"),
}

/** Exact approved copy (proposal §1 table). The Practice and End steps use catalogue/End-screen text. */
data class SittingFrame(
    val title: String,
    val lines: List<String>,
    val primary: String,
    /** Label of the End action on this screen (screen 1 keeps the approved "Not now"). */
    val endLabel: String = "End",
)

object InitiationSittingCopy {
    val stop = SittingFrame(
        "Stop for three minutes.",
        listOf("Sit somewhere safe. Put the phone where you can still read it.", "You can end this at any time."),
        "Begin", endLabel = "Not now",
    )
    val arrive = SittingFrame(
        "Feel what holds you up.",
        listOf("The chair, the floor, your own weight.", "Leave your breathing alone.", "Look around the room once."),
        "Continue",
    )
    val noise = SittingFrame(
        "Let the noise be noise.",
        listOf("Thoughts will keep running. You don't have to stop them.",
            "Stay with the weight under you, and let them run."),
        "Continue",
    )
    val question = SittingFrame(
        "Where do you want to reach?",
        listOf("Not what you should want. Not what is realistic.",
            "Take as long as you like. Nothing is generated for you here."),
        "Continue",
    )
    const val LINE_TITLE = "Write it in your own words."
    const val LINE_HELP = "One line. 60 characters."
    const val LINE_HINT = "your words"
    const val LINE_PRIVACY = "It stays on this phone. Nobody scores it. Edit or clear it whenever you like."
    const val SAFE_SETTING_NOTE = "Please don't use this while driving, in water, or doing anything that needs your attention."
    const val SAFE_CHIP = "I'm somewhere safe and still"
    const val ENTRY_TITLE = "Your first sitting"
    const val ENTRY_BODY = "About seven minutes: arrive, one short practice, and one line about where you want to reach."
    const val READY_LEAD = "Where you want to reach:"

    fun frame(step: SittingStep): SittingFrame? = when (step) {
        SittingStep.STOP -> stop
        SittingStep.ARRIVE -> arrive
        SittingStep.NOISE -> noise
        SittingStep.QUESTION -> question
        else -> null
    }

    /** Every user-visible string, for the forbidden-words test. */
    fun allText(): List<String> = listOf(stop, arrive, noise, question).flatMap { listOf(it.title) + it.lines + it.primary + it.endLabel } +
        listOf(LINE_TITLE, LINE_HELP, LINE_HINT, LINE_PRIVACY, SAFE_SETTING_NOTE, SAFE_CHIP, ENTRY_TITLE, ENTRY_BODY, READY_LEAD)
}

/** Pure flow rules, no Android. Skip always means "go to the next screen"; End always leaves. */
object SittingFlow {
    fun next(step: SittingStep): SittingStep = when (step) {
        SittingStep.STOP -> SittingStep.ARRIVE
        SittingStep.ARRIVE -> SittingStep.NOISE
        SittingStep.NOISE -> SittingStep.PRACTICE
        SittingStep.PRACTICE -> SittingStep.QUESTION
        SittingStep.QUESTION -> SittingStep.LINE
        SittingStep.LINE -> SittingStep.END
        SittingStep.END -> SittingStep.END
    }

    /** After the practice session reaches a terminal phase: a finished timer goes on to Close; the
     * person pressing End (or an interruption) goes straight to the End screen, with no more asked. */
    fun afterPractice(phase: BreathingPhase): SittingStep =
        if (phase == BreathingPhase.ENDED_TIMER) SittingStep.QUESTION else SittingStep.END

    val forbiddenWords = listOf("higher self", "deepest", "manifest", "transform", "unlock", "journey",
        "streak", "reward", "badge", "awaken")
}

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
 * `sitting_guide_<key>` (`stop`, `arrive`, `noise`, `practice`, `question`, `line`, `end`), implement
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

/** Screens that are not the practice itself: frames 1-3, 4, 5 and the Ready step. */
@Composable
fun InitiationSittingFrames(step: SittingStep, owner: BreathingSessionOwner, onExit: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SankalpStore(context) }
    val endAndLeave = { owner.leave(); InitiationSitting.end(); onExit() }
    when (step) {
        SittingStep.STOP, SittingStep.ARRIVE, SittingStep.NOISE, SittingStep.QUESTION -> {
            val frame = InitiationSittingCopy.frame(step) ?: return
            var safe by remember { mutableStateOf(owner.draft.safeSettingConfirmed) }
            Heading(frame.title)
            frame.lines.forEach { Text(it) }
            if (step == SittingStep.STOP) {
                Text(InitiationSittingCopy.SAFE_SETTING_NOTE, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(
                    selected = safe,
                    onClick = { safe = !safe; owner.draft = owner.draft.copy(safeSettingConfirmed = safe) },
                    label = { Text(InitiationSittingCopy.SAFE_CHIP) },
                )
            }
            PrimaryActionEnabled(frame.primary, enabled = step != SittingStep.STOP || safe) { InitiationSitting.skip() }
            SkipEndRow(endLabel = frame.endLabel, onSkip = { InitiationSitting.skip() }, onEnd = endAndLeave)
        }
        SittingStep.PRACTICE -> SittingReady(owner, store, endAndLeave)
        SittingStep.LINE -> SittingLine(store, endAndLeave)
        SittingStep.END -> Unit
    }
}

@Composable
private fun PrimaryActionEnabled(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled) { Text(label) }
}

@Composable
private fun SkipEndRow(endLabel: String, onSkip: () -> Unit, onEnd: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onSkip) { Text("Skip") }
        TextButton(onClick = onEnd) { Text(endLabel) }
    }
}

@Composable
private fun SittingReady(owner: BreathingSessionOwner, store: SankalpStore, onEnd: () -> Unit) {
    val context = LocalContext.current
    val practice = owner.catalogue?.practice(INITIATION_PRACTICE_ID)
    if (practice == null) {
        Text("This practice is unavailable on this device. You can skip it.")
        SkipEndRow("End", onSkip = { InitiationSitting.skip() }, onEnd = onEnd)
        return
    }
    Heading(practice.title)
    Text(practice.originLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(practice.stages[BreathingStageId.INSTRUCTION].orEmpty())
    store.line?.let {
        Text(InitiationSittingCopy.READY_LEAD, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(it, style = MaterialTheme.typography.titleMedium)
    }
    Text("${INITIATION_PRACTICE_SECONDS / 60} min")
    PrimaryAction("Start") { startSittingPractice(context, owner, practice) }
    Text(BREATHING_SAFETY_NOTE, color = MaterialTheme.colorScheme.onSurfaceVariant)
    SkipEndRow("End", onSkip = { InitiationSitting.skip() }, onEnd = onEnd)
}

/** Starts the one practice at 180 s regardless of the draft's chosen length, and marks the entry card as used. */
fun startSittingPractice(context: Context, owner: BreathingSessionOwner, practice: BreathingPractice) {
    owner.choose(practice, INITIATION_PRACTICE_SECONDS)
    owner.start(context)
}

@Composable
private fun SittingLine(store: SankalpStore, onEnd: () -> Unit) {
    var text by remember { mutableStateOf(store.line.orEmpty()) }
    Heading(InitiationSittingCopy.LINE_TITLE)
    Text(InitiationSittingCopy.LINE_HELP)
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
        TextButton(onClick = { InitiationSitting.skip() }) { Text("Leave it blank") }
        TextButton(onClick = onEnd) { Text("End") }
    }
}

/** Under the running practice: Skip moves on to Close (ending the practice the normal way, one journal row). */
@Composable
fun SittingPracticeSkip(owner: BreathingSessionOwner) {
    val context = LocalContext.current
    TextButton(onClick = { owner.end(context); InitiationSitting.goTo(SittingStep.QUESTION) }) { Text("Skip to the close") }
}
