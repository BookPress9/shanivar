package com.shanivar.nativeapp

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Android side of the Initiation: real repositories, engines and permission reads. */
class AndroidInitiationPorts(context: Context) : InitiationPorts {
    private val app = context.applicationContext
    private val gateway = AndroidProtectionGateway(app)
    private val repo = ProtectionSyncingRoutinePlanRepository(SharedPrefsRoutinePlanRepository(app), gateway)
    private val permissions = AndroidDisciplinePermissions(app)

    override suspend fun saveRoutine(plan: RoutinePlanV1) = repo.save(plan)
    override fun currentPlan() = repo.current()
    override fun scheduleWakeAlarm(wake: LocalTime, days: Set<Int>): InitiationAlarm {
        if (!permissions.hasExactAlarmPermission() || !permissions.hasNotificationPermission()) return InitiationAlarm.PERMISSION_REQUIRED
        val trigger = AlarmScheduler(app).scheduleRule(RoutineOnboardingMachine.LOCAL_WAKE_ALARM_ID, wake, days)
        return if (trigger == null) InitiationAlarm.FAILED else InitiationAlarm.SCHEDULED
    }
    override fun cancelWakeAlarm() { /* "I'll use my existing alarm": leave any alarm the person already has untouched. */ }
    override fun access() = AccessResume.Access(permissions.hasUsageAccess(), permissions.hasOverlayPermission())
    override fun protectionState(): FocusRuleState? = runCatching {
        RoutineProtectionStatus.resolve(repo.current(), gateway.evidence(), ZonedDateTime.now()).windows
            .firstOrNull { it.windowId == "initiation-first" }?.status?.state
    }.getOrNull()
    override fun stopProtectionNow() = gateway.stopNow()
    override fun installedApps(): List<ProtectionAppOption> {
        val pm = app.packageManager
        return ProtectionAppCatalog.options(emptySet()) { pkg -> runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess }.filter { it.installed }
    }
    override fun today(): LocalDate = LocalDate.now()
    override fun wallNow() = System.currentTimeMillis()
    override fun mono() = SystemClock.elapsedRealtime()
}

fun initiationReduceMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
fun InitiationApp(
    controllerFactory: (Context) -> InitiationController = { ctx ->
        val dir = File(ctx.filesDir, "initiation")
        InitiationController(OnboardingDraftRepository(FileDraftStore(File(dir, "draft.properties"))), FileDraftStore(File(dir, "ui.properties")), AndroidInitiationPorts(ctx))
    },
    reduceMotionOverride: Boolean? = null,
    sadhanaV1: Boolean = BuildConfig.SADHANA_V1,
) {
    val context = LocalContext.current
    val c = remember { controllerFactory(context) }
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    var access by remember { mutableStateOf(AccessResume.Access(false, false)) }
    val apps = remember { c.installedApps() }
    val reduceMotion = reduceMotionOverride ?: remember { initiationReduceMotion(context) }
    val run: IniRun = { block -> scope.launch { block(); version++ } }
    val pick: IniPickTime = { initial, onPicked -> TimePickerDialog(context, { _, h, m -> onPicked(LocalTime.of(h, m)); version++ }, initial.hour, initial.minute, true).show() }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> { access = runCatching { c.readAccess() }.getOrDefault(access); version++ } // re-read on return from Settings
                Lifecycle.Event.ON_STOP -> { c.pausePractice(); version++ } // backgrounding pauses the timer
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val screen = c.ui.screen
    // Sadhana V1 (0017): after the first-challenge practice, the Sadhana destination is the sadhana-v1 experience.
    if (sadhanaV1 && screen == InitiationScreen.SADHANA && c.orbitView().practicePoint) {
        SadhanaApp(reduceMotionOverride = reduceMotionOverride, onRoutine = { run { c.go(InitiationScreen.ROUTINE) } })
        return
    }
    Column(Modifier.fillMaxSize().background(Ini.Ink).systemBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 25.dp)) {
            if (screen.inOnboarding) IniProgress(screen.ordinal) { run { c.back() } }
            when (screen) {
                InitiationScreen.ARRIVAL -> ArrivalScreen(c, reduceMotion, run)
                InitiationScreen.IDENTITY -> IdentityScreen(c, run)
                InitiationScreen.AUTH -> AuthScreen(c, run)
                InitiationScreen.PROPOSITION -> PropositionScreen(c, reduceMotion, run)
                InitiationScreen.ANCHORS -> AnchorsScreen(c, run, pick)
                InitiationScreen.ALARM -> AlarmScreen(c, run)
                InitiationScreen.APPS -> AppsScreen(c, apps, run)
                InitiationScreen.WINDOW -> WindowScreen(c, apps, run, pick)
                InitiationScreen.ACCESS -> AccessScreen(c, access, run,
                    openUsage = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    openOverlay = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) })
                InitiationScreen.FOUNDATION -> FoundationScreen(c, run)
                InitiationScreen.ROUTINE -> RoutineHomeScreen(c, run)
                InitiationScreen.EDIT -> EditRoutineScreen(c, run, pick)
                InitiationScreen.SADHANA -> SadhanaEntryScreen(c, reduceMotion, run)
                InitiationScreen.PRACTICE -> PracticeScreen(c, version, run)
                InitiationScreen.RETURNED -> ReturnedScreen(c, reduceMotion, run)
                InitiationScreen.ORBIT -> OrbitScreen(c, reduceMotion, run)
                InitiationScreen.WEEK -> WeekScreen(c, run)
            }
            c.ui.message?.let { Box(Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) { IniNotice(it, alert = true) } }
        }
        if (screen.hasTabs) {
            HorizontalDivider(color = Ini.Line)
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                val sadhanaActive = screen in setOf(InitiationScreen.SADHANA, InitiationScreen.PRACTICE, InitiationScreen.RETURNED)
                TextButton(onClick = { run { c.go(InitiationScreen.ROUTINE) } }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("◷ Routine", fontSize = 12.sp, color = if (screen == InitiationScreen.ROUTINE) Ini.Bronze else Ini.Muted)
                }
                TextButton(onClick = { run { c.go(InitiationScreen.SADHANA) } }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("◌ Sadhana", fontSize = 12.sp, color = if (sadhanaActive) Ini.Bronze else Ini.Muted)
                }
            }
        }
    }
}
