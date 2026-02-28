package com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.stellar.scantext.domain.usecase.PdfUploadUseCase
import com.cosmic_struck.stellar.stellar.scantext.domain.usecase.PollJobStatusUseCase
import com.cosmic_struck.stellar.stellar.scantext.presentation.viewmodel.ScanImageScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ScanTextViewModel @Inject constructor(
    private val pdfUploadUseCase: PdfUploadUseCase,
    private val pollJobStatusUseCase: PollJobStatusUseCase,
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(ScanImageScreenState())
    val state: StateFlow<ScanImageScreenState> = _state.asStateFlow()

    /**
     * Called when user selects a PDF from the file picker.
     */
    fun onPdfSelected(uri: Uri, context: Context) {
        val fileName = getFileName(uri, context) ?: "document.pdf"
        Log.d("ScanTextViewModel", "PDF selected: $fileName")
        _state.value = _state.value.copy(
            selectedPdfUri = uri,
            selectedPdfName = fileName,
            isError = ""
        )
    }

    /**
     * Set the expected domain (Physics, Chemistry, Biology, Space)
     */
    fun setDomain(domain: String) {
        _state.value = _state.value.copy(domain = domain)
    }

    /**
     * Upload the selected PDF to the server for processing.
     * Triggers the full 5-phase pipeline.
     */
    fun uploadPdf(context: Context) {
        val uri = _state.value.selectedPdfUri ?: run {
            _state.value = _state.value.copy(isError = "No PDF selected")
            return
        }

        viewModelScope.launch {
            try {
                // Copy PDF from content URI to a temp file
                val tempFile = copyUriToTempFile(uri, context) ?: run {
                    _state.value = _state.value.copy(isError = "Failed to read the PDF file")
                    return@launch
                }

                // Build multipart request
                val requestBody = tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    _state.value.selectedPdfName ?: "document.pdf",
                    requestBody
                )
                val domainPart = _state.value.domain
                    .toRequestBody("text/plain".toMediaTypeOrNull())

                Log.d("ScanTextViewModel", "Uploading PDF: ${_state.value.selectedPdfName}")

                // Execute upload
                pdfUploadUseCase.invoke(filePart, domainPart).collect { result ->
                    when (result) {
                        is Resource.Loading<*> -> {
                            _state.value = _state.value.copy(isLoading = true)
                            Log.d("ScanTextViewModel", "LOADING")
                        }
                        is Resource.Error<*> -> {
                            _state.value = _state.value.copy(
                                isError = result.message ?: "Unknown error occurred",
                                isLoading = false
                            )
                            Log.d("ScanTextViewModel", "ERROR: ${_state.value.isError}")
                        }
                        is Resource.Success<*> -> {
                            result.data?.let { pdfResponse ->
                                Log.d("ScanTextViewModel", "PDF processed: ${pdfResponse.document?.title}")
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    pdfResponse = pdfResponse,
                                    switchToResults = true
                                )

                                // Start polling for async results (Phase 5)
                                pdfResponse.job_id?.let { jobId ->
                                    startPollingJobStatus(jobId)
                                }
                            }
                        }
                    }
                }

                // Cleanup temp file
                tempFile.delete()

            } catch (e: Exception) {
                Log.e("ScanTextViewModel", "Upload error: ${e.message}", e)
                _state.value = _state.value.copy(
                    isError = e.message ?: "Upload failed",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Phase 5: Poll for async content generation results
     */
    private fun startPollingJobStatus(jobId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isPolling = true)

            pollJobStatusUseCase.invoke(jobId).collect { result ->
                when (result) {
                    is Resource.Success<*> -> {
                        result.data?.let { status ->
                            _state.value = _state.value.copy(
                                jobStatus = status,
                                isPolling = status.status != "complete" && status.status != "error"
                            )
                            Log.d("ScanTextViewModel", "Job status: ${status.status}")
                        }
                    }
                    is Resource.Error<*> -> {
                        Log.e("ScanTextViewModel", "Polling error: ${result.message}")
                        _state.value = _state.value.copy(isPolling = false)
                    }
                    is Resource.Loading<*> -> { /* ignore */ }
                }
            }
        }
    }

    fun resetState() {
        Log.d("RESET_STATE", "Resetting view model state")
        _state.value = ScanImageScreenState()
    }

    fun getCurrentPdfResponse() = _state.value.pdfResponse
    fun getCurrentJobStatus() = _state.value.jobStatus

    // --- Helpers ---

    private fun copyUriToTempFile(uri: Uri, context: Context): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.pdf")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Log.e("ScanTextViewModel", "Failed to copy URI to temp file: ${e.message}")
            null
        }
    }

    private fun getFileName(uri: Uri, context: Context): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}