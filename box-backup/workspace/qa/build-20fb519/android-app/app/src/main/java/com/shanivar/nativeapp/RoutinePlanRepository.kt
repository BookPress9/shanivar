package com.shanivar.nativeapp

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime

/**
 * Local, schema-versioned Routine plan store. SharedPreferences JSON — no Room for this small config.
 *
 * Migrate wake/sleep once from [BreathingPreferences] (and Rhythm wake as a last resort) so there is
 * a single editable owner going forward. Saving activities does not arm alarms or Focus.
 */
interface RoutinePlanRepository {
    fun observe(): Flow<RoutinePlanV1>
    fun current(): RoutinePlanV1
    /** False when the stored blob could not be decoded — UI must show Needs attention, not Ready. */
    fun decodeHealthy(): Boolean
    suspend fun save(plan: RoutinePlanV1): Result<Unit>
}

class SharedPrefsRoutinePlanRepository(
    context: Context,
    /** DEV-38: every successful save queues the plan for sync. Null = local only. */
    private val outbox: SyncOutbox? = AppSyncOutbox.get(context),
) : RoutinePlanRepository {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(RoutinePlanV1())
    private var healthy = true

    init {
        val loaded = load()
        state.value = loaded
        // One-shot migration of anchors from older owners when the plan has none yet.
        if (loaded.wakeTime == null && loaded.intendedSleepTime == null) {
            migrateAnchorsIfNeeded()
        }
    }

    override fun observe(): Flow<RoutinePlanV1> = state.asStateFlow()
    override fun current(): RoutinePlanV1 = state.value
    override fun decodeHealthy(): Boolean = healthy

    override suspend fun save(plan: RoutinePlanV1): Result<Unit> {
        when (val result = RoutinePlanRules.validate(plan)) {
            is RoutinePlanValidation.Error -> return Result.failure(IllegalArgumentException(result.message))
            RoutinePlanValidation.Ok -> Unit
        }
        val json = RoutinePlanCodec.encode(plan)
        prefs.edit().putInt(KEY_VERSION, RoutinePlanV1.SCHEMA_VERSION).putString(KEY_PLAN, json).apply()
        healthy = true
        state.value = plan
        outbox?.enqueue("routine", "current", SyncPayloads.routine(plan))
        // Keep breathing schedule readers coherent until they also switch to RoutinePlan.
        mirrorAnchorsToBreathing(plan)
        return Result.success(Unit)
    }

    private fun load(): RoutinePlanV1 {
        val raw = prefs.getString(KEY_PLAN, null) ?: return RoutinePlanV1()
        val decoded = RoutinePlanCodec.decode(raw)
        if (decoded == null) {
            healthy = false
            return RoutinePlanV1()
        }
        healthy = true
        return decoded
    }

    private fun migrateAnchorsIfNeeded() {
        val breathing = BreathingPreferences(app)
        val wake = breathing.wakeTime?.let { parseTime(it) }
            ?: parseTime(app.getSharedPreferences("rhythm", 0).getString("wakeTime", null))
        val sleep = breathing.intendedSleepTime?.let { parseTime(it) }
        if (wake == null && sleep == null) return
        val migrated = state.value.copy(wakeTime = wake, intendedSleepTime = sleep)
        // Persist without failing on equal times from legacy data — flag via healthy if invalid.
        when (RoutinePlanRules.validate(migrated)) {
            is RoutinePlanValidation.Error -> {
                // Keep the values visible but mark unhealthy so UI does not claim Ready.
                prefs.edit().putInt(KEY_VERSION, RoutinePlanV1.SCHEMA_VERSION)
                    .putString(KEY_PLAN, RoutinePlanCodec.encode(migrated)).apply()
                healthy = false
                state.value = migrated
            }
            RoutinePlanValidation.Ok -> {
                prefs.edit().putInt(KEY_VERSION, RoutinePlanV1.SCHEMA_VERSION)
                    .putString(KEY_PLAN, RoutinePlanCodec.encode(migrated)).apply()
                healthy = true
                state.value = migrated
            }
        }
    }

    private fun mirrorAnchorsToBreathing(plan: RoutinePlanV1) {
        val breathing = BreathingPreferences(app)
        breathing.wakeTime = plan.wakeTime?.toString()?.take(5)
        breathing.intendedSleepTime = plan.intendedSleepTime?.toString()?.take(5)
    }

    private fun parseTime(raw: String?): LocalTime? =
        raw?.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it.take(5)) }.getOrNull() }

    companion object {
        private const val PREFS = "routine_plan_v1"
        private const val KEY_VERSION = "schema_version"
        private const val KEY_PLAN = "plan_json"
    }
}

