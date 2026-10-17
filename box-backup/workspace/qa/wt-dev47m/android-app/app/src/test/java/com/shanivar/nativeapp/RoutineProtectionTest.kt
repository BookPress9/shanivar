package com.shanivar.nativeapp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Routine App protection: plan ⇄ registry sync, overlap rules, status honesty, editor helpers. */
class RoutineProtectionTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val insta = "com.instagram.android"
    private val yt = "com.google.android.youtube"

    /** 2026-09-14 is a Monday. */
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)
    private fun ms(t: ZonedDateTime) = t.toInstant().toEpochMilli()
    private fun t(s: String) = LocalTime.parse(s)
    private fun w(id: String, start: String, end: String, days: Set<Int> = (0..6).toSet(), enabled: Boolean = true) =
        ProtectionWindowV1(id, t(start), t(end), days, enabled)

    private val activity = RoutineActivityV1("gym", "Gym", t("07:30"), 60, (0..6).toSet())

    // ── Fakes ────────────────────────────────────────────────────────────────────────────────

    private class FakeRepo(initial: RoutinePlanV1 = RoutinePlanV1(), var healthy: Boolean = true) : RoutinePlanRepository {
        val state = MutableStateFlow(initial)
        var saves = 0
        override fun observe(): Flow<RoutinePlanV1> = state
        override fun current() = state.value
        override fun decodeHealthy() = healthy
        override suspend fun save(plan: RoutinePlanV1): Result<Unit> {
            val v = RoutinePlanRules.validate(plan)
            if (v is RoutinePlanValidation.Error) return Result.failure(IllegalArgumentException(v.message))
            saves++; healthy = true; state.value = plan
            return Result.success(Unit)
        }
    }

    private inner class FakeGateway(
        var registry: List<FocusScheduler.Rule> = emptyList(),
        var canArm: Boolean = true,
        var exact: Boolean = true,
        var usage: Boolean = true,
        var overlay: Boolean = true,
        var guard: LocalProtection? = null,
    ) : ProtectionGateway {
        var armed: FocusScheduler.ArmedBoundary? = null
        val syncs = mutableListOf<RoutinePlanV1>()
        var rearms = 0
        var stops = 0
        override fun registryRules() = registry
        override fun sync(plan: RoutinePlanV1, now: ZonedDateTime): ProtectionSyncResult {
            syncs += plan
            registry = RoutineProtectionSync.toRules(plan)
            armed = arm(now)
            return ProtectionSyncResult.Synced(armed, registry.count { it.enabled })
        }
        override fun rearm(now: ZonedDateTime): FocusScheduler.ArmedBoundary? { rearms++; armed = arm(now); return armed }
        private fun arm(now: ZonedDateTime): FocusScheduler.ArmedBoundary? {
            if (!canArm) return null
            return registry.filter { it.enabled }.mapNotNull { r ->
                FocusWindowRecurrence.nextBoundary(r.days, r.startTime, r.endTime, now)?.let { (z, a) ->
                    FocusScheduler.ArmedBoundary(r.scheduleId, a, ms(z))
                }
            }.minByOrNull { it.triggerAtEpochMillis }
        }
        override fun evidence() = ProtectionDeviceEvidence(registry, armed, exact, usage, overlay, guard)
        override fun stopNow() { stops++; guard = null }
    }

    // ── Sync mapping ─────────────────────────────────────────────────────────────────────────

    @Test fun everyWindowSharesThePlanPackageSet() {
        val plan = RoutinePlanV1(protectedPackages = setOf(insta, yt), windows = listOf(w("a", "09:00", "10:00"), w("b", "22:00", "06:00", enabled = false)))
        val rules = RoutineProtectionSync.toRules(plan)
        assertEquals(listOf("a", "b"), rules.map { it.scheduleId })
        assertTrue(rules.all { it.blockPackages == setOf(insta, yt) })
        assertEquals(listOf(true, false), rules.map { it.enabled })
        assertEquals(t("22:00"), rules[1].startTime)
    }

    @Test fun windowsWithoutAppsAreNeverArmed() {
        val plan = RoutinePlanV1(windows = listOf(w("a", "09:00", "10:00")))
        assertFalse(RoutineProtectionSync.toRules(plan).single().enabled)
    }

    @Test fun registryDiffIsOrderInsensitive() {
        val plan = RoutinePlanV1(protectedPackages = setOf(insta), windows = listOf(w("a", "09:00", "10:00"), w("b", "11:00", "12:00")))
        val reversed = RoutineProtectionSync.toRules(plan).reversed()
        assertFalse(RoutineProtectionSync.registryDiffers(reversed, plan))
        assertTrue(RoutineProtectionSync.registryDiffers(reversed.drop(1), plan))
    }

    @Test fun seedImportsAPreV3ScheduleLosslessly() {
        val legacy = FocusScheduler.Rule("srv-1", setOf(1, 2, 3, 4, 5), t("22:00"), t("06:00"), setOf(insta))
        val seeded = RoutineProtectionSync.seedFromRegistry(RoutinePlanV1(activities = listOf(activity)), listOf(legacy))!!
        assertEquals("srv-1", seeded.windows.single().id)
        assertEquals(setOf(1, 2, 3, 4, 5), seeded.windows.single().startDays)
        assertEquals(setOf(insta), seeded.protectedPackages)
        assertEquals(listOf(activity), seeded.activities)
        assertFalse(RoutineProtectionSync.registryDiffers(listOf(legacy), seeded))
        // Never overwrites a plan that already owns protection.
        assertNull(RoutineProtectionSync.seedFromRegistry(seeded, listOf(legacy.copy(scheduleId = "x"))))
    }

    // ── Overlap rules (exact, adjacent OK) ───────────────────────────────────────────────────

    private fun overlaps(a: ProtectionWindowV1, b: ProtectionWindowV1) = RoutinePlanRules.windowsOverlap(a, b)

    @Test fun adjacentIsAllowedAnySharedMinuteIsNot() {
        assertFalse(overlaps(w("a", "09:00", "10:00"), w("b", "10:00", "11:00")))
        assertTrue(overlaps(w("a", "09:00", "10:01"), w("b", "10:00", "11:00")))
        // Off-grid minutes that a 15-minute sampler would miss.
        assertTrue(overlaps(w("a", "09:05", "09:10"), w("b", "09:07", "09:20")))
    }

    @Test fun overnightOwnsItsStartDayAndSpillsIntoTheNext() {
        val monNight = w("n", "22:00", "06:00", setOf(1))
        assertTrue(overlaps(monNight, w("m", "05:00", "07:00", setOf(2))))
        assertFalse(overlaps(monNight, w("m", "06:00", "08:00", setOf(2))))
        assertFalse(overlaps(monNight, w("m", "05:00", "07:00", setOf(1)))) // Monday morning is before Monday night
        // Saturday night wraps into Sunday morning.
        assertTrue(overlaps(w("s", "22:00", "02:00", setOf(6)), w("u", "01:00", "03:00", setOf(0))))
    }

    @Test fun fullCalendarDayIsMidnightToMidnight() {
        val monday = w("d", "00:00", "00:00", setOf(1))
        assertTrue(overlaps(monday, w("x", "23:00", "01:00", setOf(1))))
        assertFalse(overlaps(monday, w("x", "00:00", "01:00", setOf(2))))
        assertFalse(overlaps(monday, w("x", "22:00", "00:00", setOf(0)))) // Sunday 22:00 → Monday 00:00, adjacent
        assertFalse(overlaps(monday, w("y", "00:00", "00:00", setOf(2))))
        assertEquals(24 * 60, ProtectionWindowGeometry.durationMinutes(t("00:00"), t("00:00")))
        assertEquals(0, ProtectionWindowGeometry.durationMinutes(t("09:00"), t("09:00")))
    }

    @Test fun disabledWindowsDoNotCountForOverlap() {
        val plan = RoutinePlanV1(protectedPackages = setOf(insta), windows = listOf(w("a", "09:00", "12:00"), w("b", "11:00", "13:00", enabled = false)))
        assertTrue(RoutinePlanRules.validate(plan) is RoutinePlanValidation.Ok)
        assertTrue(RoutinePlanRules.validate(plan.copy(windows = plan.windows.map { it.copy(enabled = true) })) is RoutinePlanValidation.Error)
    }

    // ── Saving syncs; activities never arm ───────────────────────────────────────────────────

    @Test fun activityOrAnchorEditsNeverTouchTheRegistry() = runBlocking {
        val base = RoutinePlanV1(protectedPackages = setOf(insta), windows = listOf(w("a", "09:00", "10:00")))
        val gateway = FakeGateway()
        val repo = ProtectionSyncingRoutinePlanRepository(FakeRepo(base), gateway) { at(14, 8) }
        assertTrue(repo.save(base.copy(activities = listOf(activity), wakeTime = t("06:00"))).isSuccess)
        assertTrue(gateway.syncs.isEmpty())
        assertNull(repo.lastSync)
    }

    @Test fun windowOrPackageChangesSyncTheWholeRegistry() = runBlocking {
        val gateway = FakeGateway()
        val repo = ProtectionSyncingRoutinePlanRepository(FakeRepo(), gateway) { at(14, 8) }
        val plan = RoutinePlanV1(protectedPackages = setOf(insta), windows = listOf(w("a", "09:00", "10:00"), w("b", "22:00", "06:00", setOf(1))))
        assertTrue(repo.save(plan).isSuccess)
        assertEquals(1, gateway.syncs.size)
        assertEquals(setOf("a", "b"), gateway.registry.map { it.scheduleId }.toSet())
        val synced = repo.lastSync as ProtectionSyncResult.Synced
        assertEquals("a", synced.armed!!.ruleId) // Monday 09:00 START is the earliest boundary after 08:00
        assertEquals(FocusWindowAction.START, synced.armed!!.action)

        assertTrue(repo.save(plan.copy(protectedPackages = setOf(insta, yt))).isSuccess)
        assertEquals(2, gateway.syncs.size)
        assertTrue(gateway.registry.all { it.blockPackages == setOf(insta, yt) })

        // Removing a window drops its rule.
        assertTrue(repo.save(plan.copy(windows = plan.windows.take(1))).isSuccess)
        assertEquals(listOf("a"), gateway.registry.map { it.scheduleId })
    }

    @Test fun invalidSaveDoesNotSync() = runBlocking {
        val gateway = FakeGateway()
        val repo = ProtectionSyncingRoutinePlanRepository(FakeRepo(), gateway)
        val bad = RoutinePlanV1(protectedPackages = setOf(insta), windows = listOf(w("a", "09:00", "12:00"), w("b", "11:00", "13:00")))
        assertTrue(repo.save(bad).isFailure)
        assertTrue(gateway.syncs.isEmpty())
    }

    @Test fun reconcileNeverWipesProtectionFromAnUnreadablePlan() = runBlocking {
        val legacy = FocusScheduler.Rule("srv-1", setOf(1), t("09:00"), t("10:00"), setOf(insta))
        val gateway = FakeGateway(registry = listOf(legacy))
        val repo = ProtectionSyncingRoutinePlanRepository(FakeRepo(healthy = false), gateway)
        assertEquals(ProtectionSyncingRoutinePlanRepository.ReconcileOutcome.SKIPPED_UNHEALTHY, repo.reconcile())
        assertEquals(listOf(legacy), gateway.registry)
    }

    @Test fun reconcileImportsThenSyncsThenRearms() = runBlocking {
        val legacy = FocusScheduler.Rule("srv-1", setOf(1), t("09:00"), t("10:00"), setOf(insta))
        val gateway = FakeGateway(registry = listOf(legacy))
        val store = FakeRepo()
        val repo = ProtectionSyncingRoutinePlanRepository(store, gateway) { at(14, 8) }
        assertEquals(ProtectionSyncingRoutinePlanRepository.ReconcileOutcome.IMPORTED, repo.reconcile())
        assertEquals("srv-1", store.current().windows.single().id)
        assertTrue(gateway.syncs.isEmpty())

        // Plan changed elsewhere (e.g. a prior sync failed): registry is brought in line.
        store.state.value = store.current().copy(windows = listOf(w("srv-1", "09:00", "11:00", setOf(1))))
        assertEquals(ProtectionSyncingRoutinePlanRepository.ReconcileOutcome.SYNCED, repo.reconcile())
        assertEquals(t("11:00"), gateway.registry.single().endTime)

        // Same rules, nothing armed (exact-alarm access just came back) → re-arm only.
        gateway.armed = null
        assertEquals(ProtectionSyncingRoutinePlanRepository.ReconcileOutcome.REARMED, repo.reconcile())
        assertEquals(1, gateway.rearms)
        assertEquals(ProtectionSyncingRoutinePlanRepository.ReconcileOutcome.UNCHANGED, repo.reconcile())
    }

    // ── Status honesty (DEV-3) ──────────────────────────────────────────────────────────────

    private val twoWindows = RoutinePlanV1(
        protectedPackages = setOf(insta),
        windows = listOf(w("work", "09:00", "17:00", WindowRepeat.WEEKDAY_DAYS), w("night", "22:00", "06:00")),
    )

    private suspend fun synced(gateway: FakeGateway, plan: RoutinePlanV1, now: ZonedDateTime) =
        ProtectionSyncingRoutinePlanRepository(FakeRepo(), gateway) { now }.also { it.save(plan) }

    @Test fun savedAndArmedIsReadyNotActiveWithEachWindowsOwnNextChange() = runBlocking {
        val now = at(14, 7)
        val gateway = FakeGateway()
        synced(gateway, twoWindows, now)
        val overview = RoutineProtectionStatus.resolve(twoWindows, gateway.evidence(), now)
        assertEquals(FocusRuleState.READY, overview.statusFor("work")!!.state)
        assertEquals(ms(at(14, 9)), overview.statusFor("work")!!.nextChangeEpochMillis)
        assertEquals(FocusRuleState.READY, overview.statusFor("night")!!.state)
        assertEquals(ms(at(14, 22)), overview.statusFor("night")!!.nextChangeEpochMillis)
        assertEquals(FocusRuleState.READY, overview.summary.state)
        assertEquals(ms(at(14, 9)), overview.summary.nextChangeEpochMillis)
        assertTrue(overview.windows.none { it.status.state == FocusRuleState.ACTIVE })
    }

    @Test fun savingInsideAnOpenWindowIsNeedsAttentionNeverActive() = runBlocking {
        val now = at(14, 10)
        val gateway = FakeGateway()
        synced(gateway, twoWindows, now)
        val status = RoutineProtectionStatus.resolve(twoWindows, gateway.evidence(), now).statusFor("work")!!
        assertNotEquals(FocusRuleState.ACTIVE, status.state)
        assertEquals(FocusScheduleStatus.Problem.WINDOW_OPEN_NOT_PROTECTING, status.problem)
    }

    @Test fun activeOnlyWithAGuardCoveringTheSharedApps() = runBlocking {
        val now = at(14, 10)
        val gateway = FakeGateway()
        synced(gateway, twoWindows, now)
        gateway.guard = LocalProtection(setOf(insta), ms(at(14, 17)))
        val overview = RoutineProtectionStatus.resolve(twoWindows, gateway.evidence(), now)
        assertEquals(FocusRuleState.ACTIVE, overview.statusFor("work")!!.state)
        assertEquals(FocusRuleState.READY, overview.statusFor("night")!!.state)
        assertEquals(FocusRuleState.ACTIVE, overview.summary.state)
        assertNotNull(overview.protectionRunning)
    }

    @Test fun missingAccessAndUnarmedAreNamedNotReady() = runBlocking {
        val now = at(14, 7)
        val gateway = FakeGateway(canArm = false, exact = false, overlay = false)
        synced(gateway, twoWindows, now)
        val overview = RoutineProtectionStatus.resolve(twoWindows, gateway.evidence(), now)
        assertEquals(FocusRuleState.NEEDS_DEVICE_ACCESS, overview.summary.state)
        assertEquals(setOf(FocusDeviceAccess.EXACT_ALARM, FocusDeviceAccess.OVERLAY), overview.missingAccess)

        gateway.exact = true; gateway.overlay = true
        val unarmed = RoutineProtectionStatus.resolve(twoWindows, gateway.evidence(), now)
        assertEquals(FocusRuleState.SAVED_LOCAL, unarmed.summary.state)
    }

    @Test fun unsyncedWindowIsSavedNotReady() = runBlocking {
        val now = at(14, 7)
        val gateway = FakeGateway()
        synced(gateway, twoWindows, now)
        val edited = twoWindows.copy(windows = twoWindows.windows + w("late", "18:00", "19:00"))
        val overview = RoutineProtectionStatus.resolve(edited, gateway.evidence(), now)
        assertEquals(FocusRuleState.SAVED_LOCAL, overview.statusFor("late")!!.state)
        assertEquals(FocusRuleState.SAVED_LOCAL, overview.summary.state)
    }

    @Test fun missedGlobalBoundaryFlagsEveryEnabledWindow() = runBlocking {
        val gateway = FakeGateway()
        synced(gateway, twoWindows, at(14, 7))
        // The armed 09:00 START was never delivered/advanced; at 09:30 that is past the grace period.
        val later = at(14, 9, 30)
        val overview = RoutineProtectionStatus.resolve(twoWindows, gateway.evidence(), later)
        assertEquals(FocusScheduleStatus.Problem.MISSED_BOUNDARY, overview.statusFor("night")!!.problem)
    }

    @Test fun disabledIsOffAndNoAppsNeedsAttention() {
        val now = at(14, 7)
        val gateway = FakeGateway()
        val off = twoWindows.copy(windows = twoWindows.windows.map { it.copy(enabled = false) })
        assertEquals(FocusRuleState.OFF, RoutineProtectionStatus.resolve(off, gateway.evidence(), now).summary.state)
        val noApps = twoWindows.copy(protectedPackages = emptySet())
        val o = RoutineProtectionStatus.resolve(noApps, gateway.evidence(), now)
        assertTrue(o.needsApps)
        assertEquals(FocusRuleState.NEEDS_ATTENTION, o.summary.state)
        assertEquals(FocusRuleState.DRAFT, RoutineProtectionStatus.resolve(RoutinePlanV1(), gateway.evidence(), now).summary.state)
    }

    @Test fun saveMessagesNeverClaimActive() {
        val now = at(14, 7)
        val overview = RoutineProtectionStatus.resolve(twoWindows, FakeGateway().evidence(), now)
        val messages = listOf(
            RoutineProtectionController.saveMessage(ProtectionSyncResult.Synced(FocusScheduler.ArmedBoundary("work", FocusWindowAction.START, 1L), 2), overview),
            RoutineProtectionController.saveMessage(ProtectionSyncResult.Synced(null, 2), overview),
            RoutineProtectionController.saveMessage(ProtectionSyncResult.Synced(null, 0), overview),
            RoutineProtectionController.saveMessage(ProtectionSyncResult.Rejected("nope"), overview),
            RoutineProtectionController.saveMessage(null, overview),
        )
        messages.forEach { assertFalse(it, it.contains("Active") || it.contains("protecting")) }
    }

    @Test fun stopIsLocalFirstAndSurvivesMirrorFailure() = runBlocking {
        val gateway = FakeGateway(guard = LocalProtection(setOf(insta), ms(at(14, 17))))
        val controller = RoutineProtectionController(
            ProtectionSyncingRoutinePlanRepository(FakeRepo(), gateway), gateway,
            mirrorRelease = { throw IllegalStateException("offline") },
        )
        controller.stopNow()
        assertEquals(1, gateway.stops)
        assertNull(gateway.guard)
    }

    @Test fun controllerSaveRejectsOverlapWithoutTouchingDevice() = runBlocking {
        val gateway = FakeGateway()
        val controller = RoutineProtectionController(ProtectionSyncingRoutinePlanRepository(FakeRepo(), gateway), gateway) { at(14, 7) }
        val editor = ProtectionEditorState(listOf(w("a", "09:00", "12:00"), w("b", "11:00", "13:00")), setOf(insta))
        assertTrue(controller.save(editor).isFailure)
        assertTrue(gateway.syncs.isEmpty())
        val ok = controller.save(editor.remove("b"))
        assertTrue(ok.isSuccess)
        assertEquals(1, gateway.syncs.size)
    }

    // ── Editor helpers ───────────────────────────────────────────────────────────────────────

    @Test fun addRespectsMaxEightAndAvoidsOverlap() {
        var e = ProtectionEditorState(emptyList(), setOf(insta))
        repeat(8) { i -> e = e.add("w$i") }
        assertEquals(8, e.windows.size)
        assertFalse(e.canAdd)
        assertNull(e.validate())
        val ninth = e.add("w9")
        assertEquals(8, ninth.windows.size)
        assertNotNull(ninth.error)
        assertTrue(e.windows.all { it.enabled })
    }

    @Test fun addWhenEveryDayIsFullCreatesAnOffWindow() {
        val e = ProtectionEditorState(listOf(w("all", "00:00", "00:00")), setOf(insta)).add("new")
        assertFalse(e.windows.last().enabled)
        assertNull(e.validate())
    }

    @Test fun validateNamesTheTwoOverlappingWindows() {
        val e = ProtectionEditorState(listOf(w("a", "09:00", "10:00"), w("b", "22:00", "06:00"), w("c", "05:00", "07:00")), setOf(insta))
        val message = e.validate()!!
        assertTrue(message, message.contains("Window 2") && message.contains("window 3"))
        assertTrue(message.contains("next day"))
    }

    @Test fun validateShapesAndApps() {
        val base = ProtectionEditorState(listOf(w("a", "09:00", "10:00")), setOf(insta))
        assertNull(base.validate())
        assertNull(base.setStart("a", t("22:00")).setEnd("a", t("06:00")).validate()) // overnight
        assertNull(base.setFullCalendarDay("a").validate())
        assertNotNull(base.setEnd("a", t("09:00")).validate())
        assertNotNull(base.update("a") { it.copy(startDays = emptySet()) }.validate())
        assertEquals("Choose at least one app to protect your attention from.", base.togglePackage(insta).validate())
        assertNull(base.togglePackage(insta).allOff().validate()) // all off with no apps is savable
    }

    @Test fun repeatPresetsAndCustomDays() {
        val e = ProtectionEditorState(listOf(w("a", "09:00", "10:00", setOf(1, 3))), setOf(insta))
        assertEquals(WindowRepeat.CUSTOM, WindowRepeat.kindOf(e.windows[0].startDays))
        assertEquals(WindowRepeat.WEEKDAY_DAYS, e.setRepeat("a", WindowRepeat.WEEKDAYS).windows[0].startDays)
        assertEquals(setOf(0, 6), e.setRepeat("a", WindowRepeat.WEEKENDS).windows[0].startDays)
        assertEquals(setOf(1, 3), e.setRepeat("a", WindowRepeat.CUSTOM).windows[0].startDays)
        assertEquals(setOf(1, 3, 5), e.toggleDay("a", 5).windows[0].startDays)
        assertEquals("Mon, Wed, Sun", WindowRepeat.label(setOf(0, 1, 3)))
    }

    @Test fun applyToKeepsAnchorsAndActivities() {
        val plan = RoutinePlanV1(wakeTime = t("06:00"), activities = listOf(activity))
        val e = ProtectionEditorState.from(plan).add("x").togglePackage(insta)
        val next = e.applyTo(plan)
        assertEquals(listOf(activity), next.activities)
        assertEquals(t("06:00"), next.wakeTime)
        assertEquals(setOf(insta), next.protectedPackages)
        assertTrue(e.isDirtyAgainst(plan))
        assertFalse(e.isDirtyAgainst(next))
    }

    @Test fun catalogKeepsAGoneSelectedAppVisibleAsUnavailable() {
        val options = ProtectionAppCatalog.options(setOf("com.example.gone")) { it == insta }
        assertTrue(options.first { it.packageName == insta }.installed)
        assertFalse(options.first { it.packageName == yt }.installed)
        assertFalse(options.first { it.packageName == "com.example.gone" }.installed)
    }

    @Test fun timelineUsesPerWindowStatus() {
        val now = at(14, 7)
        val items = RoutineSnapshotComposer.compose(
            RoutineSnapshot(plan = twoWindows, protectionStatusByWindowId = mapOf("work" to "Ready", "night" to "Needs device access"), now = now),
        ).filter { it.kind == TimelineKind.PROTECTION }
        assertEquals(listOf("Ready", "Needs device access"), items.map { it.deviceStatus })
        val allDay = RoutineSnapshotComposer.compose(RoutineSnapshot(plan = twoWindows.copy(windows = listOf(w("d", "00:00", "00:00"))), now = now))
        assertEquals("All day", allDay.first { it.kind == TimelineKind.PROTECTION }.timeLabel)
    }
}
