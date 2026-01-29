package com.cosmic_struck.stellar.stellar.arlab.universe_lab.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.components.SimpleTopAppBar
import com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine.CelestialBody

@Composable
fun UniverseLabScreen(
    viewModel: UniverseLabViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            SimpleTopAppBar(
                title = "Universe Lab",
                popNavigation = navigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // AR Scene
            UniverseLabARScene(
                bodies = state.bodies,
                isPlaced = state.isPlaced,
                zoomLevel = state.zoomLevel,
                onPlaced = { viewModel.onPlaced() },
                onBodyTapped = { viewModel.selectBody(it) },
                getModelPath = { viewModel.getModelPath(it) },
                getBodyScale = { viewModel.getBodyScale(it) },
                onResetNodes = { callback -> viewModel.registerResetCallback(callback) },
                onZoomChange = { viewModel.setZoomLevel(it) }
            )

            // Placement instruction overlay
            AnimatedVisibility(
                visible = !state.isPlaced,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLANETARY PLACEMENT",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Point at a flat surface\nand tap to place the Solar System",
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Zoom controls on the right side
            AnimatedVisibility(
                visible = state.isPlaced,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                ZoomControls(
                    zoomLevel = state.zoomLevel,
                    onZoomIn = { viewModel.zoomIn() },
                    onZoomOut = { viewModel.zoomOut() }
                )
            }

            // Selected body control card
            AnimatedVisibility(
                visible = state.selectedBody != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                state.selectedBody?.let { body ->
                    BodyControlCard(
                        body = body,
                        scale = state.bodyScales[body.id] ?: 1f,
                        speedMultiplier = state.bodySpeedMultipliers[body.id] ?: 1f,
                        onScaleChange = { viewModel.setBodyScale(body.id, it) },
                        onSpeedChange = { viewModel.setBodySpeedMultiplier(body.id, it) },
                        onDismiss = { viewModel.deselectBody() }
                    )
                }
            }

            // Control panel at bottom
            AnimatedVisibility(
                visible = state.isPlaced,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                UniverseLabControlPanel(
                    isPlaying = state.isPlaying,
                    timeScale = state.timeScale,
                    simulationTime = state.simulationTime,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onResetClick = { viewModel.reset() },
                    onTimeScaleChange = { viewModel.setTimeScale(it) }
                )
            }
        }
    }
}

@Composable
private fun ZoomControls(
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom In
        IconButton(
            onClick = onZoomIn,
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = "Zoom In",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Zoom level display
        Text(
            text = String.format("%.1fx", zoomLevel),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Zoom Out
        IconButton(
            onClick = onZoomOut,
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.erase),
                contentDescription = "Zoom Out",
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BodyControlCard(
    body: CelestialBody,
    scale: Float,
    speedMultiplier: Float,
    onScaleChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E2130).copy(alpha = 0.95f),
                        Color(0xFF2C3E50).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.5f),
                        Color(0xFF7C4DFF).copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = body.name.ifEmpty { body.id.replaceFirstChar { it.uppercase() } },
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoItem(label = "Mass", value = String.format("%.2f", body.mass))
                InfoItem(label = "Radius", value = String.format("%.2f", body.radius))
                InfoItem(
                    label = "Velocity",
                    value = String.format("%.2f", body.velocity.magnitude())
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Size control - allowing much larger range
            ControlSlider(
                label = "Planet Size",
                value = scale,
                valueRange = 0.2f..5f,
                displayValue = String.format("%.1fx", scale),
                onValueChange = onScaleChange,
                accentColor = Color(0xFF00E5FF)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Speed control
            ControlSlider(
                label = "Orbit Speed",
                value = speedMultiplier,
                valueRange = 0.1f..5f,
                displayValue = String.format("%.1fx", speedMultiplier),
                onValueChange = onSpeedChange,
                accentColor = Color(0xFF7C4DFF)
            )
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = displayValue,
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color(0xFF00E5FF),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label.uppercase(),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
