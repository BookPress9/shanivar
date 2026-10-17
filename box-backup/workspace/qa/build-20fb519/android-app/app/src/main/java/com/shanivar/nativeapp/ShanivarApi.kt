package com.shanivar.nativeapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DailyNote(val text: String, val action: String, val reflection: String, val date: String, val general: Boolean)
data class Account(val id: String, val name: String, val streak: Int)
data class SignupDetails(
    val name: String,
    val email: String,
    val password: String,
    val language: String,
    val birthDate: String = "",
    val birthTime: String = "",
    val birthLocation: String = "",
)
class ApiFailure(val status: Int, message: String, val retryAfterSeconds: Long = 30) : Exception(message)

class ShanivarApi(private val baseUrl: String = BuildConfig.API_URL) {
    private var token: String? = null
    internal fun sessionToken(): String? = token
    internal fun restoreSession(value: String) { require(value.isNotBlank()); token = value }
    fun logout() { token = null }
    internal suspend fun request(path: String, body: JSONObject? = null,
        method: String = if (body == null) "GET" else "POST", key: String? = null,
        reflectionEnvelope: Boolean = false): JSONObject {
        val requestToken = token
        return withContext(Dispatchers.IO) {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            // The backend runs on a free tier that sleeps when idle; a measured cold start took
            // 22.6s, and AI-generated responses add Gemini's own latency on top. The previous
            // 25s read timeout sat under that, so the first request of the day - every day, for
            // every user - was the one most likely to fail. Generous ceilings here; the UI is
            // responsible for saying "waking the server" rather than appearing frozen.
            connection.connectTimeout = 20000
            connection.readTimeout = 60000
            connection.requestMethod = method
            connection.setRequestProperty("Content-Type", "application/json")
            requestToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            key?.let { connection.setRequestProperty("Idempotency-Key", it) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse {
                if (code in 200..299) throw ApiFailure(code, "The server returned an unreadable response.")
                JSONObject()
            }
            if (code !in 200..299 && !(reflectionEnvelope && code == 503 && json.has("session_id")))
                throw ApiFailure(code, json.optString("error", "Could not connect. Try again."),
                    connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceAtLeast(1) ?: 30)
            json
        } finally { connection.disconnect() }
        }
    }
    /**
     * Same request machinery as [request], for the handful of endpoints (currently only
     * `GET /discipline/alarms`) that return a bare JSON array rather than an object. Kept as a
     * sibling rather than folding array support into [request] itself - [request] is used by
     * every other route in the app and this codebase's convention (per NATIVE_HANDOFF.md) is to
     * touch shared client plumbing minimally and additively.
     */
    internal suspend fun requestArray(path: String): JSONArray {
        val requestToken = token
        return withContext(Dispatchers.IO) {
            val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 20000
                connection.readTimeout = 60000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                requestToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val errorJson = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
                    throw ApiFailure(code, errorJson.optString("error", "Could not connect. Try again."),
                        connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceAtLeast(1) ?: 30)
                }
                runCatching { JSONArray(text) }.getOrElse { throw ApiFailure(code, "The server returned an unreadable response.") }
            } finally { connection.disconnect() }
        }
    }

    suspend fun login(email: String, password: String): Account {
        val result = request("/auth/login", JSONObject().put("email", email).put("password", password))
        val value = result.getString("token")
        require(value.isNotBlank()) { "The server did not return a session." }
        token = value
        return try { account() } catch (e: Exception) { token = null; throw e }
    }
    suspend fun register(details: SignupDetails): Account {
        val body = JSONObject()
            .put("email", details.email.trim())
            .put("password", details.password)
            .put("display_name", details.name.trim())
            .put("preferred_language", details.language)
        if (details.birthDate.isNotBlank()) body.put("birth_date", details.birthDate.trim())
        if (details.birthTime.isNotBlank()) body.put("birth_time", details.birthTime.trim())
        if (details.birthLocation.isNotBlank()) body.put("birth_location", details.birthLocation.trim())
        val result = request("/auth/register", body)
        val value = result.getString("token")
        require(value.isNotBlank()) { "The server did not return a session." }
        token = value
        return try { account() } catch (e: Exception) { token = null; throw e }
    }
    suspend fun saveProfile(language: String, birthDate: String, birthTime: String, birthLocation: String) {
        val body = JSONObject().put("preferred_language", language)
        if (birthDate.isNotBlank()) body.put("birth_date", birthDate.trim())
        if (birthTime.isNotBlank()) body.put("birth_time", birthTime.trim())
        if (birthLocation.isNotBlank()) body.put("birth_location", birthLocation.trim())
        request("/users/me", body, method = "PUT")
    }
    suspend fun account(): Account {
        val json = request("/users/me")
        return Account(json.getString("id"), json.optString("display_name").ifBlank { json.optString("username", "Observer") }, json.optInt("streak_days"))
    }
    suspend fun today(): DailyNote {
        val json = request("/insights/today").getJSONObject("insight")
        return DailyNote(json.getString("insight_text"), json.getString("action_text"), json.getString("reflection_text"),
            json.getString("insight_date"), json.optString("id") == "00000000-0000-0000-0000-000000000000")
    }
}
