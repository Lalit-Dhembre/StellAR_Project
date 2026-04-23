package com.cosmic_struck.stellar.stellar.pdfar.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmic_struck.stellar.stellar.pdfar.data.models.Concept
import com.cosmic_struck.stellar.stellar.pdfar.data.models.NativeImage
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

    companion object {
        private const val TAG = "PdfArVM"
    }

    private val _uiState = MutableStateFlow<PdfArUiState>(PdfArUiState.Idle)
    val uiState: StateFlow<PdfArUiState> = _uiState.asStateFlow()

    // Preserve the last successful content so we can return to it after errors
    private var lastContentState: PdfArUiState.ContentLoaded? = null

    private fun setState(newState: PdfArUiState, reason: String) {
        val oldState = _uiState.value
        Log.d(TAG, "STATE: ${oldState::class.simpleName} → ${newState::class.simpleName} | reason=$reason")
        _uiState.value = newState
    }

    fun processPdf(pdfFile: File, domain: String = "biology") {
        Log.d(TAG, "processPdf() called: file=${pdfFile.name}, domain=$domain")
        viewModelScope.launch {
            setState(PdfArUiState.Uploading, "Starting PDF upload")

            repository.processPdf(pdfFile, domain)
                .onSuccess { response ->
                    Log.d(TAG, "processPdf response: success=${response.success}, " +
                            "concepts=${response.concepts?.size ?: 0}, " +
                            "nativeImages=${response.nativeImages?.size ?: 0}, " +
                            "error=${response.error}")
                    if (response.success) {
                        val concepts = response.concepts ?: emptyList()
                        val nativeImages = response.nativeImages ?: emptyList()
                        val contentState = PdfArUiState.ContentLoaded(concepts, nativeImages)
                        lastContentState = contentState
                        setState(contentState, "PDF processed: ${concepts.size} concepts, ${nativeImages.size} images")
                    } else {
                        setState(PdfArUiState.Error(response.error ?: "Invalid PDF domain"), "Server rejected PDF")
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "processPdf FAILED", error)
                    setState(PdfArUiState.Error(error.message ?: "Network error processing PDF"), "Network failure")
                }
        }
    }

    fun fetchConceptDetails(conceptId: String, entityName: String) {
        Log.d(TAG, "fetchConceptDetails() called: id=$conceptId, entity=$entityName")
        viewModelScope.launch {
            setState(PdfArUiState.GeneratingModel, "Fetching concept details for '$entityName'")

            repository.getConceptDetails(conceptId)
                .onSuccess { response ->
                    Log.d(TAG, "concept-details response: title=${response.title}, " +
                            "modelStatus=${response.modelStatus}, " +
                            "modelUrl=${response.modelUrl}, " +
                            "hasScript=${response.script != null}")
                    when {
                        response.modelStatus == "ready" && response.modelUrl != null -> {
                            setState(
                                PdfArUiState.ModelReady(
                                    modelUrl = response.modelUrl,
                                    entityName = response.title,
                                    script = response.script
                                ),
                                "Model ready for '${response.title}'"
                            )
                        }
                        response.modelStatus == "generating" || response.modelStatus == "processing" -> {
                            Log.d(TAG, "Model generating — starting poll for '$entityName'")
                            pollConceptModel(conceptId, entityName)
                        }
                        response.modelStatus == "not_available" -> {
                            setState(
                                PdfArUiState.Error("3D model not available for '$entityName'"),
                                "Model not available"
                            )
                        }
                        else -> {
                            setState(
                                PdfArUiState.Error("Unexpected model status: ${response.modelStatus}"),
                                "Unknown status: ${response.modelStatus}"
                            )
                        }
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "fetchConceptDetails FAILED for '$entityName'", error)
                    setState(
                        PdfArUiState.Error(error.message ?: "Network error fetching concept details"),
                        "Network failure for concept details"
                    )
                }
        }
    }

    fun handleNativeImageSelection(imageUrl: String, title: String) {
        Log.d(TAG, "handleNativeImageSelection(): title=$title, url=$imageUrl")
        viewModelScope.launch {
            setState(PdfArUiState.GeneratingModel, "Processing native image '$title'")
            delay(1500)
            setState(
                PdfArUiState.ModelReady(
                    modelUrl = "stub_model_url",
                    entityName = title,
                    script = "This is a native diagram extracted exactly from your uploaded PDF file."
                ),
                "Native image stub ready"
            )
        }
    }

    private fun pollConceptModel(conceptId: String, entityName: String) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 120
            Log.d(TAG, "pollConceptModel() started for '$entityName' (max $maxAttempts attempts)")

            while (attempts < maxAttempts) {
                delay(5000)
                attempts++
                Log.d(TAG, "Poll attempt $attempts/$maxAttempts for '$entityName'")

                repository.getConceptDetails(conceptId)
                    .onSuccess { response ->
                        Log.d(TAG, "Poll response: status=${response.modelStatus}, url=${response.modelUrl}")
                        when (response.modelStatus) {
                            "ready" -> {
                                if (response.modelUrl != null) {
                                    setState(
                                        PdfArUiState.ModelReady(
                                            modelUrl = response.modelUrl,
                                            entityName = response.title,
                                            script = response.script
                                        ),
                                        "Poll: model ready after $attempts attempts"
                                    )
                                    return@launch
                                }
                            }
                            "not_available" -> {
                                setState(
                                    PdfArUiState.Error("3D model not available for '$entityName'"),
                                    "Poll: model became not_available"
                                )
                                return@launch
                            }
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Poll FAILED for '$entityName' at attempt $attempts", error)
                        setState(
                            PdfArUiState.Error(error.message ?: "Error polling model status"),
                            "Poll network failure at attempt $attempts"
                        )
                        return@launch
                    }
            }
            setState(
                PdfArUiState.Error("Model generation timed out for '$entityName'"),
                "Poll: timed out after $maxAttempts attempts"
            )
        }
    }

    /**
     * Reset state back to the last content view, or Idle if no content was loaded.
     * Called after navigation to AR viewer.
     */
    fun resetState() {
        val current = _uiState.value
        Log.d(TAG, "resetState() called, current=${current::class.simpleName}")
        if (current is PdfArUiState.Uploading || current is PdfArUiState.GeneratingModel) {
            Log.d(TAG, "resetState() blocked — operation in progress")
            return
        }
        // Return to content list instead of Idle so user sees their concepts again
        val target = lastContentState ?: PdfArUiState.Idle
        setState(target, "resetState → ${target::class.simpleName}")
    }

    /**
     * Clear error state and return to the last content view (not Idle).
     * This prevents losing the concept list after a transient error.
     */
    fun clearError() {
        Log.d(TAG, "clearError() called, current=${_uiState.value::class.simpleName}")
        if (_uiState.value is PdfArUiState.Error) {
            val target = lastContentState ?: PdfArUiState.Idle
            setState(target, "clearError → ${target::class.simpleName}")
        }
    }
}
