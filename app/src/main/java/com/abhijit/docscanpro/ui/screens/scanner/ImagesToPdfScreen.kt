@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.abhijit.docscanpro.ui.screens.scanner

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.abhijit.docscanpro.data.db.AppDatabase
import com.abhijit.docscanpro.data.repository.DocumentRepository
import com.abhijit.docscanpro.pdf.PdfCreator
import com.abhijit.docscanpro.pdf.PdfQuality
import com.abhijit.docscanpro.ui.navigation.Screen
import com.abhijit.docscanpro.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImgToPdfUiState(
    val images: List<Bitmap> = emptyList(),
    val isProcessing: Boolean = false,
    val savedDocumentId: Long? = null,
    val error: String? = null
)

class ImagesToPdfViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val repository = DocumentRepository(AppDatabase.getDatabase(application))
    private val pdfCreator = PdfCreator(context)

    private val _uiState = MutableStateFlow(ImgToPdfUiState())
    val uiState: StateFlow<ImgToPdfUiState> = _uiState.asStateFlow()

    fun addImages(bitmaps: List<Bitmap>) {
        _uiState.update { it.copy(images = it.images + bitmaps) }
    }

    fun removeImage(index: Int) {
        val list = _uiState.value.images.toMutableList()
        if (index in list.indices) list.removeAt(index)
        _uiState.update { it.copy(images = list) }
    }

    fun createPdf(documentName: String) {
        val images = _uiState.value.images
        if (images.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val documentId = repository.createDocument(documentName)
                val pdfPath = FileUtils.getPdfPath(context, documentId, documentName)
                pdfCreator.createPdfFromImages(images, pdfPath, PdfQuality.HIGH)
                    .onSuccess { repository.updatePdfPath(documentId, pdfPath) }
                val thumbPath = FileUtils.getThumbnailPath(context, documentId)
                pdfCreator.createThumbnail(images.first(), thumbPath)
                    .onSuccess { repository.updateThumbnailPath(documentId, thumbPath) }
                repository.updateSize(documentId, FileUtils.getDocumentTotalSize(context, documentId))
                _uiState.update { it.copy(isProcessing = false, savedDocumentId = documentId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value.images.forEach { it.recycle() }
    }
}

@Composable
fun ImagesToPdfScreen(
    navController: NavHostController,
    viewModel: ImagesToPdfViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var documentName by remember { mutableStateOf("Gallery Import") }
    var showSaveDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(GetMultipleContents()) { uris ->
        val bitmaps = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
        if (bitmaps.isNotEmpty()) viewModel.addImages(bitmaps)
    }

    LaunchedEffect(uiState.savedDocumentId) {
        val id = uiState.savedDocumentId ?: return@LaunchedEffect
        navController.navigate(Screen.DocumentViewer.createRoute(id)) {
            popUpTo(Screen.ImagesToPdf.route) { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Images to PDF") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.images.isNotEmpty()) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Add, "Add images")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.images.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showSaveDialog = true },
                    icon = { Icon(Icons.Default.PictureAsPdf, null) },
                    text = { Text("Create PDF (${uiState.images.size})") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.images.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No images selected", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pick images from your gallery to combine into a PDF",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select Images")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "${uiState.images.size} image${if (uiState.images.size > 1) "s" else ""} selected — tap to remove",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    itemsIndexed(uiState.images) { index, bitmap ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.removeImage(index) }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Page ${index + 1}", fontWeight = FontWeight.Medium)
                                    Text(
                                        "${bitmap.width} × ${bitmap.height} px",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.RemoveCircleOutline,
                                    "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            if (uiState.isProcessing) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save as PDF") },
            text = {
                OutlinedTextField(
                    value = documentName,
                    onValueChange = { documentName = it },
                    label = { Text("Document name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSaveDialog = false
                    viewModel.createPdf(documentName.ifBlank { "Document" })
                }) { Text("Create PDF") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}
