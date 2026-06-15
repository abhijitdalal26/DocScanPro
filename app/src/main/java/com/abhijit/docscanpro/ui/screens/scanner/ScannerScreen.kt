@file:OptIn(ExperimentalMaterial3Api::class)

package com.abhijit.docscanpro.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.abhijit.docscanpro.camera.CameraManager
import com.abhijit.docscanpro.data.model.ColorMode
import com.abhijit.docscanpro.ui.navigation.Screen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    navController: NavHostController,
    viewModel: ScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val cameraManager = remember { CameraManager(context) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var documentName by remember { mutableStateOf("Scan ${System.currentTimeMillis() / 1000}") }

    // Launch permission request if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Start camera when preview view is ready and permission is granted
    LaunchedEffect(hasCameraPermission, previewView) {
        val pv = previewView
        if (hasCameraPermission && pv != null) {
            cameraManager.startCamera(lifecycleOwner, pv)
        }
    }

    // Navigate to viewer when document is saved
    LaunchedEffect(uiState.savedDocumentId) {
        val id = uiState.savedDocumentId
        if (id != null) {
            navController.navigate(Screen.DocumentViewer.createRoute(id)) {
                popUpTo(Screen.Scanner.route) { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraManager.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!hasCameraPermission) {
            // Permission denied state
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Camera permission required", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        } else {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        pv.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = pv
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top bar: back + torch + page count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }

                if (uiState.capturedPages.isNotEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "${uiState.capturedPages.size} page${if (uiState.capturedPages.size > 1) "s" else ""}",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = {
                        cameraManager.toggleTorch()
                        viewModel.setTorchState(!uiState.isTorchOn)
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        "Torch",
                        tint = if (uiState.isTorchOn) Color.Yellow else Color.White
                    )
                }
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Color mode selector
                ColorModeSelector(
                    selected = uiState.selectedColorMode,
                    onSelect = viewModel::setColorMode
                )

                Spacer(Modifier.height(12.dp))

                // Page thumbnails strip
                if (uiState.capturedPages.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.capturedPages) { index, _ ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp, 72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable { viewModel.removePage(index) },
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Text(
                                    "${index + 1}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Capture row: Done button (left) + Capture (center) + Retry (right)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Done / save
                    if (uiState.capturedPages.isNotEmpty()) {
                        IconButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.Default.Check, "Save document", tint = Color.White)
                        }
                    } else {
                        Spacer(Modifier.size(56.dp))
                    }

                    // Capture button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.White, CircleShape)
                            .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .clickable(enabled = !uiState.isProcessing) {
                                val tempFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                                cameraManager.capturePhoto(
                                    tempFile,
                                    onCaptured = { bitmap -> viewModel.addPage(bitmap) },
                                    onError = {}
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.Black
                            )
                        } else {
                            Icon(Icons.Default.CameraAlt, "Capture", tint = Color.Black, modifier = Modifier.size(32.dp))
                        }
                    }

                    // Placeholder for symmetry or gallery picker future
                    Spacer(Modifier.size(56.dp))
                }
            }
        }
    }

    // Save dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Document") },
            text = {
                OutlinedTextField(
                    value = documentName,
                    onValueChange = { documentName = it },
                    label = { Text("Document name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    viewModel.saveDocument(documentName.ifBlank { "Scan" })
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ColorModeSelector(
    selected: ColorMode,
    onSelect: (ColorMode) -> Unit
) {
    val modes = listOf(
        ColorMode.ORIGINAL to "Auto",
        ColorMode.MAGIC_COLOR to "Magic",
        ColorMode.BLACK_WHITE to "B&W",
        ColorMode.GRAYSCALE to "Gray",
        ColorMode.ENHANCED to "Vivid"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = mode == selected
            Surface(
                onClick = { onSelect(mode) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
