package com.shanivar.nativeapp

import android.content.Intent
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BootReconcileActionsTest {
    @Test
    fun appUpdateReconcilesAlarmsAndWindows() {
        assertTrue(Intent.ACTION_MY_PACKAGE_REPLACED in BootRescheduleReceiver.RECONCILE_ACTIONS)
        assertTrue(Intent.ACTION_BOOT_COMPLETED in BootRescheduleReceiver.RECONCILE_ACTIONS)
        val manifest = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml").map(::File).first { it.exists() }.readText()
        assertTrue(manifest.contains("android.intent.action.MY_PACKAGE_REPLACED"))
    }
}
