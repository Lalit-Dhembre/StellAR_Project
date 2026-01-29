package com.cosmic_struck.stellar.stellar.arlab.universe_lab.presentation

import com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine.CelestialBody

data class UniverseLabState(
    val bodies: List<CelestialBody> = emptyList(),
    val isPlaying: Boolean = false,
    val timeScale: Float = 1f,
    val isPlaced: Boolean = false,
    val selectedBody: CelestialBody? = null,
    val simulationTime: Float = 0f,
    // Zoom level for the entire scene (0.5x to 3x)
    val zoomLevel: Float = 1f,
    // Per-body visual scales (multipliers on base scale)
    val bodyScales: Map<String, Float> = mapOf(
        "sun" to 1f,
        "earth" to 1f,
        "moon" to 1f,
        "mars" to 1f,
        "jupiter" to 1f,
        "saturn" to 1f
    ),
    // Per-body speed multipliers
    val bodySpeedMultipliers: Map<String, Float> = mapOf(
        "sun" to 1f,
        "earth" to 1f,
        "moon" to 1f,
        "mars" to 1f,
        "jupiter" to 1f,
        "saturn" to 1f
    )
)
