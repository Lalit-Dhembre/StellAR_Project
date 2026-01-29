package com.cosmic_struck.stellar.classroom.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.ui.theme.Blue5
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
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
import java.nio.ByteBuffer

@Composable
fun ModuleScene(
    fullScreen:Boolean,
    onChangeScreen: () -> Unit,
    modelPath: String?,
    modifier: Modifier = Modifier
) {
    val loadedModelNode = remember { mutableStateOf<ModelNode?>(null) }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraDistance by remember { mutableFloatStateOf(10f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(if (fullScreen) 1f else 0.5f)
    ){
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
                    val file = File(modelPath ?: run {
                        Log.e("SceneDebug", "modelPath is NULL")
                        return@rememberNodes
                    })

                    Log.d("SceneDebug", "Model path = ${file.absolutePath}")
                    Log.d("SceneDebug", "Exists = ${file.exists()}, Readable = ${file.canRead()}")

                    if (!file.exists() || !file.canRead()) {
                        Log.e("SceneDebug", "Model file not accessible")
                        return@rememberNodes
                    }

                    val bytes = file.readBytes()
                    Log.d("SceneDebug", "Model file size = ${bytes.size} bytes")

                    val buffer = ByteBuffer.wrap(bytes)

                    val modelInstance = modelLoader.createModelInstance(buffer)
                    Log.d("SceneDebug", "ModelInstance created = $modelInstance")

                    val node = ModelNode(
                        modelInstance = modelInstance,
                        scaleToUnits = 1.0f,
                        centerOrigin = Position(0f)
                    ).apply {
                        position = Position(0f, 0f, 0f)
                    }

                    Log.d("SceneDebug", "ModelNode created: $node")

                    loadedModelNode.value = node
                    add(node)

                    Log.d("SceneDebug", "ModelNode added to scene")

                } catch (e: Exception) {
                    Log.e("SceneView", "Model load failed", e)
                }
            },

            // ❌ NO SCALING GESTURES
            onGestureListener = rememberOnGestureListener(),

            onTouchEvent = { _, _ -> false },

            )

        if(!fullScreen){
            IconButton(
                onClick = {
                    onChangeScreen()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Blue5
                ),
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Icon(
                    painter = painterResource(R.drawable.fullscreen),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        else{
            IconButton(
                onClick = {
                    onChangeScreen()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Blue5
                ),
                modifier = Modifier
                    .statusBarsPadding()
                    .size(50.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    painter = painterResource(R.drawable.cross),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }

}