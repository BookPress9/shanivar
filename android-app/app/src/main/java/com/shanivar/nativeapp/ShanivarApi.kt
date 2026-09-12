package com.shanivar.nativeapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DailyNote(val text: String, val action: String, val reflection: String, val date: String, val general: Boolean)
data class Account(val id: String, val name: String, val streak: Int)
class ApiFailure(val status: Int, message: String, val retryAfterSeconds: Long = 30) : Exception(message)

class ShanivarApi(private val baseUrl: String = BuildConfig.API_URL) {
    private var token: String? = null
    fun logout() { token = null }
    internal suspend fun request(path: String, body: JSONObject? = null,
        method: String = if (body == null) "GET" else "POST", key: String? = null,
        reflectionEnvelope: Boolean = false): JSONObject {
        val requestToken = token
        return withContext(Dispatchers.IO) {
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000
            connection.readTimeout = 25000
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
    suspend fun login(email: String, password: String): Account {
        val result = request("/auth/login", JSONObject().put("email", email).put("password", password))
        val value = result.getString("token")
        require(value.isNotBlank()) { "The server did not return a session." }
        token = value
        return try { account() } catch (e: Exception) { token = null; throw e }
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
