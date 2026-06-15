@file:OptIn(ExperimentalMaterial3Api::class)

package com.abhijit.docscanpro.ui.screens.viewer

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.abhijit.docscanpro.data.db.AppDatabase
import com.abhijit.docscanpro.data.repository.DocumentRepository
import com.abhijit.docscanpro.pdf.PdfEditor
import com.abhijit.docscanpro.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PdfToolsUiState(
    val isProcessing: Boolean = false,
    val resultMessage: String? = null,
    val error: String? = null
)

class PdfToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val pdfEditor = PdfEditor()
    private val repository = DocumentRepository(AppDatabase.getDatabase(application))

    private val _uiState = MutableStateFlow(PdfToolsUiState())
    val uiState: StateFlow<PdfToolsUiState> = _uiState.asStateFlow()

    fun compressPdf(documentId: Long, quality: Int = 60) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, resultMessage = null, error = null) }
            val doc = repository.getDocumentById(documentId)
            val pdfPath = doc?.pdfPath
            if (pdfPath == null || !File(pdfPath).exists()) {
                _uiState.update { it.copy(isProcessing = false, error = "PDF not found") }
                return@launch
            }
            val outputPath = pdfPath.replace(".pdf", "_compressed.pdf")
            pdfEditor.compressPdf(context, pdfPath, outputPath, quality)
                .onSuccess { file ->
                    val origSize = FileUtils.getFileSizeBytes(pdfPath)
                    val newSize = file.length()
                    val saved = origSize - newSize
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            resultMessage = "Saved ${FileUtils.formatFileSize(saved)} (${FileUtils.formatFileSize(newSize)} total)"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isProcessing = false, error = e.message) }
                }
        }
    }

    fun splitPdf(documentId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, resultMessage = null) }
            val doc = repository.getDocumentById(documentId)
            val pdfPath = doc?.pdfPath
            if (pdfPath == null || !File(pdfPath).exists()) {
                _uiState.update { it.copy(isProcessing = false, error = "PDF not found") }
                return@launch
            }
            val outputDir = FileUtils.getExportCacheDir(context).absolutePath
            pdfEditor.splitPdf(pdfPath, outputDir)
                .onSuccess { files ->
                    _uiState.update { it.copy(isProcessing = false, resultMessage = "Split into ${files.size} pages in cache") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isProcessing = false, error = e.message) }
                }
        }
    }

    fun addPageNumbers(documentId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, resultMessage = null, error = null) }
            val doc = repository.getDocumentById(documentId)
            val pdfPath = doc?.pdfPath
            if (pdfPath == null || !File(pdfPath).exists()) {
                _uiState.update { it.copy(isProcessing = false, error = "PDF not found") }
                return@launch
            }
            val outputPath = pdfPath.replace(".pdf", "_numbered.pdf")
            pdfEditor.addPageNumbers(context, pdfPath, outputPath)
                .onSuccess { _uiState.update { it.copy(isProcessing = false, resultMessage = "Page numbers added successfully") } }
                .onFailure { e -> _uiState.update { it.copy(isProcessing = false, error = e.message) } }
        }
    }

    fun addWatermark(documentId: Long, watermarkText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, resultMessage = null, error = null) }
            val doc = repository.getDocumentById(documentId)
            val pdfPath = doc?.pdfPath
            if (pdfPath == null || !File(pdfPath).exists()) {
                _uiState.update { it.copy(isProcessing = false, error = "PDF not found") }
                return@launch
            }
            val outputPath = pdfPath.replace(".pdf", "_watermarked.pdf")
            pdfEditor.addWatermarkToPdf(context, pdfPath, outputPath, watermarkText)
                .onSuccess { _uiState.update { it.copy(isProcessing = false, resultMessage = "Watermark added successfully") } }
                .onFailure { e -> _uiState.update { it.copy(isProcessing = false, error = e.message) } }
        }
    }

    fun clearResult() = _uiState.update { it.copy(resultMessage = null, error = null) }
}

