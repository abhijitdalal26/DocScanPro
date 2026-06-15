package com.abhijit.docscanpro.ui.screens.scanner

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhijit.docscanpro.data.db.AppDatabase
import com.abhijit.docscanpro.data.model.ColorMode
import com.abhijit.docscanpro.data.model.Page
import com.abhijit.docscanpro.data.repository.DocumentRepository
import com.abhijit.docscanpro.ocr.OcrEngine
import com.abhijit.docscanpro.pdf.PdfCreator
import com.abhijit.docscanpro.pdf.PdfQuality
import com.abhijit.docscanpro.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val capturedPages: List<Bitmap> = emptyList(),
    val selectedColorMode: ColorMode = ColorMode.ORIGINAL,
    val isTorchOn: Boolean = false,
    val isProcessing: Boolean = false,
    val savedDocumentId: Long? = null,
    val error: String? = null
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository = DocumentRepository(AppDatabase.getDatabase(application))
    private val ocrEngine = OcrEngine()
    private val pdfCreator = PdfCreator(context)

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun addPage(bitmap: Bitmap) {
        _uiState.update { it.copy(capturedPages = it.capturedPages + bitmap) }
    }

    fun removePage(index: Int) {
        val pages = _uiState.value.capturedPages.toMutableList()
        if (index in pages.indices) pages.removeAt(index)
        _uiState.update { it.copy(capturedPages = pages) }
    }

    fun setColorMode(mode: ColorMode) {
        _uiState.update { it.copy(selectedColorMode = mode) }
    }

    fun setTorchState(on: Boolean) {
        _uiState.update { it.copy(isTorchOn = on) }
    }

    fun saveDocument(documentName: String) {
        val pages = _uiState.value.capturedPages
        if (pages.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val documentId = repository.createDocument(documentName)
                val pageEntities = mutableListOf<Page>()

                pages.forEachIndexed { index, bitmap ->
                    val imagePath = FileUtils.getPageImagePath(context, documentId, index + 1)
                    FileUtils.saveBitmap(bitmap, imagePath)

                    val ocrText = try {
                        ocrEngine.recognizeText(bitmap).fullText
                    } catch (e: Exception) {
                        null
                    }

                    pageEntities.add(
                        Page(
                            documentId = documentId,
                            pageNumber = index + 1,
                            imagePath = imagePath,
                            ocrText = ocrText,
                            colorMode = _uiState.value.selectedColorMode.name,
                            imageWidth = bitmap.width,
                            imageHeight = bitmap.height
                        )
                    )
                }

                repository.addPages(pageEntities)

                val pdfPath = FileUtils.getPdfPath(context, documentId, documentName)
                pdfCreator.createPdfFromImages(pages, pdfPath, PdfQuality.HIGH)
                    .onSuccess { repository.updatePdfPath(documentId, pdfPath) }

                val thumbnailPath = FileUtils.getThumbnailPath(context, documentId)
                pdfCreator.createThumbnail(pages.first(), thumbnailPath)
                    .onSuccess { repository.updateThumbnailPath(documentId, thumbnailPath) }

                val totalSize = FileUtils.getDocumentTotalSize(context, documentId)
                repository.updateSize(documentId, totalSize)

                _uiState.update { it.copy(isProcessing = false, savedDocumentId = documentId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    fun clearSession() {
        _uiState.value.capturedPages.forEach { it.recycle() }
        _uiState.update { ScannerUiState() }
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.close()
    }
}
