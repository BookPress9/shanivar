package com.shanivar.nativeapp

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray

private val Void = Color(0xFF101111)
private val Bone = Color(0xFFE8E3D8)
private val Gold = Color(0xFFD1DCAA)
private val Muted = Color(0xFFB6B3AD)
private val Surface = Color(0xFF191B1A)

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
        prefs.getInt("focus", 30), prefs.getString("movementDestination", "morning_light")!!,
        prefs.getInt("stepTarget", 18), prefs.getBoolean("sceneConfirmation", false),
        prefs.getBoolean("accessibleMovement", false), prefs.getString("whatsapp", "08:00")!!,
        prefs.getString("tone", "Direct")!!, prefs.getBoolean("dailyNoteEnabled", true),
        prefs.getStringSet("blockedPackages", DisciplineTargets.DEFAULT_BLOCKED_PACKAGES) ?: DisciplineTargets.DEFAULT_BLOCKED_PACKAGES,
        prefs.getString("intentionLabel", FocusPresets.DEFAULT.label)!!)
        .takeIf { it.valid() } ?: Rhythm()
}

private fun saveRhythm(context: Context, rhythm: Rhythm) {
    context.getSharedPreferences("rhythm", Context.MODE_PRIVATE).edit()
        .putString("wake", rhythm.wakeTime).putString("quiet", rhythm.quietTime)
        .putInt("focus", rhythm.focusMinutes).putString("movementDestination", rhythm.movementDestination)
        .putInt("stepTarget", rhythm.stepTarget).putBoolean("sceneConfirmation", rhythm.sceneConfirmation)
        .putBoolean("accessibleMovement", rhythm.accessibleMovement)
        .putString("whatsapp", rhythm.whatsappTime).putString("tone", rhythm.whatsappTone)
        .putBoolean("dailyNoteEnabled", rhythm.dailyNoteEnabled)
        .putStringSet("blockedPackages", rhythm.blockedPackages)
        .putString("intentionLabel", rhythm.intentionLabel).apply()
}

/** Local bookkeeping for the one v1 alarm: which backend record (if any) it is, its selected
 * repeat days, and whether it's currently enabled. Kept separate from [Rhythm] because it is
 * alarm-specific, not part of the `/discipline/preferences` contract - see RoutineApi.kt. */
private data class AlarmPlan(val remoteId: String? = null, val days: Set<Int> = setOf(0, 1, 2, 3, 4, 5, 6), val enabled: Boolean = false)

private fun loadAlarmPlan(context: Context): AlarmPlan {
    val prefs = context.getSharedPreferences("rhythm", Context.MODE_PRIVATE)
    val days = prefs.getString("alarmDays", null)?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
    return AlarmPlan(
        remoteId = prefs.getString("alarmId", null),
        days = days?.takeIf { it.isNotEmpty() } ?: setOf(0, 1, 2, 3, 4, 5, 6),
        enabled = prefs.getBoolean("alarmEnabled", false),
    )
}

private fun saveAlarmPlan(context: Context, plan: AlarmPlan) {
    context.getSharedPreferences("rhythm", Context.MODE_PRIVATE).edit()
        .putString("alarmId", plan.remoteId)
        .putString("alarmDays", plan.days.sorted().joinToString(","))
        .putBoolean("alarmEnabled", plan.enabled)
        .apply()
}

