package com.cosmic_struck.stellar.home.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// Modern Clean/Tech Colors
val HomeBgStart = Color(0xFFF8F9FE)
val HomeBgEnd = Color(0xFFE8EAF6)
val SoftBlue = Color(0xFFE3F2FD)
val SoftPurple = Color(0xFFF3E5F5)

@Composable
fun HomeBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "home_bg_anim")
    
    // Floating Animation
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FE)) // Very Clean White/Blue
    ) {
        // Grid pattern (Graph paper style)
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.05f }) {
            val step = 40.dp.toPx()
            val strokeWidth = 1.dp.toPx()
            
            // Vertical lines
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(
                    color = Color.Black,
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), size.height),
                    strokeWidth = strokeWidth
                )
            }
            
            // Horizontal lines
            for (y in 0..size.height.toInt() step step.toInt()) {
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = strokeWidth
                )
            }
        }

        // Floating Geometric Shapes (Abstract Education Symbols)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Triangle (Math/Geometry)
            drawCircle(
                color = SoftBlue.copy(alpha = 0.5f),
                radius = 60f,
                center = Offset(width * 0.2f, height * 0.2f + floatAnim)
            )
            
            // Square (Logic)
            drawRect(
                color = SoftPurple.copy(alpha = 0.4f),
                topLeft = Offset(width * 0.8f, height * 0.15f - floatAnim),
                size = androidx.compose.ui.geometry.Size(100f, 100f)
            )
            
            // Circle (Physics/Atom)
             drawCircle(
                color = Color(0xFFFFCC80).copy(alpha = 0.3f), // Orange (Creativity)
                radius = 80f,
                center = Offset(width * 0.5f, height * 0.5f + floatAnim * 0.5f)
            )
            
             // Bottom decorations
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SoftBlue, Color.Transparent),
                    center = Offset(width, height),
                    radius = 400f
                ),
                center = Offset(width, height),
                radius = 400f
            )
        }
    }
}
