package com.cosmic_struck.stellar.create_module.presentation.components

import android.content.Context
import android.icu.number.Scale
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cosmic_struck.stellar.stellar.models.presentation.components.SceneView
import io.github.sceneview.Scene
import io.github.sceneview.collision.HitResult
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.ByteBuffer

@Composable
fun ModelViewer(
    modifier: Modifier = Modifier,
    modelUri: Uri?) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val childNodes = rememberNodes()
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val view = rememberView(engine)
    val context = LocalContext.current

    // Use Crossfade for a smooth "instant" transition
    Crossfade(targetState = modelUri, label = "SceneTransition") { uri ->
        if (uri == null) {
            // --- PLACEHOLDER UI ---
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // You can use a local vector/png placeholder here
                    Text("Select a 3D model to begin", color = Color.White)
                }
            }
        } else {
            // --- 3D SCENE VIEW ---
            Box(modifier = modifier.fillMaxSize()) {
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    view = view,
                    renderer = rememberRenderer(engine),
                    scene = rememberScene(engine),
                    modelLoader = modelLoader,
                    materialLoader = materialLoader,
                    environmentLoader = environmentLoader,
                    collisionSystem = rememberCollisionSystem(view),

                    // Main Light Setup
                    mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000.0f
                    },

                    // Camera Setup
                    cameraNode = rememberCameraNode(engine) {
                        position = io.github.sceneview.math.Position(
                            x = 0.0f,
                            y = 0.0f,
                            z = 0.0f
                        )
                    },

                    cameraManipulator = rememberCameraManipulator(),

                    // Child Nodes - Model Loading
                    childNodes = rememberNodes {
                        try {
                            Log.d("SceneView", "Loading model from file system: $uri")

                            // Read file from file system (not assets)
                            val file = uriToFile(context, uri)


                            if (!file!!.exists()) {
                                Log.e("SceneView", "File does not exist: $uri")
                                return@rememberNodes
                            }

                            if (!file.canRead()) {
                                Log.e("SceneView", "Cannot read file: $uri")
                                return@rememberNodes
                            }

                            // Read file bytes
                            val buffer = file.inputStream().buffered().use { input ->
                                val bytes = input.readBytes()
                                ByteBuffer.wrap(bytes)
                            }

                            Log.d("SceneView", "File read successfully, size: ${buffer.capacity()} bytes")

                            // Create model instance from buffer
                            // Use the correct method that accepts ByteBuffer for file system files
                            val modelInstance = modelLoader.createModelInstance(buffer)

                            Log.d("SceneView", "Model instance created successfully from file buffer")

                            // Create model node
                            val node = ModelNode(
                                modelInstance = modelInstance
                            ).apply {
                                // Set initial position
                                position =
                                    io.github.sceneview.math.Position(x = 0.0f, y = 0.0f, z = 0.0f)
                                // Set initial rotation

                                Log.d("SceneView", "ModelNode configured with initial transforms")
                            }

                            // Add to scene
                            add(node)

                            Log.d("SceneView", "Model added to scene successfully")

                        } catch (e: Exception) {
                            Log.e("SceneView", "Error loading model: ${e.message}", e)
                            e.printStackTrace()
                        }
                    },

                    // Gesture Handling
                    onGestureListener = rememberOnGestureListener(
                        onDoubleTapEvent = { _, tappedNode ->

                        }
                    ),

                    // Touch Event Handling
                    onTouchEvent = { _: MotionEvent, _: HitResult? ->
                        false  // Return false to allow camera manipulation
                    },

                    // Frame Update - Apply Transformations Every Frame
                    onFrame = { _ ->
                    }
                )
            }
        }
    }
}

fun uriToFile(context: Context, uri: Uri): File? {
    val contentResolver = context.contentResolver
    // Create a temp file in the app's internal cache
    val tempFile = File(context.cacheDir, "temp_model.glb")

    try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}