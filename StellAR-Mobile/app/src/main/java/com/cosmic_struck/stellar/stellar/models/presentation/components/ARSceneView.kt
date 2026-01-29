package com.cosmic_struck.stellar.stellar.models.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.filament.Engine
import com.google.android.filament.View
import com.google.ar.core.Config
import com.google.ar.core.Frame
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import java.io.File


@Composable
fun ARModelSceneView(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    view: View,
    modelPath: String,
    onModelPlaced: (ModelNode) -> Unit
) {
    val childNodes = rememberNodes()
    var frame by remember { mutableStateOf<Frame?>(null) }
    var planeRenderer by remember { mutableStateOf(true) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        view = view,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        childNodes = childNodes,

        planeRenderer = planeRenderer,

        sessionConfiguration = { session, config ->
            config.lightEstimationMode =
                Config.LightEstimationMode.ENVIRONMENTAL_HDR
        },

        onSessionUpdated = { _, updatedFrame ->
            frame = updatedFrame
            val tracking = frame!!.camera.trackingState
            Log.d("AR", "Camera tracking = $tracking")

        },

        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { motionEvent, node ->
                // Prevent multiple placements
                if (node != null || childNodes.isNotEmpty()) return@rememberOnGestureListener

                val hitResult = frame
                    ?.hitTest(motionEvent.x, motionEvent.y)
                    ?.firstOrNull {
                        it.isValid(
//                            plane = true,
                            depthPoint = false,
                            point = false
                        )
                    } ?: run {
                    Log.d("AR", "❌ No valid hit")
                    return@rememberOnGestureListener
                }

                val anchor = hitResult.createAnchorOrNull()
                    ?: run {
                        Log.d("AR", "❌ Anchor creation failed")
                        return@rememberOnGestureListener
                    }

                val file = File(modelPath)
                if (!file.exists() || file.length() == 0L) {
                    Log.d("AR", "❌ Invalid model file")
                    return@rememberOnGestureListener
                }

                planeRenderer = false

                val anchorNode = AnchorNode(engine, anchor)

                val modelNode = ModelNode(
                    modelInstance = modelLoader.createModelInstance(file),
                    scaleToUnits = 1.0f
                ).apply {
                    isEditable = true
                }

                anchorNode.addChildNode(modelNode)
                childNodes += anchorNode
                onModelPlaced(modelNode)

                Log.d("AR", "✅ Model placed")
            }
        )
    )
}
