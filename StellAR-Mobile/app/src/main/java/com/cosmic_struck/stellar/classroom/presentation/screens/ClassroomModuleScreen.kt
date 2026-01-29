package com.cosmic_struck.stellar.classroom.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.classroom.presentation.components.ModuleScene
import com.cosmic_struck.stellar.classroom.presentation.viewmodel.ClassroomViewModel
import com.cosmic_struck.stellar.common.util.Rajdhani
import java.io.File

// Dark Space Theme for 3D Model Viewer
private val DarkBackground = Color(0xFF0D0D1A)
private val DarkSurface = Color(0xFF1a1a3e)
private val AccentPurple = Color(0xFF7C4DFF)
private val AccentTeal = Color(0xFF26A69A)
private val AccentOrange = Color(0xFFFF7043)

@Composable
fun ClassroomModuleScreen(
    navigateToSummaryScreen: () -> Unit,
    navigateToQuizScreen: () -> Unit,
    navigateToChatScreen: () -> Unit,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.moduleState.collectAsStateWithLifecycle()
    var isModelFullScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Load module once when screen appears
    // The delegate handles caching - won't reload if already loaded
    LaunchedEffect(Unit) {
        viewModel.loadModule()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurface, DarkBackground)
                )
            )
    ) {
        when {
            state.isLoading -> LoadingView()
            state.moduleError.isNotEmpty() -> ErrorView(
                error = state.moduleError,
                onRetry = { viewModel.loadModule() }
            )
            else -> {
                // Main content
                AnimatedVisibility(
                    visible = !isModelFullScreen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 3D Model Viewer (Top half)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.5f)
                        ) {
                            key(state.model_path) {
                                ModuleScene(
                                    fullScreen = false,
                                    onChangeScreen = { isModelFullScreen = true },
                                    modelPath = state.model_path,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Module Info & Actions (Bottom half)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.5f)
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Module Info Card
                            ModuleInfoCard(
                                title = state.module?.moduleName ?: "Module",
                                description = state.module?.moduleDesc ?: "No description available"
                            )

                            // Action Buttons
                            ActionButtonsGrid(
                                onSummaryClick = navigateToSummaryScreen,
                                onNotesClick = {
                                    openPdfFile(
                                        context = context,
                                        pdfPath = state.pdf_path
                                    )
                                },
                                onQuizClick = navigateToQuizScreen,
                                onChatClick = navigateToChatScreen
                            )
                        }
                    }
                }

                // Full screen model view
                AnimatedVisibility(
                    visible = isModelFullScreen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        key(state.model_path) {
                            ModuleScene(
                                fullScreen = true,
                                onChangeScreen = { isModelFullScreen = false },
                                modelPath = state.model_path,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                color = AccentPurple,
                strokeWidth = 4.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Loading Module...",
            fontFamily = Rajdhani,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Preparing 3D model",
            fontFamily = Rajdhani,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF5350).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "❌", fontSize = 40.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Something went wrong",
            fontFamily = Rajdhani,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = error,
            fontFamily = Rajdhani,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Try Again",
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModuleInfoCard(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface.copy(alpha = 0.5f))
            .padding(20.dp)
    ) {
        Text(
            text = title,
            fontFamily = Rajdhani,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            fontFamily = Rajdhani,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ActionButtonsGrid(
    onSummaryClick: () -> Unit,
    onNotesClick: () -> Unit,
    onQuizClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: Summary & Notes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButton(
                modifier = Modifier.weight(1f),
                emoji = "📝",
                label = "Summary",
                color = AccentPurple,
                onClick = onSummaryClick
            )

            ActionButton(
                modifier = Modifier.weight(1f),
                emoji = "📄",
                label = "Notes",
                color = AccentTeal,
                onClick = onNotesClick
            )
        }

        // Middle row: Quiz & Chat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButton(
                modifier = Modifier.weight(1f),
                emoji = "🎯",
                label = "Start Quiz",
                color = AccentOrange,
                onClick = onQuizClick
            )

            ActionButton(
                modifier = Modifier.weight(1f),
                emoji = "💬",
                label = "Chat",
                color = Color(0xFF42A5F5),
                onClick = onChatClick
            )
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontFamily = Rajdhani,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
        }
    }
}

private fun openPdfFile(context: android.content.Context, pdfPath: String) {
    try {
        val file = File(pdfPath)
        if (!file.exists()) {
            Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
            Log.e("PDF", "File not found: $pdfPath")
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
        Log.e("PDF", "Error opening PDF: ${e.message}")
    } catch (e: Exception) {
        Toast.makeText(context, "Error opening PDF", Toast.LENGTH_SHORT).show()
        Log.e("PDF", "Error: ${e.message}")
    }
}
