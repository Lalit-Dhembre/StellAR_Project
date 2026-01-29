package com.cosmic_struck.stellar.create_module.presentation.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.create_module.presentation.UploadStatus
import com.cosmic_struck.stellar.create_module.presentation.viewmodel.CreateModuleViewModel

@Composable
fun UploadStatusTracker(
    viewModel: CreateModuleViewModel,
    onComplete: () -> Unit = {}
) {
    val state = viewModel.state.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D1A),
                        Color(0xFF1a1a3e),
                        Color(0xFF0D0D1A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1a1a3e),
                            Color(0xFF2a2a4e)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF3a3a5e),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(32.dp)
        ) {
            Crossfade(
                targetState = state.uploadSuccess,
                animationSpec = tween(500),
                label = "UploadStatusTransition"
            ) { status ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (status) {
                        UploadStatus.IDLE -> IdleView()
                        UploadStatus.LOADING -> LoadingView()
                        UploadStatus.SUCCESS -> SuccessView(onComplete = onComplete)
                        UploadStatus.ERROR -> ErrorView(
                            message = state.error,
                            onRetry = { viewModel.createModule() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📤", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ready to Upload",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun LoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                color = Color(0xFF7C4DFF),
                strokeWidth = 4.dp
            )
            Text(text = "🚀", fontSize = 32.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Uploading Module...",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            fontFamily = Rajdhani
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please wait while we upload your content",
            color = Color(0xFFb0b0d0),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessView(onComplete: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✅", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Upload Complete!",
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            fontFamily = Rajdhani
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your module has been created successfully",
            color = Color(0xFFb0b0d0),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Done 🎉",
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String?,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF5350).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "❌", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Upload Failed",
            color = Color(0xFFEF5350),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            fontFamily = Rajdhani
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message ?: "An unexpected error occurred",
            color = Color(0xFFb0b0d0),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C4DFF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Try Again 🔄",
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}