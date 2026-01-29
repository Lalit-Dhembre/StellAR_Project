package com.cosmic_struck.stellar.stellar.arlab.universe_lab.presentation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine.CelestialBody
import com.google.android.filament.View.AntiAliasing
import com.google.android.filament.View.Dithering
import com.google.android.filament.View.QualityLevel
import com.google.ar.core.Frame
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlin.math.sqrt

@Composable
fun UniverseLabARScene(
    bodies: List<CelestialBody>,
    isPlaced: Boolean,
    zoomLevel: Float,
    onPlaced: () -> Unit,
    onBodyTapped: (CelestialBody) -> Unit,
    getModelPath: (String) -> String,
    getBodyScale: (String) -> Float,
    onResetNodes: (() -> Unit) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)
    val childNodes = rememberNodes()

    var frame by remember { mutableStateOf<Frame?>(null) }
    var anchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    val bodyNodes = remember { mutableMapOf<String, ModelNode>() }
    
    // Configure high-quality rendering for sharper models
    LaunchedEffect(view) {
        // Anti-aliasing for smooth edges without blur
        view.antiAliasing = AntiAliasing.FXAA
        
        // Dithering reduces color banding
        view.dithering = Dithering.TEMPORAL
        
        // Set high-quality rendering
        view.renderQuality = view.renderQuality.apply {
            hdrColorBuffer = QualityLevel.HIGH
        }
        
        // Disable dynamic resolution scaling - this is a major cause of blurriness
        // Dynamic resolution can downscale rendering for performance, causing blur
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            enabled = false  // Disable to prevent blurry downscaling
        }
        
        // Enable post-processing for better visual quality
        view.isPostProcessingEnabled = true
        
        Log.d("UniverseLab", "Configured high-quality rendering: AA=FXAA, DynamicRes=DISABLED")
    }
    
    // Track initial pinch distance for zoom gesture
    var initialPinchDistance by remember { mutableFloatStateOf(0f) }
    var initialZoom by remember { mutableFloatStateOf(1f) }

    // Get position with zoom applied - planets spread out in a visible circle
    fun getVisiblePosition(body: CelestialBody, zoom: Float): Position {
        // Use sqrt to compress the huge astronomical distances
        val rawDistance = sqrt(body.position.x * body.position.x + 
                              body.position.y * body.position.y + 
                              body.position.z * body.position.z)
        
        // Map astronomical distances to AR-friendly distances (0.1 to 0.8 meters)
        val mappedDistance = when {
            rawDistance < 1f -> 0f // Sun at center
            rawDistance < 160f -> 0.15f * zoom // Earth, Moon
            rawDistance < 240f -> 0.25f * zoom // Mars
            rawDistance < 800f -> 0.45f * zoom // Jupiter
            else -> 0.65f * zoom // Saturn
        }
        
        // Calculate normalized direction from origin
        val magnitude = if (rawDistance > 0.01f) rawDistance else 1f
        val dirX = body.position.x / magnitude
        val dirY = body.position.y / magnitude
        val dirZ = body.position.z / magnitude
        
        return Position(
            x = dirX * mappedDistance,
            y = 0f, // Keep planets on a flat plane for visibility
            z = dirZ * mappedDistance
        )
    }

    // Register reset handler
    DisposableEffect(Unit) {
        onResetNodes {
            bodyNodes.values.forEach { node ->
                try {
                    node.destroy()
                } catch (e: Exception) {
                    Log.e("UniverseLab", "Error destroying node", e)
                }
            }
            bodyNodes.clear()
            
            anchorNode?.let { anchor ->
                try {
                    childNodes -= anchor
                    anchor.destroy()
                } catch (e: Exception) {
                    Log.e("UniverseLab", "Error destroying anchor", e)
                }
            }
            anchorNode = null
        }
        onDispose { }
    }

    // Update node positions when bodies or zoom changes (scale stays constant)
    LaunchedEffect(bodies, zoomLevel) {
        if (anchorNode != null && bodyNodes.isNotEmpty()) {
            bodies.forEach { body ->
                bodyNodes[body.id]?.let { node ->
                    // Only update position - scale stays as initially set
                    node.position = getVisiblePosition(body, zoomLevel)
                }
            }
        }
    }

    ARScene(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        view = view,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        childNodes = childNodes,
        planeRenderer = !isPlaced,

        onSessionUpdated = { _, updatedFrame ->
            frame = updatedFrame
        },

        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { motionEvent, node ->
                // If already placed, check for body taps
                if (isPlaced && node != null) {
                    bodies.find { it.id == node.name }?.let { body ->
                        onBodyTapped(body)
                    }
                    return@rememberOnGestureListener
                }

                // Place the solar system
                if (!isPlaced) {
                    val hit = frame
                        ?.hitTest(motionEvent.x, motionEvent.y)
                        ?.firstOrNull {
                            it.isValid(
                                depthPoint = false,
                                point = false
                            )
                        } ?: return@rememberOnGestureListener

                    val anchor = hit.createAnchorOrNull() ?: return@rememberOnGestureListener
                    val newAnchorNode = AnchorNode(engine, anchor)
                    anchorNode = newAnchorNode

                    // Create nodes for each celestial body with LARGE visible scales
                    bodies.forEach { body ->
                        try {
                            val modelPath = getModelPath(body.id)
                            Log.d("UniverseLab", "Loading model for ${body.id}: $modelPath, scale: ${getBodyScale(body.id)}")

                            val bodyScale = getBodyScale(body.id)
                            val modelNode = ModelNode(
                                modelInstance = modelLoader.createModelInstance(modelPath),
                                scaleToUnits = bodyScale
                            ).apply {
                                name = body.id
                                position = getVisiblePosition(body, zoomLevel)
                                isTouchable = true
                            }

                            bodyNodes[body.id] = modelNode
                            newAnchorNode.addChildNode(modelNode)
                            Log.d("UniverseLab", "Placed ${body.id} at position: ${getVisiblePosition(body,zoomLevel)} with scale: $bodyScale")
                        } catch (e: Exception) {
                            Log.e("UniverseLab", "Failed to load model for ${body.id}", e)
                        }
                    }

                    childNodes += newAnchorNode
                    onPlaced()
                }
            }
        )
    )
}
