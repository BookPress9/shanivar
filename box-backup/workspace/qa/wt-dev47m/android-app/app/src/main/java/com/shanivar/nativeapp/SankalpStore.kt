package com.shanivar.nativeapp

import android.content.Context

/**
 * The person's Sankalp line ("where you want to reach"), kept verbatim on this phone.
 *
 * Own SharedPreferences file on purpose: not [BreathingPreferences] (`BreathingPreferences.kt:15-18`,
 * so the line can never reach the breathing journal, and `BreathingPreferences.clearAll()` does not
 * touch it), and nothing here is ever read by an uploader, scorer or notification. The manifest
 * has `allowBackup="false"`. Design: docs/strategy/shanivar-sankalp-and-sittings-initiation-proposal.md:23-34.
 *
 * The app never edits, suggests, ranks or counts the line. It is shown back word for word on the
 * first sitting's Ready and End screens and nowhere else. 60 characters, same cap as
 * `FocusScheduleCard.kt:246`.
 */
class SankalpStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(NAMESPACE, Context.MODE_PRIVATE)

    /** The saved line exactly as written, or null. */
    val line: String? get() = prefs.getString(KEY_LINE, null)?.takeIf { it.isNotBlank() }

    /** Saves [text] verbatim. Returns false, saving nothing, when it is blank or over [MAX_LENGTH]. */
    fun save(text: String): Boolean {
        if (!isAcceptable(text)) return false
        prefs.edit().putString(KEY_LINE, text).apply()
        return true
    }

    fun clear() = prefs.edit().remove(KEY_LINE).apply()

    companion object {
        const val NAMESPACE = "sankalp_store"
        const val MAX_LENGTH = 60
        private const val KEY_LINE = "line"

        fun isAcceptable(text: String): Boolean =
            text.isNotBlank() && text.codePointCount(0, text.length) <= MAX_LENGTH

        /** For the text field: cuts at 60 characters (code points) and drops line breaks. */
        fun limitInput(text: String): String {
            val single = text.replace("\n", " ").replace("\r", " ")
            if (single.codePointCount(0, single.length) <= MAX_LENGTH) return single
            return single.substring(0, single.offsetByCodePoints(0, MAX_LENGTH))
        }
    }
}
