package com.cosmic_struck.stellar.create_module.presentation.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.savedState
import com.cosmic_struck.stellar.common.util.Resource
import com.cosmic_struck.stellar.create_module.domain.usecase.Generate3DModelUseCase
import com.cosmic_struck.stellar.create_module.domain.usecase.UploadModuleWithModelUseCase
import com.cosmic_struck.stellar.create_module.presentation.CreateModuleState
import com.cosmic_struck.stellar.create_module.presentation.ModelChoice
import com.cosmic_struck.stellar.create_module.presentation.UploadStatus
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateModuleViewModel @Inject constructor(
    private val uploadModuleWithModelUseCase: UploadModuleWithModelUseCase,
    private val generate3DModelUseCase: Generate3DModelUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val context: Application
) : ViewModel(){

    private val _state = MutableStateFlow(CreateModuleState())
    val state: StateFlow<CreateModuleState> = _state.asStateFlow()

    init {
        val classroomId = savedStateHandle.get<String>("classroom_id")
        val imagePath = savedStateHandle.get<Uri?>("image_path")
        val pdfPath = savedStateHandle.get<Uri?>("pdf_path")
        val modelPath = savedStateHandle.get<Uri?>("model_path")
        val description = savedStateHandle.get<String>("description")
        val module_name = savedStateHandle.get<String>("module_name")
        val selectedOption = savedStateHandle.get<Int>("selected_option")
        val modelChoice = savedStateHandle.get<ModelChoice>("model_choice")

        _state.value = _state.value.copy(
            classroom_id = classroomId.toString(),
            imagePath = imagePath,
            pdfPath = pdfPath,
            modelPath = modelPath,
            description = description ?: "",
            moduleName = module_name ?: "",
            selectedOption = selectedOption ?: 0,
        )
    }
    fun uploadImage(imagePath: Uri?){
        viewModelScope.launch {
            savedStateHandle["image_path"] = imagePath
            _state.value = _state.value.copy(imagePath = imagePath)
        }
    }

    fun uploadPdf(imagePath: Uri?){
        viewModelScope.launch {
            savedStateHandle["pdf_path"] = imagePath
            _state.value = _state.value.copy(pdfPath = imagePath)
        }
    }

    fun uploadModel(imagePath: Uri?){
        viewModelScope.launch {
            savedStateHandle["model_path"] = imagePath
            _state.value = _state.value.copy(modelPath = imagePath)
        }
    }

    fun changeDescription(input:String){
        viewModelScope.launch {
            savedStateHandle["description"] = input
            _state.value = _state.value.copy(description = input)
        }
    }

    fun changeModuleName(input: String){
        viewModelScope.launch {
            savedStateHandle["module_name"] = input
            _state.value = _state.value.copy(moduleName = input)
        }
    }

    fun changeModelChoice(input:Int){
        viewModelScope.launch {
            savedStateHandle["selected_option"] = input
            savedStateHandle["model_choice"] = if (input == 0) ModelChoice.UPLOAD_MODEL else ModelChoice.GENERATE_MODEL
            if(input == 0){
                _state.value = _state.value.copy(
                    selectedOption = 0,
                    modelChoice = ModelChoice.UPLOAD_MODEL)
            }
            else{
                _state.value = _state.value.copy(
                    selectedOption = 1,
                    modelChoice = ModelChoice.GENERATE_MODEL)
            }
        }
    }

    fun generateModel(imageUri: Uri) {
        viewModelScope.launch {
            generate3DModelUseCase(context, imageUri).collect { resource ->
                when(resource) {
                    is Resource.Loading -> {
                        _state.update { it.copy(generationStatus = UploadStatus.LOADING, error = null) }
                    }
                    is Resource.Success -> {
                        savedStateHandle["model_path"] = resource.data
                        _state.update { it.copy(
                            generationStatus = UploadStatus.SUCCESS,
                            modelPath = resource.data
                        ) }
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(
                            generationStatus = UploadStatus.ERROR,
                            error = resource.message
                        ) }
                    }
                }
            }
        }
    }

    fun createModule() {
        val currentState = _state.value

        // 1. Validate that mandatory URIs are not null
        val modelUri = currentState.modelPath
        val pdfUri = currentState.pdfPath

        if (modelUri == null || pdfUri == null) {
            _state.update { it.copy(
                uploadSuccess = UploadStatus.ERROR,
                error = "Please ensure both Model and PDF are selected."
            )}
            return
        }

        viewModelScope.launch {
            Log.d("Checking Values", "Name: ${currentState.moduleName}, PDF: $pdfUri")

            uploadModuleWithModelUseCase(
                moduleName = currentState.moduleName,
                description = currentState.description,
                classroomId = currentState.classroom_id,
                imageUri = currentState.imagePath,
                context = context,
                modelUri = modelUri, // No more !!
                pdfUri = pdfUri      // No more !!
            ).collect { resource ->
                _state.update {
                    when(resource) {
                        is Resource.Error -> it.copy(uploadSuccess = UploadStatus.ERROR, error = resource.message)
                        is Resource.Loading -> it.copy(uploadSuccess = UploadStatus.LOADING)
                        is Resource.Success -> it.copy(uploadSuccess = UploadStatus.SUCCESS)
                    }
                }
            }
        }
    }
}