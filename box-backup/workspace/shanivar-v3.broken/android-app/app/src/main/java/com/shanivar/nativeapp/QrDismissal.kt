package com.shanivar.nativeapp

import android.content.Context
import java.util.UUID

/**
 * Pure payload-matching logic, kept separate from `QrDismissalActivity`'s CameraX/ML Kit wiring so
 * it's unit-testable without a camera. Per the task spec: "for v1, a simple fixed app-generated QR
 * value the user prints/displays is fine; don't over-engineer QR registration" - so this is a
 * straightforward equality check, not a signed/rotating token.
 */
object QrDismissalVerifier {
    fun matches(scannedPayload: String, expectedPayload: String): Boolean {
        val scanned = scannedPayload.trim()
        val expected = expectedPayload.trim()
        return scanned.isNotEmpty() && expected.isNotEmpty() && scanned == expected
    }
}

/**
 * Generates and persists the one fixed value this install's QR sticker encodes. Not a backend
 * concept for v1 - Codex's settings UI (out of scope here) is expected to render this value as a
 * QR code the user prints/displays, per discipline_engine_squad.md's Agent B/native-handoff split.
 */
class QrRegistration(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun valueFor(accountId: String?): String {
        prefs.getString(KEY_VALUE, null)?.let { return it }
        val generated = "shanivar-dismiss-" + (accountId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString())
        prefs.edit().putString(KEY_VALUE, generated).apply()
        return generated
    }

    companion object {
        private const val PREFS_NAME = "discipline_qr"
        private const val KEY_VALUE = "qr_value"
    }
}
