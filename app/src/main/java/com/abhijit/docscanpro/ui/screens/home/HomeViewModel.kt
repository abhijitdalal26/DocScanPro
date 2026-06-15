package com.abhijit.docscanpro.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhijit.docscanpro.data.db.AppDatabase
import com.abhijit.docscanpro.data.model.Document
import com.abhijit.docscanpro.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentDocuments: List<Document> = emptyList(),
    val totalDocuments: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(AppDatabase.getDatabase(application))

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeDocuments()
    }

    private fun observeDocuments() {
        viewModelScope.launch {
            try {
                repository.getAllDocuments().collect { docs ->
                    _uiState.update { state ->
                        state.copy(
                            recentDocuments = docs.take(8),
                            totalDocuments = docs.size,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
