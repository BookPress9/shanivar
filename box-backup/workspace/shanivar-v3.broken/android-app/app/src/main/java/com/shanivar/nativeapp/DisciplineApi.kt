package com.shanivar.nativeapp

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Wire contract for the real discipline routes (under `/api/discipline`) in `backend/src/routes/discipline.js`.
 * Reconciled against the landed implementation (2026-09-10) - this file was originally written
 * against a placeholder contract in parallel with the backend agent's work; the actual routes use
 * `block_package_list` (not `package_names`), wrap the active-session check as `{active: <row|null>}`,
 * require the session id in the release path (`/discipline/focus/:id/release`), and expect
 * `verification_detail` as a JSON object (not a plain string) on dismissal recording.
 */
interface DisciplineApi {
    /** Null when there is no active focus session for this account. */
    suspend fun activeFocus(): FocusSessionInfo?
    suspend fun startFocus(minutes: Int, packageNames: Set<String>): FocusSessionInfo
    /** No-op success if [sessionId] is already released/expired or unknown - matches the
     * backend's idempotent release semantics. */
    suspend fun releaseFocus(sessionId: String)
    /** Records only - per the task spec this call never itself grants a dismissal, it just logs it. */
    suspend fun recordDismissal(method: String, verified: Boolean, detail: JSONObject, alarmScheduleId: String? = null)
}

data class FocusSessionInfo(
    val id: String,
    val endsAtEpochMillis: Long,
    val packageNames: Set<String>,
)

/** Talks to the backend using the existing hand-rolled `ShanivarApi` HTTP client - no new
 * networking library, matching every other route in this app. */
class HttpDisciplineApi(private val api: ShanivarApi) : DisciplineApi {
    override suspend fun activeFocus(): FocusSessionInfo? {
        val json = api.request("/discipline/focus/active")
        val active = json.optJSONObject("active") ?: return null
        return parseSession(active)
    }

    override suspend fun startFocus(minutes: Int, packageNames: Set<String>): FocusSessionInfo {
        val body = JSONObject()
            .put("duration_minutes", minutes)
            .put("block_package_list", JSONArray(packageNames.toList()))
        return parseSession(api.request("/discipline/focus/start", body))
    }

    override suspend fun releaseFocus(sessionId: String) {
        api.request("/discipline/focus/$sessionId/release", JSONObject())
    }

    override suspend fun recordDismissal(method: String, verified: Boolean, detail: JSONObject, alarmScheduleId: String?) {
        val body = JSONObject()
            .put("method", method)
            .put("verified", verified)
            .put("verification_detail", detail)
        if (alarmScheduleId != null) body.put("alarm_schedule_id", alarmScheduleId)
        api.request("/discipline/alarms/dismiss", body)
    }

    private fun parseSession(json: JSONObject): FocusSessionInfo {
        val packages = json.optJSONArray("block_package_list")
        val set = (0 until (packages?.length() ?: 0)).map { packages!!.getString(it) }.toSet()
        return FocusSessionInfo(
            id = json.getString("id"),
            endsAtEpochMillis = parseEndsAt(json),
            packageNames = set,
        )
    }

    // Deliberately NOT `expires_at` - that column is `started_at + requested_duration_minutes +
    // a 30min staleness-cleanup grace period` (see discipline.js), so using it here would show
    // the user 30 extra minutes of "remaining time" beyond what they actually requested. The
    // real intended end is `started_at + requested_duration_minutes`, computed client-side since
    // the backend doesn't return a separate field for it.
    private fun parseEndsAt(json: JSONObject): Long {
        val startedIso = json.optString("started_at", "")
        val minutes = json.optInt("requested_duration_minutes", -1)
        if (minutes < 0) return 0L
        val startedMillis = runCatching { Instant.parse(startedIso).toEpochMilli() }.getOrNull() ?: return 0L
        return startedMillis + minutes * 60_000L
    }
}
