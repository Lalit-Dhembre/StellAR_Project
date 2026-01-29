package com.cosmic_struck.stellar.auth.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.cosmic_struck.stellar.auth.presentation.viewmodel.AuthViewModel
import com.cosmic_struck.stellar.common.util.Rajdhani

// Educational Theme Colors
private val EduPrimary = Color(0xFF5C6BC0) // Indigo - Professional education
private val EduSecondary = Color(0xFF7E57C2) // Purple accent
private val EduBackground = Color(0xFFF8F9FE) // Light background
private val EduSurface = Color(0xFFFFFFFF) // White surfaces
private val EduAccent = Color(0xFF26A69A) // Teal for success/CTAs
private val EduTextPrimary = Color(0xFF1A1A2E) // Dark text
private val EduTextSecondary = Color(0xFF6B7280) // Gray text

@Composable
fun AuthScreen(
    navigateToLoginScreen: () -> Unit,
    navigateToSignUpScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    viewmodel: AuthViewModel = hiltViewModel<AuthViewModel>(),
    modifier: Modifier = Modifier
) {
    val state = viewmodel.state.value

    LaunchedEffect(state.success) {
        if (state.success) {
            navigateToHomeScreen()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FE),
                        Color(0xFFE8EAF6),
                        Color(0xFFEDE7F6)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            // Logo and branding
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(EduPrimary, EduSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📚",
                    fontSize = 56.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "StellAR",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Rajdhani,
                color = EduTextPrimary
            )

            Text(
                text = "Learn Beyond Boundaries",
                fontSize = 16.sp,
                color = EduTextSecondary,
                fontFamily = Rajdhani
            )

            Spacer(modifier = Modifier.weight(0.2f))

            // Features showcase
            Column(
                modifier = Modifier.padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureRow(
                    emoji = "🎓",
                    title = "Interactive Learning",
                    subtitle = "Join virtual classrooms"
                )
                FeatureRow(
                    emoji = "🔬",
                    title = "AR Experiences",
                    subtitle = "View 3D models in augmented reality"
                )
                FeatureRow(
                    emoji = "📝",
                    title = "Smart Quizzes",
                    subtitle = "AI-generated assessments"
                )
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = navigateToSignUpScreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EduPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Get Started",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                OutlinedButton(
                    onClick = navigateToLoginScreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EduPrimary
                    )
                ) {
                    Text(
                        text = "I already have an account",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    emoji: String,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF5C6BC0).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = EduTextPrimary,
                fontFamily = Rajdhani
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = EduTextSecondary,
                fontFamily = Rajdhani
            )
        }
    }
}