object RoutinePlanCodec {
    fun encode(plan: RoutinePlanV1): String {
        val root = JSONObject()
            .put("schemaVersion", RoutinePlanV1.SCHEMA_VERSION)
            .put("protectedPackages", JSONArray(plan.protectedPackages.sorted()))
        if (plan.wakeTime != null) root.put("wakeTime", plan.wakeTime.toString()) else root.put("wakeTime", JSONObject.NULL)
        if (plan.intendedSleepTime != null) root.put("intendedSleepTime", plan.intendedSleepTime.toString()) else root.put("intendedSleepTime", JSONObject.NULL)
        val activities = JSONArray()
        plan.activities.forEach { a ->
            activities.put(
                JSONObject()
                    .put("id", a.id)
                    .put("title", a.title)
                    .put("startTime", a.startTime.toString())
                    .put("durationMinutes", a.durationMinutes)
                    .put("days", JSONArray(a.days.sorted()))
                    .put("enabled", a.enabled),
            )
        }
        root.put("activities", activities)
        val windows = JSONArray()
        plan.windows.forEach { w ->
            windows.put(
                JSONObject()
                    .put("id", w.id)
                    .put("startTime", w.startTime.toString())
                    .put("endTime", w.endTime.toString())
                    .put("startDays", JSONArray(w.startDays.sorted()))
                    .put("enabled", w.enabled),
            )
        }
        root.put("windows", windows)
        return root.toString()
    }

    fun decode(raw: String): RoutinePlanV1? = runCatching {
        val root = JSONObject(raw)
        val version = root.optInt("schemaVersion", 0)
        if (version != RoutinePlanV1.SCHEMA_VERSION) return null
        val activitiesJson = root.optJSONArray("activities") ?: JSONArray()
        val activities = (0 until activitiesJson.length()).map { i ->
            val o = activitiesJson.getJSONObject(i)
            RoutineActivityV1(
                id = o.getString("id"),
                title = o.getString("title"),
                startTime = LocalTime.parse(o.getString("startTime")),
                durationMinutes = o.getInt("durationMinutes"),
                days = daysFrom(o.getJSONArray("days")),
                enabled = o.optBoolean("enabled", true),
            )
        }
        val windowsJson = root.optJSONArray("windows") ?: JSONArray()
        val windows = (0 until windowsJson.length()).map { i ->
            val o = windowsJson.getJSONObject(i)
            ProtectionWindowV1(
                id = o.getString("id"),
                startTime = LocalTime.parse(o.getString("startTime")),
                endTime = LocalTime.parse(o.getString("endTime")),
                startDays = daysFrom(o.getJSONArray("startDays")),
                enabled = o.optBoolean("enabled", true),
            )
        }
        val packagesJson = root.optJSONArray("protectedPackages") ?: JSONArray()
        val packages = (0 until packagesJson.length()).mapNotNull { packagesJson.optString(it).takeIf { s -> s.isNotBlank() } }.toSet()
        RoutinePlanV1(
            wakeTime = optLocalTime(root, "wakeTime"),
            intendedSleepTime = optLocalTime(root, "intendedSleepTime"),
            activities = activities,
            protectedPackages = packages,
            windows = windows,
        )
    }.getOrNull()

    private fun optLocalTime(root: JSONObject, key: String): LocalTime? {
        if (!root.has(key) || root.isNull(key)) return null
        val raw = root.optString(key, "")
        if (raw.isBlank() || raw == "null") return null
        return LocalTime.parse(raw)
    }

    private fun daysFrom(array: JSONArray): Set<Int> =
        (0 until array.length()).mapNotNull { array.optInt(it).takeIf { d -> d in 0..6 } }.toSet()
}
