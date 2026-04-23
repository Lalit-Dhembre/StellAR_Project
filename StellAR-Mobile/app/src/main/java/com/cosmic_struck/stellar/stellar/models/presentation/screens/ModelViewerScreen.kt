package com.cosmic_struck.stellar.stellar.models.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.components.StellarScaffold
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.stellar.models.presentation.components.ARModelSceneView
import com.cosmic_struck.stellar.stellar.models.presentation.components.BottomSheetControlPanel
import com.cosmic_struck.stellar.stellar.models.presentation.components.NarratorPanel
import com.cosmic_struck.stellar.stellar.models.presentation.components.SceneView
import com.cosmic_struck.stellar.stellar.models.presentation.viewmodel.ModelViewScreenViewModel
import com.cosmic_struck.stellar.stellar.models.presentation.viewmodel.SceneType
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelViewerScreen(
    navigateBack: () -> Unit = {},
    viewModel: ModelViewScreenViewModel = hiltViewModel()
) {
    val state = viewModel.state.value

    StellarScaffold(
        topBar = {
            SimpleTopAppBar(
                title = state.modelTitle,
                popNavigation = navigateBack
            )
        }
    ) {
        when {
            state.isLoadingModel -> {
                val composition =
                    rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.space_shuttle1))

                Box(
                    modifier = it
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition.value,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.size(220.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            state.modelError.isNotEmpty() -> {
                Column(
                    modifier = it
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error loading model", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.modelError, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.downloadModel() }) {
                        Text("Retry")
                    }
                }
            }

            else -> {
                // 🔽 Bottom Sheet owns the layout
                BottomSheetScaffold(
                    sheetPeekHeight = if (state.narrationScript.isNotBlank()) 260.dp else 100.dp,
                    sheetContainerColor = Color.Transparent,
                    sheetContent = {
                        Column {
                            // 🎙 Narrator Panel (above controls when script is available)
                            if (state.narrationScript.isNotBlank()) {
                                NarratorPanel(
                                    script = state.narrationScript,
                                    isNarrating = state.isNarrating,
                                    isPaused = state.isPaused,
                                    progress = state.narrationProgress,
                                    onPlay = { viewModel.startNarration() },
                                    onPause = { viewModel.pauseNarration() },
                                    onResume = { viewModel.resumeNarration() },
                                    onStop = { viewModel.stopNarration() }
                                )
                            }

                            // 🎛 Existing control panel
                            BottomSheetControlPanel(
                                scene = state.scene,
                                rotationSpeed = state.rotationSpeed,
                                onRotationSpeedChange = {
                                    viewModel.onChangeRotationSpeed(it)
                                },
                                onToggleScene = {
                                    viewModel.toggleScene()
                                },
                                onReset = {
                                    viewModel.resetModel()
                                }
                            )
                        }
                    }
                ) { sheetPadding ->

                    // 🎬 Scene Content

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(sheetPadding)
                    ) {
                        val engine = rememberEngine()
                        val modelLoader = rememberModelLoader(engine)
                        val materialLoader = rememberMaterialLoader(engine)
                        val environmentLoader = rememberEnvironmentLoader(engine)
                        val view = rememberView(engine)

                        DisposableEffect(engine, view) {
                            onDispose {
                                try {
                                    engine.destroyView(view)
                                } catch (_: Exception) {}
                            }
                        }

                        val isValidModelPath = remember(state.modelPath) {
                            val file = File(state.modelPath)
                            file.exists() && file.isFile && file.canRead() && file.length() > 0
                        }

                        if (isValidModelPath) {
                            if(state.scene == SceneType.SceneView){
                                SceneView(
                                    modifier = Modifier.fillMaxSize(),
                                    engine = engine,
                                    modelLoader = modelLoader,
                                    materialLoader = materialLoader,
                                    environmentLoader = environmentLoader,
                                    view = view,
                                    cameraDistance = state.cameraDistance,
                                    rotationSpeed = state.rotationSpeed,
                                    rotationAngle = state.rotationAngle,
                                    modelPath = state.modelPath,
                                    onChangeRotationAngle = {
                                        viewModel.onChangeRotationAngle(it)
                                    },
                                    onChangeModelNode = {
                                        viewModel.onChangeModelNode(it)
                                    }
                                )
                            }
                            else{
                                ARModelSceneView(
                                    engine = engine,
                                    modelLoader = modelLoader,
                                    materialLoader = materialLoader,
                                    view = view,
                                    modelPath = state.modelPath,
                                    onModelPlaced = {
                                        viewModel.onChangeModelNode(it)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
