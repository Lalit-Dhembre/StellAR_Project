package com.cosmic_struck.stellar.classroom.presentation.viewmodel.delegate

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.cosmic_struck.stellar.classroom.data.dto.ProcessResponse
import com.cosmic_struck.stellar.classroom.data.dto.Quiz
import com.cosmic_struck.stellar.classroom.data.local.repository.ModuleProcessLocalRepository
import com.cosmic_struck.stellar.classroom.domain.model.QuizResult
import com.cosmic_struck.stellar.classroom.domain.usecase.GetModuleUseCase
import com.cosmic_struck.stellar.classroom.domain.usecase.GetProcessResponseUseCase
import com.cosmic_struck.stellar.classroom.presentation.viewmodel.ClassroomModuleState
import com.cosmic_struck.stellar.common.util.DownloadFile
import com.cosmic_struck.stellar.common.util.Resource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ClassroomModuleDelegate @Inject constructor(
    private val getModuleUseCase: GetModuleUseCase,
    private val getProcessResponseUseCase: GetProcessResponseUseCase,
    private val downloadFile: DownloadFile,
    private val moduleProcessLocalRepository: ModuleProcessLocalRepository,
) {

    private val _state = MutableStateFlow(ClassroomModuleState())
    val state: StateFlow<ClassroomModuleState> = _state.asStateFlow()

    // Track which module has been fully loaded to prevent redundant calls
    private var loadedModuleId: Long? = null
    private var isLoadingInProgress = false

    suspend fun setModuleId(id: Long) {
        coroutineScope {
            // Only reset if it's a different module
            if (_state.value.module_id != id) {
                Log.d("ModuleDelegate", "Setting new module ID: $id (previous: ${_state.value.module_id})")
                _state.value = ClassroomModuleState(module_id = id)
                loadedModuleId = null // Reset loaded flag for new module
            }
        }
    }

    /**
     * Main entry point - loads module with caching
     * Returns early if already loaded for this module
     */
    suspend fun loadModuleWithCache() {
        val currentModuleId = state.value.module_id ?: return

        // Skip if already loaded for this module
        if (loadedModuleId == currentModuleId) {
            Log.d("ModuleDelegate", "Module $currentModuleId already loaded, skipping")
            return
        }

        // Skip if loading is already in progress
        if (isLoadingInProgress) {
            Log.d("ModuleDelegate", "Loading already in progress, skipping")
            return
        }

        isLoadingInProgress = true
        Log.d("ModuleDelegate", "Starting load for module: $currentModuleId")

        try {
            // 1. Get module details (always needed for title/description)
            getModuleDetails()

            // 2. Check if we have cached process info first
            val hasCachedData = moduleProcessLocalRepository.hasData(currentModuleId)
            if (hasCachedData) {
                Log.d("ModuleDelegate", "Found cached data for module: $currentModuleId")
                val cachedResponse = moduleProcessLocalRepository.getProcessResponse(currentModuleId)
                if (cachedResponse != null) {
                    _state.value = _state.value.copy(processInfo = cachedResponse)
                }
            }

            // 3. Download model and PDF (checks if files already exist)
            downloadModelIfNeeded()
            downloadPdfIfNeeded()

            // 4. Only fetch process info from server if not cached
            if (!hasCachedData) {
                getProcessInfo()
            }

            // Mark as loaded
            loadedModuleId = currentModuleId
            Log.d("ModuleDelegate", "Module $currentModuleId fully loaded")

        } finally {
            isLoadingInProgress = false
        }
    }

    suspend fun getProcessInfo() {
        coroutineScope {
            val moduleId = state.value.module_id

            // Check Room database first
            if (moduleId != null && moduleProcessLocalRepository.hasData(moduleId)) {
                Log.d("ProcessInfo", "Using database cached response for module: $moduleId")
                val cachedResponse = moduleProcessLocalRepository.getProcessResponse(moduleId)
                if (cachedResponse != null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        processInfo = cachedResponse
                    )
                    return@coroutineScope
                }
            }

            val description = state.value.module?.moduleDesc ?: ""
            val pdf = state.value.pdf_path
            
            if (pdf.isEmpty()) {
                Log.e("ProcessInfo", "PDF path is empty, cannot process")
                return@coroutineScope
            }
            
            val file = File(pdf)
            if (!file.exists()) {
                Log.e("PDF", "File not found: $pdf")
                return@coroutineScope
            }
            
            val request = file.asRequestBody(contentType = "application/pdf".toMediaTypeOrNull())
            val multipart = MultipartBody.Part.createFormData("file", file.name, request)
            
            getProcessResponseUseCase(description, multipart).collect { it ->
                when (it) {
                    is Resource.Loading<*> -> _state.value = _state.value.copy(
                        isLoading = true
                    )
                    is Resource.Success<*> -> {
                        // Save to Room database
                        if (moduleId != null && it.data != null) {
                            moduleProcessLocalRepository.saveProcessResponse(moduleId, it.data)
                            Log.d("ProcessInfo", "Saved response to database for module: $moduleId")
                        }
                        _state.value = _state.value.copy(
                            isLoading = false,
                            processInfo = it.data
                        )
                    }
                    is Resource.Error<*> -> _state.value = _state.value.copy(
                        isLoading = false,
                        processError = it.message.toString()
                    )
                }
            }
        }
    }

    private suspend fun downloadPdfIfNeeded() {
        val url = state.value.module?.pdfUrl ?: return
        val title = (state.value.module?.moduleName ?: "module") + ".pdf"
        
        // Check if already downloaded
        val existingPath = state.value.pdf_path
        if (existingPath.isNotEmpty() && File(existingPath).exists()) {
            Log.d("ModuleDelegate", "PDF already exists at: $existingPath")
            return
        }

        Log.d("Module Info", "Downloading PDF: $url")
        coroutineScope {
            downloadFile(url = url, title = title).collect { it ->
                when (it) {
                    is Resource.Loading<*> -> {
                        _state.value = _state.value.copy(isLoading = true)
                    }
                    is Resource.Success<*> -> {
                        val pdfPath = it.data ?: ""
                        Log.d("Module Info", "PDF downloaded: $pdfPath")
                        _state.value = _state.value.copy(
                            pdf_path = pdfPath,
                            isLoading = false
                        )
                    }
                    is Resource.Error<*> -> {
                        Log.e("Module Info", "PDF download error: ${it.message}")
                        _state.value = _state.value.copy(isLoading = false)
                    }
                }
            }
        }
    }

    private suspend fun downloadModelIfNeeded() {
        val url = state.value.module?.modelUrl ?: return
        val title = state.value.module?.moduleName ?: "model"

        // Check if already downloaded
        val existingPath = state.value.model_path
        if (existingPath.isNotEmpty() && File(existingPath).exists()) {
            Log.d("ModuleDelegate", "Model already exists at: $existingPath")
            return
        }

        Log.d("Module Info", "Downloading Model: $url")
        coroutineScope {
            downloadFile(url = url, title = title).collect { it ->
                when (it) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        val modelPath = it.data ?: ""
                        Log.d("Module Info", "Model downloaded: $modelPath")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            model_path = modelPath
                        )
                    }
                    is Resource.Error -> {
                        Log.e("Module Info", "Model download error: ${it.message}")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            moduleError = it.message ?: "Download failed"
                        )
                    }
                }
            }
        }
    }

    // Legacy methods kept for compatibility
    suspend fun downloadPdf() = downloadPdfIfNeeded()
    suspend fun downloadModel() = downloadModelIfNeeded()

    suspend fun getModuleDetails() {
        coroutineScope {
            state.value.module_id?.let { getModuleUseCase(it) }?.collect { it ->
                when (it) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(isLoading = true)
                    }
                    is Resource.Success<*> -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            module = it.data
                        )
                        Log.d("Module Info", "${it.data}")
                    }
                    is Resource.Error<*> -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            moduleError = it.message.toString()
                        )
                        Log.d("Module Info", "${it.message}")
                    }
                }
            }
        }
    }

    /**
     * Force refresh - clears cache and reloads
     */
    suspend fun forceRefresh() {
        val moduleId = state.value.module_id ?: return
        moduleProcessLocalRepository.deleteData(moduleId)
        loadedModuleId = null
        loadModuleWithCache()
    }
}