@Composable
fun PdfToolsScreen(
    documentId: Long,
    navController: NavHostController,
    viewModel: PdfToolsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCompressDialog by remember { mutableStateOf(false) }
    var compressionQuality by remember { mutableStateOf(60f) }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }

    LaunchedEffect(uiState.resultMessage) {
        if (uiState.resultMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Tools") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = {
            if (uiState.resultMessage != null || uiState.error != null) {
                SnackbarHost(hostState = remember { SnackbarHostState() }.also { host ->
                    LaunchedEffect(uiState.resultMessage, uiState.error) {
                        val msg = uiState.resultMessage ?: uiState.error ?: return@LaunchedEffect
                        host.showSnackbar(msg)
                    }
                })
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())
        ) {
            PdfToolsSection("Compression") {
                PdfToolItem(
                    icon = Icons.Default.Compress,
                    title = "Compress PDF",
                    subtitle = "Reduce file size by re-encoding images",
                    isLoading = uiState.isProcessing,
                    onClick = { showCompressDialog = true }
                )
            }

            PdfToolsSection("Pages") {
                PdfToolItem(
                    icon = Icons.Default.CallSplit,
                    title = "Split PDF",
                    subtitle = "Save each page as a separate PDF",
                    isLoading = uiState.isProcessing,
                    onClick = { viewModel.splitPdf(documentId) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                PdfToolItem(
                    icon = Icons.Default.FormatListNumbered,
                    title = "Add Page Numbers",
                    subtitle = "Stamp page numbers at the bottom of each page",
                    isLoading = uiState.isProcessing,
                    onClick = { viewModel.addPageNumbers(documentId) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                PdfToolItem(
                    icon = Icons.Default.MergeType,
                    title = "Merge PDFs",
                    subtitle = "Coming soon — merge multiple documents",
                    isLoading = false,
                    onClick = {},
                    enabled = false
                )
            }

            PdfToolsSection("Security") {
                PdfToolItem(
                    icon = Icons.Default.Lock,
                    title = "Password Protect",
                    subtitle = "Add AES-256 password to this PDF",
                    isLoading = uiState.isProcessing,
                    onClick = { navController.popBackStack() } // Handled in DocumentViewerScreen
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                PdfToolItem(
                    icon = Icons.Default.Watermark,
                    title = "Add Watermark",
                    subtitle = "Overlay diagonal text watermark on all pages",
                    isLoading = uiState.isProcessing,
                    onClick = { showWatermarkDialog = true }
                )
            }

            if (uiState.resultMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.resultMessage!!, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }

    // Compress quality dialog
    if (showCompressDialog) {
        AlertDialog(
            onDismissRequest = { showCompressDialog = false },
            title = { Text("Compression Quality") },
            text = {
                Column {
                    Text("Lower quality = smaller file size")
                    Spacer(Modifier.height(16.dp))
                    Slider(
                        value = compressionQuality,
                        onValueChange = { compressionQuality = it },
                        valueRange = 20f..90f,
                        steps = 6
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Smaller", fontSize = 12.sp)
                        Text("${compressionQuality.toInt()}%", fontWeight = FontWeight.Bold)
                        Text("Better quality", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showCompressDialog = false
                    viewModel.compressPdf(documentId, compressionQuality.toInt())
                }) { Text("Compress") }
            },
            dismissButton = {
                TextButton(onClick = { showCompressDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Watermark text dialog
    if (showWatermarkDialog) {
        AlertDialog(
            onDismissRequest = { showWatermarkDialog = false },
            title = { Text("Watermark Text") },
            text = {
                Column {
                    Text("Enter the text to stamp diagonally across each page")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        label = { Text("Watermark text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showWatermarkDialog = false
                    viewModel.addWatermark(documentId, watermarkText)
                }) { Text("Add Watermark") }
            },
            dismissButton = {
                TextButton(onClick = { showWatermarkDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PdfToolsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(content = content)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PdfToolItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        leadingContent = { Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Icon(Icons.Default.ChevronRight, null)
        },
        modifier = Modifier.clickable(enabled = enabled && !isLoading, onClick = onClick)
    )
}
