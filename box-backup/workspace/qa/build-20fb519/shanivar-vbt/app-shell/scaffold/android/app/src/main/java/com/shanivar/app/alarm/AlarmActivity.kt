package com.shanivar.app.alarm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shanivar.app.R

/**
 * The one deliberately NON-WebView screen in the app.
 *
 * Why native, not WebView (see ../../../../../architecture-decision.md §3):
 * this activity must reliably draw full-screen over the lock screen, resist
 * being swiped away, and host a physical-verification dismissal (QR scan /
 * puzzle / movement, per ai_org/shanivar_vbt_track.md's locked decision) —
 * behavior that is fragile inside a WebView across OEM Android skins, and
 * that must not depend on the Capacitor WebView/MainActivity being alive.
 *
 * PHASE 1 SCAFFOLD — layout and dismissal logic are stubs. Real
 * verification mechanics are the Discipline Engine Agent's deliverable.
 * On successful dismissal, this activity deep-links directly into the
 * session player for the scheduled session (session-player-spec.md §3) —
 * skipping the dashboard, which is the actual enforcement mechanic: the
 * alarm's only "off switch" is starting the session it was set for.
 */
class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)
        // sessionId carried from AlarmReceiver — used both to render which
        // session this alarm is for, and to build the deep link below.
        val sessionId = intent.getStringExtra("sessionId")

        // TODO(discipline-engine): wire the physical-verification dismissal
        // (QR scan / puzzle / movement) to a real "onVerified()" callback
        // instead of this placeholder no-op wiring.
        onVerifiedDismissalStub(sessionId)
    }

    private fun onVerifiedDismissalStub(sessionId: String?) {
        // TODO(discipline-engine): call this only after real physical
        // verification succeeds — currently unwired/inert in this scaffold.
        // Deep-links into MainActivity's WebView, resolved by the frontend
        // router to the practice/session-player route with sessionId
        // pre-loaded (session-player-spec.md §3, "Alarm-triggered" entry point).
        val deepLink = Intent(this, com.shanivar.app.MainActivity::class.java).apply {
            data = Uri.parse("https://shanivar.com/practice?session_id=$sessionId&source=alarm")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        // NOTE: not started automatically in this stub — left as documentation
        // of the intended deep-link shape until real verification exists.
        // startActivity(deepLink)
    }
}
