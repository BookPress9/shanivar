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

/** `daysOfWeek` uses the 0=Sunday..6=Saturday convention the backend validates
 * (`isValidDaysOfWeek`) - see `AlarmRecurrence`'s doc comment for why this differs from
 * `java.time.DayOfWeek`. `nextConfirmedOccurrence` only ever reflects what this client explicitly
 * confirmed via [RoutineApi.confirmAlarm] - the server never assumes native scheduling happened. */
data class RemoteAlarm(
    val id: String,
    val localTime: String,
    val daysOfWeek: Set<Int>,
    val enabled: Boolean,
    val movementDestination: String,
    val stepTarget: Int,
    val accessibleMovement: Boolean,
    val nextConfirmedOccurrence: String?,
)

/** `mode` is display-only (which of the three presets the user picked) - `days`/`startTime`/
 * `endTime` are the actual rule. Full calendar day ("all day" / few-days full-day) is encoded
 * as startTime=00:00 and endTime=00:00 — midnight through next midnight — not 00:00–23:59
 * (DEV-6). `commitmentTrigger`/`commitmentAction` are the user's own when-then pair
 * ("When ___, I will ___") - see decision 0010 - never server-generated, and blank is valid. */
data class RemoteFocusSchedule(
    val id: String,
    val mode: String,
    val daysOfWeek: Set<Int>,
    val startTime: String,
    val endTime: String,
    val blockedPackages: Set<String>,
    val intentionLabel: String,
    val commitmentTrigger: String,
    val commitmentAction: String,
    val enabled: Boolean,
)

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

    /** v1 ships exactly one editable alarm per account. Discovering it (rather than only trusting
     * a locally-cached id) matters on a fresh login/reinstall/second device, and avoids a stale id
     * left over from a previous signed-in account on this device ever being reused. */
    suspend fun listAlarms(): List<RemoteAlarm> {
        val json = api.requestArray("/discipline/alarms")
        return (0 until json.length()).map { parseAlarm(json.getJSONObject(it)) }
    }

    suspend fun createAlarm(
        localTime: String, daysOfWeek: Set<Int>, movementDestination: String,
        stepTarget: Int, accessibleMovement: Boolean,
    ): RemoteAlarm {
        val body = JSONObject()
            .put("local_time", localTime)
            .put("days_of_week", JSONArray(daysOfWeek.sorted()))
            .put("sound", "default")
            .put("dismissal_method", if (accessibleMovement) "accessible" else "movement")
            .put("movement_destination", movementDestination)
            .put("step_target", stepTarget)
            .put("enabled", true)
        return parseAlarm(api.request("/discipline/alarms", body))
    }

    suspend fun editAlarm(
        id: String, localTime: String, daysOfWeek: Set<Int>, movementDestination: String,
        stepTarget: Int, accessibleMovement: Boolean,
    ): RemoteAlarm {
        // PUT, not PATCH: the backend route (backend/src/routes/discipline.js) is
        // `router.put('/alarms/:id', ...)`, and separately, PATCH isn't in the JDK
        // HttpURLConnection method whitelist that ShanivarApi's client is built on - it throws
        // ProtocolException at request time, not at compile time, so this is worth stating
        // explicitly rather than rediscovering it. The backend resets next_confirmed_occurrence
        // to NULL on any edit (it never assumes scheduling happened) - callers must re-schedule
        // natively and call confirmAlarm() again.
        val body = JSONObject()
            .put("local_time", localTime)
            .put("days_of_week", JSONArray(daysOfWeek.sorted()))
            .put("dismissal_method", if (accessibleMovement) "accessible" else "movement")
            .put("movement_destination", movementDestination)
            .put("step_target", stepTarget)
        return parseAlarm(api.request("/discipline/alarms/$id", body, method = "PUT"))
    }

    suspend fun setAlarmEnabled(id: String, enabled: Boolean): RemoteAlarm =
        parseAlarm(api.request("/discipline/alarms/$id", JSONObject().put("enabled", enabled), method = "PUT"))

    suspend fun deleteAlarm(id: String) {
        api.request("/discipline/alarms/$id", method = "DELETE")
    }

    suspend fun confirmAlarm(id: String, nextOccurrenceIso: String): RemoteAlarm {
        val body = JSONObject().put("next_occurrence", nextOccurrenceIso)
        return parseAlarm(api.request("/discipline/alarms/$id/confirm-scheduled", body))
    }

    /** v1 ships exactly one recurring focus schedule per account, mirroring the one-alarm limit -
     * discovering it rather than only trusting a local cache matters for the same reasons as
     * [listAlarms]. */
    suspend fun listFocusSchedules(): List<RemoteFocusSchedule> {
        val json = api.requestArray("/discipline/focus-schedules")
        return (0 until json.length()).map { parseFocusSchedule(json.getJSONObject(it)) }
    }

    suspend fun createFocusSchedule(
        mode: String, days: Set<Int>, startTime: String, endTime: String, blockedPackages: Set<String>,
        intentionLabel: String, commitmentTrigger: String, commitmentAction: String,
    ): RemoteFocusSchedule {
        val body = JSONObject()
            .put("mode", mode)
            .put("days_of_week", JSONArray(days.sorted()))
            .put("start_time", startTime)
            .put("end_time", endTime)
            .put("block_package_list", JSONArray(blockedPackages.toList()))
            .put("intention_label", intentionLabel)
            .put("commitment_trigger", commitmentTrigger)
            .put("commitment_action", commitmentAction)
        return parseFocusSchedule(api.request("/discipline/focus-schedules", body))
    }

    suspend fun editFocusSchedule(
        id: String, mode: String, days: Set<Int>, startTime: String, endTime: String, blockedPackages: Set<String>,
        intentionLabel: String, commitmentTrigger: String, commitmentAction: String,
    ): RemoteFocusSchedule {
        val body = JSONObject()
            .put("mode", mode)
            .put("days_of_week", JSONArray(days.sorted()))
            .put("start_time", startTime)
            .put("end_time", endTime)
            .put("block_package_list", JSONArray(blockedPackages.toList()))
            .put("intention_label", intentionLabel)
            .put("commitment_trigger", commitmentTrigger)
            .put("commitment_action", commitmentAction)
        return parseFocusSchedule(api.request("/discipline/focus-schedules/$id", body, method = "PUT"))
    }

    suspend fun setFocusScheduleEnabled(id: String, enabled: Boolean): RemoteFocusSchedule =
        parseFocusSchedule(api.request("/discipline/focus-schedules/$id", JSONObject().put("enabled", enabled), method = "PUT"))

    suspend fun deleteFocusSchedule(id: String) {
        api.request("/discipline/focus-schedules/$id", method = "DELETE")
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

    private fun parseAlarm(json: JSONObject): RemoteAlarm {
        val days = json.optJSONArray("days_of_week")
        val daySet = (0 until (days?.length() ?: 0)).map { days!!.getInt(it) }.toSet()
        return RemoteAlarm(
            id = json.getString("id"),
            localTime = json.optString("local_time", "07:00").take(5),
            daysOfWeek = daySet.ifEmpty { setOf(0, 1, 2, 3, 4, 5, 6) },
            enabled = json.optBoolean("enabled", true),
            movementDestination = json.optString("movement_destination", "morning_light"),
            stepTarget = json.optInt("step_target", 18),
            accessibleMovement = json.optString("dismissal_method") == "accessible",
            // org.json's optString() stringifies a JSON `null` value to the literal string
            // "null" rather than returning an empty string - isNull() must be checked first, or
            // a freshly created/edited alarm (which always responds with this field JSON-null)
            // parses as the text "null" instead of the absence this type otherwise represents.
            nextConfirmedOccurrence = if (json.isNull("next_confirmed_occurrence")) null
                else json.optString("next_confirmed_occurrence").takeIf { it.isNotBlank() },
        )
    }

    private fun parseFocusSchedule(json: JSONObject): RemoteFocusSchedule {
        val days = json.optJSONArray("days_of_week")
        val daySet = (0 until (days?.length() ?: 0)).map { days!!.getInt(it) }.toSet()
        val packages = json.optJSONArray("block_package_list")
        val blocked = (0 until (packages?.length() ?: 0)).map { packages!!.getString(it) }.toSet()
        return RemoteFocusSchedule(
            id = json.getString("id"),
            mode = json.optString("mode", "custom"),
            daysOfWeek = daySet.ifEmpty { setOf(0, 1, 2, 3, 4, 5, 6) },
            startTime = json.optString("start_time", "09:00").take(5),
            endTime = json.optString("end_time", "17:00").take(5),
            blockedPackages = blocked.ifEmpty { DisciplineTargets.DEFAULT_BLOCKED_PACKAGES },
            intentionLabel = json.optString("intention_label", ""),
            commitmentTrigger = json.optString("commitment_trigger", ""),
            commitmentAction = json.optString("commitment_action", ""),
            enabled = json.optBoolean("enabled", true),
        )
    }
}
