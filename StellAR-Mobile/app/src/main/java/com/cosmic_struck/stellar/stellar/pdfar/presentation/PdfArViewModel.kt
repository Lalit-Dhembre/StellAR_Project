package com.cosmic_struck.stellar.stellar.pdfar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmic_struck.stellar.stellar.pdfar.domain.repository.PdfArRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfArViewModel @Inject constructor(
    private val repository: PdfArRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PdfArUiState>(PdfArUiState.Idle)
    val uiState: StateFlow<PdfArUiState> = _uiState.asStateFlow()

    fun processPdf(pdfFile: File, domain: String = "biology") {
        viewModelScope.launch {
            _uiState.value = PdfArUiState.Uploading
            repository.processPdf(pdfFile, domain)
                .onSuccess { response ->
                    if (response.success && response.domain_match) {
                        val sections = response.documents
                            ?.filter { doc -> !doc.metadata?.entity.isNullOrBlank() }
                            ?.take(3)
                            ?.mapIndexed { index, doc ->
                                val entityName = doc.metadata?.entity ?: "Unknown Entity"
                                com.cosmic_struck.stellar.stellar.pdfar.data.models.Section(
                                    id = index.toString(),
                                    title = entityName,
                                    entities = listOf(entityName),
                                    imageUrl = doc.metadata?.image_url
                                )
                            } ?: emptyList()
                        _uiState.value = PdfArUiState.SectionsLoaded(sections)
                    } else {
                        _uiState.value = PdfArUiState.Error(response.message ?: "Invalid PDF domain")
                    }
                }
                .onFailure {
                    _uiState.value = PdfArUiState.Error(it.message ?: "Network error processing PDF")
                }
        }
    }

    fun resolveEntityAndPoll(entityName: String, cacheDir: File) {
        viewModelScope.launch {
            _uiState.value = PdfArUiState.GeneratingModel
            repository.resolveEntity(entityName)
                .onSuccess { response ->
                    if (response.status == "ready" && response.modelUrl != null) {
                        _uiState.value = PdfArUiState.ModelReady(response.modelUrl, entityName)
                    } else if (response.status == "processing" && response.taskId != null) {
                        pollTaskStatus(response.taskId, entityName, cacheDir)
                    } else {
                        _uiState.value = PdfArUiState.Error("Unexpected response resolving entity")
                    }
                }
                .onFailure {
                    _uiState.value = PdfArUiState.Error(it.message ?: "Network error resolving entity")
                }
        }
    }

    private suspend fun pollTaskStatus(taskId: String, entityName: String, cacheDir: File) {
        var isCompleted = false
        var attempts = 0
        while (!isCompleted && attempts < 900) { // Max 1 hour (900 * 4s)
            delay(4000) // Poll every 4 seconds
            repository.getTaskStatus(taskId)
                .onSuccess { response ->
                    if (response.status == "completed" && response.modelUrl != null) {
                        isCompleted = true
                        _uiState.value = PdfArUiState.ModelReady(response.modelUrl, entityName)
                    } else if (response.status == "failed") {
                        isCompleted = true
                        _uiState.value = PdfArUiState.Error("Model generation failed on server")
                    }
                }
                .onFailure {
                    isCompleted = true
                    _uiState.value = PdfArUiState.Error(it.message ?: "Error during polling")
                }
            attempts++
        }
        if (!isCompleted) {
            _uiState.value = PdfArUiState.Error("Model generation timeout")
        }
    }



    fun resetState() {
        if (_uiState.value !is PdfArUiState.Uploading && _uiState.value !is PdfArUiState.GeneratingModel) {
            _uiState.value = PdfArUiState.Idle
        }
    }
    
    fun clearError() {
        if (_uiState.value is PdfArUiState.Error) {
            _uiState.value = PdfArUiState.Idle
        }
    }
}
