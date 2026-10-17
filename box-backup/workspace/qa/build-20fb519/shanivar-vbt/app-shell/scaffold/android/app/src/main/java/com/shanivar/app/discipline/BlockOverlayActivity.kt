package com.shanivar.app.discipline

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * REFERENCE SKETCH ONLY — see ../../../../../../scaffold/README.md.
 *
 * Shown by LockoutMonitorService when the blocked package is detected in
 * the foreground during an active session window (see
 * ../../../../../../session-player-spec.md §2.2 for the status pill this
 * feeds). Uses a SYSTEM_ALERT_WINDOW-class presentation per
 * shanivar-vbt/discipline-engine/platform-research.md's recommended
 * architecture — not an AccessibilityService intercept.
 *
 * PHASE 1 SCAFFOLD — structural stub. Real content (block message,
 * "return to session" CTA) and the redirect-back-to-session-player flow
 * are Discipline Engine Agent / Codex deliverables.
 */
class BlockOverlayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO(discipline-engine / codex): render the block screen and wire
        // a redirect back into the active session (deep link, same shape as
        // AlarmActivity's session deep link).
    }
}
