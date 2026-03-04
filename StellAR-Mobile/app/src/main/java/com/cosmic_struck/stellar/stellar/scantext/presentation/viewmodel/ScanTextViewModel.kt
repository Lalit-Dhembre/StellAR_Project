package com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmic_struck.stellar.common.util.GetImageFromKeyword
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.stellar.scantext.domain.usecase.ImageUploadUseCase
import com.cosmic_struck.stellar.stellar.scantext.presentation.viewmodel.ScanImageScreenState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
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
import java.util.Collections.emptyList
import javax.inject.Inject

@HiltViewModel
class ScanTextViewModel @Inject constructor(
    private val getImageFromKeyword: GetImageFromKeyword,
    private val imageUploadUseCase: ImageUploadUseCase,
    private val textRecognizer: TextRecognizer,
    private val application: Application
) : ViewModel() {

    // Change from mutableStateOf to MutableStateFlow for proper reactive updates
    private val _state = MutableStateFlow(ScanImageScreenState())
    val state: StateFlow<ScanImageScreenState> = _state.asStateFlow()

    private val _imageUrls = MutableStateFlow<List<String>>(emptyList())
    val imageUrls: StateFlow<List<String>> = _imageUrls.asStateFlow()

    fun captureImage(
        context: Context,
        imageCapture: ImageCapture,
        onImageCaptured: (File) -> Unit
    ) {
        val photoFile = File(
            context.cacheDir,
            "scan_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    onImageCaptured(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Capture failed", exception)
                }
            }
        )
    }

    fun resetState() {
        Log.d("RESET_STATE", "Resetting view model state - STACK TRACE:")
        Log.d("RESET_STATE", Log.getStackTraceString(Exception()))
        _state.value = ScanImageScreenState()
        _imageUrls.value = emptyList()
    }

    // Get current scan results (for ScanResultsScreen to read)
    fun getCurrentScanResults() = _state.value.scanResults
    fun getCurrentImageUrls() = _imageUrls.value

    // getImagesUrl() removed — old detection-based image fetching no longer applies

    fun uploadDocument(context: Context, uri: Uri, domain: String = "") {
        viewModelScope.launch {
            try {
                Log.d("DocumentUpload", "Starting upload for URI: $uri, domain: $domain")
                _state.value = _state.value.copy(isLoading = true)
                
                // Copy URI contents to a temporary file with the correct extension
                val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"
                val ext = when {
                    mimeType.contains("pdf") -> ".pdf"
                    mimeType.contains("text") -> ".txt"
                    mimeType.contains("png") -> ".png"
                    mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
                    else -> ".pdf"
                }
                val tempFile = File(context.cacheDir, "upload_doc_${System.currentTimeMillis()}$ext")
                
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val multipart = MultipartBody.Part.createFormData("files", tempFile.name, requestBody)
                val domainBody = domain.toRequestBody("text/plain".toMediaTypeOrNull())
                
                imageUploadUseCase.invoke(multipart, domainBody).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            Log.d("DocumentUpload", "Loading...")
                        }
                        is Resource.Error -> {
                            Log.e("DocumentUpload", "Error: ${result.message}")
                            _state.value = _state.value.copy(isError = result.message ?: "Unknown error", isLoading = false)
                        }
                        is Resource.Success -> {
                            result.data?.let { dto ->
                                Log.d("DocumentUpload", "--- UPLOAD RESPONSE ---")
                                Log.d("DocumentUpload", "Success: ${dto.success}, Domain match: ${dto.domain_match}")
                                
                                if (dto.domain_match == false) {
                                    // Domain mismatch!
                                    Log.w("DocumentUpload", "DOMAIN MISMATCH: ${dto.message}")
                                    Log.w("DocumentUpload", "Detected domain: ${dto.detected_domain}, Reason: ${dto.reason}")
                                    _state.value = _state.value.copy(
                                        isLoading = false,
                                        isError = dto.message ?: "Document does not match the expected domain."
                                    )
                                } else {
                                    // Domain matched — show documents
                                    if (dto.documents != null) {
                                        Log.d("DocumentUpload", "--- LANGCHAIN DOCUMENTS PARSED ---")
                                        dto.documents.forEachIndexed { index, doc ->
                                            Log.d("DocumentUpload", "Document #$index:")
                                            Log.d("DocumentUpload", "  Metadata: ${doc.metadata}")
                                            Log.d("DocumentUpload", "  Content (first 100 chars): ${doc.page_content.take(100)}...")
                                        }
                                    }
                                    
                                    _state.value = _state.value.copy(
                                        isLoading = false,
                                        scanResults = dto,
                                        count = dto.count
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DocumentUpload", "Exception during upload setup: ${e.message}", e)
                _state.value = _state.value.copy(isError = e.message ?: "Setup error", isLoading = false)
            }
        }
    }
}