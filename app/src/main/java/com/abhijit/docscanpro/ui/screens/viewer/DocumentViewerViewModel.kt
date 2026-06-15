package com.abhijit.docscanpro.ui.screens.viewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhijit.docscanpro.data.db.AppDatabase
import com.abhijit.docscanpro.data.model.Document
import com.abhijit.docscanpro.data.model.Page
import com.abhijit.docscanpro.data.repository.DocumentRepository
import com.abhijit.docscanpro.export.ExportManager
import com.abhijit.docscanpro.pdf.PdfEditor
import com.abhijit.docscanpro.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ViewerUiState(
    val document: Document? = null,
    val pages: List<Page> = emptyList(),
    val currentPageIndex: Int = 0,
    val showOcrPanel: Boolean = false,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val error: String? = null
)

class DocumentViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository = DocumentRepository(AppDatabase.getDatabase(application))
    private val pdfEditor = PdfEditor()
    private val exportManager = ExportManager(context)

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val document = repository.getDocumentById(documentId)
            _uiState.update { it.copy(document = document, isLoading = false) }
        }
        viewModelScope.launch {
            repository.getPagesForDocument(documentId).collect { pages ->
                _uiState.update { it.copy(pages = pages) }
            }
        }
    }

    fun setCurrentPage(index: Int) = _uiState.update { it.copy(currentPageIndex = index) }

    fun toggleOcrPanel() = _uiState.update { it.copy(showOcrPanel = !it.showOcrPanel) }

    fun sharePdf() {
        val pdfPath = _uiState.value.document?.pdfPath ?: return
        val file = File(pdfPath)
        if (!file.exists()) return
        exportManager.shareFile(file, "application/pdf")
    }

    fun shareCurrentPageAsImage() {
        val page = _uiState.value.pages.getOrNull(_uiState.value.currentPageIndex) ?: return
        val file = File(page.imagePath)
        if (!file.exists()) return
        exportManager.shareFile(file, "image/jpeg")
    }

    fun toggleFavorite() {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            repository.setFavorite(doc.id, !doc.isFavorite)
            _uiState.update { it.copy(document = doc.copy(isFavorite = !doc.isFavorite)) }
        }
    }

    fun deleteDocument(onDeleted: () -> Unit) {
        val docId = _uiState.value.document?.id ?: return
        viewModelScope.launch {
            repository.moveToRecycleBin(docId)
            onDeleted()
        }
    }

    fun protectWithPassword(password: String) {
        val doc = _uiState.value.document ?: return
        val pdfPath = doc.pdfPath ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            pdfEditor.protectWithPassword(pdfPath, pdfPath, password)
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    fun copyOcrText() {
        val text = getCurrentOcrText()
        if (text.isNotEmpty()) exportManager.copyTextToClipboard(text)
    }

    fun getCurrentOcrText(): String =
        _uiState.value.pages.getOrNull(_uiState.value.currentPageIndex)?.ocrText ?: ""
}
