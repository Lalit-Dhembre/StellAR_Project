package com.cosmic_struck.stellar.history.common

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
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.random.Random

// History Theme Colors
val HistoryBgDark = Color(0xFF1B1205)
val HistoryBgLight = Color(0xFF3E2723)
val GoldParticle = Color(0xFFFFD700)
val SandParticle = Color(0xFFD7CCC8)

@Composable
fun HistoryScaffold(
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
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
            // Animated History Background
            HistoryBackground()

            // Content
            content(Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun HistoryBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "sands_of_time")
    val translationYState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "falling_dust"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(HistoryBgLight, HistoryBgDark),
                )
            )
    ) {
        // Dust Motes / Time Particles
        SandParticlesLayer(
            count = 100,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = translationYState.value * 0.5f }
        )
         SandParticlesLayer(
            count = 50,
            scale = 1.5f,
             modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = translationYState.value * 0.8f }
        )
    }
}

@Composable
private fun SandParticlesLayer(
    count: Int,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val particles = remember {
        List(count) {
            Triple(
                Offset(Random.nextFloat(), Random.nextFloat()),
                Random.nextFloat() * 1.5f * scale, // size
                Random.nextBoolean() // isGold
            )
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        particles.forEach { (relPos, particleSize, isGold) ->
            val color = if (isGold) GoldParticle.copy(alpha = 0.6f) else SandParticle.copy(alpha = 0.4f)
            
            // Wrap around effect handled by restart animation mostly, 
            // but for simple continuous falling we rely on restart or larger canvas.
            // Using simple relative pos for now.
            
            drawCircle(
                color = color,
                radius = particleSize,
                center = Offset(relPos.x * width, relPos.y * height)
            )
        }
    }
}
