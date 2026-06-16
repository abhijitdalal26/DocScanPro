package com.abhijit.docscanpro.ui.screens.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhijit.docscanpro.data.db.AppDatabase
import com.abhijit.docscanpro.data.model.Document
import com.abhijit.docscanpro.data.model.DocumentType
import com.abhijit.docscanpro.data.repository.DocumentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    SIZE_DESC("Largest first")
}

data class LibraryUiState(
    val documents: List<Document> = emptyList(),
    val filteredDocuments: List<Document> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val selectedType: DocumentType? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(AppDatabase.getDatabase(application))

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var ocrDocumentIds: Set<Long> = emptySet()
    private var ocrSearchJob: Job? = null

    init {
        observeDocuments()
    }

    private fun observeDocuments() {
        viewModelScope.launch {
            try {
                repository.getAllDocuments().collect { docs ->
                    _uiState.update { state ->
                        val sorted = applySort(docs, state.sortOrder)
                        val filtered = applyFilter(sorted, state.searchQuery, state.selectedType)
                        state.copy(documents = sorted, filteredDocuments = filtered, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.documents, query, state.selectedType)
            state.copy(searchQuery = query, filteredDocuments = filtered)
        }
        ocrSearchJob?.cancel()
        if (query.isNotBlank()) {
            ocrSearchJob = viewModelScope.launch {
                repository.searchInOcrText(query).collect { pages ->
                    ocrDocumentIds = pages.map { it.documentId }.toSet()
                    _uiState.update { state ->
                        state.copy(filteredDocuments = applyFilter(state.documents, query, state.selectedType))
                    }
                }
            }
        } else {
            ocrDocumentIds = emptySet()
        }
    }

    fun setSearchQuery(query: String) = search(query)

    fun setTypeFilter(typeName: String?) {
        val type = typeName?.let { runCatching { DocumentType.valueOf(it) }.getOrNull() }
        filterByType(type)
    }

    fun enterSelectionMode(id: Long) {
        _uiState.update { it.copy(isSelectionMode = true, selectedIds = setOf(id)) }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { state ->
            val sorted = applySort(state.documents, order)
            val filtered = applyFilter(sorted, state.searchQuery, state.selectedType)
            state.copy(sortOrder = order, documents = sorted, filteredDocuments = filtered)
        }
    }

    fun filterByType(type: DocumentType?) {
        _uiState.update { state ->
            val filtered = applyFilter(state.documents, state.searchQuery, type)
            state.copy(selectedType = type, filteredDocuments = filtered)
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newIds = state.selectedIds.toMutableSet()
            if (id in newIds) newIds.remove(id) else newIds.add(id)
            state.copy(selectedIds = newIds, isSelectionMode = newIds.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isSelectionMode = false) }
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveToRecycleBinBatch(ids)
            clearSelection()
        }
    }

    fun toggleFavorite(documentId: Long, current: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(documentId, !current)
        }
    }

    private fun applySort(docs: List<Document>, order: SortOrder): List<Document> = when (order) {
        SortOrder.DATE_DESC -> docs.sortedByDescending { it.updatedAt }
        SortOrder.DATE_ASC -> docs.sortedBy { it.updatedAt }
        SortOrder.NAME_ASC -> docs.sortedBy { it.name.lowercase() }
        SortOrder.NAME_DESC -> docs.sortedByDescending { it.name.lowercase() }
        SortOrder.SIZE_DESC -> docs.sortedByDescending { it.totalSizeBytes }
    }

    private fun applyFilter(docs: List<Document>, query: String, type: DocumentType?): List<Document> {
        var result = docs
        if (query.isNotBlank()) {
            // Search by document name OR OCR text (via Page IDs lookup from ocrDocumentIds cache)
            result = result.filter { doc ->
                doc.name.contains(query, ignoreCase = true) ||
                    ocrDocumentIds.contains(doc.id)
            }
        }
        if (type != null) {
            result = result.filter { it.documentType == type.name }
        }
        return result
    }
}
