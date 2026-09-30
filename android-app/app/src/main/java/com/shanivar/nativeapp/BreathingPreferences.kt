package com.shanivar.nativeapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only storage for the breathing pillar.
 *
 * Deliberately its own SharedPreferences file rather than a few extra keys in `rhythm`. Two reasons,
 * both practical: "Clear practice history" must be able to delete everything here without any chance
 * of touching an alarm time or a focus schedule (test guide I14), and an intended-sleep time stored
 * next to `quietTime` would invite exactly the conflation that T06 exists to prevent.
 *
 * What is never stored: the current feeling, the comfort answers, the safe-setting confirmation, or
 * any free text. Those are ephemeral per visit by design - a saved mood history is a different
 * product with different consent, and the journal keeps only what a person would need to see their
 * own pattern of use.
 */
class BreathingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(NAMESPACE, 0)

    var soundConsent: Boolean
        get() = prefs.getBoolean(KEY_SOUND_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_CONSENT, value).apply()

    /** Set only by an explicit "Comfortable with breath awareness" choice. Never inferred from
     * minutes practised, age, or how many sessions someone finished. */
    var priorBreathComfort: Boolean
        get() = prefs.getBoolean(KEY_PRIOR_COMFORT, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIOR_COMFORT, value).apply()

    var wakeTime: String?
        get() = prefs.getString(KEY_WAKE, null)
        set(value) = prefs.edit().putString(KEY_WAKE, value).apply()

    var intendedSleepTime: String?
        get() = prefs.getString(KEY_SLEEP, null)
        set(value) = prefs.edit().putString(KEY_SLEEP, value).apply()

    fun schedule(): BreathingSchedule = BreathingSchedule.parse(wakeTime, intendedSleepTime)

    /** Seeds the wake time from the alarm rhythm the person already set, and only that. The sleep
     * time stays absent until they state one, which leaves the schedule UNKNOWN - an honest state. */
    fun seedWakeTimeFrom(rhythm: Rhythm) {
        if (wakeTime == null) wakeTime = rhythm.wakeTime
    }

    fun journal(): List<BreathingRecord> {
        val raw = prefs.getString(KEY_JOURNAL, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            runCatching {
                BreathingRecord(
                    sessionId = item.getString("sessionId"),
                    practiceId = item.getString("practiceId"),
                    catalogVersion = item.optString("catalogVersion"),
                    contentVersion = item.optInt("contentVersion", 1),
                    entryContext = enumOrNull<BreathingEntry>(item.optString("entryContext")) ?: BreathingEntry.HOME,
                    selectedDurationSeconds = item.optInt("selectedDurationSeconds"),
                    activeSeconds = item.optInt("activeSeconds"),
                    outcome = enumOrNull<BreathingPhase>(item.optString("outcome")) ?: BreathingPhase.INTERRUPTED,
                    feedback = enumOrNull<BreathingFeedback>(item.optString("feedback")),
                )
            }.getOrNull()
        }
    }

    fun append(record: BreathingRecord) = writeAll(trimBreathingJournal(journal() + record))

    private fun writeAll(kept: List<BreathingRecord>) {
        val array = JSONArray()
        kept.forEach { entry ->
            array.put(
                JSONObject()
                    .put("sessionId", entry.sessionId)
                    .put("practiceId", entry.practiceId)
                    .put("catalogVersion", entry.catalogVersion)
                    .put("contentVersion", entry.contentVersion)
                    .put("entryContext", entry.entryContext.name)
                    .put("selectedDurationSeconds", entry.selectedDurationSeconds)
                    .put("activeSeconds", entry.activeSeconds)
                    .put("outcome", entry.outcome.name)
                    .put("feedback", entry.feedback?.name)
            )
        }
        prefs.edit().putString(KEY_JOURNAL, array.toString()).apply()
    }

    /** Replaces the newest record's feedback. Only ever called for the session just finished. */
    fun setFeedbackOnLatest(sessionId: String, feedback: BreathingFeedback) =
        writeAll(journal().map { if (it.sessionId == sessionId) it.copy(feedback = feedback) else it })

    /**
     * A session that began but has no terminal record - i.e. the process died mid-practice. Stored
     * as the *fact of a session*, never an elapsedRealtime reading: that clock resets on reboot, so
     * comparing a persisted value against a fresh one could invent hours of practice (S11).
     */
    fun markInFlight(record: BreathingRecord) {
        prefs.edit()
            .putString(KEY_IN_FLIGHT, JSONObject()
                .put("sessionId", record.sessionId)
                .put("practiceId", record.practiceId)
                .put("catalogVersion", record.catalogVersion)
                .put("contentVersion", record.contentVersion)
                .put("entryContext", record.entryContext.name)
                .put("selectedDurationSeconds", record.selectedDurationSeconds)
                .toString())
            .apply()
    }

    fun clearInFlight() = prefs.edit().remove(KEY_IN_FLIGHT).apply()

    /** Converts an abandoned session into one INTERRUPTED record. Returns true when it did so, so a
     * caller can explain the interruption once and never resume or replay anything. */
    fun settleInFlight(): Boolean {
        val raw = prefs.getString(KEY_IN_FLIGHT, null) ?: return false
        clearInFlight()
        val item = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        append(
            BreathingRecord(
                sessionId = item.optString("sessionId"),
                practiceId = item.optString("practiceId"),
                catalogVersion = item.optString("catalogVersion"),
                contentVersion = item.optInt("contentVersion", 1),
                entryContext = enumOrNull<BreathingEntry>(item.optString("entryContext")) ?: BreathingEntry.HOME,
                selectedDurationSeconds = item.optInt("selectedDurationSeconds"),
                // Unknown rather than guessed: nothing survived that could measure it honestly.
                activeSeconds = 0,
                outcome = BreathingPhase.INTERRUPTED,
            )
        )
        return true
    }

    /** Clears breathing data only. Alarms, focus schedules, the account and recurrence all live in
     * other preference files and are untouched by this. */
    fun clearAll() = prefs.edit().clear().apply()

    companion object {
        const val NAMESPACE = "breathing_preferences"
        private const val KEY_SOUND_CONSENT = "soundConsent"
        private const val KEY_PRIOR_COMFORT = "priorBreathComfort"
        private const val KEY_WAKE = "wakeTime"
        private const val KEY_SLEEP = "intendedSleepTime"
        private const val KEY_JOURNAL = "journal"
        private const val KEY_IN_FLIGHT = "inFlight"
    }
}

private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    name?.takeIf { it.isNotBlank() }?.let { value -> enumValues<T>().firstOrNull { it.name == value } }
