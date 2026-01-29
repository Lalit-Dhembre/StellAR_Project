package com.cosmic_struck.stellar.physics.common

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

// Physics Theme Colors
val PhysicsBgDark = Color(0xFF050510)
val PhysicsBgLight = Color(0xFF1A1A3D)
val AtomPurple = Color(0xFF6200EA)
val AtomCyan = Color(0xFF00E5FF)
val AtomPink = Color(0xFFFF4081)

@Composable
fun PhysicsScaffold(
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
            // Animated Physics Background
            PhysicsBackground()

            // Content
            content(Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun PhysicsBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "atoms")
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(PhysicsBgLight, PhysicsBgDark),
                    radius = 2000f
                )
            )
    ) {
        // Nebula/Glow effects
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = AtomPurple.copy(alpha = 0.15f),
                radius = size.minDimension * 0.6f,
                center = Offset(size.width * 0.2f, size.height * 0.2f)
            )
            drawCircle(
                color = AtomCyan.copy(alpha = 0.1f),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * 0.8f, size.height * 0.8f)
            )
        }

        // Particle Layers (Electrons/Atoms)
        AtomParticlesLayer(
            count = 60,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.value * 0.5f }
        )
        AtomParticlesLayer(
            count = 30,
            scale = 1.5f,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -rotation.value * 0.3f }
        )
    }
}

@Composable
private fun AtomParticlesLayer(
    count: Int,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val particles = remember {
        List(count) {
            Triple(
                Offset(Random.nextFloat(), Random.nextFloat()),
                Random.nextFloat() * 2f * scale, // size
                Random.nextInt(3) // color type
            )
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        particles.forEach { (relPos, particleSize, colorType) ->
            val color = when (colorType) {
                0 -> AtomPurple.copy(alpha = Random.nextFloat() * 0.4f + 0.3f)
                1 -> AtomCyan.copy(alpha = Random.nextFloat() * 0.4f + 0.3f)
                else -> AtomPink.copy(alpha = Random.nextFloat() * 0.4f + 0.3f)
            }
            drawCircle(
                color = color,
                radius = particleSize,
                center = Offset(relPos.x * width, relPos.y * height)
            )
        }
    }
}
