package com.cosmic_struck.stellar.stellar.models.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmic_struck.stellar.common.util.DownloadFile
import com.cosmic_struck.stellar.common.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelViewScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val downloadFile: DownloadFile
): ViewModel() {

    private val _state = mutableStateOf(ModelViewScreenState())
    val state: State<ModelViewScreenState> = _state
    val modelName = savedStateHandle.get<String>("name")
    val modelUrl = savedStateHandle.get<String>("url")
    val decodedUrl = Uri.decode(modelUrl)
    init {
        Log.d("ModelViewScreenDelegate", "Received URL: $modelUrl")
        Log.d("ModelViewScreenDelegate", "Decoded URL: $decodedUrl")

        viewModelScope.launch {
            _state.value = _state.value.copy(
                modelTitle = modelName ?: "",
                modelURL = decodedUrl ?: ""
            )
            downloadModel()
        }
    }
    fun downloadModel() {
        val url = state.value.modelURL
        val title = state.value.modelTitle

        Log.d("ModelViewScreenDelegate", "🔽 downloadModel called")
        Log.d("ModelViewScreenDelegate", "   Current URL: '$url'")
        Log.d("ModelViewScreenDelegate", "   Current Title: '$title'")
        viewModelScope.launch {
        Log.d("ModelViewScreenDelegate", "🚀 Starting download from: $url")
                downloadFile(url = url, title = title).collect { resource ->
                    Log.d("ModelViewScreenDelegate", "📦 Download resource: $resource")

                    when (resource) {
                        is Resource.Loading<*> -> {
                            Log.d("ModelViewScreenDelegate", "⏳ LOADING...")
                            val loadingState = _state.value.copy(
                                isLoadingModel = true,
                                modelError = ""
                            )
                            _state.value = loadingState
                            Log.d("ModelViewScreenDelegate", "   State: isLoading=true")
                        }

                        is Resource.Success<*> -> {
                            val modelPath = resource.data ?: ""
                            Log.d("ModelViewScreenDelegate", "✅ SUCCESS! Model path: $modelPath")
                            val successState = _state.value.copy(
                                isLoadingModel = false,
                                modelPath = modelPath,
                                modelError = ""
                            )
                            _state.value = successState
                            Log.d("ModelViewScreenDelegate", "   State: isLoading=false, modelPath=$modelPath")
                        }

                        is Resource.Error<*> -> {
                            val errorMsg = resource.message ?: "Unexpected Error during download"
                            Log.e("ModelViewScreenDelegate", "❌ ERROR: $errorMsg")
                            val errorState = _state.value.copy(
                                isLoadingModel = false,
                                modelError = errorMsg
                            )
                            _state.value = errorState
                            Log.d("ModelViewScreenDelegate", "   State: error=$errorMsg")
                        }
                    }
                }
            }
    }

    fun toggleScene() {
        viewModelScope.launch {
            if (_state.value.scene == SceneType.SceneView) {
                _state.value = _state.value.copy(
                    scene = SceneType.ARSceneView
                )
            } else {
                _state.value = _state.value.copy(
                    scene = SceneType.SceneView
                )
            }
        }
    }

    fun onChangeRotationSpeed(speed: Float) {
        _state.value = _state.value.copy(
            rotationSpeed = speed.coerceIn(0f, 3f)
        )
    }

    fun onChangeCameraDistance(distance: Float) {
        _state.value = _state.value.copy(
            cameraDistance = distance.coerceIn(1.5f, 6f)
        )
    }

    fun onChangeModelNode(modelNode: ModelNode) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                modelNode = modelNode
            )
        }
    }

    fun onChangeRotationAngle(rotationAngle: Float) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                rotationAngle = rotationAngle
            )
        }
    }

    fun resetModel() {
        _state.value = ModelViewScreenState()
    }
}