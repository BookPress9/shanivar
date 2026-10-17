package com.shanivar.nativeapp

import org.json.JSONObject
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId

// Entry markers are only constructed by in-app navigation, never read from Intent extras.
enum class ReflectionEntryPoint(val wire: String) { TODAY("today"), BLOCKED_APP_SURFACE("blocked_app") }
data class ReflectionOption(val id: String, val label: String)
data class ReflectionCard(val id: String, val index: Int, val sender: String, val message: String,
    val question: String, val options: List<ReflectionOption>, val context: List<String>, val provenance: String)
data class ReflectionAnswer(val card: ReflectionCard, val kind: String, val value: String)
data class ReflectionSession(val id: String, val version: Int, val status: String, val mode: String,
    val asOf: String, val localDate: String, val timezone: String, val answered: Int, val limit: Int,
    val current: ReflectionCard?, val history: List<ReflectionAnswer>) {
    fun savedDay(): Boolean = runCatching { LocalDate.now(ZoneId.of(timezone)).toString() != localDate.take(10) }.getOrDefault(true)
    companion object {
        fun parse(j: JSONObject): ReflectionSession = ReflectionSession(
            j.getString("session_id"), j.getInt("version"), j.getString("status"), j.getString("mode"),
            j.getString("context_as_of"), j.getString("local_date"), j.getString("timezone"),
            j.getInt("answered_count"), j.getInt("session_limit"), j.optJSONObject("current_card")?.let(::card),
            j.optJSONArray("answered_history").objects().map { ReflectionAnswer(card(it.getJSONObject("card")),
                it.getJSONObject("response").getString("kind"), it.getJSONObject("response").getString("value")) })
        private fun card(j: JSONObject) = ReflectionCard(j.getString("id"), j.getInt("index"), j.getString("sender"),
            j.getString("message"), j.getString("question"), j.optJSONArray("options").objects().map {
                ReflectionOption(it.getString("id"), it.getString("label")) },
            j.optJSONArray("context_refs").let { a -> (0 until (a?.length() ?: 0)).map { a!!.getString(it) } },
            j.getString("provenance_label"))
    }
}
private fun JSONArray?.objects(): List<JSONObject> = (0 until (this?.length() ?: 0)).map { this!!.getJSONObject(it) }

fun ReflectionCard.contextLabel(): String = when (provenance) {
    "general_fallback" -> "General reflection · service fallback"
    "general" -> "General reflection"
    "limited_chart" -> "Limited chart context"
    "personalized" -> "Chart-informed reflection"
    else -> "Context unavailable"
}

interface ReflectionRemote {
    suspend fun call(path: String, method: String = "GET", body: JSONObject? = null, key: String? = null): JSONObject
}
class ReflectionApi(private val api: ShanivarApi) : ReflectionRemote {
    override suspend fun call(path: String, method: String, body: JSONObject?, key: String?) =
        api.request("/reflections$path", body, method, key, reflectionEnvelope = true)
}

/** The scroller can read status but has no capability to start/release locks or dismiss alarms. */
fun interface ReflectionProtectionReader { suspend fun status(): DisciplineStatus }
val UnavailableReflectionProtection = ReflectionProtectionReader { DisciplineStatus(ProtectionState.UNAVAILABLE, false) }
fun DisciplineGateway.reflectionReader() = ReflectionProtectionReader { status() }
