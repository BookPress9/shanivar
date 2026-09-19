package com.shanivar.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * MVP streamline task (package 2), rewritten 2026-09-18. Replaces the earlier five-page
 * LANGUAGE/ACCOUNT/BIRTH/CAPABILITIES/ARRIVAL flow, which asked for birth details and ran a
 * generic feature tour before either tool was reachable. This flow reaches a usable choice (Focus
 * or Alarm, on the Home screen behind [onComplete]) with no chart intake, no consciousness scoring
 * and no mandatory faith step - see INITIAL_MVP_SCOPE_2026-09-13.md and
 * docs/strategy/shanivar-tone-and-values.md, which this file's copy follows throughout.
 */
private enum class SetupPage { LANGUAGE, WELCOME, HELPER_GUIDE, PLAN, ACCOUNT, ARRIVAL }
private enum class AccountMode { CHOICE, SIGN_IN, CREATE }

@Composable
fun OnboardingFlow(
    api: ShanivarApi,
    account: Account?,
    initialLanguage: String,
    onLanguage: (String) -> Unit,
    onConnected: (Account) -> Unit,
    onIntentionChosen: (FocusPreset) -> Unit,
    onComplete: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(SetupPage.LANGUAGE) }
    var language by rememberSaveable { mutableStateOf(initialLanguage) }
    var mode by rememberSaveable { mutableStateOf(AccountMode.CHOICE) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPause by rememberSaveable { mutableStateOf(false) }
    var showShaniInfo by remember { mutableStateOf(false) }
    var selectedPreset by rememberSaveable { mutableStateOf(FocusPresets.DEFAULT.label) }
    val scope = rememberCoroutineScope()
    val hi = language == "hi"
    fun copy(en: String, hindi: String) = if (hi) hindi else en

    if (showPause) {
        // Pre-account, offline, no permissions - available the moment someone opens the app,
        // per "offer the local pause without sign-in" and "never count preview use as protected
        // activation." Exiting returns to wherever the pause was opened from.
        LocalPauseScreen(isPreview = true, onExit = { showPause = false })
        return
    }

    if (showShaniInfo) {
        AlertDialog(
            onDismissRequest = { showShaniInfo = false },
            title = { Text(copy("About the Shani framing", "शनि-प्रतीक के बारे में")) },
            text = { Text(copy(
                "Shani can be held as a symbol of time, consequence and patient discipline. This is entirely optional - Shanivar works the same whether or not you choose it, and it is never a requirement or a judgment.",
                "शनि को समय, कर्मफल और धैर्यपूर्ण अनुशासन के प्रतीक के रूप में देखा जा सकता है। यह पूरी तरह वैकल्पिक है - आप इसे चुनें या न चुनें, Shanivar वैसा ही काम करता है। यह कभी कोई अनिवार्यता या निर्णय नहीं है।",
            )) },
            confirmButton = { TextButton(onClick = { showShaniInfo = false }) { Text(copy("Close", "बंद करें")) } },
        )
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("SHANIVAR", color = MaterialTheme.colorScheme.primary, letterSpacing = 3.sp)
        when (page) {
            SetupPage.LANGUAGE -> {
                Heading("Choose your language\nअपनी भाषा चुनें")
                Text("Shanivar will keep the same quiet, direct character in both languages.")
                PrimaryAction("English") { language = "en"; onLanguage("en"); page = SetupPage.WELCOME }
                OutlinedButton({ language = "hi"; onLanguage("hi"); page = SetupPage.WELCOME }, Modifier.fillMaxWidth()) {
                    Text("हिंदी")
                }
            }
            SetupPage.WELCOME -> {
                Heading(copy("Make time for what matters.", "जो ज़रूरी है उसके लिए समय बनाएँ।"))
                Text(copy(
                    "Shanivar helps you begin a chosen action and protect a little time for it.",
                    "Shanivar आपको एक चुनी हुई गतिविधि शुरू करने और उसके लिए थोड़ा समय सुरक्षित रखने में मदद करता है।",
                ))
                PrimaryAction(copy("This is for me", "यह मेरे लिए है"), onComplete)
                OutlinedButton({ page = SetupPage.HELPER_GUIDE }, Modifier.fillMaxWidth()) {
                    Text(copy("I'm helping a parent or family member", "मैं अपने परिवार के किसी सदस्य की मदद कर रहा/रही हूँ"))
                }
                TextButton({ showPause = true }, Modifier.fillMaxWidth()) { Text(copy("Try a one-minute pause first", "पहले एक मिनट का विराम आज़माएँ")) }
                TextButton({ showShaniInfo = true }, Modifier.fillMaxWidth()) { Text(copy("About the optional Shani framing", "वैकल्पिक शनि-प्रतीक के बारे में")) }
            }
            SetupPage.HELPER_GUIDE -> {
                Heading(copy("Set it up together.", "साथ मिलकर शुरू करें।"))
                SectionCard(copy("Before you start", "शुरू करने से पहले")) {
                    Text(copy("Ask first. Explain that Shanivar pauses the whole app they choose, not just its feed, for the time they set.",
                        "पहले पूछें। समझाएँ कि Shanivar उनके चुने ऐप को पूरी तरह रोकता है, केवल उसकी फ़ीड को नहीं, और उतने ही समय के लिए जितना वे तय करें।"))
                    Text(copy("Help with setup where they want help.", "जहाँ चाहें, वहाँ सेटअप में मदद करें।"))
                    Text(copy("Then hand the phone back. The plan and every choice from here are theirs.",
                        "फिर फ़ोन वापस दें। यहाँ से हर चुनाव उन्हीं का है।"))
                }
                Text(copy("You will not see their private activity, reflections or messages.", "आप उनकी निजी गतिविधि, चिंतन या संदेश नहीं देख पाएँगे।"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton({ showShaniInfo = true }, Modifier.fillMaxWidth()) { Text(copy("About the optional Shani framing", "वैकल्पिक शनि-प्रतीक के बारे में")) }
                PrimaryAction(copy("Continue to their plan", "उनकी योजना पर जाएँ"), onComplete)
                TextButton({ page = SetupPage.WELCOME }, Modifier.fillMaxWidth()) { Text(copy("Back", "पीछे")) }
            }
            SetupPage.PLAN -> {
                Heading(copy("What deserves your attention?", "आपका ध्यान किस चीज़ का हकदार है?"))
                Text(copy(
                    "Choose what this time is for. You can change it anytime, and set a wake-up alarm as well - neither tool needs the other.",
                    "तय करें कि यह समय किसके लिए है। इसे कभी भी बदला जा सकता है, और साथ में एक अलार्म भी लगाया जा सकता है - दोनों एक-दूसरे पर निर्भर नहीं हैं।",
                ))
                FocusPresets.ALL.forEach { preset ->
                    FilterChip(selected = selectedPreset == preset.label, onClick = { selectedPreset = preset.label },
                        label = { Text("${preset.label} · ${preset.suggestedMinutes} min") })
                }
                Text(copy(
                    "When you protect time, the app you choose stays paused - fully, not just its feed - until your session ends.",
                    "समय सुरक्षित करते समय, आपका चुना ऐप पूरी तरह रुका रहता है - केवल फ़ीड नहीं - जब तक सत्र समाप्त न हो।",
                ), color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryAction(copy("Continue", "आगे बढ़ें")) {
                    onIntentionChosen(FocusPresets.ALL.first { it.label == selectedPreset })
                    page = SetupPage.ACCOUNT
                }
            }
            SetupPage.ACCOUNT -> {
                Heading(copy("Arrive as you are.", "जैसे हैं, वैसे आइए।"))
                if (account != null) {
                    Text(copy("Signed in as ${account.name}.", "${account.name} के रूप में साइन इन हैं।"))
                    PrimaryAction(copy("Continue", "आगे बढ़ें")) { page = SetupPage.ARRIVAL }
                } else when (mode) {
                    AccountMode.CHOICE -> {
                        Text(copy("An account keeps your plan and progress with you across devices.", "खाता आपकी योजना और प्रगति को सभी उपकरणों पर सुरक्षित रखता है।"))
                        PrimaryAction(copy("Create my account", "अपना खाता बनाएँ")) { mode = AccountMode.CREATE }
                        OutlinedButton({ mode = AccountMode.SIGN_IN }, Modifier.fillMaxWidth()) { Text(copy("Sign in", "साइन इन करें")) }
                        TextButton({ page = SetupPage.ARRIVAL }, Modifier.fillMaxWidth()) { Text(copy("Explore first", "पहले देखें")) }
                    }
                    AccountMode.SIGN_IN, AccountMode.CREATE -> {
                        if (mode == AccountMode.CREATE) {
                            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(copy("Name", "नाम")) }, singleLine = true)
                        }
                        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(copy("Email", "ईमेल")) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(copy("Password", "पासवर्ड")) }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        PrimaryAction(copy(if (mode == AccountMode.CREATE) "Create account" else "Sign in", if (mode == AccountMode.CREATE) "खाता बनाएँ" else "साइन इन करें")) {
                            busy = true; error = null
                            scope.launch {
                                try {
                                    val connected = if (mode == AccountMode.CREATE) {
                                        api.register(SignupDetails(name.ifBlank { "Observer" }, email, password, language))
                                    } else api.login(email.trim(), password)
                                    onConnected(connected); password = ""; page = SetupPage.ARRIVAL
                                } catch (e: Exception) {
                                    error = if (e is ApiFailure) e.message else copy("Could not connect. Try again.", "कनेक्ट नहीं हो सका। फिर प्रयास करें।")
                                } finally { busy = false }
                            }
                        }
                        TextButton({ mode = AccountMode.CHOICE; error = null }, Modifier.fillMaxWidth()) { Text(copy("Back", "पीछे")) }
                    }
                }
            }
            SetupPage.ARRIVAL -> {
                SaturnRing()
                Heading(copy("Your attention.\nBack in your hands.", "आपका ध्यान।\nफिर आपके हाथ में।"))
                Text(copy("One chosen activity. One protected window. Your call, every time.", "एक चुनी गतिविधि। एक सुरक्षित समय। हर बार आपका निर्णय।"))
                PrimaryAction(copy("Go to my Home", "अपने होम पर जाएँ"), onComplete)
            }
        }
    }
}
