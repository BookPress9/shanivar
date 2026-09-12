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

private enum class SetupPage { LANGUAGE, ACCOUNT, BIRTH, CAPABILITIES, ARRIVAL }
private enum class AccountMode { CHOICE, SIGN_IN, CREATE }

@Composable
fun OnboardingFlow(
    api: ShanivarApi,
    account: Account?,
    initialLanguage: String,
    onLanguage: (String) -> Unit,
    onConnected: (Account) -> Unit,
    onComplete: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(SetupPage.LANGUAGE) }
    var language by rememberSaveable { mutableStateOf(initialLanguage) }
    var mode by rememberSaveable { mutableStateOf(AccountMode.CHOICE) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var birthTime by rememberSaveable { mutableStateOf("") }
    var birthPlace by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val hi = language == "hi"
    fun copy(en: String, hindi: String) = if (hi) hindi else en

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("SHANIVAR", color = MaterialTheme.colorScheme.primary, letterSpacing = 3.sp)
        Text("${page.ordinal + 1} / 5", color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (page) {
            SetupPage.LANGUAGE -> {
                Heading("Choose your language\nअपनी भाषा चुनें")
                Text("Shanivar will keep the same quiet, direct character in both languages.")
                PrimaryAction("English") { language = "en"; onLanguage("en"); page = SetupPage.ACCOUNT }
                OutlinedButton({ language = "hi"; onLanguage("hi"); page = SetupPage.ACCOUNT }, Modifier.fillMaxWidth()) {
                    Text("हिंदी")
                }
            }
            SetupPage.ACCOUNT -> {
                Heading(copy("Arrive as you are.", "जैसे हैं, वैसे आइए।"))
                if (account != null) {
                    Text(copy("Signed in as ${account.name}.", "${account.name} के रूप में साइन इन हैं।"))
                    PrimaryAction(copy("Continue", "आगे बढ़ें")) { page = SetupPage.BIRTH }
                } else when (mode) {
                    AccountMode.CHOICE -> {
                        Text(copy("An account keeps your rhythm and reflections with you across devices.", "खाता आपकी दिनचर्या और चिंतन को सभी उपकरणों पर सुरक्षित रखता है।"))
                        PrimaryAction(copy("Create my account", "अपना खाता बनाएँ")) { mode = AccountMode.CREATE }
                        OutlinedButton({ mode = AccountMode.SIGN_IN }, Modifier.fillMaxWidth()) { Text(copy("Sign in", "साइन इन करें")) }
                        TextButton({ page = SetupPage.BIRTH }, Modifier.fillMaxWidth()) { Text(copy("Explore first", "पहले देखें")) }
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
                                    onConnected(connected); password = ""; page = SetupPage.BIRTH
                                } catch (e: Exception) {
                                    error = if (e is ApiFailure) e.message else copy("Could not connect. Try again.", "कनेक्ट नहीं हो सका। फिर प्रयास करें।")
                                } finally { busy = false }
                            }
                        }
                        TextButton({ mode = AccountMode.CHOICE; error = null }, Modifier.fillMaxWidth()) { Text(copy("Back", "पीछे")) }
                    }
                }
            }
            SetupPage.BIRTH -> {
                Heading(copy("A little more context.", "थोड़ा और संदर्भ।"))
                Text(copy("Birth details are optional. Complete details can make chart reflections more relevant; they do not determine your choices.", "जन्म विवरण वैकल्पिक हैं। पूरा विवरण ज्योतिषीय चिंतन को अधिक प्रासंगिक बना सकता है; आपके निर्णय आपके अपने हैं।"))
                OutlinedTextField(birthDate, { birthDate = it }, Modifier.fillMaxWidth(), label = { Text(copy("Birth date · YYYY-MM-DD", "जन्म तिथि · YYYY-MM-DD")) }, singleLine = true)
                OutlinedTextField(birthTime, { birthTime = it }, Modifier.fillMaxWidth(), label = { Text(copy("Birth time · HH:MM", "जन्म समय · HH:MM")) }, singleLine = true)
                OutlinedTextField(birthPlace, { birthPlace = it }, Modifier.fillMaxWidth(), label = { Text(copy("Birth place", "जन्म स्थान")) }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                PrimaryAction(copy("Save and continue", "सहेजें और आगे बढ़ें")) {
                    if (birthDate.isNotBlank() && !Regex("\\d{4}-\\d{2}-\\d{2}").matches(birthDate)) {
                        error = copy("Use YYYY-MM-DD for the date.", "तिथि के लिए YYYY-MM-DD लिखें।")
                    } else if (birthTime.isNotBlank() && !Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(birthTime)) {
                        error = copy("Use HH:MM for the time.", "समय के लिए HH:MM लिखें।")
                    } else if (account == null) page = SetupPage.CAPABILITIES
                    else {
                        busy = true; scope.launch {
                            try { api.saveProfile(language, birthDate, birthTime, birthPlace); page = SetupPage.CAPABILITIES }
                            catch (e: Exception) { error = if (e is ApiFailure) e.message else copy("Could not save yet.", "अभी सहेजा नहीं जा सका।") }
                            finally { busy = false }
                        }
                    }
                }
                TextButton({ page = SetupPage.CAPABILITIES }, Modifier.fillMaxWidth()) { Text(copy("Skip for now", "अभी छोड़ें")) }
            }
            SetupPage.CAPABILITIES -> {
                Heading(copy("You stay in control.", "नियंत्रण आपके हाथ में।"))
                SectionCard(copy("Focus windows", "ध्यान का समय")) { Text(copy("Choose one app and one short window. Android access is requested only when you start protection.", "एक ऐप और छोटा समय चुनें। सुरक्षा शुरू करते समय ही Android अनुमति माँगी जाएगी।")) }
                SectionCard(copy("Movement alarm", "चलने वाला अलार्म")) { Text(copy("Choose a familiar destination and a small step target. Camera confirmation remains optional and on-device.", "परिचित स्थान और कुछ कदम चुनें। कैमरे से पुष्टि वैकल्पिक है और फ़ोन पर ही होती है।")) }
                Text(copy("Shani can be held as a symbol of time, consequence and patient discipline. Faith is welcomed, never assumed.", "शनि समय, कर्मफल और धैर्यपूर्ण अनुशासन के प्रतीक हैं। आपकी आस्था का सम्मान है, पर उसे माना नहीं जाता।"))
                PrimaryAction(copy("I understand", "मैं समझता/समझती हूँ")) { page = SetupPage.ARRIVAL }
            }
            SetupPage.ARRIVAL -> {
                SaturnRing()
                Heading(copy("Your attention.\nBack in your hands.", "आपका ध्यान।\nफिर आपके हाथ में।"))
                Text(copy("One rhythm. One deliberate return at a time.", "एक दिनचर्या। हर बार एक सजग वापसी।"))
                PrimaryAction(copy("Build my rhythm", "अपनी दिनचर्या बनाएँ"), onComplete)
            }
        }
    }
}
