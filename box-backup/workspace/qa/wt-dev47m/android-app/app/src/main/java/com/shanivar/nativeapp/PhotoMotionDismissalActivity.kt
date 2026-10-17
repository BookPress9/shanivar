package com.shanivar.nativeapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Photo + genuine-motion dismissal. Requires `GenuineMotionTracker` (see MotionDismissal.kt for
 * the threshold/duration reasoning) to have registered sustained ambulatory motion since this
 * screen opened, AND a photo capture - the locked founder decision in place of ML scene-matching:
 * deterministic, no reference photo/sticker required, but not spoofable by a phone left still on a
 * nightstand with a QR sticker nearby, nor by a single pickup jolt without actually walking.
 *
 * The photo is captured in-memory (`ImageCapture.OnImageCapturedCallback`, not saved to a file) -
 * per the task spec, capturing proves the camera flow was exercised, it is not matched against
 * anything and does not need to persist.
 */
class PhotoMotionDismissalActivity : ComponentActivity(), SensorEventListener {
    private val state = PhotoMotionDismissalState()
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var motionSatisfied by mutableStateOf(false)

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraPermissionGranted = granted
    }
    private var cameraPermissionGranted by mutableStateOf(false)
    private var captureRequested by mutableStateOf(false)
    private var statusText by mutableStateOf("Start walking, then take your photo.")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        setContent {
            ShanivarTheme {
                PhotoMotionScreen(
                    hasCameraPermission = cameraPermissionGranted,
                    onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    motionSatisfied = motionSatisfied,
                    statusText = statusText,
                    onCapture = { captureRequested = true },
                    captureRequested = captureRequested,
                    onCaptureHandled = { captureRequested = false },
                    onPhotoTaken = ::onPhotoTaken,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onStop() {
        sensorManager?.unregisterListener(this)
        super.onStop()
    }

    override fun onSensorChanged(event: SensorEvent) {
        state.onAccelerometerSample(event.values[0], event.values[1], event.values[2], System.currentTimeMillis())
        if (state.motionSatisfied() && !motionSatisfied) motionSatisfied = true
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onPhotoTaken() {
        state.onPhotoCaptured()
        if (state.canDismiss()) {
            recordDismissalBestEffort(
                "photo_motion", verified = true,
                motionSatisfied = state.motionSatisfied(), photoCaptured = true,
            )
            setResult(RESULT_OK, Intent().putExtra(DismissalContract.EXTRA_VERIFIED, true))
            finish()
        } else {
            statusText = "Motion not yet verified - keep moving around, then try the photo again."
        }
    }
}

@Composable
private fun PhotoMotionScreen(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    motionSatisfied: Boolean,
    statusText: String,
    onCapture: () -> Unit,
    captureRequested: Boolean,
    onCaptureHandled: () -> Unit,
    onPhotoTaken: () -> Unit,
) {
    LaunchedEffect(Unit) { if (!hasCameraPermission) onRequestPermission() }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Motion + photo to dismiss", style = MaterialTheme.typography.headlineSmall)
            Text(if (motionSatisfied) "Motion verified." else "Waiting for genuine motion - walk a few steps.")
            if (hasCameraPermission) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    PhotoMotionCameraPreview(
                        captureRequested = captureRequested,
                        onCaptureHandled = onCaptureHandled,
                        onPhotoTaken = onPhotoTaken,
                    )
                }
                Button(onClick = onCapture, modifier = Modifier.fillMaxWidth()) { Text("Take photo") }
            } else {
                Text("Camera permission is needed for this dismissal method.", Modifier.weight(1f))
                Button(onClick = onRequestPermission) { Text("Grant camera access") }
            }
            Text(statusText)
        }
    }
}

@Composable
private fun PhotoMotionCameraPreview(
    captureRequested: Boolean,
    onCaptureHandled: () -> Unit,
    onPhotoTaken: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }
                val capture = ImageCapture.Builder().build()
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, capture)
                    imageCapture = capture
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
    LaunchedEffect(captureRequested) {
        if (captureRequested) {
            val capture = imageCapture
            if (capture != null) {
                capture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            image.close() // in-memory only - proves the camera flow ran, nothing is persisted
                            onPhotoTaken()
                        }
                        override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                            // Leave capture ungated on failure; the user can just press "Take photo" again.
                        }
                    },
                )
            }
            onCaptureHandled()
        }
    }
}

// Fire-and-forget, mirrors QrDismissalActivity's helper - see its comment for why this must never
// block or gate the local dismissal decision.
private fun recordDismissalBestEffort(method: String, verified: Boolean, motionSatisfied: Boolean, photoCaptured: Boolean) {
    val api = ShanivarSession.api ?: return
    val detail = org.json.JSONObject()
        .put("motion_satisfied", motionSatisfied)
        .put("photo_captured", photoCaptured)
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        runCatching { HttpDisciplineApi(api).recordDismissal(method, verified, detail) }
    }
}