@Composable
fun ShanivarApp(openBreathing: Boolean = false) {
    val context = LocalContext.current
    var rhythm by remember { mutableStateOf(loadRhythm(context)) }
    var alarmPlan by remember { mutableStateOf(loadAlarmPlan(context)) }
    var language by rememberSaveable { mutableStateOf(context.getSharedPreferences("rhythm", 0).getString("language", "en") ?: "en") }
    var tab by rememberSaveable { mutableStateOf("Today") } // LegacyTabShell only
    var welcome by rememberSaveable { mutableStateOf(!context.getSharedPreferences("rhythm", 0).getBoolean("introduced", false)) }
    var accountDialog by rememberSaveable { mutableStateOf(false) }
    val session: AccountState = viewModel()
    LaunchedEffect(Unit) { session.restore(context.applicationContext) }
    val reflections: ReflectionState = viewModel() // LegacyTabShell only in the streamlined build
    val reflectionStore = remember { EncryptedReflectionStore(context) }
    var reflectionOpen by rememberSaveable { mutableStateOf(false) }
    var reflectionEntry by rememberSaveable { mutableStateOf(ReflectionEntryPoint.TODAY) }
    val reflectionProtection = remember(session.api) {
        DisciplineEngine.create(context, session.api, AlarmScheduler(context)).reflectionReader()
    }
    val discipline = remember(session.api) { DisciplineEngine.create(context, session.api, AlarmScheduler(context)) }
    val account = session.account
    LaunchedEffect(account?.id) {
        account?.let {
            val local = context.getSharedPreferences("rhythm", 0)
            val previousOwner = local.getString("planOwner", null)
            if (previousOwner != null && previousOwner != it.id) {
                AlarmScheduler(context).cancel()
                rhythm = Rhythm()
                alarmPlan = AlarmPlan()
                saveRhythm(context, rhythm)
                saveAlarmPlan(context, alarmPlan)
                context.getSharedPreferences("session_reviews", 0).edit().clear().apply()
            }
            local.edit().putString("planOwner", it.id).apply()
            reflections.bind(it.id, ReflectionApi(session.api), reflectionStore)
            runCatching { RoutineApi(session.api).load() }.getOrNull()?.let { remote ->
                rhythm = rhythm.copy(wakeTime = remote.wakeTime, quietTime = remote.quietTime, focusMinutes = remote.focusMinutes,
                    movementDestination = remote.movementDestination, stepTarget = remote.stepTarget,
                    sceneConfirmation = remote.sceneConfirmation, accessibleMovement = remote.accessibleMovement,
                    whatsappTime = remote.whatsappTime, whatsappTone = remote.whatsappTone,
                    dailyNoteEnabled = remote.dailyNoteEnabled, blockedPackages = remote.blockedPackages)
                language = remote.language
                saveRhythm(context, rhythm)
                context.getSharedPreferences("rhythm", 0).edit().putString("language", remote.language).apply()
            }
            // Discover an existing alarm on this account rather than only trusting a locally
            // cached id - covers re-login, reinstall, and a second device, and means a stale id
            // left over from a *different* signed-in account on this device is never reused (see
            // RoutineApi.listAlarms's doc comment).
            runCatching { RoutineApi(session.api).listAlarms() }.getOrNull()?.firstOrNull()?.let { remote ->
                val locallyDisabled = alarmPlan.remoteId == remote.id && !alarmPlan.enabled
                alarmPlan = AlarmPlan(remote.id, remote.daysOfWeek, remote.enabled && !locallyDisabled)
                saveAlarmPlan(context, alarmPlan)
                if (remote.enabled && !locallyDisabled) {
                    rhythm = rhythm.copy(wakeTime = remote.localTime, movementDestination = remote.movementDestination,
                        stepTarget = remote.stepTarget, accessibleMovement = remote.accessibleMovement)
                    saveRhythm(context, rhythm)
                    AlarmScheduler(context).scheduleRule(remote.id, LocalTime.parse(remote.localTime), remote.daysOfWeek)
                }
            }
        }
    }
    LaunchedEffect(ReflectionNavigation.requested, account?.id) {
        ReflectionNavigation.requested?.let { entry ->
            if (account == null) accountDialog = true
            else { reflectionEntry = entry; reflectionOpen = true; ReflectionNavigation.consume() }
        }
    }
    var sessionId by rememberSaveable { mutableStateOf<String?>(null) } // LegacyTabShell (Practice) only
    val techniques = remember { loadTechniques(context) }

    ShanivarTheme {
        Surface(Modifier.fillMaxSize(), color = Void, contentColor = Bone) {
        // Stable across the welcome toggle so Compose hooks are not conditional.
        val onboardingGateway = remember(context) { AndroidProtectionGateway(context) }
        val onboardingPlanRepo = remember(context) {
            ProtectionSyncingRoutinePlanRepository(SharedPrefsRoutinePlanRepository(context), onboardingGateway)
        }
        if (welcome) {
            val useV3Onboarding = shouldUseRoutineV3Onboarding(
                routineV3 = BuildConfig.ROUTINE_V3,
                debugBuild = BuildConfig.DEBUG,
                debugPrefEnabled = readDebugV3OnboardingPref(context),
            )
            if (useV3Onboarding) {
                // V3 first-run sets up Routine (actual day). ROUTINE_V3 stays default-false;
                // debug builds can force this path with SharedPreferences debug_v3_onboarding=true.
                RoutineOnboardingFlow(
                    repository = onboardingPlanRepo,
                    onWakeAlarmArmed = { days, _ ->
                        val wake = onboardingPlanRepo.current().wakeTime?.toString()?.take(5) ?: rhythm.wakeTime
                        rhythm = rhythm.copy(wakeTime = wake)
                        saveRhythm(context, rhythm)
                        // Local arm only — no account id. AlarmCard can sync to the server later if signed in.
                        alarmPlan = AlarmPlan(remoteId = null, days = days, enabled = true)
                        saveAlarmPlan(context, alarmPlan)
                    },
                    onComplete = {
                        val plan = onboardingPlanRepo.current()
                        plan.wakeTime?.let { wake ->
                            rhythm = rhythm.copy(wakeTime = wake.toString().take(5))
                        }
                        plan.intendedSleepTime?.let { sleep ->
                            // quietTime remains the legacy rhythm field; breathing prefs are mirrored by the repo.
                            rhythm = rhythm.copy(quietTime = sleep.toString().take(5))
                        }
                        saveRhythm(context, rhythm)
                        context.getSharedPreferences("rhythm", 0).edit().putBoolean("introduced", true).apply()
                        welcome = false
                        tab = "Rhythm"
                    },
                )
            } else {
                OnboardingFlow(session.api, account, language,
                    onLanguage = {
                        language = it
                        context.getSharedPreferences("rhythm", 0).edit().putString("language", it).apply()
                    },
                    onConnected = { session.connect(it) },
                    onIntentionChosen = { preset ->
                        rhythm = rhythm.copy(intentionLabel = preset.label, focusMinutes = preset.suggestedMinutes)
                        saveRhythm(context, rhythm)
                    }) {
                    context.getSharedPreferences("rhythm", 0).edit().putBoolean("introduced", true).apply()
                    welcome = false
                    tab = "Rhythm"
                }
            }
        } else if (!BuildConfig.STREAMLINED_LAUNCH && reflectionOpen && account != null && reflections.owner == account.id) {
            ReflectionScreen(reflections, reflectionEntry, protection = reflectionProtection,
                onExit = { reflectionOpen = false },
                onPractices = { reflectionOpen = false; tab = "Practice" })
        } else if (!BuildConfig.STREAMLINED_LAUNCH && sessionId != null) {
            val technique = techniques.firstOrNull { it.id == sessionId }
            if (technique != null) PracticePlayer(technique) { sessionId = null }
        } else if (BuildConfig.STREAMLINED_LAUNCH) {
            HomeScreen(
                rhythm = rhythm, onRhythmChange = { rhythm = it; saveRhythm(context, it) },
                alarmPlan = alarmPlan, onAlarmPlanChange = { alarmPlan = it; saveAlarmPlan(context, it) },
                connected = account != null, account = account, api = session.api, language = language,
                discipline = discipline, signIn = { accountDialog = true }, onOpenAccount = { accountDialog = true },
                openBreathing = openBreathing,
            )
        } else {
            LegacyTabShell(rhythm, { rhythm = it; saveRhythm(context, it) }, tab, { tab = it }, account, session,
                discipline, techniques, sessionId, { sessionId = it }, reflections, reflectionStore,
                { reflectionEntry = it }, { reflectionOpen = it }, { accountDialog = true }, language)
        }
        if (accountDialog) AccountDialog(account, session.api, onClose = { accountDialog = false },
            canLogout = !alarmPlan.enabled && context.getSharedPreferences("focus_guard", 0).getLong("endsAt", 0L) <= System.currentTimeMillis(),
            onConnected = { session.connect(it); accountDialog = false },
            onLogout = {
                ReflectionNavigation.consume(); reflections.clear(); reflectionStore.clear(); reflectionOpen = false
                session.logout()
                // A different account signing in afterward must not inherit this account's cached
                // alarm id - the discovery LaunchedEffect above re-derives it from the server on
                // the next connect, but clear the local cache immediately so an in-between guest
                // state can't act on it.
                alarmPlan = AlarmPlan()
                saveAlarmPlan(context, alarmPlan)
                rhythm = Rhythm()
                saveRhythm(context, rhythm)
                context.getSharedPreferences("session_reviews", 0).edit().clear().apply()
                FocusScheduler(context).cancel()
                FocusCommitmentCache.clear(context)
                accountDialog = false
            })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────────────────────
// Streamlined Home (MVP streamline task, package 3): one screen, two independent tools. Replaces
// the four-tab shell for new installs - see BuildConfig.STREAMLINED_LAUNCH and LegacyTabShell
// below, which is kept intact rather than deleted.
// ─────────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeScreen(
    rhythm: Rhythm, onRhythmChange: (Rhythm) -> Unit,
    alarmPlan: AlarmPlan, onAlarmPlanChange: (AlarmPlan) -> Unit,
    connected: Boolean, account: Account?, api: ShanivarApi, language: String,
    discipline: DisciplineGateway, signIn: () -> Unit, onOpenAccount: () -> Unit,
    openBreathing: Boolean = false,
) {
    val context = LocalContext.current
    var operationBusy by remember { mutableStateOf(false) }
    if (BuildConfig.ROUTINE_V3) {
        val gateway = remember(context) { AndroidProtectionGateway(context) }
        // Saving through this wrapper syncs plan.windows → FocusScheduler registry only when the
        // protection config changed; anchor/activity edits never arm anything.
        val repo = remember(context) { ProtectionSyncingRoutinePlanRepository(SharedPrefsRoutinePlanRepository(context), gateway) }
        val protection = remember(repo, connected, discipline) {
            RoutineProtectionController(repo, gateway, mirrorRelease = { if (connected) discipline.releaseFocus() })
        }
        val scope = rememberCoroutineScope()
        var resumeCount by remember { mutableIntStateOf(0) }
        var protectionRevision by remember { mutableIntStateOf(0) }
        OnAppResume { resumeCount++ }
        val nextAlarm = remember(resumeCount) { AlarmScheduler(context).pendingTriggerAtEpochMillis() }
        val alarmArmed = nextAlarm != null
        val overview = rememberProtectionOverview(protection, protectionRevision)
        val windowLabels = remember(overview) {
            val now = java.time.ZonedDateTime.now()
            overview.windows.associate { it.windowId to it.status.chip { ms -> epochToClock(ms, now) } }
        }
        val protectionApps = remember(resumeCount, protectionRevision) {
            ProtectionAppCatalog.options(repo.current().protectedPackages) { pkg ->
                runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
            }
        }
        var shani by rememberSaveable { mutableStateOf(context.getSharedPreferences("rhythm", 0).getBoolean("shaniFraming", false)) }
        RoutineShell(
            routine = {
                RoutineScreen(
                    repository = repo,
                    alarm = { AlarmCard(rhythm, onRhythmChange, alarmPlan, onAlarmPlanChange, connected, api, language, signIn) { operationBusy = it } },
                    // V3: App protection replaces the single-rule FocusScheduleCard (which would cancel the
                    // whole registry when the account has no schedule). Manual focus stays.
                    focus = { FocusCard(rhythm, onRhythmChange, connected, api, discipline, signIn, showSchedule = false) { operationBusy = it } },
                    nextAlarmEpochMillis = nextAlarm,
                    alarmArmed = alarmArmed,
                    protectionStatusByWindowId = windowLabels,
                    protectionSummary = { plan, open ->
                        RoutineProtectionSummary(plan, overview, onEdit = open, onStop = {
                            scope.launch { protection.stopNow(); protectionRevision++ }
                        })
                    },
                    protectionEditor = { close ->
                        AppProtectionEditor(protection, protectionApps, overview, onRefresh = { protectionRevision++ }, onClose = close)
                    },
                )
            },
            sadhana = { entry, exit -> BreathingHost(entry, onExit = exit) },
            settings = {
                Heading("Your choices.")
                PrimaryAction(if (account == null) "Sign in or create account" else "Account · ${account.name}", onOpenAccount)
                SharedSettingsCards(shani) { value ->
                    shani = value
                    context.getSharedPreferences("rhythm", 0).edit().putBoolean("shaniFraming", value).apply()
                }
            },
            navigationEnabled = !operationBusy,
            initialDestination = if (openBreathing) RoutineDestination.SADHANA else RoutineDestination.ROUTINE,
            initialSadhanaEntry = if (openBreathing) BreathingEntry.AFTER_ALARM else BreathingEntry.HOME,
        )
        return
    }
    if (BuildConfig.THREE_TAB_NAV) {
        var resumeCount by remember { mutableIntStateOf(0) }
        OnAppResume { resumeCount++ }
        // Re-read on resume rather than observing: all three sources are written by engines outside
        // this composition (the focus guard, AlarmManager, the schedule receiver), and returning to the
        // app is exactly when a stale value would be visible.
        val snapshot = remember(resumeCount, rhythm) {
            HomeSnapshot(
                nowEpochMillis = System.currentTimeMillis(),
                focusEndsAtEpochMillis = context.getSharedPreferences("focus_guard", 0).getLong("endsAt", 0L),
                nextAlarmEpochMillis = AlarmScheduler(context).pendingTriggerAtEpochMillis(),
                hasFocusSchedule = FocusScheduler(context).pendingRule() != null,
                intentionLabel = rhythm.intentionLabel,
                focusMinutes = rhythm.focusMinutes,
                // BreathingRecord now carries a date (wish-list B2, shown per practice in Steady), but
                // this superseded Dashboard scaffold (decision 0014) is deliberately not rewired to it:
                // left false rather than widening B2 into deciding when a practice is *offered*.
                practisedToday = false,
                scheduleWindow = BreathingPreferences(context).schedule().window(LocalTime.now()),
            )
        }
        var shani by rememberSaveable { mutableStateOf(context.getSharedPreferences("rhythm", 0).getBoolean("shaniFraming", false)) }
        ShanivarShell(
            snapshot = snapshot,
            accountName = account?.name,
            onOpenAccount = onOpenAccount,
            onReturnToDay = {
                context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            routineAlarm = { AlarmCard(rhythm, onRhythmChange, alarmPlan, onAlarmPlanChange, connected, api, language, signIn) { operationBusy = it } },
            routineFocus = { FocusCard(rhythm, onRhythmChange, connected, api, discipline, signIn) { operationBusy = it } },
            sadhana = { entry, exit -> BreathingHost(entry, onExit = exit) },
            settings = {
                Heading("Your choices.")
                PrimaryAction(if (account == null) "Sign in or create account" else "Account · ${account.name}", onOpenAccount)
                SharedSettingsCards(shani) { value ->
                    shani = value
                    context.getSharedPreferences("rhythm", 0).edit().putBoolean("shaniFraming", value).apply()
                }
            },
            navigationEnabled = !operationBusy,
            initialTab = if (openBreathing) ShanivarTab.SADHANA else ShanivarTab.DASHBOARD,
        )
        return
    }
    V1Home(rhythm.intentionLabel, rhythm.focusMinutes, account?.name, onOpenAccount, navigationEnabled = !operationBusy,
        focus = { FocusCard(rhythm, onRhythmChange, connected, api, discipline, signIn) { operationBusy = it } },
        alarm = { AlarmCard(rhythm, onRhythmChange, alarmPlan, onAlarmPlanChange, connected, api, language, signIn) { operationBusy = it } },
        // Note what is not passed: `discipline`. The breathing host gets navigation and nothing else,
        // so it has no way to start focus, release it, or touch an alarm (handoff section 6).
        breathe = { entry, exit -> BreathingHost(entry, onExit = exit) },
        initialRoute = if (openBreathing) "breathe" else "home",
        initialBreatheEntry = if (openBreathing) BreathingEntry.AFTER_ALARM else BreathingEntry.HOME)
}

@Composable
private fun FocusCard(rhythm: Rhythm, onRhythmChange: (Rhythm) -> Unit, connected: Boolean, api: ShanivarApi, gateway: DisciplineGateway, signIn: () -> Unit, showSchedule: Boolean = true, onBusy: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<DisciplineStatus?>(null) }
    var busy by remember { mutableStateOf(false) }
    var scheduleBusy by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    SideEffect { onBusy(busy || scheduleBusy) }
    DisposableEffect(Unit) { onDispose { onBusy(false) } }
    var error by remember { mutableStateOf<String?>(null) }
    var showReview by rememberSaveable { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val permissions = remember { AndroidDisciplinePermissions(context) }
    var permissionRevision by remember { mutableIntStateOf(0) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionRevision++ }
    fun refresh() {
        if (!connected || checking || busy) return
        checking = true
        scope.launch {
            try { status = gateway.status(); error = null }
            catch (e: Exception) { error = e.message ?: "Could not check protection. Try again." }
            finally { checking = false }
        }
    }
    LaunchedEffect(connected) { if (connected) refresh() else status = null }
    OnAppResume { permissionRevision++; refresh() }
    LaunchedEffect(status?.focusEndsAtEpochMillis) {
        val end = status?.focusEndsAtEpochMillis ?: return@LaunchedEffect
        while (now < end) { delay(1000); now = System.currentTimeMillis() }
        if (status?.protection == ProtectionState.ACTIVE) {
            status = status?.copy(protection = ProtectionState.OFF)
            showReview = true
        }
    }
    // Read permission state again after returning from Android settings.
    val notificationReady = remember(permissionRevision) { permissions.hasNotificationPermission() }
    val installedTargets = remember(permissionRevision) {
        KnownDistractingApps.ALL.filter { app ->
            runCatching { context.packageManager.getPackageInfo(app.packageName, 0) }.isSuccess
        }.map { it.packageName }.toSet()
    }
    LaunchedEffect(installedTargets) {
        val available = rhythm.blockedPackages.intersect(installedTargets)
        if (available != rhythm.blockedPackages) onRhythmChange(rhythm.copy(blockedPackages = available))
    }
    val active = status?.protection == ProtectionState.ACTIVE
    SectionCard(if (active) "Focus in progress" else "What is this time for?") {
        if (!active) {
            FocusPresets.ALL.forEach { preset ->
                FilterChip(selected = rhythm.intentionLabel == preset.label, enabled = !busy,
                    onClick = { onRhythmChange(rhythm.copy(intentionLabel = preset.label, focusMinutes = preset.suggestedMinutes)) },
                    label = { Text(preset.label) })
            }
            OutlinedTextField(rhythm.intentionLabel, { onRhythmChange(rhythm.copy(intentionLabel = it.take(60))) },
                label = { Text("Your chosen activity") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("${rhythm.focusMinutes} minutes", fontSize = 24.sp)
            Slider(value = rhythm.focusMinutes.toFloat(), onValueChange = { onRhythmChange(rhythm.copy(focusMinutes = it.toInt())) },
                enabled = !busy, valueRange = 5f..120f, steps = 22,
                modifier = Modifier.semantics { contentDescription = "Focus duration in minutes" })
            Text("Apps to pause", color = Muted)
            KnownDistractingApps.ALL.forEach { app ->
                FilterChip(selected = rhythm.blockedPackages.contains(app.packageName), enabled = !busy && app.packageName in installedTargets, onClick = {
                    val next = if (rhythm.blockedPackages.contains(app.packageName)) rhythm.blockedPackages - app.packageName
                        else rhythm.blockedPackages + app.packageName
                    onRhythmChange(rhythm.copy(blockedPackages = next))
                }, label = { Text(app.label + if (app.packageName !in installedTargets) " · not installed" else "") })
            }
            Text("The whole selected app is blocked, including messages. Calls and system apps are not selected.", color = Muted)
            if (installedTargets.isEmpty()) Text("No supported distracting apps are installed on this device.", color = Muted)
        }
        when {
            !connected -> {
                Text("Sign in to start protection.", color = Muted)
                PrimaryAction("Sign in to protect this window", signIn)
            }
            active -> {
                Text(rhythm.intentionLabel, style = MaterialTheme.typography.headlineMedium)
                val seconds = ((status!!.focusEndsAtEpochMillis!! - now) / 1000).coerceAtLeast(0)
                Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayMedium)
                Text("Focus protected", color = Gold)
                OutlinedButton(onClick = { confirmEnd = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Ending…" else "End this session now")
                }
            }
            status?.protection == ProtectionState.NEEDS_PERMISSION -> {
                Text("Allow Android access, then return here.", color = Gold)
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Grant Usage access") }
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }, modifier = Modifier.fillMaxWidth()) { Text("Allow display over apps") }
                TextButton(onClick = { refresh() }) { Text("Check access again") }
            }
            else -> {
                if (!notificationReady && Build.VERSION.SDK_INT >= 33)
                    OutlinedButton(onClick = { notifications.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow focus notification") }
                Button(enabled = !busy && !checking && status != null && error == null && rhythm.intentionLabel.isNotBlank() && rhythm.blockedPackages.isNotEmpty(),
                    onClick = {
                        busy = true; error = null
                        scope.launch {
                            try { status = gateway.startFocus(rhythm.focusMinutes, rhythm.blockedPackages); now = System.currentTimeMillis() }
                            catch (e: Exception) { error = e.message ?: "Protection did not start. Check status before retrying." }
                            finally { busy = false }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Starting…" else "Start protected focus") }
            }
        }
        if (checking || busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Connecting… The server may take a moment to wake.", color = Muted) }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            TextButton(onClick = { refresh() }, enabled = !busy && !checking) { Text("Check status & retry") }
            TextButton(onClick = signIn) { Text("Reconnect account") }
        }
    }
    if (showReview) SessionReviewDialog(rhythm.intentionLabel) { showReview = false }
    if (confirmEnd) AlertDialog(onDismissRequest = { confirmEnd = false },
        title = { Text("End focus now?") }, text = { Text("Your selected apps will become available after release is confirmed.") },
        confirmButton = { TextButton(onClick = {
            confirmEnd = false; busy = true; error = null
            scope.launch {
                try { status = gateway.releaseFocus(); showReview = true }
                catch (e: Exception) { error = e.message ?: "Could not end focus. Your protection remains active." }
                finally { busy = false }
            }
        }) { Text("End focus") } },
        dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Keep focusing") } })
    if (showSchedule) FocusScheduleCard(connected, api, signIn) { scheduleBusy = it }
}

@Composable
private fun AlarmCard(
    rhythm: Rhythm, onRhythmChange: (Rhythm) -> Unit,
    plan: AlarmPlan, onPlanChange: (AlarmPlan) -> Unit,
    connected: Boolean, api: ShanivarApi, language: String, signIn: () -> Unit,
    onBusy: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheduler = remember { AlarmScheduler(context) }
    val permissions = remember { AndroidDisciplinePermissions(context) }
    var busy by remember { mutableStateOf(false) }
    SideEffect { onBusy(busy) }
    DisposableEffect(Unit) { onDispose { onBusy(false) } }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(!plan.enabled) }
    var permissionRevision by remember { mutableIntStateOf(0) }
    OnAppResume { permissionRevision++ }
    key(permissionRevision) { /* Recompose readiness after Android settings. */ }
    var days by remember(plan) { mutableStateOf(plan.days) }
    // Recomputed on every recomposition, not cached in state: this is the actual native
    // AlarmManager record, which is the "actual native scheduling" ground truth the task requires
    // showing separately from whatever the backend last confirmed.
    val nextTrigger = scheduler.pendingTriggerAtEpochMillis()

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionRevision++ }

    suspend fun scheduleNow() {
        if (busy) return
        if (days.isEmpty()) { error = "Choose at least one repeat day."; return }
        error = null; result = null
        if (!permissions.hasExactAlarmPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
            error = "Allow Alarms & reminders, then return and try again."
            return
        }
        if (!permissions.hasNotificationPermission()) {
            error = "This alarm cannot show its wake screen until notifications are allowed. Tap \"Allow notifications\" above, then try again."
            return
        }
        busy = true
        try {
            val routineApi = RoutineApi(api)
            routineApi.save(rhythm, language)
            val remote = if (plan.remoteId == null) {
                routineApi.createAlarm(rhythm.wakeTime, days, rhythm.movementDestination, rhythm.stepTarget, rhythm.accessibleMovement)
            } else {
                routineApi.editAlarm(plan.remoteId, rhythm.wakeTime, days, rhythm.movementDestination, rhythm.stepTarget, rhythm.accessibleMovement)
            }
            val triggerAt = scheduler.scheduleRule(remote.id, LocalTime.parse(rhythm.wakeTime), days)
            check(triggerAt != null) { "Android did not confirm the alarm." }
            onPlanChange(AlarmPlan(remote.id, days, true))
            val formatted = Instant.ofEpochMilli(triggerAt).atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))
            result = "Scheduled on this device · $formatted"
            // Backend confirmation is a sync step, not the thing that makes the alarm ring - the
            // native AlarmManager trigger above is already armed regardless of what happens here.
            runCatching { routineApi.confirmAlarm(remote.id, Instant.ofEpochMilli(triggerAt).toString()) }
                .onFailure { result += " · not yet confirmed with your account" }
            editing = false
        } catch (e: Exception) {
            error = e.message ?: "Could not schedule the alarm."
        } finally { busy = false }
    }

    SectionCard("Set alarm") {
        if (!connected) {
            Text("Sign in to set a wake-up alarm.", color = Gold)
            PrimaryAction("Sign in to schedule", signIn)
            return@SectionCard
        }
        if (!editing) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            result?.let { Text(it, color = Gold) }
            Text(rhythm.wakeTime, style = MaterialTheme.typography.titleLarge)
            Text(dayLabelsFor(days), color = Muted)
            Text(
                if (nextTrigger != null) "Next: ${Instant.ofEpochMilli(nextTrigger).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))}"
                else "Not scheduled on this device yet.",
                color = if (nextTrigger != null) Gold else MaterialTheme.colorScheme.error,
            )
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = plan.enabled && nextTrigger != null, enabled = !busy, onCheckedChange = { checked ->
                    if (checked) scope.launch { scheduleNow() }
                    else {
                        scheduler.cancel()
                        onPlanChange(plan.copy(enabled = false))
                        plan.remoteId?.let { id -> scope.launch { runCatching { RoutineApi(api).setAlarmEnabled(id, false) }.onFailure { error = "Stopped on this device. Account sync failed; reconnect before using another device." } } }
                    }
                })
                TextButton(onClick = { editing = true }) { Text("Edit") }
                TextButton(onClick = { AlarmSoundPreview.play(context) }) { Text("Test sound") }
                if (plan.remoteId != null) TextButton(onClick = {
                    scheduler.cancel()
                    scope.launch { runCatching { RoutineApi(api).deleteAlarm(plan.remoteId) }.onFailure { error = "Cancelled on this device. Could not remove the account record." } }
                    onPlanChange(AlarmPlan())
                    result = null
                }) { Text("Cancel alarm") }
            }
        } else {
            fun pickTime() {
                val time = LocalTime.parse(rhythm.wakeTime)
                TimePickerDialog(context, { _, h, m -> onRhythmChange(rhythm.copy(wakeTime = "%02d:%02d".format(h, m))) }, time.hour, time.minute, true).show()
            }
            OutlinedButton(onClick = { pickTime() }, modifier = Modifier.fillMaxWidth()) { Text("Time · ${rhythm.wakeTime}") }
            Text("Repeat on", color = Muted)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DAY_LABELS.forEachIndexed { index, label ->
                    FilterChip(selected = days.contains(index), onClick = {
                        days = if (days.contains(index)) days - index else days + index
                    }, label = { Text(label) })
                }
            }
            Text("Choose a safe destination cue. Steps do not verify arrival.", color = Muted)
            listOf("morning_light" to "Morning light", "kitchen" to "Kitchen", "washroom" to "Washroom", "custom" to "Another place").forEach { (value, label) ->
                FilterChip(selected = rhythm.movementDestination == value, onClick = { onRhythmChange(rhythm.copy(movementDestination = value)) }, label = { Text(label) })
            }
            Text("${rhythm.stepTarget} steps", fontSize = 20.sp)
            Slider(rhythm.stepTarget.toFloat(), { onRhythmChange(rhythm.copy(stepTarget = it.toInt())) }, valueRange = 8f..40f, steps = 7,
                modifier = Modifier.semantics { contentDescription = "Movement step target" })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Use non-walking alternative", Modifier.weight(1f))
                Switch(rhythm.accessibleMovement, { onRhythmChange(rhythm.copy(accessibleMovement = it)) })
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.hasNotificationPermission()) {
                OutlinedButton(onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow notifications")
                }
                Text("Without this, the alarm cannot show its wake screen on this phone.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            if (!permissions.hasExactAlarmPermission()) {
                Text("Alarms & reminders access is needed for this alarm to be exact.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            if (Build.VERSION.SDK_INT >= 34 &&
                !(context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).canUseFullScreenIntent()) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")))
                }, modifier = Modifier.fillMaxWidth()) { Text("Allow full-screen alarm") }
                Text("Without this access, you may need to tap the alarm notification.", color = Muted)
            }
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            if (audio.getStreamVolume(android.media.AudioManager.STREAM_ALARM) == 0)
                Text("Alarm volume is zero. Raise it in Android sound settings before relying on this alarm.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { AlarmSoundPreview.play(context) }, modifier = Modifier.fillMaxWidth()) { Text("Test sound") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            result?.let { Text(it, color = Gold, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { scope.launch { scheduleNow() } }, enabled = !busy && days.isNotEmpty()) {
                    Text(if (busy) "Scheduling…" else if (plan.remoteId == null) "Save & schedule" else "Update alarm")
                }
                if (plan.enabled) TextButton(onClick = { editing = false }, enabled = !busy) { Text("Cancel edit") }
            }
        }
    }
}

