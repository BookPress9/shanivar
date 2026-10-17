package com.shanivar.nativeapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * QR dismissal: live CameraX preview + ML Kit barcode analyzer, compared against this install's
 * fixed registered QR value (`QrRegistration`). Per the task spec this is a deterministic decode
 * check, not fuzzy image matching - the locked decision this squad rejected ML scene-matching in
 * favor of.
 */
class QrDismissalActivity : ComponentActivity() {
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraPermissionGranted = granted
    }
    private var cameraPermissionGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val expected = QrRegistration(this).valueFor(null)
        setContent {
            ShanivarTheme {
                QrDismissalScreen(
                    hasCameraPermission = cameraPermissionGranted,
                    onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    expectedPayload = expected,
                    onVerified = { finishVerified(it) },
                )
            }
        }
    }

    private fun finishVerified(detail: String) {
        recordDismissalBestEffort("qr", verified = true, detail = detail)
        setResult(RESULT_OK, Intent().putExtra(DismissalContract.EXTRA_VERIFIED, true))
        finish()
    }
}

// Fire-and-forget: per the task spec this call only records the event and never itself grants
// anything, so it must never block or be awaited before letting the (already locally-verified)
// user out of the alarm. Not tied to the Activity's lifecycleScope since the Activity finishes
// immediately after calling this.
private fun recordDismissalBestEffort(method: String, verified: Boolean, detail: String) {
    val api = ShanivarSession.api ?: return
    val detailJson = org.json.JSONObject().put("matched_payload", detail)
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        runCatching { HttpDisciplineApi(api).recordDismissal(method, verified, detailJson) }
    }
}

@Composable
private fun QrDismissalScreen(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    expectedPayload: String,
    onVerified: (String) -> Unit,
) {
    var status by remember { mutableStateOf("Point your camera at your dismissal QR code.") }
    LaunchedEffect(Unit) { if (!hasCameraPermission) onRequestPermission() }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Scan to dismiss", style = MaterialTheme.typography.headlineSmall)
            if (hasCameraPermission) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    QrCameraPreview(onPayloadDecoded = { payload ->
                        if (QrDismissalVerifier.matches(payload, expectedPayload)) {
                            status = "Verified."
                            onVerified("qr_decoded")
                        } else {
                            status = "That code doesn't match your registered dismissal code."
                        }
                    })
                }
            } else {
                Text("Camera permission is needed to scan your dismissal QR code.", Modifier.weight(1f))
                Button(onClick = onRequestPermission) { Text("Grant camera access") }
            }
            Text(status)
        }
    }
}

@Composable
private fun QrCameraPreview(onPayloadDecoded: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    DisposableEffect(Unit) { onDispose { scanner.close() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx), QrBarcodeAnalyzer(scanner, onPayloadDecoded))
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

/** Decodes each analyzed frame; `onPayloadDecoded` may be called repeatedly (once per matching
 * frame) until the caller acts on it and finishes the Activity. */
private class QrBarcodeAnalyzer(
    private val scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    private val onPayloadDecoded: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    @androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let(onPayloadDecoded)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
