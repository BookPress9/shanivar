package com.shanivar.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The only thing this extra can do is open a screen. It cannot dismiss an alarm, cancel a
        // recurrence or release protection, so a forged Intent carrying it gains nothing (test guide
        // I12) - and launching this Activity from a locked device shows the keyguard first, which is
        // what keeps practice history off the lock screen.
        setContent { ShanivarApp(openBreathing = intent?.getBooleanExtra(EXTRA_OPEN_BREATHING, false) == true) }
    }

    companion object {
        const val EXTRA_OPEN_BREATHING = "open_breathing"
    }
}
