package com.shanivar.nativeapp

import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Supabase sync client behind the [SyncOutbox]. Off unless a Supabase URL is configured
 * (BuildConfig.SUPABASE_URL, empty by default). Uses PostgREST upserts on (user_id, id), so a
 * retried outbox item never duplicates. Rows are owned by auth.uid() and protected by RLS
 * (supabase/migrations). Guest = Supabase anonymous sign-in. Google / email OTP need the founder's
 * provider credentials and are not wired here.
 */
object SupabaseTables {
    fun forKind(kind: String): String? = when (kind) {
        "attempt" -> "practice_attempts"; "self_report" -> "self_reports"; "routine" -> "routine_plans"
        "window" -> "protection_windows"; "orbit" -> "orbit_events"; else -> null
    }
}

object SyncPayloads {
    private fun q(s: String?) = if (s == null) "null" else "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    private fun ts(ms: Long?) = q(ms?.let { Instant.ofEpochMilli(it).toString() })
    fun attempt(a: SadhanaAttempt) = """{"id":${q(a.id)},"practice_id":${q(a.practiceId)},"content_version":${q(a.contentVersion)},"started_at":${ts(a.startedAtUtc)},"zone_id":${q(a.zoneId)},"intended_seconds":${a.intendedSeconds},"active_ms":${a.activeMillis},"outcome":${q(a.outcome.name)},"ended_at":${ts(a.endedAtUtc)}}"""
    fun selfReport(r: IndependentPracticeRecord) = """{"id":${q(r.id)},"local_date":${q(r.localDate.toString())},"intended_minutes":${r.intendedMinutes ?: "null"},"actual_minutes":${r.actualMinutes ?: "null"},"practice_id":${q(r.practiceId)}}"""
}

class SupabaseClient(private val baseUrl: String, private val anonKey: String) {
    var accessToken: String? = null
        private set
    var userId: String? = null
        private set

    private fun call(method: String, path: String, body: String?, extra: Map<String, String> = emptyMap()): Pair<Int, String> {
        val c = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        c.requestMethod = method; c.connectTimeout = 10_000; c.readTimeout = 15_000
        c.setRequestProperty("apikey", anonKey)
        c.setRequestProperty("Authorization", "Bearer " + (accessToken ?: anonKey))
        c.setRequestProperty("Content-Type", "application/json")
        extra.forEach { (k, v) -> c.setRequestProperty(k, v) }
        if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray()) } }
        val code = c.responseCode
        val text = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty()
        c.disconnect(); return code to text
    }

    /** Guest: Supabase anonymous sign-in. Returns true when a session was issued. */
    fun signInAnonymously(): Boolean {
        val (code, text) = call("POST", "/auth/v1/signup", "{}")
        if (code !in 200..299) return false
        accessToken = Regex("\"access_token\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
        userId = Regex("\"user\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
            ?: Regex("\"id\"\\s*:\\s*\"([0-9a-f-]{36})\"").find(text.substringAfter("\"user\""))?.groupValues?.get(1)
        return accessToken != null
    }

    fun upsert(table: String, json: String): Boolean = accessToken != null &&
        call("POST", "/rest/v1/$table?on_conflict=user_id,id", json, mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")).first in 200..299

    fun select(table: String, query: String = "select=*"): Pair<Int, String> = call("GET", "/rest/v1/$table?$query", null)

    fun sender() = SyncSender { item -> SupabaseTables.forKind(item.kind)?.let { upsert(it, item.payload) } ?: false }
}
