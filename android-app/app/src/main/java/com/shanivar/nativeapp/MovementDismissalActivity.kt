package com.shanivar.nativeapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class MovementDismissalActivity : ComponentActivity(), SensorEventListener {
    private lateinit var tracker: MovementProgressTracker
    private var manager: SensorManager? = null
    private var sensor: Sensor? = null
    private var useHardwareSteps = false
    private var steps by mutableIntStateOf(0)
    private var destination = "morning_light"
    private var accessible = false
    private var alarmId = ""
    // MVP streamline task (2026-09-18): non-negotiable #2 requires the accessible path whenever a
    // sensor cannot deliver movement data, not only when the user pre-selected it in settings.
    // Previously, a device with neither TYPE_STEP_DETECTOR nor TYPE_ACCELEROMETER (or one whose
    // ACTIVITY_RECOGNITION grant is denied) left the counter frozen at 0/target with no visible way
    // out, since the accessible button only rendered when the `accessible` preference was already
    // on - that traps exactly the person this invariant exists to protect.
    private var sensorUnavailable by mutableStateOf(false)

    private val activityPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) registerSensor() else sensorUnavailable = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmId = intent.getStringExtra(AlarmActivity.EXTRA_SESSION_ID).orEmpty()
        val rhythm = getSharedPreferences("rhythm", MODE_PRIVATE)
        val target = rhythm.getInt("stepTarget", 18).coerceIn(1, 200)
        destination = rhythm.getString("movementDestination", "morning_light").orEmpty()
        accessible = rhythm.getBoolean("accessibleMovement", false)
        tracker = MovementProgressTracker(target)
        manager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = manager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        useHardwareSteps = sensor != null
        if (sensor == null) sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorUnavailable = sensor == null // neither sensor exists on this hardware at all
        setContent {
            ShanivarTheme {
                MovementScreen(destination, steps, target, useHardwareSteps, accessible || sensorUnavailable,
                    sensorUnavailable, onAccessible = {
                        recordAndFinish("accessible", JSONObject().put("confirmation", "user_deliberate"))
                    })
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && useHardwareSteps &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            activityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    override fun onStart() { super.onStart(); registerSensor() }
    override fun onStop() { manager?.unregisterListener(this); super.onStop() }

    private fun registerSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && useHardwareSteps &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) return
        sensor?.let { manager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) tracker.onHardwareStep(event.values.firstOrNull() ?: 1f)
        else tracker.onAccelerometer(event.values[0], event.values[1], event.values[2], System.currentTimeMillis())
        steps = tracker.steps
        if (tracker.complete()) {
            recordAndFinish("movement", JSONObject()
                .put("destination", destination).put("steps", steps)
                .put("sensor", if (useHardwareSteps) "step_detector" else "accelerometer_fallback"))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun recordAndFinish(method: String, detail: JSONObject) {
        manager?.unregisterListener(this)
        ShanivarSession.api?.let { api ->
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { HttpDisciplineApi(api).recordDismissal(method, true, detail, alarmId.takeIf { it.isNotBlank() }) }
            }
        }
        setResult(RESULT_OK, Intent().putExtra(DismissalContract.EXTRA_VERIFIED, true))
        finish()
    }
}

@Composable
private fun MovementScreen(
    destination: String, steps: Int, target: Int, hardware: Boolean,
    showAccessible: Boolean, sensorUnavailable: Boolean, onAccessible: () -> Unit,
) {
    val label = when (destination) {
        "morning_light" -> "morning light"
        "kitchen" -> "the kitchen"
        "washroom" -> "the washroom"
        else -> "your chosen place"
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)) {
            Text("Move into your morning.", style = MaterialTheme.typography.headlineLarge)
            if (sensorUnavailable) {
                Text("This phone can't detect movement, so this alarm ends the same way every time here: with your own confirmation.")
            } else {
                Text("Walk toward $label. Shanivar counts only movement after this alarm began.")
                Text("$steps / $target steps", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                LinearProgressIndicator(progress = { steps.toFloat() / target }, modifier = Modifier.fillMaxWidth())
                Text(if (hardware) "Step detector active" else "Calibrated movement fallback active", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showAccessible) OutlinedButton(onAccessible, Modifier.fillMaxWidth()) { Text("Use my accessible wake check") }
        }
    }
}
