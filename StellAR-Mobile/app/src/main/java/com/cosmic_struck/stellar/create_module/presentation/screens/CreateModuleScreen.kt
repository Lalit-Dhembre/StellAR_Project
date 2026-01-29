package com.cosmic_struck.stellar.create_module.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.common.util.Rajdhani
import com.cosmic_struck.stellar.create_module.presentation.components.ImagePicker
import com.cosmic_struck.stellar.create_module.presentation.viewmodel.CreateModuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateModuleScreen(
    navigateToModelScreen: () -> Unit,
    viewmodel: CreateModuleViewModel = hiltViewModel<CreateModuleViewModel>(),
    modifier: Modifier = Modifier
) {
    val state = viewmodel.state.collectAsState().value

    val pdfUriLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { it ->
            viewmodel.uploadPdf(it)
        }
    )

    val isFormValid = state.pdfPath != null && state.moduleName.trim().isNotEmpty()
    val buttonAlpha by animateFloatAsState(
        targetValue = if (isFormValid) 1f else 0.5f,
        animationSpec = tween(300),
        label = "button_alpha"
    )

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Module",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
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
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Image Picker with glow effect
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF7C4DFF).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ImagePicker(
                        onChangeImage = viewmodel::uploadImage
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Module Name Field
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Module Name",
                        color = Color(0xFFb0b0d0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.moduleName,
                        onValueChange = { viewmodel.changeModuleName(it) },
                        placeholder = {
                            Text(
                                text = "Enter module name",
                                fontFamily = Rajdhani,
                                color = Color(0xFF6a6a8e)
                            )
                        },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = Rajdhani,
                            fontSize = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C4DFF),
                            unfocusedBorderColor = Color(0xFF3a3a5e),
                            cursorColor = Color(0xFF7C4DFF),
                            focusedContainerColor = Color(0xFF1a1a3e),
                            unfocusedContainerColor = Color(0xFF1a1a3e)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Description Field
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Description",
                        color = Color(0xFFb0b0d0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        value = state.description,
                        onValueChange = { viewmodel.changeDescription(it) },
                        placeholder = {
                            Text(
                                text = "Add description for this module...",
                                fontFamily = Rajdhani,
                                color = Color(0xFF6a6a8e)
                            )
                        },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = Rajdhani,
                            fontSize = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C4DFF),
                            unfocusedBorderColor = Color(0xFF3a3a5e),
                            cursorColor = Color(0xFF7C4DFF),
                            focusedContainerColor = Color(0xFF1a1a3e),
                            unfocusedContainerColor = Color(0xFF1a1a3e)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // PDF Selection Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
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
                            color = if (state.pdfPath != null) Color(0xFF4CAF50) else Color(0xFF3a3a5e),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // PDF Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.pdfPath != null)
                                        Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    else
                                        Color(0xFF7C4DFF).copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.pdfPath != null) "✓" else "📄",
                                fontSize = 28.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (state.pdfPath != null)
                                "PDF Selected"
                            else
                                "Add PDF for Quiz & Summary",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )

                        if (state.pdfPath != null) {
                            Text(
                                text = state.pdfPath?.lastPathSegment ?: "",
                                color = Color(0xFF4CAF50),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { pdfUriLauncher.launch("application/pdf") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.pdfPath != null)
                                    Color(0xFF3a3a5e)
                                else
                                    Color(0xFF7C4DFF)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (state.pdfPath != null) "Change PDF" else "Select PDF",
                                fontFamily = Rajdhani,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Continue Button - Fixed at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0D0D1A)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = { navigateToModelScreen() },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C4DFF),
                        disabledContainerColor = Color(0xFF7C4DFF).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue to 3D Model",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "→",
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}