package com.cosmic_struck.stellar.stellar.arlab.universe_lab.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine.CelestialBody
import com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine.UniverseSimulator
import com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine.Vector3D
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UniverseLabViewModel @Inject constructor() : ViewModel() {

    private val simulator = UniverseSimulator()
    private var simulationJob: Job? = null
    private var resetNodesCallback: (() -> Unit)? = null

    private val _state = MutableStateFlow(UniverseLabState())
    val state: StateFlow<UniverseLabState> = _state.asStateFlow()

    // Model paths for each celestial body
    val bodyModelPaths = mapOf(
        "sun" to "models/sun.glb",
        "earth" to "models/earth.glb",
        "moon" to "models/moon.glb",
        "mars" to "models/mars.glb",
        "jupiter" to "models/jupiter.glb",
        "saturn" to "models/saturn.glb"
    )

    // Base visual scales for each body (in AR meters) - visible from arm's length
    private val baseBodyScales = mapOf(
        "sun" to 0.25f,     // 25cm diameter - very visible
        "earth" to 0.10f,   // 10cm diameter
        "moon" to 0.04f,    // 4cm diameter
        "mars" to 0.08f,    // 8cm diameter
        "jupiter" to 0.18f, // 18cm diameter
        "saturn" to 0.15f   // 15cm diameter (excluding rings)
    )

    init {
        initializeSolarSystem()
    }

    private fun initializeSolarSystem() {
        simulator.createSolarSystem()
        updateBodiesState()
    }

    private fun updateBodiesState() {
        _state.update { currentState ->
            currentState.copy(
                bodies = simulator.getBodies(),
                simulationTime = simulator.getEngine().getSimulationTime()
            )
        }
    }

    fun onPlaced() {
        _state.update { it.copy(isPlaced = true) }
    }

    fun play() {
        if (_state.value.isPlaying) return

        _state.update { it.copy(isPlaying = true) }

        simulationJob = viewModelScope.launch {
            while (isActive && _state.value.isPlaying) {
                val speedMultipliers = _state.value.bodySpeedMultipliers
                simulator.stepWithMultipliers(speedMultipliers)
                updateBodiesState()
                delay(33L) // ~30 FPS
            }
        }
    }

    fun pause() {
        _state.update { it.copy(isPlaying = false) }
        simulationJob?.cancel()
        simulationJob = null
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun reset() {
        pause()
        
        // Clear AR nodes first
        resetNodesCallback?.invoke()
        
        simulator.getEngine().reset()
        simulator.createSolarSystem()
        updateBodiesState()
        
        // Reset to default scales, speeds, and zoom
        _state.update { 
            it.copy(
                isPlaced = false,
                zoomLevel = 1f,
                bodyScales = mapOf(
                    "sun" to 1f,
                    "earth" to 1f,
                    "moon" to 1f,
                    "mars" to 1f,
                    "jupiter" to 1f,
                    "saturn" to 1f
                ),
                bodySpeedMultipliers = mapOf(
                    "sun" to 1f,
                    "earth" to 1f,
                    "moon" to 1f,
                    "mars" to 1f,
                    "jupiter" to 1f,
                    "saturn" to 1f
                ),
                selectedBody = null
            )
        }
    }

    fun setTimeScale(scale: Float) {
        simulator.getEngine().setTimeScale(scale.coerceIn(0.1f, 5f))
        _state.update { it.copy(timeScale = scale) }
    }
    
    fun setZoomLevel(zoom: Float) {
        _state.update { it.copy(zoomLevel = zoom.coerceIn(0.3f, 3f)) }
    }
    
    fun zoomIn() {
        val currentZoom = _state.value.zoomLevel
        setZoomLevel(currentZoom + 0.2f)
    }
    
    fun zoomOut() {
        val currentZoom = _state.value.zoomLevel
        setZoomLevel(currentZoom - 0.2f)
    }

    fun selectBody(body: CelestialBody?) {
        _state.update { it.copy(selectedBody = body) }
    }
    
    fun deselectBody() {
        _state.update { it.copy(selectedBody = null) }
    }

    fun setBodyScale(bodyId: String, scale: Float) {
        _state.update { currentState ->
            currentState.copy(
                bodyScales = currentState.bodyScales + (bodyId to scale.coerceIn(0.2f, 5f))
            )
        }
    }
    
    fun setBodySpeedMultiplier(bodyId: String, multiplier: Float) {
        _state.update { currentState ->
            currentState.copy(
                bodySpeedMultipliers = currentState.bodySpeedMultipliers + (bodyId to multiplier.coerceIn(0.1f, 5f))
            )
        }
    }

    fun getModelPath(bodyId: String): String {
        return bodyModelPaths[bodyId] ?: "models/earth.glb"
    }
    
    fun getBodyScale(bodyId: String): Float {
        val baseScale = baseBodyScales[bodyId] ?: 0.10f
        val multiplier = _state.value.bodyScales[bodyId] ?: 1f
        // Only apply base scale and user multiplier - zoom only affects positions, not sizes
        return baseScale * multiplier
    }
    
    fun registerResetCallback(callback: () -> Unit) {
        resetNodesCallback = callback
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}
