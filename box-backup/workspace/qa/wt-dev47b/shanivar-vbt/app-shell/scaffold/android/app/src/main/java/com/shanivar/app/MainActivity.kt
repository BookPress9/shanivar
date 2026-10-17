package com.shanivar.app

import android.os.Bundle
import com.getcapacitor.BridgeActivity
import com.shanivar.app.alarm.AlarmPlugin
import com.shanivar.app.discipline.DisciplineLockoutPlugin

/**
 * Shanivar VBT — main (and only) WebView-hosting activity.
 *
 * Loads the live Next.js frontend per capacitor.config.ts (`server.url`).
 * Registers the two discipline-layer native plugins so the web session
 * player (see ../../../session-player-spec.md) can call into native code
 * for lockout status and alarm scheduling without either feature living
 * inside the WebView itself. See ../../../../architecture-decision.md.
 */
class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        registerPlugin(DisciplineLockoutPlugin::class.java)
        registerPlugin(AlarmPlugin::class.java)
        super.onCreate(savedInstanceState)
    }
}
