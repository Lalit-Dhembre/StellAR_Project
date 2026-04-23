package com.cosmic_struck.stellar.stellar.models.presentation.viewmodel

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ModelViewScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val downloadFile: DownloadFile,
    private val application: Application
): ViewModel() {

    companion object {
        private const val TAG = "ModelViewerVM"
    }

    private val _state = mutableStateOf(ModelViewScreenState())
    val state: State<ModelViewScreenState> = _state
    
    val modelName = savedStateHandle.get<String>("name")
    val modelUrl = savedStateHandle.get<String>("url")
    val decodedUrl = Uri.decode(modelUrl)
    private val narrationScript = Uri.decode(savedStateHandle.get<String>("script") ?: "")

    // ── TTS Engine ──────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var narrationSentences: List<String> = emptyList()
    private var currentSentenceIndex = 0

    init {
        Log.d(TAG, "Received URL: $modelUrl")
        Log.d(TAG, "Decoded URL: $decodedUrl")
        Log.d(TAG, "Narration script: ${narrationScript.take(100)}...")

        // Initialize TTS
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.0f)
                isTtsReady = true
                Log.d(TAG, "TTS initialized successfully")
                
                // Set up utterance listener for progress tracking
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS speaking: sentence ${currentSentenceIndex + 1}/${narrationSentences.size}")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS done with utterance: $utteranceId")
                        // Move to next sentence
                        currentSentenceIndex++
                        if (currentSentenceIndex < narrationSentences.size && _state.value.isNarrating && !_state.value.isPaused) {
                            speakCurrentSentence()
                        } else if (currentSentenceIndex >= narrationSentences.size) {
                            // Narration complete
                            _state.value = _state.value.copy(
                                isNarrating = false,
                                isPaused = false,
                                narrationProgress = 1f
                            )
                            currentSentenceIndex = 0
                            Log.d(TAG, "Narration complete")
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS error on utterance: $utteranceId")
                    }
                })
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                modelTitle = modelName ?: "",
                modelURL = decodedUrl ?: "",
                narrationScript = narrationScript
            )
            
            // Pre-split script into sentences
            if (narrationScript.isNotBlank()) {
                narrationSentences = splitIntoSentences(narrationScript)
                Log.d(TAG, "Script split into ${narrationSentences.size} sentences")
            }
            
            downloadModel()
        }
    }

    private fun splitIntoSentences(text: String): List<String> {
        // Split on sentence boundaries, keeping meaningful chunks
        return text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 2 }
    }

    // ── TTS Controls ────────────────────────────────────────────────────

    fun startNarration() {
        if (!isTtsReady || narrationSentences.isEmpty()) {
            Log.w(TAG, "Cannot start narration: ttsReady=$isTtsReady, sentences=${narrationSentences.size}")
            return
        }
        Log.d(TAG, "Starting narration from sentence $currentSentenceIndex")
        currentSentenceIndex = 0
        _state.value = _state.value.copy(
            isNarrating = true,
            isPaused = false,
            narrationProgress = 0f
        )
        speakCurrentSentence()
    }

    fun pauseNarration() {
        Log.d(TAG, "Pausing narration at sentence $currentSentenceIndex")
        tts?.stop()
        _state.value = _state.value.copy(isPaused = true)
    }

    fun resumeNarration() {
        if (!isTtsReady || narrationSentences.isEmpty()) return
        Log.d(TAG, "Resuming narration from sentence $currentSentenceIndex")
        _state.value = _state.value.copy(isPaused = false)
        speakCurrentSentence()
    }

    fun stopNarration() {
        Log.d(TAG, "Stopping narration")
        tts?.stop()
        currentSentenceIndex = 0
        _state.value = _state.value.copy(
            isNarrating = false,
            isPaused = false,
            narrationProgress = 0f
        )
    }

    private fun speakCurrentSentence() {
        if (currentSentenceIndex >= narrationSentences.size) return
        
        val sentence = narrationSentences[currentSentenceIndex]
        val progress = if (narrationSentences.isNotEmpty()) {
            currentSentenceIndex.toFloat() / narrationSentences.size
        } else 0f

        _state.value = _state.value.copy(narrationProgress = progress)

        val params = android.os.Bundle()
        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, "sentence_$currentSentenceIndex")
    }

    // ── Existing Functions ──────────────────────────────────────────────

    fun downloadModel() {
        val url = state.value.modelURL
        val title = state.value.modelTitle

        Log.d(TAG, "downloadModel: url='$url', title='$title'")
        viewModelScope.launch {
            downloadFile(url = url, title = title).collect { resource ->
                when (resource) {
                    is Resource.Loading<*> -> {
                        _state.value = _state.value.copy(
                            isLoadingModel = true,
                            modelError = ""
                        )
                    }
                    is Resource.Success<*> -> {
                        val modelPath = resource.data ?: ""
                        Log.d(TAG, "Model downloaded: $modelPath")
                        _state.value = _state.value.copy(
                            isLoadingModel = false,
                            modelPath = modelPath,
                            modelError = ""
                        )
                    }
                    is Resource.Error<*> -> {
                        val errorMsg = resource.message ?: "Unexpected Error during download"
                        Log.e(TAG, "Model download error: $errorMsg")
                        _state.value = _state.value.copy(
                            isLoadingModel = false,
                            modelError = errorMsg
                        )
                    }
                }
            }
        }
    }

    fun toggleScene() {
        viewModelScope.launch {
            if (_state.value.scene == SceneType.SceneView) {
                _state.value = _state.value.copy(scene = SceneType.ARSceneView)
            } else {
                _state.value = _state.value.copy(scene = SceneType.SceneView)
            }
        }
    }

    fun onChangeRotationSpeed(speed: Float) {
        _state.value = _state.value.copy(rotationSpeed = speed.coerceIn(0f, 3f))
    }

    fun onChangeCameraDistance(distance: Float) {
        _state.value = _state.value.copy(cameraDistance = distance.coerceIn(1.5f, 6f))
    }

    fun onChangeModelNode(modelNode: ModelNode) {
        viewModelScope.launch {
            _state.value = _state.value.copy(modelNode = modelNode)
        }
    }

    fun onChangeRotationAngle(rotationAngle: Float) {
        viewModelScope.launch {
            _state.value = _state.value.copy(rotationAngle = rotationAngle)
        }
    }

    fun resetModel() {
        _state.value = ModelViewScreenState()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared — shutting down TTS")
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}