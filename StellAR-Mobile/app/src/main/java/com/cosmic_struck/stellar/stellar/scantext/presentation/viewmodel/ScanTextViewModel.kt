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

    private fun getImagesUrl() {
        viewModelScope.launch {
            try {
                val detections = state.value.scanResults?.detections
                val urls = mutableListOf<String>()

                for (detection in detections ?: emptyList()) {
                    val keyword = detection.name
                    Log.d("IMAGE_URL", "Fetching image for: $keyword")

                    val imageUrl = getImageFromKeyword.getImageUrl(keyword)
                    if (imageUrl != null) {
                        urls.add(imageUrl)
                        Log.d("IMAGE_URL", "Found: $imageUrl")
                    } else {
                        Log.d("IMAGE_URL", "No image found for: $keyword")
                        urls.add("")
                    }
                }

                _imageUrls.value = urls
                Log.d("IMAGE_URL_COMPLETE", urls.toString())

                // Now that images are fetched, mark as ready for navigation
                _state.value = _state.value.copy(switchToResults = true)
                Log.d("READY_FOR_RESULTS", "Navigation ready")

            } catch (e: Exception) {
                Log.e("IMAGE_URL_ERROR", "Error fetching images", e)
                _imageUrls.value = emptyList()
                // Still allow navigation even if images failed
                _state.value = _state.value.copy(switchToResults = true)
            }
        }
    }

    fun uploadImage(file: File) {
        val image = InputImage.fromFilePath(
            application,
            Uri.fromFile(file)
        )

        textRecognizer.process(image)
            .addOnSuccessListener { it ->
                val extractedText = it.text
                val count = it.textBlocks.size
                Log.d("EXTRACTED TEXT", extractedText)
            }
            .addOnFailureListener { e ->
                Log.d("FAILURE EXTRACTION", e.localizedMessage ?: "Unknown error occurred")
            }

        val requestBody =
            file.asRequestBody("image/jpeg".toMediaTypeOrNull())

        val multipart =
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                requestBody
            )
        Log.d("VIEWMODEL", multipart.toString())

        viewModelScope.launch {
            imageUploadUseCase.invoke(multipart).collect { result ->
                when (result) {
                    is Resource.Loading<*> -> {
                        _state.value = _state.value.copy(
                            isLoading = true
                        )
                        Log.d("VIEWMODEL", "LOADING")
                    }
                    is Resource.Error<*> -> {
                        _state.value = _state.value.copy(
                            isError = result.message ?: "Unknown error occurred",
                            isLoading = false
                        )
                        Log.d("VIEWMODEL_ERROR", _state.value.isError)
                    }
                    is Resource.Success<*> -> {
                        result.data?.let {
                            Log.d("SCAN RESULTS FROM VIEWMODEL", it.toString())

                            // Update state with new data (NOT switchToResults yet)
                            _state.value = _state.value.copy(
                                isLoading = false,
                                scanResults = it,
                                count = it.count
                            )

                            Log.d("STATE_UPDATED", "New state: ${_state.value.scanResults?.count} items")

                            // Fetch images after state is updated
                            // switchToResults will be set to true AFTER images are fetched
                            getImagesUrl()
                        }
                    }
                }
            }
        }
    }
}