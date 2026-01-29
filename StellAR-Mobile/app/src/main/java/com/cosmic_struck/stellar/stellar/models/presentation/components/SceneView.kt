package com.cosmic_struck.stellar.stellar.models.presentation.components

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.filament.Engine
import com.google.android.filament.View
import io.github.sceneview.Scene
import io.github.sceneview.collision.HitResult
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import java.io.File
import java.nio.ByteBuffer

@Composable
fun SceneView(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    environmentLoader: EnvironmentLoader,
    view: View,
    cameraDistance: Float,
    rotationSpeed: Float,
    rotationAngle: Float,
    modelPath: String?,
    onChangeRotationAngle: (Float) -> Unit,
    onChangeModelNode: (ModelNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val loadedModelNode = remember { mutableStateOf<ModelNode?>(null) }

    Scene(
        modifier = modifier
            .fillMaxSize(),
        engine = engine,
        view = view,
        renderer = rememberRenderer(engine),
        scene = rememberScene(engine),
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        environmentLoader = environmentLoader,
        collisionSystem = rememberCollisionSystem(view),

        // Light
        mainLightNode = rememberMainLightNode(engine) {
            intensity = 80_000f
        },

        // Camera (ZOOM IS DONE HERE)
        cameraNode = rememberCameraNode(engine) {
            position = Position(0f, 0f, cameraDistance)
        },

        cameraManipulator = rememberCameraManipulator(),

        childNodes = rememberNodes {
            try {
                val file = File(modelPath ?: return@rememberNodes)
                if (!file.exists() || !file.canRead()) return@rememberNodes

                val buffer = file.inputStream().use {
                    ByteBuffer.wrap(it.readBytes())
                }

                val modelInstance = modelLoader.createModelInstance(buffer)

                val node = ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = 1.0f,            // ✅ FIX
                    centerOrigin = Position(0f)     // ✅ FIX
                ).apply {
                    position = Position(0f, 0f, 0f)
                    rotation = Rotation(y = rotationAngle)
                }

                loadedModelNode.value = node
                onChangeModelNode(node)
                add(node)

            } catch (e: Exception) {
                Log.e("SceneView", "Model load failed", e)
            }
        },

        // ❌ NO SCALING GESTURES
        onGestureListener = rememberOnGestureListener(),

        onTouchEvent = { _, _ -> false },

        // Rotation ONLY (no scale here)
        onFrame = {
            loadedModelNode.value?.let { node ->
                val newAngle = rotationAngle + rotationSpeed * 0.016f
                onChangeRotationAngle(newAngle)
                node.rotation = Rotation(y = newAngle)
            }
        }
    )
}
