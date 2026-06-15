@file:OptIn(ExperimentalMaterial3Api::class)

package com.abhijit.docscanpro.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }

    var scannedResult by remember { mutableStateOf<BarcodeResult?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
            Text(
                "Scan QR / Barcode",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (!hasCameraPermission) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission required", color = Color.White)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        } else {
            // Camera preview with ML Kit barcode analysis
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val analysisExecutor = Executors.newSingleThreadExecutor()
                    val barcodeScanner = BarcodeScanning.getClient()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { imageAnalysis ->
                                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    if (!isScanning) { imageProxy.close(); return@setAnalyzer }
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                val first = barcodes.firstOrNull { it.rawValue != null }
                                                if (first != null) {
                                                    isScanning = false
                                                    scannedResult = BarcodeResult(
                                                        rawValue = first.rawValue ?: "",
                                                        displayValue = first.displayValue ?: first.rawValue ?: "",
                                                        format = formatName(first.format),
                                                        type = typeName(first.valueType)
                                                    )
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scan overlay frame
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.Transparent)
                ) {
                    // Corners
                    val cornerColor = if (scannedResult != null) Color.Green else Color.White
                    val cornerSize = 24.dp
                    val cornerStroke = 3.dp
                    // top-left
                    Box(Modifier.width(cornerStroke).height(cornerSize).align(Alignment.TopStart).background(cornerColor))
                    Box(Modifier.height(cornerStroke).width(cornerSize).align(Alignment.TopStart).background(cornerColor))
                    // top-right
                    Box(Modifier.width(cornerStroke).height(cornerSize).align(Alignment.TopEnd).background(cornerColor))
                    Box(Modifier.height(cornerStroke).width(cornerSize).align(Alignment.TopEnd).background(cornerColor))
                    // bottom-left
                    Box(Modifier.width(cornerStroke).height(cornerSize).align(Alignment.BottomStart).background(cornerColor))
                    Box(Modifier.height(cornerStroke).width(cornerSize).align(Alignment.BottomStart).background(cornerColor))
                    // bottom-right
                    Box(Modifier.width(cornerStroke).height(cornerSize).align(Alignment.BottomEnd).background(cornerColor))
                    Box(Modifier.height(cornerStroke).width(cornerSize).align(Alignment.BottomEnd).background(cornerColor))
                }
            }

            // Hint text
            if (scannedResult == null) {
                Text(
                    "Align barcode or QR code within frame",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 140.dp)
                        .padding(horizontal = 32.dp)
                )
            }
        }

        // Result bottom sheet
        val result = scannedResult
        if (result != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(result.format, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            Text(result.type, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { scannedResult = null; isScanning = true }) {
                            Text("Scan Again")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        result.displayValue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val ctx = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val clipboard = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("barcode", result.rawValue))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy")
                        }
                        Button(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                try {
                                    intent.data = android.net.Uri.parse(result.rawValue)
                                    ctx.startActivity(intent)
                                } catch (e: Exception) { /* not a URL */ }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open")
                        }
                    }
                }
            }
        }
    }
}

data class BarcodeResult(
    val rawValue: String,
    val displayValue: String,
    val format: String,
    val type: String
)

private fun formatName(format: Int) = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR Code"
    Barcode.FORMAT_CODE_128 -> "Code 128"
    Barcode.FORMAT_CODE_39 -> "Code 39"
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
    Barcode.FORMAT_AZTEC -> "Aztec"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_UPC_E -> "UPC-E"
    else -> "Barcode"
}

private fun typeName(type: Int) = when (type) {
    Barcode.TYPE_URL -> "URL"
    Barcode.TYPE_EMAIL -> "Email"
    Barcode.TYPE_PHONE -> "Phone"
    Barcode.TYPE_SMS -> "SMS"
    Barcode.TYPE_CONTACT_INFO -> "Contact"
    Barcode.TYPE_GEO -> "Location"
    Barcode.TYPE_WIFI -> "Wi-Fi"
    Barcode.TYPE_TEXT -> "Text"
    Barcode.TYPE_PRODUCT -> "Product"
    else -> "Data"
}
