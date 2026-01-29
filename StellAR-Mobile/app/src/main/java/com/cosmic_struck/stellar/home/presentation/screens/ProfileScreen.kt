package com.cosmic_struck.stellar.home.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.home.presentation.viewmodel.HomeScreenViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

// Educational Theme Colors
private val EduPrimary = Color(0xFF5C6BC0)
private val EduSecondary = Color(0xFF7E57C2)
private val EduBackground = Color(0xFFF8F9FE)
private val EduSurface = Color(0xFFFFFFFF)
private val EduTextPrimary = Color(0xFF1A1A2E)
private val EduTextSecondary = Color(0xFF6B7280)
private val EduSuccess = Color(0xFF26A69A)
private val EduWarning = Color(0xFFFF7043)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsState().value
    val context = LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateProfilePicture(it, context) }
    }

    Scaffold(
        containerColor = EduBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = EduTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EduSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.back),
                                contentDescription = "Back",
                                tint = EduTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FE),
                            Color(0xFFE8EAF6),
                            Color(0xFFF8F9FE)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Profile Picture
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EduPrimary, EduSecondary)
                                )
                            )
                            .padding(4.dp)
                    ) {
                        CoilImage(
                            imageModel = {
                                state.profile.ifEmpty {
                                    "https://ui-avatars.com/api/?name=${state.userName}&background=5C6BC0&color=fff&size=200"
                                }
                            },
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        
                        // Edit overlay hint
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = "EDIT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(124.dp),
                            color = EduPrimary,
                            strokeWidth = 4.dp
                        )
                    }
                    
                    // Camera icon badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EduPrimary)
                            .border(2.dp, Color.White, CircleShape)
                            .padding(6.dp)
                    ) {
                        // Use a generic icon or text if drawable not available, assuming 'add' exists or similar
                        // Using text/emoji for safety if specific camera icon resource unsure
                        Text("📷", fontSize = 12.sp, modifier = Modifier.align(Alignment.Center)) 
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Name
                Text(
                    text = state.userName,
                    fontFamily = Rajdhani,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = EduTextPrimary
                )

                // Level Badge
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EduPrimary, EduSecondary)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⭐ Level ${state.userLevel} Explorer",
                        fontFamily = Rajdhani,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🏫",
                        value = "${state.joinedClassrooms.size}",
                        label = "Classrooms"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📚",
                        value = "0",
                        label = "Modules"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🎯",
                        value = "0",
                        label = "Quizzes"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EduSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Level Progress",
                                fontFamily = Rajdhani,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduTextPrimary
                            )
                            Text(
                                text = "250/500 XP",
                                fontFamily = Rajdhani,
                                fontSize = 14.sp,
                                color = EduTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { 0.5f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EduSuccess,
                            trackColor = Color(0xFFE0E0E0)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "250 XP to next level",
                            fontFamily = Rajdhani,
                            fontSize = 12.sp,
                            color = EduTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Settings Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EduSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        SettingsItem(
                            emoji = "🔔",
                            title = "Notifications",
                            subtitle = "Manage push notifications"
                        )
                        SettingsItem(
                            emoji = "🎨",
                            title = "Appearance",
                            subtitle = "Theme and display settings"
                        )
                        SettingsItem(
                            emoji = "🔒",
                            title = "Privacy",
                            subtitle = "Account privacy settings"
                        )
                        SettingsItem(
                            emoji = "❓",
                            title = "Help & Support",
                            subtitle = "FAQ and contact us"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Logout Button
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EduWarning
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🚪", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Out",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Version
                Text(
                    text = "StellAR v1.0.0",
                    fontFamily = Rajdhani,
                    fontSize = 12.sp,
                    color = EduTextSecondary
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EduSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontFamily = Rajdhani,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EduTextPrimary
            )
            Text(
                text = label,
                fontFamily = Rajdhani,
                fontSize = 12.sp,
                color = EduTextSecondary
            )
        }
    }
}

@Composable
private fun SettingsItem(
    emoji: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EduPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Rajdhani,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EduTextPrimary
            )
            Text(
                text = subtitle,
                fontFamily = Rajdhani,
                fontSize = 12.sp,
                color = EduTextSecondary
            )
        }

        Text(
            text = "→",
            fontSize = 18.sp,
            color = EduTextSecondary
        )
    }
}
