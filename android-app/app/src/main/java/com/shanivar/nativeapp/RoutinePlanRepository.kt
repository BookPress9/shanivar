package com.shanivar.nativeapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * DEV-11 R1 local store for [RoutinePlanV1].
 *
 * Reads the existing rhythm preference keys `wake` and `quiet` at most once, and only when no
 * plan is stored yet. The copied anchors are then this store's own data. Rhythm preferences are
 * never written. A second [load] does not read them again.
 *
 * This class only reads and writes its own preference file. It does not schedule, start, or
 * release anything else. Stored activities are not armed.
 */
class RoutinePlanRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): RoutineSnapshot {
        val stored = prefs.getString(KEY_PLAN, null)
        if (stored != null) {
            decode(stored)?.let { plan ->
                return snapshot(plan, prefs.getBoolean(KEY_MIGRATED, false))
            }
            // Unreadable payload stays on disk. Do not replace it and do not migrate over it.
            return snapshot(RoutinePlanV1(), migratedFromRhythm = false)
        }
        val migrated = migrateFromRhythm()
        if (migrated != null) {
            persist(migrated, migratedFromRhythm = true)
            return snapshot(migrated, migratedFromRhythm = true)
        }
        return snapshot(RoutinePlanV1(), migratedFromRhythm = false)
    }

    fun save(plan: RoutinePlanV1): RoutineSnapshot {
        val migrated = prefs.getBoolean(KEY_MIGRATED, false)
        persist(plan, migrated)
        return snapshot(plan, migrated)
    }

    private fun snapshot(plan: RoutinePlanV1, migratedFromRhythm: Boolean) =
        RoutineSnapshot(plan = plan, migratedFromRhythm = migratedFromRhythm)

    /**
     * Copies rhythm `wake` into [RoutinePlanV1.wakeTime] and rhythm `quiet` into
     * [RoutinePlanV1.sleepTime] when those keys are actually present. Missing keys stay null
     * rather than picking up rhythm defaults. Returns null when neither anchor exists.
     */
    private fun migrateFromRhythm(): RoutinePlanV1? {
        val rhythm = appContext.getSharedPreferences(RHYTHM_PREFS, Context.MODE_PRIVATE)
        val hasWake = rhythm.contains(RHYTHM_WAKE)
        val hasQuiet = rhythm.contains(RHYTHM_QUIET)
        if (!hasWake && !hasQuiet) return null
        return RoutinePlanV1(
            wakeTime = if (hasWake) rhythm.getString(RHYTHM_WAKE, null) else null,
            sleepTime = if (hasQuiet) rhythm.getString(RHYTHM_QUIET, null) else null,
            activities = emptyList(),
        )
    }

    private fun persist(plan: RoutinePlanV1, migratedFromRhythm: Boolean) {
        prefs.edit()
            .putInt(KEY_SCHEMA, SCHEMA_VERSION)
            .putBoolean(KEY_MIGRATED, migratedFromRhythm)
            .putString(KEY_PLAN, encode(plan))
            .commit()
    }

    companion object {
        const val PREFS_NAME = "routine_plan_v1"
        private const val SCHEMA_VERSION = 1
        private const val KEY_SCHEMA = "schema"
        private const val KEY_MIGRATED = "migrated_from_rhythm"
        private const val KEY_PLAN = "plan"

        private const val RHYTHM_PREFS = "rhythm"
        private const val RHYTHM_WAKE = "wake"
        private const val RHYTHM_QUIET = "quiet"

        internal fun encode(plan: RoutinePlanV1): String {
            val activities = JSONArray()
            plan.activities.forEach { activity ->
                val days = JSONArray()
                activity.recurrence.days.sorted().forEach { days.put(it) }
                activities.put(
                    JSONObject()
                        .put("id", activity.id)
                        .put("title", activity.title)
                        .put("time", activity.time)
                        .put("durationMinutes", activity.durationMinutes)
                        .put(
                            "recurrence",
                            JSONObject()
                                .put("kind", activity.recurrence.kind.name)
                                .put("days", days),
                        ),
                )
            }
            return JSONObject()
                .put("wakeTime", plan.wakeTime ?: JSONObject.NULL)
                .put("sleepTime", plan.sleepTime ?: JSONObject.NULL)
                .put("activities", activities)
                .toString()
        }

        internal fun decode(raw: String): RoutinePlanV1? = runCatching {
            val json = JSONObject(raw)
            val activitiesJson = json.optJSONArray("activities") ?: JSONArray()
            val activities = buildList {
                for (i in 0 until activitiesJson.length()) {
                    val item = activitiesJson.getJSONObject(i)
                    val recurrence = item.getJSONObject("recurrence")
                    val daysJson = recurrence.optJSONArray("days") ?: JSONArray()
                    val days = buildSet {
                        for (d in 0 until daysJson.length()) add(daysJson.getInt(d))
                    }
                    add(
                        RoutineActivityV1(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            time = item.getString("time"),
                            durationMinutes = item.getInt("durationMinutes"),
                            recurrence = RoutineRecurrenceV1(
                                kind = RoutineRecurrenceKind.valueOf(recurrence.getString("kind")),
                                days = days,
                            ),
                        ),
                    )
                }
            }
            RoutinePlanV1(
                wakeTime = json.nullableString("wakeTime"),
                sleepTime = json.nullableString("sleepTime"),
                activities = activities,
            )
        }.getOrNull()

        private fun JSONObject.nullableString(key: String): String? =
            if (!has(key) || isNull(key)) null else getString(key)
    }
}
