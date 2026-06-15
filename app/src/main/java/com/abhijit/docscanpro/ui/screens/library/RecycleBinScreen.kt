@file:OptIn(ExperimentalMaterial3Api::class)

package com.abhijit.docscanpro.ui.screens.library

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.abhijit.docscanpro.data.db.AppDatabase
import com.abhijit.docscanpro.data.model.Document
import com.abhijit.docscanpro.data.repository.DocumentRepository
import com.abhijit.docscanpro.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class RecycleBinUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = true
)

class RecycleBinViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository = DocumentRepository(AppDatabase.getDatabase(application))

    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRecycleBin().collect { docs ->
                _uiState.update { it.copy(documents = docs, isLoading = false) }
            }
        }
    }

    fun restore(document: Document) {
        viewModelScope.launch { repository.restoreFromRecycleBin(document.id) }
    }

    fun permanentlyDelete(document: Document) {
        viewModelScope.launch {
            repository.permanentlyDelete(document)
            FileUtils.deleteDocumentFiles(context, document.id)
        }
    }

    fun emptyBin() {
        val docs = _uiState.value.documents
        viewModelScope.launch {
            docs.forEach { doc ->
                repository.permanentlyDelete(doc)
                FileUtils.deleteDocumentFiles(context, doc.id)
            }
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun RecycleBinScreen(
    navController: NavHostController,
    viewModel: RecycleBinViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEmptyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.documents.isNotEmpty()) {
                        IconButton(onClick = { showEmptyDialog = true }) {
                            Icon(Icons.Default.DeleteForever, "Empty bin", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.documents.isEmpty()) {
            Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("Recycle bin is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(uiState.documents, key = { it.id }) { doc ->
                    BinDocCard(
                        document = doc,
                        onRestore = { viewModel.restore(doc) },
                        onDelete = { viewModel.permanentlyDelete(doc) }
                    )
                }
            }
        }
    }

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("Empty Recycle Bin?") },
            text = { Text("All ${uiState.documents.size} documents will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyBin(); showEmptyDialog = false }) {
                    Text("Empty Bin", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BinDocCard(document: Document, onRestore: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                if (document.thumbnailPath != null) {
                    AsyncImage(
                        model = document.thumbnailPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(document.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    SimpleDateFormat("d MMM", Locale.ENGLISH).format(Date(document.updatedAt)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onRestore, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Restore", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