private val DAY_LABELS = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
private fun dayLabelsFor(days: Set<Int>): String =
    if (days.size == 7) "Every day" else DAY_LABELS.filterIndexed { i, _ -> days.contains(i) }.joinToString(", ")

// ─────────────────────────────────────────────────────────────────────────────────────────────
// Legacy four-tab shell (pre-streamline). Kept intact rather than deleted, per CLAUDE.md section 5
// ("keep tested code") - reachable only with BuildConfig.STREAMLINED_LAUNCH = false.
// ─────────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun LegacyTabShell(
    rhythm: Rhythm, onRhythmChange: (Rhythm) -> Unit, tab: String, onTab: (String) -> Unit,
    account: Account?, session: AccountState, discipline: DisciplineGateway, techniques: List<Technique>,
    sessionId: String?, onSessionId: (String?) -> Unit, reflections: ReflectionState, reflectionStore: EncryptedReflectionStore,
    onReflectionEntry: (ReflectionEntryPoint) -> Unit, onReflectionOpen: (Boolean) -> Unit, onAccountDialog: () -> Unit, language: String,
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    fun refreshNote() = session.refreshNote()
    LaunchedEffect(Unit) { while (true) { now = LocalTime.now(); date = LocalDate.now(); delay(30_000) } }
    LaunchedEffect(account?.id, date) { if (account != null) refreshNote() }
    val note = session.note
    val noteBusy = session.noteBusy
    val noteError = session.noteError
    Scaffold(containerColor = Void, bottomBar = {
        NavigationBar(containerColor = Void) {
            listOf("Today" to "01", "Focus" to "02", "Practice" to "03", "Rhythm" to "04").forEach { (name, number) ->
                NavigationBarItem(selected = tab == name, onClick = { onTab(name) },
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
                TextButton(onClick = onAccountDialog) { Text(if (account == null) "Sign in" else "Account") }
            }
            when (tab) {
                "Today" -> {
                    Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), color = Muted)
                    Heading(rhythm.moment(now).title)
                    Text(if (account == null) "A little structure. More room to live." else "Welcome, ${account!!.name}. Make this day your own.", color = Muted)
                    PrimaryAction(rhythm.moment(now).action) {
                        when (rhythm.moment(now)) {
                            DayMoment.MORNING, DayMoment.EVENING -> onSessionId(techniques.first().id)
                            DayMoment.FOCUS -> onTab("Focus")
                            DayMoment.NIGHT -> onTab("Rhythm")
                        }
                    }
                    NoteCard(note, noteBusy, noteError, account != null, date, onAccountDialog, { refreshNote() })
                    SectionCard("Your rhythm") {
                        RhythmLine("Rise", rhythm.wakeTime, "${rhythm.stepTarget} steps toward ${rhythm.movementDestination}")
                        HorizontalDivider()
                        RhythmLine("Protect", "${rhythm.focusMinutes} min", rhythm.intentionLabel)
                        HorizontalDivider()
                        RhythmLine("Unwind", rhythm.quietTime, "Make space before sleep")
                        TextButton(onClick = { onTab("Rhythm") }) { Text("Adjust your day") }
                    }
                }
                "Focus" -> LegacyFocusScreen(rhythm, account != null, discipline, signIn = onAccountDialog, edit = { onTab("Rhythm") })
                "Practice" -> {
                    Heading("Room to be.")
                    Text("Choose a quiet practice. Read at your pace, then sit with it.", color = Muted)
                    techniques.forEach { technique ->
                        SectionCard(technique.title) {
                            Text(technique.duration, color = Muted)
                            Text(technique.essence, maxLines = 3)
                            PrimaryAction("Open practice") { onSessionId(technique.id) }
                        }
                    }
                }
                "Rhythm" -> LegacyRhythmScreen(rhythm, account != null, session.api, language, signIn = onAccountDialog, onSave = onRhythmChange)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LegacyFocusScreen(rhythm: Rhythm, connected: Boolean, gateway: DisciplineGateway, signIn: () -> Unit, edit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<DisciplineStatus?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun refresh() {
        if (!connected) return
        busy = true; error = null
        scope.launch {
            try { status = gateway.status() }
            catch (e: Exception) { error = e.message ?: "Could not read protection status." }
            finally { busy = false }
        }
    }
    LaunchedEffect(connected) { refresh() }
    Heading("Less noise.\nMore of you.")
    SectionCard("Your focus window") {
        Text("${rhythm.focusMinutes} minutes", fontSize = 32.sp)
        when {
            !connected -> PrimaryAction("Sign in to protect this window", signIn)
            status?.protection == ProtectionState.ACTIVE -> {
                val minutes = (((status?.focusEndsAtEpochMillis ?: 0L) - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
                Text("Focus protected · $minutes min remaining", color = Gold)
                OutlinedButton(onClick = { scope.launch { status = gateway.releaseFocus() } }, modifier = Modifier.fillMaxWidth()) { Text("Release focus") }
            }
            status?.protection == ProtectionState.NEEDS_PERMISSION -> {
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Grant Usage access") }
                TextButton(onClick = { refresh() }) { Text("Check access again") }
            }
            else -> PrimaryAction(if (busy) "Checking…" else "Start protected focus") {
                busy = true; scope.launch {
                    try { status = gateway.startFocus(rhythm.focusMinutes, rhythm.blockedPackages) }
                    catch (e: Exception) { error = e.message ?: "Could not start protection." }
                    finally { busy = false }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = edit) { Text("Change focus duration") }
    }
}

@Composable
private fun LegacyRhythmScreen(initial: Rhythm, connected: Boolean, api: ShanivarApi, language: String, signIn: () -> Unit, onSave: (Rhythm) -> Unit) {
    val context = LocalContext.current
    var wake by rememberSaveable { mutableStateOf(initial.wakeTime) }
    var movement by rememberSaveable { mutableStateOf(initial.movementDestination) }
    var stepTarget by rememberSaveable { mutableStateOf(initial.stepTarget.toFloat()) }
    var accessibleMovement by rememberSaveable { mutableStateOf(initial.accessibleMovement) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scheduler = remember { AlarmScheduler(context) }
    val permissions = remember { AndroidDisciplinePermissions(context) }
    fun current() = initial.copy(wakeTime = wake, movementDestination = movement, stepTarget = stepTarget.toInt(), accessibleMovement = accessibleMovement)
    Heading("Built around\nyour day.")
    SectionCard("Rise with a reason") {
        OutlinedButton(onClick = {
            val time = LocalTime.parse(wake)
            TimePickerDialog(context, { _, h, m -> wake = "%02d:%02d".format(h, m) }, time.hour, time.minute, true).show()
        }, modifier = Modifier.fillMaxWidth()) { Text("Wake time · $wake") }
        listOf("morning_light" to "Morning light", "kitchen" to "Kitchen", "washroom" to "Washroom", "custom" to "Another place").forEach { (value, label) ->
            FilterChip(selected = movement == value, onClick = { movement = value }, label = { Text(label) })
        }
        Text("${stepTarget.toInt()} steps", fontSize = 24.sp)
        Slider(stepTarget, { stepTarget = it }, valueRange = 8f..40f, steps = 7)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Use non-walking alternative", Modifier.weight(1f)); Switch(accessibleMovement, { accessibleMovement = it })
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    result?.let { Text(it, color = Gold) }
    PrimaryAction(if (busy) "Saving…" else "Save my rhythm") {
        onSave(current()); result = "Saved on this device."
    }
    OutlinedButton(onClick = {
        if (!connected) { signIn(); return@OutlinedButton }
        val next = current(); onSave(next)
        if (!permissions.hasExactAlarmPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
            error = "Allow Alarms & reminders, then return and schedule again."
            return@OutlinedButton
        }
        busy = true; error = null; result = null
        scope.launch {
            try {
                val routineApi = RoutineApi(api)
                routineApi.save(next, language)
                val allDays = setOf(0, 1, 2, 3, 4, 5, 6)
                val remote = routineApi.createAlarm(wake, allDays, movement, stepTarget.toInt(), accessibleMovement)
                val triggerAt = scheduler.scheduleRule(remote.id, LocalTime.parse(wake), allDays)
                check(triggerAt != null) { "Android did not confirm the alarm." }
                routineApi.confirmAlarm(remote.id, Instant.ofEpochMilli(triggerAt).toString())
                result = "Alarm confirmed · ${Instant.ofEpochMilli(triggerAt).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))}"
            } catch (e: Exception) { error = e.message ?: "Could not schedule the alarm." }
            finally { busy = false }
        }
    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (connected) "Schedule movement alarm" else "Sign in to schedule") }
}

@Composable
fun SaturnRing(progress: Float = 0.72f) {
    Canvas(Modifier.fillMaxWidth().height(140.dp).clearAndSetSemantics { }) {
        val radius = size.height * .36f
        drawCircle(Gold.copy(alpha = .28f), radius, style = Stroke(2.dp.toPx()))
        drawArc(Gold, -90f, progress * 360, false, Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2), style = Stroke(3.dp.toPx()))
        drawOval(Gold.copy(alpha = .65f), Offset(center.x - radius * 1.7f, center.y - radius * .32f),
            Size(radius * 3.4f, radius * .64f), style = Stroke(1.dp.toPx()))
    }
}

@Composable
fun Heading(text: String) { Text(text, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() }) }

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(8.dp)) { Text(label) }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
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
internal fun AccountDialog(account: Account?, api: ShanivarApi, onClose: () -> Unit, onConnected: (Account) -> Unit, onLogout: () -> Unit, canLogout: Boolean = true, inDialog: Boolean = true) {
    if (inDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onClose,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            AccountDialog(account, api, onClose, onConnected, onLogout, canLogout, inDialog = false)
        }
        return
    }
    var creating by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Surface(shape = RoundedCornerShape(20.dp), color = Surface) {
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(account?.name ?: if (creating) "Create your account." else "Welcome back.", style = MaterialTheme.typography.headlineSmall)
                if (account != null) {
                    Text("Your plan and progress come from your Shanivar account.")
                    if (!canLogout) Text("End focus and turn off your alarm before signing out, so you can still manage them.")
                } else {
                    Text("Connect your Shanivar account to keep your plan and alarm across devices.")
                    if (creating) OutlinedTextField(displayName, { displayName = it }, label = { Text("Name") }, singleLine = true)
                    TextButton(onClick = { creating = !creating; error = null }, enabled = !busy) {
                        Text(if (creating) "I already have an account" else "Create an account")
                    }
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    error?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }

            if (account != null) TextButton(onClick = onLogout, enabled = canLogout) { Text("Sign out") }
            else TextButton(enabled = !busy && email.isNotBlank() && password.isNotBlank() && (!creating || displayName.isNotBlank()), onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        val result = if (creating) api.register(SignupDetails(displayName.trim(), email.trim(), password, "en"))
                            else api.login(email.trim(), password)
                        onConnected(result); password = ""
                    }
                    catch (e: Exception) { error = if (e is ApiFailure) e.message else "Could not sign in. Check your connection and try again." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Connecting…" else if (creating) "Create account" else "Sign in") }
            TextButton(onClick = onClose, enabled = !busy) { Text("Close") }
        }
    }
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
