package com.shanivar.nativeapp

import org.json.JSONArray
import org.json.JSONObject

data class RemoteRhythm(
    val wakeTime: String,
    val quietTime: String,
    val focusMinutes: Int,
    val language: String,
    val movementDestination: String,
    val stepTarget: Int,
    val sceneConfirmation: Boolean,
    val accessibleMovement: Boolean,
    val whatsappTime: String,
    val whatsappTone: String,
    val dailyNoteEnabled: Boolean,
    val blockedPackages: Set<String>,
)

data class RemoteAlarm(val id: String, val nextConfirmedOccurrence: String?)

/** Account-scoped persistence for the rhythm and alarm screens. Supabase remains behind the
 * Express API so the APK never contains a service-role key and all writes retain ownership checks. */
class RoutineApi(private val api: ShanivarApi) {
    suspend fun load(): RemoteRhythm = parse(api.request("/discipline/preferences"))

    suspend fun save(rhythm: Rhythm, language: String): RemoteRhythm {
        val body = JSONObject()
            .put("wake_time", rhythm.wakeTime)
            .put("quiet_time", rhythm.quietTime)
            .put("focus_minutes", rhythm.focusMinutes)
            .put("preferred_language", language)
            .put("dismissal_method", if (rhythm.accessibleMovement) "accessible" else "movement")
            .put("movement_destination", rhythm.movementDestination)
            .put("step_target", rhythm.stepTarget)
            .put("scene_confirmation", rhythm.sceneConfirmation)
            .put("whatsapp_time", rhythm.whatsappTime)
            .put("whatsapp_tone", rhythm.whatsappTone.lowercase())
            .put("daily_note_enabled", rhythm.dailyNoteEnabled)
            .put("block_package_list", JSONArray(rhythm.blockedPackages.toList()))
        return parse(api.request("/discipline/preferences", body, method = "PUT"))
    }

    suspend fun createAlarm(rhythm: Rhythm): RemoteAlarm {
        val body = JSONObject()
            .put("local_time", rhythm.wakeTime)
            .put("days_of_week", JSONArray(listOf(0, 1, 2, 3, 4, 5, 6)))
            .put("sound", "default")
            .put("dismissal_method", if (rhythm.accessibleMovement) "accessible" else "movement")
            .put("movement_destination", rhythm.movementDestination)
            .put("step_target", rhythm.stepTarget)
            .put("scene_confirmation", rhythm.sceneConfirmation)
            .put("enabled", true)
        return parseAlarm(api.request("/discipline/alarms", body))
    }

    suspend fun confirmAlarm(id: String, nextOccurrenceIso: String): RemoteAlarm {
        val body = JSONObject().put("next_occurrence", nextOccurrenceIso)
        return parseAlarm(api.request("/discipline/alarms/$id/confirm-scheduled", body))
    }

    private fun parse(json: JSONObject): RemoteRhythm {
        val packages = json.optJSONArray("block_package_list")
        val blocked = (0 until (packages?.length() ?: 0)).map { packages!!.getString(it) }.toSet()
        return RemoteRhythm(
            wakeTime = json.optString("wake_time", "07:00").take(5),
            quietTime = json.optString("quiet_time", "21:30").take(5),
            focusMinutes = json.optInt("focus_minutes", 30),
            language = json.optString("preferred_language", "en"),
            movementDestination = json.optString("movement_destination", "morning_light"),
            stepTarget = json.optInt("step_target", 18),
            sceneConfirmation = json.optBoolean("scene_confirmation", false),
            accessibleMovement = json.optString("dismissal_method") == "accessible",
            whatsappTime = json.optString("whatsapp_time", "08:00").take(5),
            whatsappTone = json.optString("whatsapp_tone", "direct").replaceFirstChar { it.uppercase() },
            dailyNoteEnabled = json.optBoolean("daily_note_enabled", true),
            blockedPackages = blocked.ifEmpty { DisciplineTargets.DEFAULT_BLOCKED_PACKAGES },
        )
    }

    private fun parseAlarm(json: JSONObject) = RemoteAlarm(
        id = json.getString("id"),
        nextConfirmedOccurrence = json.optString("next_confirmed_occurrence").takeIf { it.isNotBlank() },
    )
}
