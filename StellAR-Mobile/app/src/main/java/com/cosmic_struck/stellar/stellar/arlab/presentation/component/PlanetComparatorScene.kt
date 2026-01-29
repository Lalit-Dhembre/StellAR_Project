package com.cosmic_struck.stellar.stellar.arlab.presentation.component

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cosmic_struck.stellar.stellar.arlab.domain.model.PlanetComparatorModel
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import java.io.File

@Composable
fun ARPlanetComparatorScene(
    leftPlanet: PlanetComparatorModel?,
    rightPlanet: PlanetComparatorModel?,
    onPlanetTapped: (String) -> Unit,
) {
    if (leftPlanet == null || rightPlanet == null) return

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)
    val childNodes = rememberNodes()

    var frame by remember { mutableStateOf<Frame?>(null) }
    var isPlaced by remember { mutableStateOf(false) }

    ARScene(
        modifier = Modifier.fillMaxSize(),
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
                // Only place once
                if (isPlaced || node != null) return@rememberOnGestureListener

                val hit = frame
                    ?.hitTest(motionEvent.x, motionEvent.y)
                    ?.firstOrNull {
                        it.isValid(
//                            plane = true,
                            depthPoint = false,
                            point = false
                        )
                    } ?: return@rememberOnGestureListener

                val anchor = hit.createAnchorOrNull() ?: return@rememberOnGestureListener
                val anchorNode = AnchorNode(engine, anchor)

                val earthRadius = 6371f

                fun createPlanetNode(
                    planet: PlanetComparatorModel,
                    x: Float
                ): ModelNode {
                    Log.d("Planet Model Path","$${planet.modelPath}")
                    return ModelNode(
                        modelInstance = modelLoader.createModelInstance(
                            planet.modelPath
                        ),
                        scaleToUnits = planet.radiusKm / earthRadius,

                    ).apply {
                        position = io.github.sceneview.math.Position(x, 0f, 0f)
                        isTouchable = true
                        onTouch = { event, hitResult ->
                            if (event.action != MotionEvent.ACTION_UP)  false

                            val tappedNode = hitResult.node

                            when (tappedNode.name) {
                                "earth", "mars", "jupiter", "saturn" -> {
                                    onPlanetTapped(tappedNode.name.toString())
                                    true
                                }

                                else -> false
                            }
                        }
                    }
                }

                anchorNode.addChildNode(
                    createPlanetNode(leftPlanet, -0.4f)
                )
                anchorNode.addChildNode(
                    createPlanetNode(rightPlanet, 0.4f)
                )

                childNodes += anchorNode
                isPlaced = true
            }
        )
    )
}
