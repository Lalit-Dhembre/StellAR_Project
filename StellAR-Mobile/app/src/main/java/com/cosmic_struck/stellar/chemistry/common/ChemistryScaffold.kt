package com.cosmic_struck.stellar.chemistry.common

import android.app.Activity
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlin.random.Random

@Composable
fun ChemistryScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    val view = LocalView.current
    val window = (view.context as Activity).window

    // Immersive mode
    SideEffect {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, view).apply {
            hide(WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated Chemistry Background
            ChemistryBackground()

            // Content
            content(Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun ChemistryBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "molecules")
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(90000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val pulse = infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(ChemistryBackground2, ChemistryBackground1),
                    radius = 2000f
                )
            )
    ) {
        // Molecular orbital effects (like electron clouds)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Cyan atomic glow
            drawCircle(
                color = MoleculeParticle1,
                radius = size.minDimension * 0.45f * pulse.value,
                center = Offset(size.width * 0.25f, size.height * 0.35f)
            )
            // Blue molecular glow
            drawCircle(
                color = MoleculeParticle2,
                radius = size.minDimension * 0.4f * pulse.value,
                center = Offset(size.width * 0.75f, size.height * 0.65f)
            )
            // Orange reaction glow
            drawCircle(
                color = MoleculeParticle3,
                radius = size.minDimension * 0.35f * pulse.value,
                center = Offset(size.width * 0.6f, size.height * 0.25f)
            )
        }

        // Floating atom particles
        AtomParticlesLayer(
            count = 80,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.value * 0.25f }
        )
        AtomParticlesLayer(
            count = 40,
            scale = 2f,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -rotation.value * 0.12f }
        )
    }
}

@Composable
private fun AtomParticlesLayer(
    count: Int,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    // Generate static atom particle positions
    val particles = remember {
        List(count) {
            Triple(
                Offset(Random.nextFloat(), Random.nextFloat()),
                Random.nextFloat() * 3f * scale, // size
                Random.nextInt(3) // color type
            )
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        particles.forEach { (relPos, particleSize, colorType) ->
            val color = when (colorType) {
                0 -> AtomicCyan.copy(alpha = Random.nextFloat() * 0.4f + 0.2f)
                1 -> MolecularBlue.copy(alpha = Random.nextFloat() * 0.3f + 0.1f)
                else -> ReactionOrange.copy(alpha = Random.nextFloat() * 0.25f + 0.1f)
            }
            drawCircle(
                color = color,
                radius = particleSize,
                center = Offset(relPos.x * width, relPos.y * height)
            )
        }
    }
}
