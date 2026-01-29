package com.cosmic_struck.stellar.common.components

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlin.random.Random

// Deep Space Colors
val SpaceBlack = Color(0xFF0B0D17)
val SpaceBlue = Color(0xFF1B2735)
val NebulaPurple = Color(0xFF7C4DFF).copy(alpha = 0.1f)
val NebulaCyan = Color(0xFF00E5FF).copy(alpha = 0.1f)

@Composable
fun StellarScaffold(
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
            // Animated Space Background
            SpaceBackground()
            
            // Content
            content(Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun SpaceBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(SpaceBlue, SpaceBlack),
                    radius = 2000f
                )
            )
    ) {
        // Nebula effects
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = NebulaPurple,
                radius = size.minDimension * 0.6f,
                center = Offset(size.width * 0.2f, size.height * 0.2f)
            )
            drawCircle(
                color = NebulaCyan,
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * 0.8f, size.height * 0.8f)
            )
        }

        // Stars
        StarsLayer(
            count = 100, 
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.value * 0.5f }
        )
        StarsLayer(
            count = 50,
            scale = 2f,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -rotation.value * 0.2f }
        )
    }
}

@Composable
private fun StarsLayer(
    count: Int,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    // Generate static star positions
    val stars = remember {
        List(count) {
            Offset(
                Random.nextFloat(),
                Random.nextFloat()
            ) to Random.nextFloat() * 2f * scale // size
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // We artificially extend drawing bounds to avoid clipping during rotation
        // but simple rotation in graphicsLayer handles it mostly. 
        // Better: draw in a larger area or just ignore edge clipping for background.
        
        stars.forEach { (relPos, size) ->
            drawCircle(
                color = Color.White.copy(alpha = Random.nextFloat() * 0.5f + 0.3f),
                radius = size,
                center = Offset(relPos.x * width, relPos.y * height)
            )
        }
    }
}
