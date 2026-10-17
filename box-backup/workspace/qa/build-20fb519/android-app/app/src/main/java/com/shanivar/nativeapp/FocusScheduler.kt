package com.shanivar.nativeapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Persists and arms recurring focus/app-lock windows.
 *
 * R2 / decision 0014: a bounded registry of up to [MAX_RULES] rules (default 8), sharing one
 * package set at the plan layer. Arms one deterministic global next-boundary alarm that names the
 * expected rule id, action and instant. Overlapping enabled windows are rejected; adjacent is OK.
 *
 * Legacy single-rule SharedPreferences keys are migrated into the registry on first read so
 * existing installs keep their first schedule (FOCUS schedule card / boot catch-up).
 *
 * END handling stays multi-rule safe via [FocusWindowEndHandler] — the receiver must pass
 * [rules] (all enabled) on every END.
 */
class FocusScheduler(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class Rule(
        val scheduleId: String,
        val days: Set<Int>,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val blockPackages: Set<String>,
        val enabled: Boolean = true,
    )

    data class ArmedBoundary(
        val ruleId: String,
        val action: FocusWindowAction,
        val triggerAtEpochMillis: Long,
    )

    /** All persisted rules (enabled and disabled), after legacy migration. */
    fun rules(): List<Rule> {
        migrateLegacyIfNeeded()
        val raw = prefs.getString(KEY_RULES_JSON, null) ?: return emptyList()
        return decodeRules(raw)
    }

    fun enabledRules(): List<Rule> = rules().filter { it.enabled }

    /**
     * Replaces the full registry (max [MAX_RULES]), rejects overlap among enabled rules, then
     * arms the global next boundary. Empty list clears arming.
     */
    fun replaceAll(next: List<Rule>, after: ZonedDateTime = ZonedDateTime.now()): Result<ArmedBoundary?> {
        if (next.size > MAX_RULES) {
            return Result.failure(IllegalArgumentException("At most $MAX_RULES protection windows."))
        }
        next.forEach { rule ->
            if (!FocusWindowRecurrence.isValidShape(rule.startTime, rule.endTime)) {
                return Result.failure(IllegalArgumentException("Invalid window shape for ${rule.scheduleId}."))
            }
            if (rule.days.isEmpty() || rule.days.any { it !in 0..6 }) {
                return Result.failure(IllegalArgumentException("Each window needs at least one start day."))
            }
        }
        val asWindows = next.map {
            ProtectionWindowV1(it.scheduleId, it.startTime, it.endTime, it.days, it.enabled)
        }
        RoutinePlanRules.findEnabledOverlap(asWindows)?.let { (a, b) ->
            return Result.failure(
                IllegalArgumentException("Protection windows overlap: ${a.id} and ${b.id}. Adjacent is fine."),
            )
        }
        prefs.edit().putString(KEY_RULES_JSON, encodeRules(next)).apply()
        clearLegacyKeys()
        return Result.success(armGlobalNext(after))
    }

    /**
     * Upserts one rule into the registry and rearms. Backward-compatible entry for the existing
     * FocusScheduleCard / single-schedule path.
     */
    fun scheduleNext(rule: Rule, after: ZonedDateTime = ZonedDateTime.now()): Pair<Long, FocusWindowAction>? {
        val current = rules().toMutableList()
        val idx = current.indexOfFirst { it.scheduleId == rule.scheduleId }
        if (idx >= 0) current[idx] = rule else current.add(rule.copy(enabled = true))
        val result = replaceAll(current, after)
        val armed = result.getOrNull() ?: return null
        return armed.triggerAtEpochMillis to armed.action
    }

    /** Rearms from the current registry without changing rules. */
    fun rearm(after: ZonedDateTime = ZonedDateTime.now()): ArmedBoundary? = armGlobalNext(after)

    fun pendingAction(): FocusWindowAction? =
        prefs.getString(KEY_PENDING_ACTION, null)?.let { runCatching { FocusWindowAction.valueOf(it) }.getOrNull() }

    fun pendingTriggerAtEpochMillis(): Long? = prefs.getLong(KEY_NEXT_TRIGGER, -1L).takeIf { it > 0 }

    fun pendingRuleId(): String? = prefs.getString(KEY_PENDING_RULE_ID, null)

    /** The rule that owns the currently armed boundary, or null. */
    fun pendingRule(): Rule? {
        migrateLegacyIfNeeded()
        val id = pendingRuleId()
        if (id != null) return rules().firstOrNull { it.scheduleId == id }
        // Legacy: single-rule keys without registry.
        return legacyRule()
    }

    fun pendingArmed(): ArmedBoundary? {
        val id = pendingRuleId() ?: return null
        val action = pendingAction() ?: return null
        val at = pendingTriggerAtEpochMillis() ?: return null
        return ArmedBoundary(id, action, at)
    }

    fun cancel() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(firePendingIntent())
        prefs.edit().clear().apply()
    }

    private fun armGlobalNext(after: ZonedDateTime): ArmedBoundary? {
        val enabled = enabledRules()
        if (enabled.isEmpty()) {
            clearArmed()
            return null
        }
        var best: Triple<Rule, ZonedDateTime, FocusWindowAction>? = null
        for (rule in enabled) {
            val (occurrence, action) = FocusWindowRecurrence.nextBoundary(
                rule.days, rule.startTime, rule.endTime, after,
            ) ?: continue
            if (best == null || occurrence.isBefore(best.second)) {
                best = Triple(rule, occurrence, action)
            }
        }
        val winner = best ?: run {
            clearArmed()
            return null
        }
        val triggerAt = winner.second.toInstant().toEpochMilli()
        if (!armNativeAlarm(triggerAt)) {
            // Persist registry already saved; mark not armed so UI can say Needs device access.
            prefs.edit()
                .remove(KEY_PENDING_ACTION)
                .remove(KEY_NEXT_TRIGGER)
                .remove(KEY_PENDING_RULE_ID)
                .apply()
            return null
        }
        prefs.edit()
            .putString(KEY_PENDING_RULE_ID, winner.first.scheduleId)
            .putString(KEY_PENDING_ACTION, winner.third.name)
            .putLong(KEY_NEXT_TRIGGER, triggerAt)
            .apply()
        return ArmedBoundary(winner.first.scheduleId, winner.third, triggerAt)
    }

    private fun clearArmed() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(firePendingIntent())
        prefs.edit()
            .remove(KEY_PENDING_ACTION)
            .remove(KEY_NEXT_TRIGGER)
            .remove(KEY_PENDING_RULE_ID)
            .apply()
    }

    private fun armNativeAlarm(triggerAtEpochMillis: Long): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return false
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, firePendingIntent())
        return true
    }

    private fun firePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, FocusScheduleReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun migrateLegacyIfNeeded() {
        if (prefs.contains(KEY_RULES_JSON)) return
        val legacy = legacyRule() ?: return
        prefs.edit().putString(KEY_RULES_JSON, encodeRules(listOf(legacy))).apply()
        // Keep pending_* keys; map schedule id into pending rule id if missing.
        if (prefs.getString(KEY_PENDING_RULE_ID, null) == null) {
            prefs.edit().putString(KEY_PENDING_RULE_ID, legacy.scheduleId).apply()
        }
    }

    private fun legacyRule(): Rule? {
        val scheduleId = prefs.getString(KEY_SCHEDULE_ID, null) ?: return null
        val start = prefs.getString(KEY_START, null)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val end = prefs.getString(KEY_END, null)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val days = prefs.getString(KEY_DAYS, null)?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
        if (days.isNullOrEmpty()) return null
        val packages = prefs.getStringSet(KEY_PACKAGES, null) ?: return null
        return Rule(scheduleId, days, start, end, packages, enabled = true)
    }

    private fun clearLegacyKeys() {
        prefs.edit()
            .remove(KEY_SCHEDULE_ID)
            .remove(KEY_DAYS)
            .remove(KEY_START)
            .remove(KEY_END)
            .remove(KEY_PACKAGES)
            .apply()
    }

    companion object {
        const val MAX_RULES = 8
        private const val PREFS_NAME = "focus_schedule"
        private const val KEY_RULES_JSON = "rules_json"
        private const val KEY_PENDING_RULE_ID = "pending_rule_id"
        // Legacy single-rule keys (migrated):
        private const val KEY_SCHEDULE_ID = "schedule_id"
        private const val KEY_DAYS = "days"
        private const val KEY_START = "start_time"
        private const val KEY_END = "end_time"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_PENDING_ACTION = "pending_action"
        private const val KEY_NEXT_TRIGGER = "next_trigger_at"
        private const val REQUEST_CODE = 4301

        fun encodeRules(rules: List<Rule>): String {
            val array = JSONArray()
            rules.forEach { r ->
                array.put(
                    JSONObject()
                        .put("id", r.scheduleId)
                        .put("days", JSONArray(r.days.sorted()))
                        .put("start", r.startTime.toString())
                        .put("end", r.endTime.toString())
                        .put("packages", JSONArray(r.blockPackages.sorted()))
                        .put("enabled", r.enabled),
                )
            }
            return array.toString()
        }

        fun decodeRules(raw: String): List<Rule> = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val days = (0 until (o.optJSONArray("days")?.length() ?: 0))
                    .mapNotNull { o.getJSONArray("days").optInt(it).takeIf { d -> d in 0..6 } }
                    .toSet()
                val packages = (0 until (o.optJSONArray("packages")?.length() ?: 0))
                    .mapNotNull { o.getJSONArray("packages").optString(it).takeIf { s -> s.isNotBlank() } }
                    .toSet()
                val start = runCatching { LocalTime.parse(o.getString("start")) }.getOrNull() ?: return@mapNotNull null
                val end = runCatching { LocalTime.parse(o.getString("end")) }.getOrNull() ?: return@mapNotNull null
                Rule(
                    scheduleId = o.getString("id"),
                    days = days,
                    startTime = start,
                    endTime = end,
                    blockPackages = packages,
                    enabled = o.optBoolean("enabled", true),
                )
            }
        }.getOrDefault(emptyList())
    }
}
